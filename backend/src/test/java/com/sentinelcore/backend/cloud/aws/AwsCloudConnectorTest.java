package com.sentinelcore.backend.cloud.aws;

import com.sentinelcore.backend.cloud.CloudErrorType;
import com.sentinelcore.backend.cloud.CloudModels.*;
import com.sentinelcore.backend.cloud.CloudProvider;
import com.sentinelcore.backend.cloud.CloudProviderException;
import com.sentinelcore.backend.cloud.RemediationActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;
import software.amazon.awssdk.services.sts.model.StsException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Connector behaviour against mocked AWS SDK clients. No AWS credentials or network access needed.
 */
class AwsCloudConnectorTest {

    private static final String ACCOUNT_ID = "123456789012";
    private static final AccountContext ACCOUNT = new AccountContext(CloudProvider.AWS, ACCOUNT_ID,
            AuthMode.ASSUME_ROLE, "arn:aws:iam::123456789012:role/SentinelCoreReadOnly", "ext-id-123",
            List.of("us-east-1", "eu-west-1"));

    private final AwsClientFactory factory = mock(AwsClientFactory.class);
    private final StsClient sts = mock(StsClient.class);
    private final S3Client s3 = mock(S3Client.class);
    private final Ec2Client ec2UsEast = mock(Ec2Client.class);
    private final Ec2Client ec2EuWest = mock(Ec2Client.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC);

    private AwsCloudConnector connector;

    @BeforeEach
    void setUp() {
        connector = new AwsCloudConnector(settings(true), factory, clock);
        when(factory.sts(any())).thenReturn(sts);
        when(factory.s3(any())).thenReturn(s3);
        when(factory.ec2(any(), eq("us-east-1"))).thenReturn(ec2UsEast);
        when(factory.ec2(any(), eq("eu-west-1"))).thenReturn(ec2EuWest);
        when(sts.getCallerIdentity()).thenReturn(GetCallerIdentityResponse.builder()
                .account(ACCOUNT_ID).arn("arn:aws:sts::123456789012:assumed-role/SentinelCoreReadOnly/sentinelcore").build());

        when(s3.listBuckets(any(ListBucketsRequest.class))).thenReturn(ListBucketsResponse.builder().build());
        when(ec2UsEast.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
                .thenReturn(DescribeSecurityGroupsResponse.builder().build());
        when(ec2EuWest.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
                .thenReturn(DescribeSecurityGroupsResponse.builder().build());
    }

    private static AwsSettings settings(boolean enabled) {
        return new AwsSettings(enabled, "us-east-1", 30, 10, 900);
    }

    private static S3Exception s3Error(String code, int status) {
        return (S3Exception) S3Exception.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(code).build())
                .statusCode(status).requestId("req-1").build();
    }

    private static Ec2Exception ec2Error(String code, int status) {
        return (Ec2Exception) Ec2Exception.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(code).build())
                .statusCode(status).build();
    }

    // ------------------------------------------------------------ configuration & identity

    @Test
    void disabledIntegrationMakesNoAwsCalls() {
        AwsCloudConnector disabled = new AwsCloudConnector(settings(false), factory, clock);
        CloudProviderException e = assertThrows(CloudProviderException.class, () -> disabled.validateConnection(ACCOUNT));
        assertEquals(CloudErrorType.NOT_CONFIGURED, e.getErrorType());
        verifyNoInteractions(factory);
    }

    @Test
    void credentialsForDifferentAccountAreRejected() {
        when(sts.getCallerIdentity()).thenReturn(GetCallerIdentityResponse.builder()
                .account("999999999999").arn("arn:aws:sts::999999999999:assumed-role/x/y").build());

        CloudProviderException e = assertThrows(CloudProviderException.class, () -> connector.collectSecurityFindings(ACCOUNT));
        assertEquals(CloudErrorType.INVALID_CONFIGURATION, e.getErrorType());
        verify(factory, never()).s3(any());
    }

    @Test
    void expiredCredentialsAreClassified() {
        when(sts.getCallerIdentity()).thenThrow(StsException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("ExpiredToken").build()).statusCode(400).build());

