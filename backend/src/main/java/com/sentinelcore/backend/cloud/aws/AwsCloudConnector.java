package com.sentinelcore.backend.cloud.aws;

import com.sentinelcore.backend.cloud.*;
import com.sentinelcore.backend.cloud.CloudModels.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * AWS implementation of {@link CloudProviderConnector}. All AWS SDK usage is confined to this
 * package. Every operation first confirms that the resolved credentials belong to the registered
 * account, so a misconfigured role can never act on a different account.
 */
@Component
@Slf4j
public class AwsCloudConnector implements CloudProviderConnector {

    private final AwsSettings settings;
    private final AwsClientFactory clientFactory;
    private final Clock clock;
    private final AwsS3Assessor s3Assessor;
    private final AwsSecurityGroupAssessor sgAssessor;

    @Autowired
    public AwsCloudConnector(AwsSettings settings, AwsClientFactory clientFactory) {
        this(settings, clientFactory, Clock.systemUTC());
    }

    AwsCloudConnector(AwsSettings settings, AwsClientFactory clientFactory, Clock clock) {
        this.settings = settings;
        this.clientFactory = clientFactory;
        this.clock = clock;
        this.s3Assessor = new AwsS3Assessor(clock);
        this.sgAssessor = new AwsSecurityGroupAssessor(clock);
    }

    @Override
    public CloudProvider getProvider() {
        return CloudProvider.AWS;
    }

    @Override
    public ConnectionValidation validateConnection(AccountContext account) {
        GetCallerIdentityResponse identity = verifiedIdentity(account);
        return new ConnectionValidation(identity.account(), identity.arn(), clock.instant());
    }

    @Override
    public List<DiscoveredAccount> listAccounts(AccountContext account) {
        // Organizations-wide discovery is not implemented; report the account the credentials resolve to.
        GetCallerIdentityResponse identity = verifiedIdentity(account);
        return List.of(new DiscoveredAccount(identity.account(), identity.arn()));
    }

    @Override
    public List<ResourceObservation> listResources(AccountContext account) {
        verifiedIdentity(account);
        List<ResourceObservation> resources = new ArrayList<>();
        try (S3Client s3 = clientFactory.s3(account)) {
            for (AwsS3Assessor.BucketRef bucket : s3Assessor.listBuckets(s3, account.externalAccountId())) {
                resources.add(s3Assessor.toResource(account.externalAccountId(), bucket));
            }
        } catch (RuntimeException e) {
            throw AwsErrorMapper.map("ListBuckets", e);
        }
        for (String region : regions(account)) {
            try (Ec2Client ec2 = clientFactory.ec2(account, region)) {
                for (SecurityGroup group : sgAssessor.listSecurityGroups(ec2, null)) {
                    resources.add(sgAssessor.toResource(account.externalAccountId(), region, group));
                }
            } catch (RuntimeException e) {
                throw AwsErrorMapper.map("DescribeSecurityGroups in " + region, e);
            }
        }
        return resources;
    }

