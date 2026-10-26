package com.sentinelcore.backend.cloud;

import com.sentinelcore.backend.cloud.CloudModels.*;

import java.util.List;

/**
 * Provider-neutral contract for cloud operations. Implementations encapsulate all provider SDK
 * calls; services and controllers never use provider SDKs directly.
 * <p>
 * Implementations must throw {@link CloudProviderException} for classified provider failures and
 * {@link UnsupportedCloudOperationException} for capabilities they do not implement.
 */
public interface CloudProviderConnector {

    CloudProvider getProvider();

    /** Read-only: confirms the platform can authenticate to the account and that it is the expected account. */
    ConnectionValidation validateConnection(AccountContext account);

    /** Read-only: accounts reachable with the configured identity. */
    List<DiscoveredAccount> listAccounts(AccountContext account);

    /** Read-only: inventory of supported resource types. */
    List<ResourceObservation> listResources(AccountContext account);

    /** Read-only: inventory plus security findings, with per-scope completeness. */
    AssessmentResult collectSecurityFindings(AccountContext account);

    /** Read-only: re-evaluates a single resource. */
    AssessmentResult evaluateResourceConfiguration(AccountContext account, RemediationTarget target);

    /** Read-only: current state and planned change for an allowlisted action. */
    RemediationPreview previewRemediation(AccountContext account, RemediationActionType action, RemediationTarget target);

    /**
     * Mutating. Must only be invoked by the remediation workflow after approval checks have passed.
     */
    ExecutionResult executeApprovedRemediation(AccountContext account, RemediationActionType action, RemediationTarget target);

    /** Read-only: checks whether the resource is now in the state the action is meant to produce. */
    VerificationResult verifyRemediation(AccountContext account, RemediationActionType action, RemediationTarget target);
}