        CloudProviderException e = assertThrows(CloudProviderException.class, () -> connector.validateConnection(ACCOUNT));
        assertEquals(CloudErrorType.CREDENTIALS_EXPIRED, e.getErrorType());
    }

    @Test
    void validateConnectionReturnsResolvedIdentity() {
        ConnectionValidation result = connector.validateConnection(ACCOUNT);
        assertEquals(ACCOUNT_ID, result.resolvedAccountId());
        assertTrue(result.callerIdentity().contains("SentinelCoreReadOnly"));
    }

    // ------------------------------------------------------------ security groups

    @Test
    void securityGroupsArePaginatedAndOpenRulesReported() {
        SecurityGroup sshOpen = SecurityGroup.builder().groupId("sg-1").groupName("ssh").vpcId("vpc-1")
                .ipPermissions(IpPermission.builder().ipProtocol("tcp").fromPort(22).toPort(22)
                        .ipRanges(IpRange.builder().cidrIp("0.0.0.0/0").build()).build())
                .build();
        SecurityGroup allOpen = SecurityGroup.builder().groupId("sg-2").groupName("all").vpcId("vpc-1")
                .ipPermissions(IpPermission.builder().ipProtocol("-1")
                        .ipv6Ranges(Ipv6Range.builder().cidrIpv6("::/0").build()).build())
                .build();
        SecurityGroup httpsOnly = SecurityGroup.builder().groupId("sg-3").groupName("web").vpcId("vpc-1")
                .ipPermissions(IpPermission.builder().ipProtocol("tcp").fromPort(443).toPort(443)
                        .ipRanges(IpRange.builder().cidrIp("0.0.0.0/0").build()).build())
                .build();
        SecurityGroup privateDb = SecurityGroup.builder().groupId("sg-4").groupName("db").vpcId("vpc-1")
                .ipPermissions(IpPermission.builder().ipProtocol("tcp").fromPort(5432).toPort(5432)
                        .ipRanges(IpRange.builder().cidrIp("10.0.0.0/8").build()).build())
                .build();

        when(ec2UsEast.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class))).thenAnswer(inv -> {
            DescribeSecurityGroupsRequest req = inv.getArgument(0);
            if (req.nextToken() == null) {
                return DescribeSecurityGroupsResponse.builder().securityGroups(sshOpen, httpsOnly).nextToken("page-2").build();
            }
            assertEquals("page-2", req.nextToken());
            return DescribeSecurityGroupsResponse.builder().securityGroups(allOpen, privateDb).build();
        });

        AssessmentResult result = connector.collectSecurityFindings(ACCOUNT);

        verify(ec2UsEast, times(2)).describeSecurityGroups(any(DescribeSecurityGroupsRequest.class));
        assertEquals(4, result.resources().stream().filter(r -> r.region().equals("us-east-1")).count());
        assertTrue(result.isComplete());

        List<FindingObservation> findings = result.findings();
        assertEquals(2, findings.size(), "only internet-facing sensitive/all-traffic rules are findings");
        FindingObservation ssh = findings.stream().filter(f -> f.findingType().equals("EC2_SG_SENSITIVE_PORT_OPEN")).findFirst().orElseThrow();
        assertEquals(Severity.HIGH, ssh.severity());
        assertEquals("arn:aws:ec2:us-east-1:123456789012:security-group/sg-1", ssh.resourceId());
        assertEquals("22/SSH", ssh.evidence().get("sensitivePorts"));
        assertEquals("AWS EC2 DescribeSecurityGroups", ssh.source());
        assertEquals(Instant.parse("2026-10-01T12:00:00Z"), ssh.observedAt());

        FindingObservation all = findings.stream().filter(f -> f.findingType().equals("EC2_SG_ALL_TRAFFIC_OPEN")).findFirst().orElseThrow();
        assertEquals(Severity.CRITICAL, all.severity());
    }

    @Test
    void regionalFailureProducesPartialResult() {
        when(ec2EuWest.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
                .thenThrow(ec2Error("UnauthorizedOperation", 403));

        AssessmentResult result = connector.collectSecurityFindings(ACCOUNT);

        assertFalse(result.isComplete());
        ScopeResult eu = result.scopes().stream().filter(s -> s.scope().equals("ec2:eu-west-1")).findFirst().orElseThrow();
        assertFalse(eu.complete());
        assertEquals(CloudErrorType.ACCESS_DENIED, eu.errorType());
        assertTrue(result.scopes().stream().filter(s -> s.scope().equals("ec2:us-east-1")).findFirst().orElseThrow().complete());
        assertTrue(result.scopes().stream().filter(s -> s.scope().equals("s3")).findFirst().orElseThrow().complete());
    }

    @Test
    void throttlingIsClassified() {
        when(ec2UsEast.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
                .thenThrow(ec2Error("RequestLimitExceeded", 503));

        ScopeResult us = connector.collectSecurityFindings(ACCOUNT).scopes().stream()
                .filter(s -> s.scope().equals("ec2:us-east-1")).findFirst().orElseThrow();
        assertEquals(CloudErrorType.THROTTLED, us.errorType());
    }

    // ------------------------------------------------------------ S3

    private void givenBucket(String name) {
        when(s3.listBuckets(any(ListBucketsRequest.class))).thenReturn(ListBucketsResponse.builder()
                .buckets(Bucket.builder().name(name).bucketRegion("eu-west-1").build()).build());
        when(s3.getBucketAcl(any(GetBucketAclRequest.class))).thenReturn(GetBucketAclResponse.builder().build());
        when(s3.getBucketEncryption(any(GetBucketEncryptionRequest.class))).thenReturn(GetBucketEncryptionResponse.builder()
                .serverSideEncryptionConfiguration(ServerSideEncryptionConfiguration.builder()
                        .rules(ServerSideEncryptionRule.builder().applyServerSideEncryptionByDefault(
                                ServerSideEncryptionByDefault.builder().sseAlgorithm("AES256").build()).build())
                        .build())
                .build());
    }

    private static PublicAccessBlockConfiguration fullBlock() {
        return PublicAccessBlockConfiguration.builder().blockPublicAcls(true).ignorePublicAcls(true)
                .blockPublicPolicy(true).restrictPublicBuckets(true).build();
    }

    @Test
    void bucketWithoutPublicAccessBlockAndPublicPolicyIsReported() {
        givenBucket("open-bucket");
        when(s3.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class)))
                .thenThrow(s3Error("NoSuchPublicAccessBlockConfiguration", 404));
        when(s3.getBucketPolicyStatus(any(GetBucketPolicyStatusRequest.class))).thenReturn(GetBucketPolicyStatusResponse.builder()
                .policyStatus(PolicyStatus.builder().isPublic(true).build()).build());

        AssessmentResult result = connector.collectSecurityFindings(ACCOUNT);

        assertEquals(2, result.findings().size());
        FindingObservation pab = result.findings().stream()
                .filter(f -> f.findingType().equals("S3_PUBLIC_ACCESS_BLOCK_DISABLED")).findFirst().orElseThrow();
        assertEquals(Severity.MEDIUM, pab.severity());
        assertEquals("arn:aws:s3:::open-bucket", pab.resourceId());
        assertEquals("eu-west-1", pab.region());
        assertEquals(RemediationActionType.S3_BLOCK_PUBLIC_ACCESS, pab.supportedRemediation());
        assertTrue(pab.observedFact().contains("NoSuchPublicAccessBlockConfiguration"));
        assertTrue(pab.inferredRisk().contains("Account-level Block Public Access was not evaluated"));

        FindingObservation policy = result.findings().stream()
                .filter(f -> f.findingType().equals("S3_BUCKET_POLICY_PUBLIC")).findFirst().orElseThrow();
        assertEquals(Severity.CRITICAL, policy.severity());

        // Every S3 call is pinned to the expected owner account
        verify(s3).getPublicAccessBlock(argThat((GetPublicAccessBlockRequest r) -> ACCOUNT_ID.equals(r.expectedBucketOwner())));
    }

    @Test
    void fullyBlockedBucketHasNoFindingsEvenIfPolicyIsPublic() {
        givenBucket("locked-bucket");
        when(s3.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class))).thenReturn(
                GetPublicAccessBlockResponse.builder().publicAccessBlockConfiguration(fullBlock()).build());
        when(s3.getBucketPolicyStatus(any(GetBucketPolicyStatusRequest.class))).thenReturn(GetBucketPolicyStatusResponse.builder()
                .policyStatus(PolicyStatus.builder().isPublic(true).build()).build());

        assertTrue(connector.collectSecurityFindings(ACCOUNT).findings().isEmpty());
    }

    @Test
    void bucketCheckFailureMarksS3ScopeIncomplete() {
        givenBucket("denied-bucket");
        when(s3.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class))).thenThrow(s3Error("AccessDenied", 403));

        AssessmentResult result = connector.collectSecurityFindings(ACCOUNT);

        ScopeResult s3Scope = result.scopes().stream().filter(s -> s.scope().equals("s3")).findFirst().orElseThrow();
        assertFalse(s3Scope.complete());
        assertEquals(CloudErrorType.ACCESS_DENIED, s3Scope.errorType());
        assertTrue(s3Scope.errorMessage().contains("AccessDenied"));
        assertEquals(1, result.resources().stream().filter(r -> r.resourceType().equals("s3:bucket")).count(),
                "inventory is still recorded when a check fails");
    }

    @Test
    void bucketListingIsPaginated() {
        when(s3.listBuckets(any(ListBucketsRequest.class))).thenAnswer(inv -> {
            ListBucketsRequest req = inv.getArgument(0);
            if (req.continuationToken() == null) {
                return ListBucketsResponse.builder().buckets(Bucket.builder().name("bucket-a").bucketRegion("us-east-1").build())
                        .continuationToken("next").build();
            }
            return ListBucketsResponse.builder().buckets(Bucket.builder().name("bucket-b").bucketRegion("us-east-1").build()).build();
        });

        List<ResourceObservation> resources = connector.listResources(ACCOUNT);
        assertEquals(List.of("bucket-a", "bucket-b"),
                resources.stream().filter(r -> r.resourceType().equals("s3:bucket")).map(ResourceObservation::name).toList());
    }

    // ------------------------------------------------------------ remediation

    private static final RemediationTarget BUCKET = new RemediationTarget("s3:bucket", "arn:aws:s3:::open-bucket", "eu-west-1");

    @Test
    void previewIsLabelledAsLocalValidation() {
        when(s3.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class)))
                .thenThrow(s3Error("NoSuchPublicAccessBlockConfiguration", 404));

        RemediationPreview preview = connector.previewRemediation(ACCOUNT, RemediationActionType.S3_BLOCK_PUBLIC_ACCESS, BUCKET);

        assertEquals(PreviewType.LOCAL_VALIDATION, preview.previewType());
        assertTrue(preview.notes().contains("no provider dry-run"));
        verify(s3, never()).putPublicAccessBlock(any(PutPublicAccessBlockRequest.class));
    }

    @Test
    void executeAppliesAllFourSettingsWithExpectedOwner() {
        when(s3.putPublicAccessBlock(any(PutPublicAccessBlockRequest.class))).thenReturn(
                (PutPublicAccessBlockResponse) PutPublicAccessBlockResponse.builder().build());

        ExecutionResult result = connector.executeApprovedRemediation(ACCOUNT, RemediationActionType.S3_BLOCK_PUBLIC_ACCESS, BUCKET);

        assertTrue(result.success());
        verify(s3).putPublicAccessBlock(argThat((PutPublicAccessBlockRequest r) ->
                "open-bucket".equals(r.bucket())
                        && ACCOUNT_ID.equals(r.expectedBucketOwner())
                        && r.publicAccessBlockConfiguration().blockPublicAcls()
                        && r.publicAccessBlockConfiguration().ignorePublicAcls()
                        && r.publicAccessBlockConfiguration().blockPublicPolicy()
                        && r.publicAccessBlockConfiguration().restrictPublicBuckets()));
    }

    @Test
    void executeTimeoutIsOutcomeUnknownButAccessDeniedIsDefinitive() {
        when(s3.putPublicAccessBlock(any(PutPublicAccessBlockRequest.class)))
                .thenThrow(ApiCallTimeoutException.builder().message("timed out").build());
        CloudProviderException timeout = assertThrows(CloudProviderException.class,
                () -> connector.executeApprovedRemediation(ACCOUNT, RemediationActionType.S3_BLOCK_PUBLIC_ACCESS, BUCKET));
        assertEquals(CloudErrorType.OUTCOME_UNKNOWN, timeout.getErrorType());

        reset(s3);
        when(s3.putPublicAccessBlock(any(PutPublicAccessBlockRequest.class))).thenThrow(s3Error("AccessDenied", 403));
        CloudProviderException denied = assertThrows(CloudProviderException.class,
                () -> connector.executeApprovedRemediation(ACCOUNT, RemediationActionType.S3_BLOCK_PUBLIC_ACCESS, BUCKET));
        assertEquals(CloudErrorType.ACCESS_DENIED, denied.getErrorType());
    }

    @Test
    void verificationReflectsObservedState() {
        when(s3.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class))).thenReturn(
                GetPublicAccessBlockResponse.builder().publicAccessBlockConfiguration(fullBlock()).build());
        assertTrue(connector.verifyRemediation(ACCOUNT, RemediationActionType.S3_BLOCK_PUBLIC_ACCESS, BUCKET).verified());

        when(s3.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class))).thenReturn(
                GetPublicAccessBlockResponse.builder().publicAccessBlockConfiguration(
                        fullBlock().toBuilder().restrictPublicBuckets(false).build()).build());
        assertFalse(connector.verifyRemediation(ACCOUNT, RemediationActionType.S3_BLOCK_PUBLIC_ACCESS, BUCKET).verified());
    }

    @Test
    void invalidBucketArnIsRejectedBeforeAnyMutation() {
        RemediationTarget bad = new RemediationTarget("s3:bucket", "arn:aws:s3:::Bad_Bucket/../x", "us-east-1");
        assertThrows(CloudProviderException.class,
                () -> connector.executeApprovedRemediation(ACCOUNT, RemediationActionType.S3_BLOCK_PUBLIC_ACCESS, bad));
        verify(s3, never()).putPublicAccessBlock(any(PutPublicAccessBlockRequest.class));
    }
}