    @Override
    public AssessmentResult collectSecurityFindings(AccountContext account) {
        verifiedIdentity(account);
        String accountId = account.externalAccountId();
        List<ResourceObservation> resources = new ArrayList<>();
        List<FindingObservation> findings = new ArrayList<>();
        List<ScopeResult> scopes = new ArrayList<>();

        // S3 is a global listing; one failed bucket check makes the whole scope incomplete so that no
        // S3 finding is resolved on the basis of missing data.
        try (S3Client s3 = clientFactory.s3(account)) {
            List<AwsS3Assessor.BucketRef> buckets = s3Assessor.listBuckets(s3, accountId);
            List<String> bucketErrors = new ArrayList<>();
            CloudErrorType firstErrorType = null;
            for (AwsS3Assessor.BucketRef bucket : buckets) {
                resources.add(s3Assessor.toResource(accountId, bucket));
                try {
                    findings.addAll(s3Assessor.assessBucket(s3, accountId, bucket));
                } catch (RuntimeException e) {
                    CloudProviderException mapped = AwsErrorMapper.map("S3 checks for bucket " + bucket.name(), e);
                    if (firstErrorType == null) {
                        firstErrorType = mapped.getErrorType();
                    }
                    bucketErrors.add(mapped.getMessage());
                }
            }
            scopes.add(bucketErrors.isEmpty()
                    ? new ScopeResult(AwsS3Assessor.SCOPE, true, null, null)
                    : new ScopeResult(AwsS3Assessor.SCOPE, false, firstErrorType, summarize(bucketErrors)));
        } catch (RuntimeException e) {
            CloudProviderException mapped = AwsErrorMapper.map("ListBuckets", e);
            scopes.add(new ScopeResult(AwsS3Assessor.SCOPE, false, mapped.getErrorType(), mapped.getMessage()));
        }

        for (String region : regions(account)) {
            String scope = AwsSecurityGroupAssessor.scope(region);
            try (Ec2Client ec2 = clientFactory.ec2(account, region)) {
                for (SecurityGroup group : sgAssessor.listSecurityGroups(ec2, null)) {
                    resources.add(sgAssessor.toResource(accountId, region, group));
                    findings.addAll(sgAssessor.assess(accountId, region, group));
                }
                scopes.add(new ScopeResult(scope, true, null, null));
            } catch (RuntimeException e) {
                CloudProviderException mapped = AwsErrorMapper.map("DescribeSecurityGroups in " + region, e);
                scopes.add(new ScopeResult(scope, false, mapped.getErrorType(), mapped.getMessage()));
            }
        }

        return new AssessmentResult(resources, findings, scopes);
    }

    @Override
    public AssessmentResult evaluateResourceConfiguration(AccountContext account, RemediationTarget target) {
        verifiedIdentity(account);
        String accountId = account.externalAccountId();
        switch (target.resourceType()) {
            case AwsS3Assessor.RESOURCE_TYPE -> {
                String bucket = AwsS3Assessor.bucketNameFromArn(target.resourceId());
                AwsS3Assessor.BucketRef ref = new AwsS3Assessor.BucketRef(bucket, target.region());
                try (S3Client s3 = clientFactory.s3(account)) {
                    List<FindingObservation> findings = s3Assessor.assessBucket(s3, accountId, ref);
                    return new AssessmentResult(List.of(s3Assessor.toResource(accountId, ref)), findings,
                            List.of(new ScopeResult(AwsS3Assessor.SCOPE, true, null, null)));
                } catch (RuntimeException e) {
                    throw AwsErrorMapper.map("S3 checks for bucket " + bucket, e);
                }
            }
            case AwsSecurityGroupAssessor.RESOURCE_TYPE -> {
                String groupId = groupId(target.resourceId());
                try (Ec2Client ec2 = clientFactory.ec2(account, target.region())) {
                    List<ResourceObservation> resources = new ArrayList<>();
                    List<FindingObservation> findings = new ArrayList<>();
                    for (SecurityGroup group : sgAssessor.listSecurityGroups(ec2, List.of(groupId))) {
                        resources.add(sgAssessor.toResource(accountId, target.region(), group));
                        findings.addAll(sgAssessor.assess(accountId, target.region(), group));
                    }
                    return new AssessmentResult(resources, findings,
                            List.of(new ScopeResult(AwsSecurityGroupAssessor.scope(target.region()), true, null, null)));
                } catch (RuntimeException e) {
                    throw AwsErrorMapper.map("DescribeSecurityGroups for " + groupId, e);
                }
            }
            default -> throw new UnsupportedCloudOperationException(
                    "Resource type " + target.resourceType() + " cannot be evaluated");
        }
    }

    @Override
    public RemediationPreview previewRemediation(AccountContext account, RemediationActionType action,
                                                 RemediationTarget target) {
        requireSupported(action);
        verifiedIdentity(account);
        String bucket = AwsS3Assessor.bucketNameFromArn(target.resourceId());
        try (S3Client s3 = clientFactory.s3(account)) {
            var current = s3Assessor.getPublicAccessBlock(s3, account.externalAccountId(), bucket);
            return new RemediationPreview(
                    PreviewType.LOCAL_VALIDATION,
                    "Bucket " + bucket + ": " + AwsS3Assessor.describe(current),
                    "PutPublicAccessBlock on bucket " + bucket + " with BlockPublicAcls=true, IgnorePublicAcls=true, "
                            + "BlockPublicPolicy=true, RestrictPublicBuckets=true (ExpectedBucketOwner="
                            + account.externalAccountId() + ")",
                    "Local validation only: S3 has no provider dry-run for this call. Applying it stops any "
                            + "intentional public access (for example static website hosting). The change is "
                            + "reversible by editing the bucket's Block Public Access settings.");
        } catch (RuntimeException e) {
            throw AwsErrorMapper.map("GetPublicAccessBlock for bucket " + bucket, e);
        }
    }

