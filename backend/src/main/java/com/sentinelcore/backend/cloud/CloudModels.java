package com.sentinelcore.backend.cloud;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Stable, provider-neutral models exchanged between services and {@link CloudProviderConnector}s.
 */
public final class CloudModels {

    private CloudModels() {
    }

    public enum AuthMode {
        /** Use the platform's own identity (instance profile, workload identity, SSO, env) directly. */
        PLATFORM_DEFAULT,
        /** Assume a role in the target account using short-lived credentials from the platform identity. */
        ASSUME_ROLE
    }

    public enum Severity {
        CRITICAL, HIGH, MEDIUM, LOW
    }

    /** Connection metadata for one cloud account. Contains no secret material. */
    public record AccountContext(
            CloudProvider provider,
            String externalAccountId,
            AuthMode authMode,
            String roleArn,
            String externalId,
            List<String> regions) {
    }

    public record ConnectionValidation(
            String resolvedAccountId,
            String callerIdentity,
            Instant checkedAt) {
    }

    public record DiscoveredAccount(String accountId, String displayName) {
    }

    public record ResourceObservation(
            String resourceType,
            String resourceId,
            String region,
            String name,
            String scope,
            Instant observedAt) {
    }

    /**
     * A security finding as observed from provider APIs. {@code observedFact} states what the API
     * returned; {@code inferredRisk} is the platform's interpretation of that fact.
     * {@code discriminator} distinguishes multiple findings of the same type on one resource
     * (e.g. two different open rules) and is part of the idempotency fingerprint.
     */
    public record FindingObservation(
            String findingType,
            String discriminator,
            Severity severity,
            String resourceType,
            String resourceId,
            String region,
            String scope,
            String title,
            String observedFact,
            String inferredRisk,
            Map<String, String> evidence,
            String source,
            String remediationGuidance,
            RemediationActionType supportedRemediation,
            Instant observedAt) {
    }

    /**
     * Outcome for one assessment scope (e.g. "s3", "ec2:eu-west-1"). Findings in a scope are only
     * resolved by absence when the scope completed without errors.
     */
    public record ScopeResult(String scope, boolean complete, CloudErrorType errorType, String errorMessage) {
    }

    public record AssessmentResult(
            List<ResourceObservation> resources,
            List<FindingObservation> findings,
            List<ScopeResult> scopes) {

        public boolean isComplete() {
            return scopes.stream().allMatch(ScopeResult::complete);
        }
    }

    public record RemediationTarget(String resourceType, String resourceId, String region) {
    }

    public enum PreviewType {
        /** The provider simulated the change. */
        PROVIDER_DRY_RUN,
        /** The platform read current state and computed the change locally; nothing was simulated by the provider. */
        LOCAL_VALIDATION
    }

    public record RemediationPreview(
            PreviewType previewType,
            String currentState,
            String plannedChange,
            String notes) {
    }

    public record ExecutionResult(
            boolean success,
            String providerRequestId,
            String message) {
    }

    public record VerificationResult(
            boolean verified,
            String observedState,
            Instant checkedAt) {
    }
}
