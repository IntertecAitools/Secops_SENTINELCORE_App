package com.sentinelcore.backend.cloud;

import java.util.Set;

/**
 * Explicit allowlist of remediation actions. Anything not listed here cannot be requested.
 */
public enum RemediationActionType {

    /**
     * Enables all four S3 bucket-level Block Public Access settings (BlockPublicAcls, IgnorePublicAcls,
     * BlockPublicPolicy, RestrictPublicBuckets). Non-destructive and reversible, but high impact:
     * it breaks any intended public access (e.g. static website hosting).
     */
    S3_BLOCK_PUBLIC_ACCESS(
            CloudProvider.AWS,
            "s3:bucket",
            true,
            Set.of("S3_PUBLIC_ACCESS_BLOCK_DISABLED", "S3_BUCKET_POLICY_PUBLIC", "S3_BUCKET_ACL_PUBLIC"));

    private final CloudProvider provider;
    private final String resourceType;
    private final boolean highImpact;
    private final Set<String> applicableFindingTypes;

    RemediationActionType(CloudProvider provider, String resourceType, boolean highImpact,
                          Set<String> applicableFindingTypes) {
        this.provider = provider;
        this.resourceType = resourceType;
        this.highImpact = highImpact;
        this.applicableFindingTypes = applicableFindingTypes;
    }

    public CloudProvider getProvider() {
        return provider;
    }

    public String getResourceType() {
        return resourceType;
    }

    /** High-impact actions require approval by a different user than the requester. */
    public boolean isHighImpact() {
        return highImpact;
    }

    public boolean appliesTo(String findingType) {
        return applicableFindingTypes.contains(findingType);
    }
}