    @Override
    public ExecutionResult executeApprovedRemediation(AccountContext account, RemediationActionType action,
                                                      RemediationTarget target) {
        requireSupported(action);
        verifiedIdentity(account);
        String bucket = AwsS3Assessor.bucketNameFromArn(target.resourceId());
        try (S3Client s3 = clientFactory.s3(account)) {
            return s3Assessor.blockPublicAccess(s3, account.externalAccountId(), bucket);
        } catch (AwsServiceException e) {
            // AWS returned a definitive error: the change was not applied
            throw AwsErrorMapper.map("PutPublicAccessBlock for bucket " + bucket, e);
        } catch (RuntimeException e) {
            // Timeouts / connection failures: AWS may or may not have applied the change
            CloudProviderException mapped = AwsErrorMapper.map("PutPublicAccessBlock for bucket " + bucket, e);
            throw new CloudProviderException(CloudErrorType.OUTCOME_UNKNOWN,
                    mapped.getMessage() + "; the outcome is unknown and must be verified", e);
        }
    }

    @Override
    public VerificationResult verifyRemediation(AccountContext account, RemediationActionType action,
                                                RemediationTarget target) {
        requireSupported(action);
        verifiedIdentity(account);
        String bucket = AwsS3Assessor.bucketNameFromArn(target.resourceId());
        try (S3Client s3 = clientFactory.s3(account)) {
            var current = s3Assessor.getPublicAccessBlock(s3, account.externalAccountId(), bucket);
            return new VerificationResult(AwsS3Assessor.isFullyBlocked(current),
                    AwsS3Assessor.describe(current), clock.instant());
        } catch (RuntimeException e) {
            throw AwsErrorMapper.map("GetPublicAccessBlock for bucket " + bucket, e);
        }
    }

    /** Fails unless AWS is enabled and the credentials resolve to the registered account. */
    private GetCallerIdentityResponse verifiedIdentity(AccountContext account) {
        if (!settings.isEnabled()) {
            throw new CloudProviderException(CloudErrorType.NOT_CONFIGURED,
                    "AWS integration is disabled (set APP_CLOUD_AWS_ENABLED=true to enable it)");
        }
        GetCallerIdentityResponse identity;
        try (StsClient sts = clientFactory.sts(account)) {
            identity = sts.getCallerIdentity();
        } catch (RuntimeException e) {
            throw AwsErrorMapper.map("GetCallerIdentity", e);
        }
        if (!account.externalAccountId().equals(identity.account())) {
            throw new CloudProviderException(CloudErrorType.INVALID_CONFIGURATION,
                    "Credentials resolve to AWS account " + identity.account()
                            + ", not the registered account " + account.externalAccountId());
        }
        return identity;
    }

    private List<String> regions(AccountContext account) {
        return account.regions() == null || account.regions().isEmpty()
                ? List.of(settings.getDefaultRegion()) : account.regions();
    }

    private static void requireSupported(RemediationActionType action) {
        if (action != RemediationActionType.S3_BLOCK_PUBLIC_ACCESS) {
            throw new UnsupportedCloudOperationException("Remediation " + action + " is not supported by the AWS connector");
        }
    }

    private static String groupId(String arn) {
        try {
            return AwsSecurityGroupAssessor.groupIdFromArn(arn);
        } catch (IllegalArgumentException e) {
            throw new CloudProviderException(CloudErrorType.INVALID_CONFIGURATION, "Not a security group ARN");
        }
    }

    private static String summarize(List<String> errors) {
        String joined = String.join("; ", errors.subList(0, Math.min(5, errors.size())));
        return errors.size() > 5 ? joined + "; and " + (errors.size() - 5) + " more" : joined;
    }
}
