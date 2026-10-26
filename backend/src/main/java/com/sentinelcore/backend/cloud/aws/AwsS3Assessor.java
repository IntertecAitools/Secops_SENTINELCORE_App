package com.sentinelcore.backend.cloud.aws;

import com.sentinelcore.backend.cloud.CloudErrorType;
import com.sentinelcore.backend.cloud.CloudModels.*;
import com.sentinelcore.backend.cloud.CloudProviderException;
import com.sentinelcore.backend.cloud.RemediationActionType;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only S3 checks. Every call passes ExpectedBucketOwner so a bucket owned by a different
 * account is never evaluated or modified by mistake.
 */
class AwsS3Assessor {

    static final String RESOURCE_TYPE = "s3:bucket";
    static final String SCOPE = "s3";
    private static final String ALL_USERS_URI = "http://acs.amazonaws.com/groups/global/AllUsers";
    private static final String AUTHENTICATED_USERS_URI = "http://acs.amazonaws.com/groups/global/AuthenticatedUsers";
    private static final int PAGE_SIZE = 1000;

    private final Clock clock;

    AwsS3Assessor(Clock clock) {
        this.clock = clock;
    }

    record BucketRef(String name, String region) {
    }

    /** Paginates ListBuckets. */
    List<BucketRef> listBuckets(S3Client s3, String accountId) {
        List<BucketRef> buckets = new ArrayList<>();
        String token = null;
        do {
            ListBucketsRequest.Builder request = ListBucketsRequest.builder().maxBuckets(PAGE_SIZE);
            if (token != null) {
                request.continuationToken(token);
            }
            ListBucketsResponse response = s3.listBuckets(request.build());
            for (Bucket bucket : response.buckets()) {
                buckets.add(new BucketRef(bucket.name(), resolveRegion(s3, bucket, accountId)));
            }
            token = response.continuationToken();
        } while (token != null && !token.isEmpty());
        return buckets;
    }

    private String resolveRegion(S3Client s3, Bucket bucket, String accountId) {
        if (bucket.bucketRegion() != null && !bucket.bucketRegion().isEmpty()) {
            return bucket.bucketRegion();
        }
        String constraint = s3.getBucketLocation(GetBucketLocationRequest.builder()
                .bucket(bucket.name()).expectedBucketOwner(accountId).build()).locationConstraintAsString();
        if (constraint == null || constraint.isEmpty()) {
            return "us-east-1";
        }
        return "EU".equals(constraint) ? "eu-west-1" : constraint;
    }

    ResourceObservation toResource(String accountId, BucketRef bucket) {
        return new ResourceObservation(RESOURCE_TYPE, bucketArn(bucket.name()), bucket.region(), bucket.name(),
                SCOPE, clock.instant());
    }

