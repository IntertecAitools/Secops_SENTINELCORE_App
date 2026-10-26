package com.sentinelcore.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelcore.backend.cloud.CloudConnectorRegistry;
import com.sentinelcore.backend.cloud.CloudErrorType;
import com.sentinelcore.backend.cloud.CloudModels.*;
import com.sentinelcore.backend.cloud.CloudProviderException;
import com.sentinelcore.backend.dto.cloud.CloudFindingDTO;
import com.sentinelcore.backend.dto.cloud.CloudResourceDTO;
import com.sentinelcore.backend.dto.cloud.CloudScanDTO;
import com.sentinelcore.backend.entity.CloudAccount;
import com.sentinelcore.backend.entity.CloudFinding;
import com.sentinelcore.backend.entity.CloudResource;
import com.sentinelcore.backend.entity.CloudScan;
import com.sentinelcore.backend.exception.ConflictException;
import com.sentinelcore.backend.exception.ResourceNotFoundException;
import com.sentinelcore.backend.repository.CloudAccountRepository;
import com.sentinelcore.backend.repository.CloudFindingRepository;
import com.sentinelcore.backend.repository.CloudResourceRepository;
import com.sentinelcore.backend.repository.CloudScanRepository;
import com.sentinelcore.backend.security.service.CurrentUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Runs read-only assessments and reconciles results with stored inventory and findings.
 */
@Service
@Slf4j
public class CloudScanService {

    /** A RUNNING scan older than this is considered abandoned (e.g. process restart) and no longer blocks new scans. */
    private static final long STALE_SCAN_MINUTES = 30;

