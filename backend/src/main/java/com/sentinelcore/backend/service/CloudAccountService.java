package com.sentinelcore.backend.service;

import com.sentinelcore.backend.cloud.*;
import com.sentinelcore.backend.cloud.CloudModels.AccountContext;
import com.sentinelcore.backend.cloud.CloudModels.AuthMode;
import com.sentinelcore.backend.cloud.CloudModels.ConnectionValidation;
import com.sentinelcore.backend.cloud.aws.AwsSettings;
import com.sentinelcore.backend.dto.cloud.CloudAccountDTO;
import com.sentinelcore.backend.dto.cloud.CloudAccountRequest;
import com.sentinelcore.backend.dto.cloud.CloudCapabilitiesDTO;
import com.sentinelcore.backend.entity.CloudAccount;
import com.sentinelcore.backend.exception.BadRequestException;
import com.sentinelcore.backend.exception.ConflictException;
import com.sentinelcore.backend.exception.ResourceNotFoundException;
import com.sentinelcore.backend.repository.CloudAccountRepository;
import com.sentinelcore.backend.security.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CloudAccountService {

    private static final Pattern AWS_ACCOUNT_ID = Pattern.compile("^\\d{12}$");
    private static final Pattern AWS_ROLE_ARN = Pattern.compile("^arn:aws(-cn|-us-gov)?:iam::(\\d{12}):role/[\\w+=,.@/-]{1,512}$");

    private final CloudAccountRepository cloudAccountRepository;
    private final CloudConnectorRegistry connectorRegistry;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final AwsSettings awsSettings;

    @Value("${app.cloud.remediation.execution-enabled:false}")
    private boolean remediationExecutionEnabled;

    public CloudCapabilitiesDTO capabilities() {
        List<CloudProvider> supported = connectorRegistry.supportedProviders().stream().sorted().toList();
        List<CloudProvider> unsupported = EnumSet.allOf(CloudProvider.class).stream()
                .filter(p -> !supported.contains(p)).toList();
        return CloudCapabilitiesDTO.builder()
                .supportedProviders(supported)
                .knownButUnsupportedProviders(unsupported)
                .awsIntegrationEnabled(awsSettings.isEnabled())
                .remediationExecutionEnabled(remediationExecutionEnabled)
                .remediationActions(List.of(RemediationActionType.values()))
                .build();
    }

    public List<CloudAccountDTO> listAccounts() {
        return cloudAccountRepository.findByTenantKeyOrderByNameAsc(currentUserService.currentTenant())
                .stream().map(CloudAccountService::toDTO).toList();
    }

    public CloudAccountDTO getAccount(Long id) {
        return toDTO(requireAccount(id));
    }

    /** Tenant-scoped lookup. Accounts of other tenants are reported as not found. */
    public CloudAccount requireAccount(Long id) {
        return cloudAccountRepository.findByIdAndTenantKey(id, currentUserService.currentTenant())
                .orElseThrow(() -> new ResourceNotFoundException("Cloud account not found with id: " + id));
    }

    @Transactional
    public CloudAccountDTO createAccount(CloudAccountRequest request) {
        String tenant = currentUserService.currentTenant();
        validate(request);
        if (cloudAccountRepository.existsByTenantKeyAndProviderAndExternalAccountId(
                tenant, request.getProvider(), request.getExternalAccountId())) {
            throw new ConflictException("This cloud account is already registered");
        }
        CloudAccount account = CloudAccount.builder()
                .tenantKey(tenant)
                .name(request.getName().trim())
                .provider(request.getProvider())
                .externalAccountId(request.getExternalAccountId())
                .authMode(request.getAuthMode())
                .roleArn(request.getAuthMode() == AuthMode.ASSUME_ROLE ? request.getRoleArn() : null)
                .externalId(request.getAuthMode() == AuthMode.ASSUME_ROLE && StringUtils.hasText(request.getExternalId())
                        ? request.getExternalId() : null)
                .regions(joinRegions(request.getRegions()))
                .connectionStatus(CloudAccount.ConnectionStatus.NOT_VERIFIED)
                .createdBy(currentUserService.currentUsername())
                .build();
        CloudAccount saved = cloudAccountRepository.save(account);
        auditLogService.logSecurityEvent("CLOUD_ACCOUNT_REGISTERED",
                "Registered " + saved.getProvider() + " account " + saved.getExternalAccountId()
                        + " (id " + saved.getId() + ", auth " + saved.getAuthMode() + ")");
        return toDTO(saved);
    }

    @Transactional
    public CloudAccountDTO updateAccount(Long id, CloudAccountRequest request) {
        CloudAccount account = requireAccount(id);
        validate(request);
        if (request.getProvider() != account.getProvider()
                || !request.getExternalAccountId().equals(account.getExternalAccountId())) {
            throw new BadRequestException("Provider and account id cannot be changed; register a new account instead");
        }
        account.setName(request.getName().trim());
        account.setAuthMode(request.getAuthMode());
        if (request.getAuthMode() == AuthMode.ASSUME_ROLE) {
            account.setRoleArn(request.getRoleArn());
            if (request.getExternalId() != null) {
                account.setExternalId(StringUtils.hasText(request.getExternalId()) ? request.getExternalId() : null);
            }
        } else {
            account.setRoleArn(null);
            account.setExternalId(null);
        }
        account.setRegions(joinRegions(request.getRegions()));
        // Connection settings changed: previous verification no longer applies
        account.setConnectionStatus(CloudAccount.ConnectionStatus.NOT_VERIFIED);
        auditLogService.logSecurityEvent("CLOUD_ACCOUNT_UPDATED",
                "Updated cloud account id " + account.getId() + " (" + account.getExternalAccountId() + ")");
        return toDTO(cloudAccountRepository.save(account));
    }

    /**
     * Read-only connection check. Failures are recorded on the account and returned, not thrown,
     * except when the integration is not configured at all.
     */
    @Transactional
    public CloudAccountDTO verifyConnection(Long id) {
        CloudAccount account = requireAccount(id);
        CloudProviderConnector connector = connectorRegistry.get(account.getProvider());
        try {
            ConnectionValidation validation = connector.validateConnection(toContext(account));
            account.setConnectionStatus(CloudAccount.ConnectionStatus.CONNECTED);
            account.setLastVerificationMessage("Connected as " + validation.callerIdentity());
        } catch (CloudProviderException e) {
            if (e.getErrorType() == CloudErrorType.NOT_CONFIGURED) {
                throw e;
            }
            account.setConnectionStatus(CloudAccount.ConnectionStatus.FAILED);
            account.setLastVerificationMessage(e.getErrorType() + ": " + e.getMessage());
        }
        account.setLastVerifiedAt(LocalDateTime.now());
        auditLogService.logSecurityEvent("CLOUD_ACCOUNT_VERIFIED",
                "Connection check for cloud account id " + account.getId() + ": " + account.getConnectionStatus());
        return toDTO(cloudAccountRepository.save(account));
    }

    private void validate(CloudAccountRequest request) {
        if (!connectorRegistry.isSupported(request.getProvider())) {
            throw new UnsupportedCloudOperationException(
                    "Cloud provider " + request.getProvider() + " is not supported yet");
        }
        if (request.getProvider() == CloudProvider.AWS) {
            if (!AWS_ACCOUNT_ID.matcher(request.getExternalAccountId()).matches()) {
                throw new BadRequestException("AWS account id must be 12 digits");
            }
            if (request.getAuthMode() == AuthMode.ASSUME_ROLE) {
                if (!StringUtils.hasText(request.getRoleArn())) {
                    throw new BadRequestException("roleArn is required for ASSUME_ROLE");
                }
                var matcher = AWS_ROLE_ARN.matcher(request.getRoleArn());
                if (!matcher.matches()) {
                    throw new BadRequestException("roleArn must be an IAM role ARN");
                }
                if (!request.getExternalAccountId().equals(matcher.group(2))) {
                    throw new BadRequestException("roleArn must belong to the registered account");
                }
            }
        }
    }

    public static AccountContext toContext(CloudAccount account) {
        return new AccountContext(account.getProvider(), account.getExternalAccountId(), account.getAuthMode(),
                account.getRoleArn(), account.getExternalId(), splitRegions(account.getRegions()));
    }

    static List<String> splitRegions(String regions) {
        if (!StringUtils.hasText(regions)) {
            return List.of();
        }
        return Arrays.stream(regions.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static String joinRegions(List<String> regions) {
        if (regions == null || regions.isEmpty()) {
            return null;
        }
        return String.join(",", regions.stream().map(String::trim).distinct().toList());
    }

    public static CloudAccountDTO toDTO(CloudAccount account) {
        return CloudAccountDTO.builder()
                .id(account.getId())
                .name(account.getName())
                .provider(account.getProvider())
                .externalAccountId(account.getExternalAccountId())
                .authMode(account.getAuthMode())
                .roleArn(account.getRoleArn())
                .externalIdConfigured(StringUtils.hasText(account.getExternalId()))
                .regions(splitRegions(account.getRegions()))
                .connectionStatus(account.getConnectionStatus())
                .lastVerifiedAt(account.getLastVerifiedAt())
                .lastVerificationMessage(account.getLastVerificationMessage())
                .lastScanAt(account.getLastScanAt())
                .createdBy(account.getCreatedBy())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