    /** Evaluates one bucket. Throws on API errors so the caller can mark the scope incomplete. */
    List<FindingObservation> assessBucket(S3Client s3, String accountId, BucketRef bucket) {
        Instant now = clock.instant();
        List<FindingObservation> findings = new ArrayList<>();
        String arn = bucketArn(bucket.name());

        PublicAccessBlockConfiguration pab = getPublicAccessBlock(s3, accountId, bucket.name());
        boolean fullyBlocked = isFullyBlocked(pab);
        if (!fullyBlocked) {
            Map<String, String> evidence = pabEvidence(pab);
            findings.add(new FindingObservation(
                    "S3_PUBLIC_ACCESS_BLOCK_DISABLED", "bucket-pab",
                    Severity.MEDIUM, RESOURCE_TYPE, arn, bucket.region(), SCOPE,
                    "S3 bucket Block Public Access is not fully enabled: " + bucket.name(),
                    pab == null
                            ? "GetPublicAccessBlock returned NoSuchPublicAccessBlockConfiguration for the bucket."
                            : "GetPublicAccessBlock returned " + describe(pab) + ".",
                    "Without all four bucket-level settings, an ACL or bucket policy change can make objects public. "
                            + "Account-level Block Public Access was not evaluated and may still prevent public access.",
                    evidence, "AWS S3 GetPublicAccessBlock",
                    "Enable all four Block Public Access settings on the bucket unless public access is intentional.",
                    RemediationActionType.S3_BLOCK_PUBLIC_ACCESS, now));
        }

        boolean policyRestricted = pab != null && Boolean.TRUE.equals(pab.restrictPublicBuckets());
        Boolean policyPublic = getPolicyIsPublic(s3, accountId, bucket.name());
        if (Boolean.TRUE.equals(policyPublic) && !policyRestricted) {
            findings.add(new FindingObservation(
                    "S3_BUCKET_POLICY_PUBLIC", "bucket-policy",
                    Severity.CRITICAL, RESOURCE_TYPE, arn, bucket.region(), SCOPE,
                    "S3 bucket policy allows public access: " + bucket.name(),
                    "GetBucketPolicyStatus returned IsPublic=true and bucket-level RestrictPublicBuckets is not enabled.",
                    "Anyone on the internet may be able to access objects in this bucket, unless account-level "
                            + "Block Public Access restricts it.",
                    Map.of("policyStatus.isPublic", "true",
                            "restrictPublicBuckets", String.valueOf(policyRestricted)),
                    "AWS S3 GetBucketPolicyStatus",
                    "Remove public principals from the bucket policy or enable RestrictPublicBuckets.",
                    RemediationActionType.S3_BLOCK_PUBLIC_ACCESS, now));
        }

        boolean aclsIgnored = pab != null && Boolean.TRUE.equals(pab.ignorePublicAcls());
        List<String> publicGrants = getPublicAclGrants(s3, accountId, bucket.name());
        if (!publicGrants.isEmpty() && !aclsIgnored) {
            findings.add(new FindingObservation(
                    "S3_BUCKET_ACL_PUBLIC", "bucket-acl",
                    Severity.HIGH, RESOURCE_TYPE, arn, bucket.region(), SCOPE,
                    "S3 bucket ACL grants access to a public group: " + bucket.name(),
                    "GetBucketAcl returned grants " + publicGrants + " and bucket-level IgnorePublicAcls is not enabled.",
                    "The listed permissions are available to everyone (AllUsers) or to any AWS account (AuthenticatedUsers).",
                    Map.of("publicGrants", String.join(",", publicGrants),
                            "ignorePublicAcls", String.valueOf(aclsIgnored)),
                    "AWS S3 GetBucketAcl",
                    "Remove public grants from the bucket ACL, enable IgnorePublicAcls, or disable ACLs via Object Ownership.",
                    RemediationActionType.S3_BLOCK_PUBLIC_ACCESS, now));
        }

        String encryption = getDefaultEncryption(s3, accountId, bucket.name());
        if (encryption == null) {
            findings.add(new FindingObservation(
                    "S3_DEFAULT_ENCRYPTION_MISSING", "bucket-encryption",
                    Severity.LOW, RESOURCE_TYPE, arn, bucket.region(), SCOPE,
                    "S3 bucket has no default encryption configuration: " + bucket.name(),
                    "GetBucketEncryption returned ServerSideEncryptionConfigurationNotFoundError.",
                    "Objects uploaded without explicit encryption headers may not use the intended encryption key.",
                    Map.of("defaultEncryption", "none"),
                    "AWS S3 GetBucketEncryption",
                    "Configure default encryption (SSE-S3 or SSE-KMS) on the bucket.",
                    null, now));
        }
        return findings;
    }

    PublicAccessBlockConfiguration getPublicAccessBlock(S3Client s3, String accountId, String bucket) {
        try {
            return s3.getPublicAccessBlock(GetPublicAccessBlockRequest.builder()
                    .bucket(bucket).expectedBucketOwner(accountId).build()).publicAccessBlockConfiguration();
        } catch (S3Exception e) {
            if ("NoSuchPublicAccessBlockConfiguration".equals(AwsErrorMapper.errorCode(e))) {
                return null;
            }
            throw e;
        }
    }

    private Boolean getPolicyIsPublic(S3Client s3, String accountId, String bucket) {
        try {
            PolicyStatus status = s3.getBucketPolicyStatus(GetBucketPolicyStatusRequest.builder()
                    .bucket(bucket).expectedBucketOwner(accountId).build()).policyStatus();
            return status != null && Boolean.TRUE.equals(status.isPublic());
        } catch (S3Exception e) {
            if ("NoSuchBucketPolicy".equals(AwsErrorMapper.errorCode(e))) {
                return false;
            }
            throw e;
        }
    }