    private final CloudAccountService cloudAccountService;
    private final CloudAccountRepository cloudAccountRepository;
    private final CloudConnectorRegistry connectorRegistry;
    private final CloudScanRepository scanRepository;
    private final CloudFindingRepository findingRepository;
    private final CloudResourceRepository resourceRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public CloudScanService(CloudAccountService cloudAccountService,
                            CloudAccountRepository cloudAccountRepository,
                            CloudConnectorRegistry connectorRegistry,
                            CloudScanRepository scanRepository,
                            CloudFindingRepository findingRepository,
                            CloudResourceRepository resourceRepository,
                            CurrentUserService currentUserService,
                            AuditLogService auditLogService,
                            ObjectMapper objectMapper,
                            PlatformTransactionManager transactionManager) {
        this.cloudAccountService = cloudAccountService;
        this.cloudAccountRepository = cloudAccountRepository;
        this.connectorRegistry = connectorRegistry;
        this.scanRepository = scanRepository;
        this.findingRepository = findingRepository;
        this.resourceRepository = resourceRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Runs a synchronous scan. Only one scan per account may run at a time. Provider calls happen
     * outside any database transaction; results are applied in a single transaction.
     */
    public CloudScanDTO runScan(Long accountId) {
        CloudAccount account = cloudAccountService.requireAccount(accountId);
        String username = currentUserService.currentUsername();

        if (scanRepository.existsByCloudAccountIdAndStatusAndStartedAtAfter(account.getId(), CloudScan.Status.RUNNING,
                LocalDateTime.now().minusMinutes(STALE_SCAN_MINUTES))) {
            throw new ConflictException("A scan is already running for this account");
        }

        CloudScan scan = scanRepository.save(CloudScan.builder()
                .tenantKey(account.getTenantKey())
                .cloudAccount(account)
                .status(CloudScan.Status.RUNNING)
                .startedAt(LocalDateTime.now())
                .triggeredBy(username)
                .build());
        auditLogService.logSecurityEvent("CLOUD_SCAN_STARTED",
                "Scan " + scan.getId() + " started for cloud account id " + account.getId());

        AssessmentResult result;
        try {
            result = connectorRegistry.get(account.getProvider())
                    .collectSecurityFindings(CloudAccountService.toContext(account));
        } catch (CloudProviderException e) {
            finishFailed(scan, e.getErrorType() + ": " + e.getMessage());
            if (e.getErrorType() == CloudErrorType.NOT_CONFIGURED) {
                throw e;
            }
            return toDTO(scanRepository.findById(scan.getId()).orElseThrow());
        } catch (RuntimeException e) {
            log.error("Scan {} failed unexpectedly", scan.getId(), e);
            finishFailed(scan, "Unexpected error during scan");
            throw e;
        }

        CloudScan finished = transactionTemplate.execute(status ->
                applyAssessment(account, scan.getId(), result, finding -> true));
        auditLogService.logSecurityEvent("CLOUD_SCAN_COMPLETED",
                "Scan " + scan.getId() + " finished with status " + finished.getStatus()
                        + " (observed " + finished.getFindingsObserved() + ", resolved " + finished.getFindingsResolved() + ")");
        return toDTO(finished);
    }

    /**
     * Re-evaluates the resource of one finding and reconciles findings for that resource only.
     */
    public CloudFindingDTO recheckFinding(Long findingId) {
        CloudFinding finding = findingRepository.findByIdAndTenantKey(findingId, currentUserService.currentTenant())
                .orElseThrow(() -> new ResourceNotFoundException("Finding not found with id: " + findingId));
        CloudAccount account = cloudAccountService.requireAccount(finding.getCloudAccount().getId());

        AssessmentResult result = connectorRegistry.get(account.getProvider())
                .evaluateResourceConfiguration(CloudAccountService.toContext(account),
                        new RemediationTarget(finding.getResourceType(), finding.getResourceId(), finding.getRegion()));

        String resourceId = finding.getResourceId();
        transactionTemplate.executeWithoutResult(status ->
                applyAssessment(account, null, result, f -> resourceId.equals(f.getResourceId())));
        auditLogService.logSecurityEvent("CLOUD_FINDING_RECHECKED", "Re-evaluated finding " + findingId);
        return transactionTemplate.execute(status -> toDTO(findingRepository.findById(findingId).orElseThrow()));
    }

    /**
     * Applies an assessment: upserts resources and findings by fingerprint, reopens findings that
     * reappear, and resolves open findings in {@code candidates} that were not observed — but only
     * when the scope they belong to completed without errors.
     */
    CloudScan applyAssessment(CloudAccount account, Long scanId, AssessmentResult result,
                              Predicate<CloudFinding> candidates) {
        LocalDateTime now = LocalDateTime.now();

        Map<String, CloudResource> resources = resourceRepository.findByCloudAccountId(account.getId()).stream()
                .collect(Collectors.toMap(CloudResource::getResourceId, r -> r, (a, b) -> a));
        for (ResourceObservation observation : result.resources()) {
            CloudResource resource = resources.get(observation.resourceId());
            if (resource == null) {
                resource = CloudResource.builder()
                        .tenantKey(account.getTenantKey())
                        .cloudAccount(account)
                        .resourceId(observation.resourceId())
                        .resourceType(observation.resourceType())
                        .firstSeenAt(toLocal(observation.observedAt()))
                        .build();
                resources.put(observation.resourceId(), resource);
            }
            resource.setRegion(observation.region());
            resource.setName(observation.name());
            resource.setLastSeenAt(toLocal(observation.observedAt()));
            resource.setLastScanId(scanId);
            resourceRepository.save(resource);
        }

        Map<String, CloudFinding> existing = findingRepository.findByCloudAccountId(account.getId()).stream()
                .collect(Collectors.toMap(CloudFinding::getFingerprint, f -> f, (a, b) -> a));
        Set<String> observed = new HashSet<>();
        int reopened = 0;

        for (FindingObservation observation : result.findings()) {
            String fingerprint = fingerprint(observation);
            if (!observed.add(fingerprint)) {
                continue; // duplicate observation within one result
            }
            CloudFinding finding = existing.get(fingerprint);
            if (finding == null) {
                finding = CloudFinding.builder()
                        .tenantKey(account.getTenantKey())
                        .cloudAccount(account)
                        .fingerprint(fingerprint)
                        .findingType(observation.findingType())
                        .resourceType(observation.resourceType())
                        .resourceId(observation.resourceId())
                        .status(CloudFinding.Status.OPEN)
                        .firstDetectedAt(toLocal(observation.observedAt()))
                        .build();
                existing.put(fingerprint, finding);
            } else if (finding.getStatus() == CloudFinding.Status.RESOLVED) {
                finding.setStatus(CloudFinding.Status.OPEN);
                finding.setResolvedAt(null);
                finding.setResolutionReason(null);
                finding.setReopenCount(finding.getReopenCount() + 1);
                reopened++;
            }
            finding.setSeverity(observation.severity().name());
            finding.setRegion(observation.region());
            finding.setScope(observation.scope());
            finding.setTitle(truncate(observation.title(), 512));
            finding.setObservedFact(observation.observedFact());
            finding.setInferredRisk(observation.inferredRisk());
            finding.setEvidenceJson(toJson(observation.evidence()));
            finding.setSource(observation.source());
            finding.setRemediationGuidance(observation.remediationGuidance());
            finding.setSupportedRemediation(observation.supportedRemediation());
            finding.setLastObservedAt(toLocal(observation.observedAt()));
            if (scanId != null) {
                finding.setLastScanId(scanId);
            }
            findingRepository.save(finding);
        }

        Set<String> completedScopes = result.scopes().stream()
                .filter(ScopeResult::complete).map(ScopeResult::scope).collect(Collectors.toCollection(TreeSet::new));
        int resolved = 0;
        for (CloudFinding finding : existing.values()) {
            if (finding.getStatus() == CloudFinding.Status.OPEN
                    && !observed.contains(finding.getFingerprint())
                    && completedScopes.contains(finding.getScope())
                    && candidates.test(finding)) {
                finding.setStatus(CloudFinding.Status.RESOLVED);
                finding.setResolvedAt(now);
                finding.setResolutionReason(scanId != null
                        ? "Not observed by scan #" + scanId + " (scope " + finding.getScope() + " completed)"
                        : "Not observed when the resource was re-evaluated");
                findingRepository.save(finding);
                resolved++;
            }
        }

        CloudAccount managedAccount = cloudAccountRepository.findById(account.getId()).orElseThrow();
        managedAccount.setLastScanAt(now);
        cloudAccountRepository.save(managedAccount);
        if (scanId == null) {
            return null;
        }

        CloudScan scan = scanRepository.findById(scanId).orElseThrow();
        boolean anyComplete = !completedScopes.isEmpty();
        scan.setStatus(result.isComplete() ? CloudScan.Status.SUCCEEDED
                : anyComplete ? CloudScan.Status.PARTIAL : CloudScan.Status.FAILED);
        scan.setCompletedAt(now);
        scan.setResourcesObserved(result.resources().size());
        scan.setFindingsObserved(observed.size());
        scan.setFindingsResolved(resolved);
        scan.setFindingsReopened(reopened);
        scan.setCompletedScopes(String.join(",", completedScopes));
        String errors = result.scopes().stream()
                .filter(s -> !s.complete())
                .map(s -> s.scope() + ": " + s.errorType() + " - " + s.errorMessage())
                .collect(Collectors.joining("\n"));
        scan.setErrorSummary(errors.isEmpty() ? null : errors);
        return scanRepository.save(scan);
    }

    private void finishFailed(CloudScan scan, String message) {
        transactionTemplate.executeWithoutResult(status -> {
            CloudScan s = scanRepository.findById(scan.getId()).orElseThrow();
            s.setStatus(CloudScan.Status.FAILED);
            s.setCompletedAt(LocalDateTime.now());
            s.setErrorSummary(message);
            scanRepository.save(s);
        });
        auditLogService.logSecurityEvent("CLOUD_SCAN_FAILED", "Scan " + scan.getId() + " failed: " + message);
    }

    @Transactional(readOnly = true)
    public Page<CloudFindingDTO> listFindings(Long accountId, String status, String severity, String search,
                                              Pageable pageable) {
        String tenant = currentUserService.currentTenant();
        Specification<CloudFinding> spec = (root, query, cb) -> cb.equal(root.get("tenantKey"), tenant);
        if (accountId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("cloudAccount").get("id"), accountId));
        }
        if (StringUtils.hasText(status)) {
            CloudFinding.Status parsed = parseEnum(CloudFinding.Status.class, status);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), parsed));
        }
        if (StringUtils.hasText(severity)) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("severity")), severity.toUpperCase()));
        }
        if (StringUtils.hasText(search)) {
            String like = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("resourceId")), like),
                    cb.like(cb.lower(root.get("findingType")), like)));
        }
        return findingRepository.findAll(spec, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public CloudFindingDTO getFinding(Long id) {
        return toDTO(findingRepository.findByIdAndTenantKey(id, currentUserService.currentTenant())
                .orElseThrow(() -> new ResourceNotFoundException("Finding not found with id: " + id)));
    }

    @Transactional(readOnly = true)
    public Page<CloudResourceDTO> listResources(Long accountId, Pageable pageable) {
        CloudAccount account = cloudAccountService.requireAccount(accountId);
        return resourceRepository.findByTenantKeyAndCloudAccountId(account.getTenantKey(), account.getId(), pageable)
                .map(r -> CloudResourceDTO.builder()
                        .id(r.getId())
                        .cloudAccountId(account.getId())
                        .resourceType(r.getResourceType())
                        .resourceId(r.getResourceId())
                        .region(r.getRegion())
                        .name(r.getName())
                        .firstSeenAt(r.getFirstSeenAt())
                        .lastSeenAt(r.getLastSeenAt())
                        .build());
    }

    @Transactional(readOnly = true)
    public Page<CloudScanDTO> listScans(Long accountId, Pageable pageable) {
        CloudAccount account = cloudAccountService.requireAccount(accountId);
        return scanRepository.findByTenantKeyAndCloudAccountIdOrderByStartedAtDesc(
                account.getTenantKey(), account.getId(), pageable).map(this::toDTO);
    }

    static String fingerprint(FindingObservation observation) {
        String canonical = observation.findingType() + "|" + observation.resourceId() + "|"
                + (observation.discriminator() == null ? "" : observation.discriminator());
        return sha256(canonical);
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String toJson(Map<String, String> evidence) {
        try {
            return objectMapper.writeValueAsString(evidence == null ? Map.of() : new TreeMap<>(evidence));
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, String> fromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private static LocalDateTime toLocal(java.time.Instant instant) {
        return instant == null ? LocalDateTime.now() : LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault());
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.sentinelcore.backend.exception.BadRequestException("Invalid value: " + value);
        }
    }

    CloudFindingDTO toDTO(CloudFinding finding) {
        return CloudFindingDTO.builder()
                .id(finding.getId())
                .cloudAccountId(finding.getCloudAccount().getId())
                .cloudAccountName(finding.getCloudAccount().getName())
                .findingType(finding.getFindingType())
                .severity(finding.getSeverity())
                .status(finding.getStatus())
                .resourceType(finding.getResourceType())
                .resourceId(finding.getResourceId())
                .region(finding.getRegion())
                .title(finding.getTitle())
                .observedFact(finding.getObservedFact())
                .inferredRisk(finding.getInferredRisk())
                .evidence(fromJson(finding.getEvidenceJson()))
                .source(finding.getSource())
                .remediationGuidance(finding.getRemediationGuidance())
                .supportedRemediation(finding.getSupportedRemediation())
                .firstDetectedAt(finding.getFirstDetectedAt())
                .lastObservedAt(finding.getLastObservedAt())
                .resolvedAt(finding.getResolvedAt())
                .resolutionReason(finding.getResolutionReason())
                .reopenCount(finding.getReopenCount())
                .build();
    }

    private CloudScanDTO toDTO(CloudScan scan) {
        return CloudScanDTO.builder()
                .id(scan.getId())
                .cloudAccountId(scan.getCloudAccount().getId())
                .status(scan.getStatus())
                .startedAt(scan.getStartedAt())
                .completedAt(scan.getCompletedAt())
                .triggeredBy(scan.getTriggeredBy())
                .resourcesObserved(scan.getResourcesObserved())
                .findingsObserved(scan.getFindingsObserved())
                .findingsResolved(scan.getFindingsResolved())
                .findingsReopened(scan.getFindingsReopened())
                .completedScopes(StringUtils.hasText(scan.getCompletedScopes())
                        ? List.of(scan.getCompletedScopes().split(",")) : List.of())
                .errorSummary(scan.getErrorSummary())
                .build();
    }
}
