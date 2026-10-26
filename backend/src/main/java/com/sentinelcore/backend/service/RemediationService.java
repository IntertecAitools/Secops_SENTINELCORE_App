package com.sentinelcore.backend.service;

import com.sentinelcore.backend.cloud.*;
import com.sentinelcore.backend.cloud.CloudModels.*;
import com.sentinelcore.backend.dto.cloud.*;
import com.sentinelcore.backend.entity.CloudAccount;
import com.sentinelcore.backend.entity.CloudFinding;
import com.sentinelcore.backend.entity.Permission;
import com.sentinelcore.backend.entity.RemediationRequest;
import com.sentinelcore.backend.entity.RemediationRequest.Status;
import com.sentinelcore.backend.exception.BadRequestException;
import com.sentinelcore.backend.exception.ConflictException;
import com.sentinelcore.backend.exception.ResourceNotFoundException;
import com.sentinelcore.backend.repository.CloudFindingRepository;
import com.sentinelcore.backend.repository.RemediationRequestRepository;
import com.sentinelcore.backend.security.service.CurrentUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Controlled remediation workflow. Provider mutations happen only through
 * {@link #execute(Long, RemediationExecuteRequest)} after every check below has passed:
 * tenant scoping, allowlisted action, open finding, approval by a different user bound to the
 * exact request hash, typed confirmation of the target, the global execution switch, and an
 * optimistic-lock guarded transition so the action runs at most once per attempt.
 */
@Service
@Slf4j
public class RemediationService {

    static final Set<Status> ACTIVE = EnumSet.of(Status.PROPOSED, Status.VALIDATED, Status.PENDING_APPROVAL,
            Status.APPROVED, Status.EXECUTING, Status.OUTCOME_UNKNOWN);
    private static final Set<Status> CANCELLABLE = EnumSet.of(Status.PROPOSED, Status.VALIDATED,
            Status.PENDING_APPROVAL, Status.APPROVED);
    private static final Set<Status> EXECUTABLE = EnumSet.of(Status.APPROVED, Status.FAILED);
    private static final Set<Status> VERIFIABLE = EnumSet.of(Status.SUCCEEDED, Status.OUTCOME_UNKNOWN,
            Status.VERIFICATION_FAILED);
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("^[A-Za-z0-9_-]{8,128}$");
    private static final Pattern S3_BUCKET_ARN = Pattern.compile("^arn:aws:s3:::[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$");

    private final RemediationRequestRepository remediationRepository;
    private final CloudFindingRepository findingRepository;
    private final CloudConnectorRegistry connectorRegistry;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final TransactionTemplate tx;
    private final boolean executionEnabled;

    public RemediationService(RemediationRequestRepository remediationRepository,
                              CloudFindingRepository findingRepository,
                              CloudConnectorRegistry connectorRegistry,
                              CurrentUserService currentUserService,
                              AuditLogService auditLogService,
                              PlatformTransactionManager transactionManager,
                              @Value("${app.cloud.remediation.execution-enabled:false}") boolean executionEnabled) {
        this.remediationRepository = remediationRepository;
        this.findingRepository = findingRepository;
        this.connectorRegistry = connectorRegistry;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.tx = new TransactionTemplate(transactionManager);
        this.executionEnabled = executionEnabled;
    }

    // ------------------------------------------------------------------ create

    public RemediationDTO create(RemediationCreateRequest request, String idempotencyKey) {
        if (idempotencyKey == null || !IDEMPOTENCY_KEY.matcher(idempotencyKey).matches()) {
            throw new BadRequestException("Idempotency-Key header is required (8-128 characters: letters, digits, '-' or '_')");
        }
        String tenant = currentUserService.currentTenant();
        String username = currentUserService.currentUsername();

        var replay = tx.execute(status -> remediationRepository.findByTenantKeyAndIdempotencyKey(tenant, idempotencyKey)
                .map(existing -> {
                    if (!existing.getFinding().getId().equals(request.getFindingId())
                            || existing.getActionType() != request.getActionType()
                            || !existing.getRequestedBy().equals(username)) {
                        throw new ConflictException("Idempotency-Key was already used for a different request");
                    }
                    return toDTO(existing);
                }));
        if (replay.isPresent()) {
            return replay.get();
        }

        try {
            RemediationDTO created = tx.execute(status -> {
                CloudFinding finding = findingRepository.findByIdAndTenantKey(request.getFindingId(), tenant)
                        .orElseThrow(() -> new ResourceNotFoundException("Finding not found with id: " + request.getFindingId()));
                CloudAccount account = finding.getCloudAccount();
                RemediationActionType action = request.getActionType();

                RemediationRequest remediation = RemediationRequest.builder()
                        .tenantKey(tenant)
                        .cloudAccount(account)
                        .finding(finding)
                        .actionType(action)
                        .resourceType(finding.getResourceType())
                        .resourceId(finding.getResourceId())
                        .region(finding.getRegion())
                        .parametersJson("{}")
                        .idempotencyKey(idempotencyKey)
                        .status(Status.PROPOSED)
                        .requestedBy(username)
                        .requestedAt(LocalDateTime.now())
                        .justification(request.getJustification())
                        .build();

                validateProposal(remediation, finding, account);
                remediation.setStatus(Status.VALIDATED);
                remediation.setValidatedAt(LocalDateTime.now());
                remediation.setValidationNotes("Action is allowlisted for " + finding.getFindingType()
                        + "; finding is open; target taken from the finding; no other active remediation.");
                // All allowlisted actions currently require approval
                remediation.setStatus(Status.PENDING_APPROVAL);
                remediation.setRequestHash(computeHash(remediation));

                RemediationRequest saved = remediationRepository.saveAndFlush(remediation);
                return toDTO(saved);
            });
            auditLogService.logSecurityEvent("REMEDIATION_REQUESTED",
                    "Remediation " + created.getId() + " " + created.getActionType() + " on " + created.getResourceId()
                            + " requested; hash " + created.getRequestHash().substring(0, 12));
            return created;
        } catch (DataIntegrityViolationException e) {
            // Concurrent request with the same idempotency key won the race
            return tx.execute(status -> remediationRepository.findByTenantKeyAndIdempotencyKey(tenant, idempotencyKey)
                    .map(this::toDTO)
                    .orElseThrow(() -> new ConflictException("Remediation could not be created")));
        }
    }

    private void validateProposal(RemediationRequest remediation, CloudFinding finding, CloudAccount account) {
        RemediationActionType action = remediation.getActionType();
        if (action.getProvider() != account.getProvider()) {
            throw new BadRequestException(action + " is not available for " + account.getProvider() + " accounts");
        }
        if (!connectorRegistry.isSupported(account.getProvider())) {
            throw new UnsupportedCloudOperationException("Cloud provider " + account.getProvider() + " is not supported yet");
        }
        if (!action.appliesTo(finding.getFindingType()) || finding.getSupportedRemediation() != action) {
            throw new BadRequestException(action + " is not an allowed remediation for finding type " + finding.getFindingType());
        }
        if (!action.getResourceType().equals(finding.getResourceType())) {
            throw new BadRequestException("Finding resource type does not match the action");
        }
        if (finding.getStatus() != CloudFinding.Status.OPEN) {
            throw new ConflictException("Finding is not open");
        }
        if (action == RemediationActionType.S3_BLOCK_PUBLIC_ACCESS
                && !S3_BUCKET_ARN.matcher(finding.getResourceId()).matches()) {
            throw new BadRequestException("Finding does not reference a valid S3 bucket");
        }
        if (remediationRepository.existsByFindingIdAndStatusIn(finding.getId(), ACTIVE)) {
            throw new ConflictException("Another remediation for this finding is already in progress");
        }
    }

    // ------------------------------------------------------------------ preview

    public RemediationPreviewDTO preview(Long id) {
        RemediationRequest remediation = tx.execute(status -> {
            RemediationRequest r = require(id);
            r.getCloudAccount().getExternalAccountId(); // initialize for use outside the transaction
            return r;
        });
        CloudAccount account = remediation.getCloudAccount();
        RemediationTarget target = target(remediation);
        try {
            RemediationPreview preview = connectorRegistry.get(account.getProvider())
                    .previewRemediation(CloudAccountService.toContext(account), remediation.getActionType(), target);
            return RemediationPreviewDTO.builder()
                    .remediationId(remediation.getId())
                    .requestHash(remediation.getRequestHash())
                    .previewType(preview.previewType())
                    .currentStateAvailable(true)
                    .currentState(preview.currentState())
                    .plannedChange(preview.plannedChange())
                    .notes(preview.notes())
                    .build();
        } catch (CloudProviderException e) {
            if (e.getErrorType() != CloudErrorType.NOT_CONFIGURED) {
                throw e;
            }
            return RemediationPreviewDTO.builder()
                    .remediationId(remediation.getId())
                    .requestHash(remediation.getRequestHash())
                    .previewType(PreviewType.LOCAL_VALIDATION)
                    .currentStateAvailable(false)
                    .currentState("Unavailable: " + e.getMessage())
                    .plannedChange(describePlannedChange(remediation))
                    .notes("Local validation only. The current resource state could not be read, and the provider "
                            + "did not simulate this change.")
                    .build();
        }
    }

    // ------------------------------------------------------------------ approve / reject / cancel

    public RemediationDTO approve(Long id, RemediationDecisionRequest decision) {
        String username = currentUserService.currentUsername();
        RemediationDTO result = tx.execute(status -> {
            RemediationRequest r = require(id);
            checkDecision(r, decision, username, "approve");
            CloudFinding finding = r.getFinding();
            if (finding.getStatus() != CloudFinding.Status.OPEN) {
                throw new ConflictException("Finding is no longer open; reject or cancel this request");
            }
            r.setStatus(Status.APPROVED);
            r.setDecidedBy(username);
            r.setDecidedAt(LocalDateTime.now());
            r.setDecisionComment(decision.getComment());
            r.setApprovalHash(r.getRequestHash());
            return toDTO(remediationRepository.saveAndFlush(r));
        });
        auditLogService.logSecurityEvent("REMEDIATION_APPROVED",
                "Remediation " + id + " approved for hash " + result.getRequestHash().substring(0, 12));
        return result;
    }

    public RemediationDTO reject(Long id, RemediationDecisionRequest decision) {
        String username = currentUserService.currentUsername();
        RemediationDTO result = tx.execute(status -> {
            RemediationRequest r = require(id);
            checkDecision(r, decision, username, "reject");
            r.setStatus(Status.REJECTED);
            r.setDecidedBy(username);
            r.setDecidedAt(LocalDateTime.now());
            r.setDecisionComment(decision.getComment());
            return toDTO(remediationRepository.saveAndFlush(r));
        });
        auditLogService.logSecurityEvent("REMEDIATION_REJECTED", "Remediation " + id + " rejected");
        return result;
    }

    private void checkDecision(RemediationRequest r, RemediationDecisionRequest decision, String username, String verb) {
        if (r.getStatus() != Status.PENDING_APPROVAL) {
            throw new ConflictException("Only requests pending approval can be " + verb + "d (current status " + r.getStatus() + ")");
        }
        if (r.getActionType().isHighImpact() && r.getRequestedBy().equals(username)) {
            auditLogService.logSecurityEvent("REMEDIATION_SOD_VIOLATION",
                    username + " attempted to " + verb + " their own remediation " + r.getId());
            throw new AccessDeniedException("Separation of duties: the requester cannot " + verb + " their own request");
        }
        if (!r.getRequestHash().equals(decision.getRequestHash())) {
            throw new ConflictException("The request differs from the version you reviewed; reload it before deciding");
        }
        requireIntegrity(r, r.getRequestHash());
    }

    public RemediationDTO cancel(Long id) {
        String username = currentUserService.currentUsername();
        RemediationDTO result = tx.execute(status -> {
            RemediationRequest r = require(id);
            if (!CANCELLABLE.contains(r.getStatus())) {
                throw new ConflictException("Request cannot be cancelled in status " + r.getStatus());
            }
            if (!r.getRequestedBy().equals(username) && !hasAuthority(Permission.REMEDIATION_APPROVE)) {
                throw new AccessDeniedException("Only the requester or an approver can cancel this request");
            }
            r.setStatus(Status.CANCELLED);
            r.setCancelledBy(username);
            r.setCancelledAt(LocalDateTime.now());
            return toDTO(remediationRepository.saveAndFlush(r));
        });
        auditLogService.logSecurityEvent("REMEDIATION_CANCELLED", "Remediation " + id + " cancelled");
        return result;
    }

    // ------------------------------------------------------------------ execute

    public RemediationDTO execute(Long id, RemediationExecuteRequest request) {
        if (!executionEnabled) {
            auditLogService.logSecurityEvent("REMEDIATION_EXECUTION_BLOCKED",
                    "Execution of remediation " + id + " blocked: execution disabled by configuration");
            throw new CloudProviderException(CloudErrorType.NOT_CONFIGURED,
                    "Remediation execution is disabled by configuration (APP_CLOUD_REMEDIATION_EXECUTION_ENABLED=false)");
        }
        String username = currentUserService.currentUsername();

        // Phase 1: claim the request. The optimistic lock on `version` guarantees a single winner.
        RemediationRequest claimed = tx.execute(status -> {
            RemediationRequest r = require(id);
            if (!EXECUTABLE.contains(r.getStatus())) {
                throw new ConflictException("Request cannot be executed in status " + r.getStatus());
            }
            if (r.getApprovalHash() == null || r.getDecidedBy() == null) {
                throw new ConflictException("Request has not been approved");
            }
            requireIntegrity(r, r.getApprovalHash());
            if (!confirmationMatches(r, request.getConfirmResourceId())) {
                throw new BadRequestException("Confirmation does not match the target resource");
            }
            if (r.getFinding().getStatus() != CloudFinding.Status.OPEN) {
                throw new ConflictException("Finding is no longer open; run a scan before executing");
            }
            r.setStatus(Status.EXECUTING);
            r.setExecutedBy(username);
            r.setExecutionStartedAt(LocalDateTime.now());
            r.setExecutionCompletedAt(null);
            r.setExecutionAttempts(r.getExecutionAttempts() + 1);
            RemediationRequest saved = remediationRepository.saveAndFlush(r);
            saved.getCloudAccount().getExternalAccountId(); // initialize for use outside the transaction
            return saved;
        });
        auditLogService.logSecurityEvent("REMEDIATION_EXECUTION_STARTED",
                "Remediation " + id + " " + claimed.getActionType() + " on " + claimed.getResourceId()
                        + " attempt " + claimed.getExecutionAttempts());

        // Phase 2: provider call, outside any transaction.
        CloudAccount account = claimed.getCloudAccount();
        Status outcome;
        String message;
        String providerRequestId = null;
        try {
            ExecutionResult result = connectorRegistry.get(account.getProvider())
                    .executeApprovedRemediation(CloudAccountService.toContext(account), claimed.getActionType(), target(claimed));
            outcome = result.success() ? Status.SUCCEEDED : Status.FAILED;
            message = result.message();
            providerRequestId = result.providerRequestId();
        } catch (CloudProviderException e) {
            outcome = e.getErrorType() == CloudErrorType.OUTCOME_UNKNOWN ? Status.OUTCOME_UNKNOWN : Status.FAILED;
            message = e.getErrorType() + ": " + e.getMessage();
        } catch (RuntimeException e) {
            log.error("Unexpected error executing remediation {}", id, e);
            outcome = Status.OUTCOME_UNKNOWN;
            message = "Unexpected error during execution; the outcome must be verified";
        }

        // Phase 3: record the outcome.
        Status finalOutcome = outcome;
        String finalMessage = message;
        String finalRequestId = providerRequestId;
        tx.executeWithoutResult(status -> {
            RemediationRequest r = remediationRepository.findById(id).orElseThrow();
            r.setStatus(finalOutcome);
            r.setExecutionMessage(truncate(finalMessage, 2048));
            r.setProviderRequestId(finalRequestId);
            r.setExecutionCompletedAt(LocalDateTime.now());
            remediationRepository.saveAndFlush(r);
        });
        auditLogService.logSecurityEvent("REMEDIATION_EXECUTION_" + finalOutcome,
                "Remediation " + id + " execution result " + finalOutcome
                        + (finalRequestId != null ? " (provider request id " + finalRequestId + ")" : ""));

        // Phase 4: verify the resource state. A finding is never resolved on the strength of the API response alone.
        if (finalOutcome == Status.SUCCEEDED || finalOutcome == Status.OUTCOME_UNKNOWN) {
            return verifyInternal(id, false);
        }
        return getRemediation(id);
    }

    // ------------------------------------------------------------------ verify

    public RemediationDTO verify(Long id) {
        return verifyInternal(id, true);
    }

    private RemediationDTO verifyInternal(Long id, boolean propagateErrors) {
        RemediationRequest remediation = tx.execute(status -> {
            RemediationRequest r = require(id);
            if (!VERIFIABLE.contains(r.getStatus())) {
                throw new ConflictException("Request cannot be verified in status " + r.getStatus());
            }
            r.getCloudAccount().getExternalAccountId();
            return r;
        });
        CloudAccount account = remediation.getCloudAccount();
        VerificationResult verification;
        try {
            verification = connectorRegistry.get(account.getProvider())
                    .verifyRemediation(CloudAccountService.toContext(account), remediation.getActionType(), target(remediation));
        } catch (CloudProviderException e) {
            tx.executeWithoutResult(status -> {
                RemediationRequest r = remediationRepository.findById(id).orElseThrow();
                r.setVerificationResult(truncate("Verification could not be completed: " + e.getErrorType()
                        + ": " + e.getMessage(), 2048));
                remediationRepository.saveAndFlush(r);
            });
            if (propagateErrors) {
                throw e;
            }
            return getRemediation(id);
        }

        RemediationDTO result = tx.execute(status -> {
            RemediationRequest r = remediationRepository.findById(id).orElseThrow();
            r.setVerifiedAt(LocalDateTime.now());
            r.setVerificationResult(truncate(verification.observedState(), 2048));
            if (verification.verified()) {
                r.setStatus(Status.VERIFIED);
                CloudFinding finding = r.getFinding();
                if (finding.getStatus() == CloudFinding.Status.OPEN) {
                    finding.setStatus(CloudFinding.Status.RESOLVED);
                    finding.setResolvedAt(LocalDateTime.now());
                    finding.setResolutionReason("Verified by post-remediation check for remediation #" + r.getId());
                    findingRepository.save(finding);
                }
            } else {
                r.setStatus(Status.VERIFICATION_FAILED);
            }
            return toDTO(remediationRepository.saveAndFlush(r));
        });
        auditLogService.logSecurityEvent(verification.verified() ? "REMEDIATION_VERIFIED" : "REMEDIATION_VERIFICATION_FAILED",
                "Remediation " + id + ": " + truncate(verification.observedState(), 500));
        return result;
    }

    // ------------------------------------------------------------------ queries

    public Page<RemediationDTO> list(String status, Pageable pageable) {
        String tenant = currentUserService.currentTenant();
        return tx.execute(s -> {
            if (StringUtils.hasText(status)) {
                Status parsed;
                try {
                    parsed = Status.valueOf(status.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Invalid status: " + status);
                }
                return remediationRepository.findByTenantKeyAndStatusOrderByRequestedAtDesc(tenant, parsed, pageable)
                        .map(this::toDTO);
            }
            return remediationRepository.findByTenantKeyOrderByRequestedAtDesc(tenant, pageable).map(this::toDTO);
        });
    }

    public RemediationDTO getRemediation(Long id) {
        return tx.execute(status -> toDTO(require(id)));
    }

    // ------------------------------------------------------------------ helpers

    /** Tenant-scoped lookup; requests of other tenants are reported as not found. */
    private RemediationRequest require(Long id) {
        return remediationRepository.findByIdAndTenantKey(id, currentUserService.currentTenant())
                .orElseThrow(() -> new ResourceNotFoundException("Remediation not found with id: " + id));
    }

    /** Recomputes the hash from stored fields; detects any modification after creation or approval. */
    private void requireIntegrity(RemediationRequest r, String expectedHash) {
        String actual = computeHash(r);
        if (!actual.equals(r.getRequestHash()) || !actual.equals(expectedHash)) {
            auditLogService.logSecurityEvent("REMEDIATION_INTEGRITY_FAILURE",
                    "Remediation " + r.getId() + " no longer matches its recorded hash");
            throw new ConflictException("Remediation request integrity check failed; it must be re-requested");
        }
    }

    static String computeHash(RemediationRequest r) {
        String canonical = String.join("|",
                "v1",
                r.getTenantKey(),
                String.valueOf(r.getCloudAccount().getId()),
                r.getCloudAccount().getProvider().name(),
                r.getCloudAccount().getExternalAccountId(),
                String.valueOf(r.getFinding().getId()),
                r.getActionType().name(),
                r.getResourceType(),
                r.getResourceId(),
                String.valueOf(r.getRegion()),
                r.getParametersJson());
        return CloudScanService.sha256(canonical);
    }

    private static boolean confirmationMatches(RemediationRequest r, String confirmation) {
        if (confirmation == null) {
            return false;
        }
        String typed = confirmation.trim();
        if (typed.equals(r.getResourceId())) {
            return true;
        }
        // Allow typing the bare bucket name for S3 targets
        return r.getResourceId().startsWith("arn:aws:s3:::")
                && typed.equals(r.getResourceId().substring("arn:aws:s3:::".length()));
    }

    private static RemediationTarget target(RemediationRequest r) {
        return new RemediationTarget(r.getResourceType(), r.getResourceId(), r.getRegion());
    }

    private static String describePlannedChange(RemediationRequest r) {
        if (r.getActionType() == RemediationActionType.S3_BLOCK_PUBLIC_ACCESS) {
            return "PutPublicAccessBlock on " + r.getResourceId() + " with BlockPublicAcls=true, IgnorePublicAcls=true, "
                    + "BlockPublicPolicy=true, RestrictPublicBuckets=true";
        }
        return r.getActionType().name() + " on " + r.getResourceId();
    }

    private static boolean hasAuthority(Permission permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> permission.name().equals(a.getAuthority()));
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private RemediationDTO toDTO(RemediationRequest r) {
        return RemediationDTO.builder()
                .id(r.getId())
                .cloudAccountId(r.getCloudAccount().getId())
                .cloudAccountName(r.getCloudAccount().getName())
                .findingId(r.getFinding().getId())
                .findingTitle(r.getFinding().getTitle())
                .actionType(r.getActionType())
                .highImpact(r.getActionType().isHighImpact())
                .resourceType(r.getResourceType())
                .resourceId(r.getResourceId())
                .region(r.getRegion())
                .requestHash(r.getRequestHash())
                .status(r.getStatus())
                .requestedBy(r.getRequestedBy())
                .requestedAt(r.getRequestedAt())
                .justification(r.getJustification())
                .validatedAt(r.getValidatedAt())
                .validationNotes(r.getValidationNotes())
                .decidedBy(r.getDecidedBy())
                .decidedAt(r.getDecidedAt())
                .decisionComment(r.getDecisionComment())
                .cancelledBy(r.getCancelledBy())
                .cancelledAt(r.getCancelledAt())
                .executedBy(r.getExecutedBy())
                .executionStartedAt(r.getExecutionStartedAt())
                .executionCompletedAt(r.getExecutionCompletedAt())
                .executionMessage(r.getExecutionMessage())
                .providerRequestId(r.getProviderRequestId())
                .executionAttempts(r.getExecutionAttempts())
                .verifiedAt(r.getVerifiedAt())
                .verificationResult(r.getVerificationResult())
                .build();
    }
}