    private List<String> getPublicAclGrants(S3Client s3, String accountId, String bucket) {
        GetBucketAclResponse acl = s3.getBucketAcl(GetBucketAclRequest.builder()
                .bucket(bucket).expectedBucketOwner(accountId).build());
        List<String> grants = new ArrayList<>();
        for (Grant grant : acl.grants()) {
            String uri = grant.grantee() != null ? grant.grantee().uri() : null;
            if (ALL_USERS_URI.equals(uri)) {
                grants.add("AllUsers:" + grant.permissionAsString());
            } else if (AUTHENTICATED_USERS_URI.equals(uri)) {
                grants.add("AuthenticatedUsers:" + grant.permissionAsString());
            }
        }
        return grants;
    }

    private String getDefaultEncryption(S3Client s3, String accountId, String bucket) {
        try {
            ServerSideEncryptionConfiguration config = s3.getBucketEncryption(GetBucketEncryptionRequest.builder()
                    .bucket(bucket).expectedBucketOwner(accountId).build()).serverSideEncryptionConfiguration();
            if (config == null || config.rules().isEmpty()
                    || config.rules().get(0).applyServerSideEncryptionByDefault() == null) {
                return null;
            }
            return config.rules().get(0).applyServerSideEncryptionByDefault().sseAlgorithmAsString();
        } catch (S3Exception e) {
            if ("ServerSideEncryptionConfigurationNotFoundError".equals(AwsErrorMapper.errorCode(e))) {
                return null;
            }
            throw e;
        }
    }

    /** Applies all four Block Public Access settings. Mutating. */
    ExecutionResult blockPublicAccess(S3Client s3, String accountId, String bucket) {
        PutPublicAccessBlockResponse response = s3.putPublicAccessBlock(PutPublicAccessBlockRequest.builder()
                .bucket(bucket)
                .expectedBucketOwner(accountId)
                .publicAccessBlockConfiguration(PublicAccessBlockConfiguration.builder()
                        .blockPublicAcls(true)
                        .ignorePublicAcls(true)
                        .blockPublicPolicy(true)
                        .restrictPublicBuckets(true)
                        .build())
                .build());
        String requestId = response.responseMetadata() != null ? response.responseMetadata().requestId() : null;
        return new ExecutionResult(true, requestId, "PutPublicAccessBlock accepted by AWS");
    }

    static boolean isFullyBlocked(PublicAccessBlockConfiguration pab) {
        return pab != null
                && Boolean.TRUE.equals(pab.blockPublicAcls())
                && Boolean.TRUE.equals(pab.ignorePublicAcls())
                && Boolean.TRUE.equals(pab.blockPublicPolicy())
                && Boolean.TRUE.equals(pab.restrictPublicBuckets());
    }

    static String describe(PublicAccessBlockConfiguration pab) {
        if (pab == null) {
            return "no bucket-level Public Access Block configuration";
        }
        return "BlockPublicAcls=" + pab.blockPublicAcls()
                + ", IgnorePublicAcls=" + pab.ignorePublicAcls()
                + ", BlockPublicPolicy=" + pab.blockPublicPolicy()
                + ", RestrictPublicBuckets=" + pab.restrictPublicBuckets();
    }

    private static Map<String, String> pabEvidence(PublicAccessBlockConfiguration pab) {
        Map<String, String> evidence = new LinkedHashMap<>();
        evidence.put("configured", String.valueOf(pab != null));
        evidence.put("blockPublicAcls", pab == null ? "false" : String.valueOf(pab.blockPublicAcls()));
        evidence.put("ignorePublicAcls", pab == null ? "false" : String.valueOf(pab.ignorePublicAcls()));
        evidence.put("blockPublicPolicy", pab == null ? "false" : String.valueOf(pab.blockPublicPolicy()));
        evidence.put("restrictPublicBuckets", pab == null ? "false" : String.valueOf(pab.restrictPublicBuckets()));
        return evidence;
    }

    static String bucketArn(String bucket) {
        return "arn:aws:s3:::" + bucket;
    }

    static String bucketNameFromArn(String resourceId) {
        if (resourceId == null || !resourceId.startsWith("arn:aws:s3:::")) {
            throw new CloudProviderException(CloudErrorType.INVALID_CONFIGURATION, "Not an S3 bucket ARN");
        }
        String name = resourceId.substring("arn:aws:s3:::".length());
        if (!name.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")) {
            throw new CloudProviderException(CloudErrorType.INVALID_CONFIGURATION, "Invalid S3 bucket name");
        }
        return name;
    }
}
