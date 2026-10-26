package com.sentinelcore.backend.controller;

import com.sentinelcore.backend.cloud.aws.AwsClientFactory;
import com.sentinelcore.backend.entity.CloudFinding;
import com.sentinelcore.backend.entity.Role;
import com.sentinelcore.backend.entity.User;
import com.sentinelcore.backend.repository.AuditLogRepository;
import com.sentinelcore.backend.repository.CloudFindingRepository;
import com.sentinelcore.backend.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end remediation workflow through the real controllers, services and AWS connector, with
 * the AWS SDK clients mocked. No real AWS account is contacted.
 */
@TestPropertySource(properties = {
        "app.cloud.aws.enabled=true",
        "app.cloud.remediation.execution-enabled=true"
})
class CloudRemediationWorkflowIntegrationTest extends IntegrationTestSupport {

    private static final String AWS_ACCOUNT = "123456789012";

    @MockitoBean
    private AwsClientFactory awsClientFactory;

    @Autowired
    private CloudFindingRepository findingRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final StsClient sts = mock(StsClient.class);
    private final S3Client s3 = mock(S3Client.class);
    private final Ec2Client ec2 = mock(Ec2Client.class);
    private final AtomicBoolean blocked = new AtomicBoolean(false);

    private String tenant;
    private String admin;
    private String requester;
    private String approver;
    private String analyst;

    @BeforeEach
    void setUp() {
        tenant = "wf-" + UUID.randomUUID().toString().substring(0, 8);
        admin = bearer(createUser(Role.ADMIN, tenant));
        User requesterUser = createUser(Role.VULNERABILITY_MANAGER, tenant);
        requester = bearer(requesterUser);
        approver = bearer(createUser(Role.SUPER_ADMIN, tenant));
        analyst = bearer(createUser(Role.SECURITY_ANALYST, tenant));

        when(awsClientFactory.sts(any())).thenReturn(sts);
        when(awsClientFactory.s3(any())).thenReturn(s3);
        when(awsClientFactory.ec2(any(), any())).thenReturn(ec2);
        when(sts.getCallerIdentity()).thenReturn(GetCallerIdentityResponse.builder()
                .account(AWS_ACCOUNT).arn("arn:aws:sts::123456789012:assumed-role/SentinelCoreReadOnly/test").build());
        when(ec2.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
                .thenReturn(DescribeSecurityGroupsResponse.builder().build());

        when(s3.listBuckets(any(ListBucketsRequest.class))).thenReturn(ListBucketsResponse.builder()
                .buckets(Bucket.builder().name("public-bucket").bucketRegion("us-east-1").build()).build());
        when(s3.getBucketPolicyStatus(any(GetBucketPolicyStatusRequest.class))).thenThrow(S3Exception.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchBucketPolicy").build()).statusCode(404).build());
        when(s3.getBucketAcl(any(GetBucketAclRequest.class))).thenReturn(GetBucketAclResponse.builder().build());
        when(s3.getBucketEncryption(any(GetBucketEncryptionRequest.class))).thenReturn(GetBucketEncryptionResponse.builder()
                .serverSideEncryptionConfiguration(ServerSideEncryptionConfiguration.builder().rules(
                        ServerSideEncryptionRule.builder().applyServerSideEncryptionByDefault(
                                ServerSideEncryptionByDefault.builder().sseAlgorithm("AES256").build()).build()).build())
                .build());
        // Simulated bucket state: Public Access Block is absent until PutPublicAccessBlock succeeds
        when(s3.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class))).thenAnswer(inv -> {
            if (!blocked.get()) {
                throw S3Exception.builder().awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("NoSuchPublicAccessBlockConfiguration").build()).statusCode(404).build();
            }
            return GetPublicAccessBlockResponse.builder().publicAccessBlockConfiguration(
                    PublicAccessBlockConfiguration.builder().blockPublicAcls(true).ignorePublicAcls(true)
                            .blockPublicPolicy(true).restrictPublicBuckets(true).build()).build();
        });
    }

    private String registerAccount() throws Exception {
        String body = mockMvc.perform(post("/api/cloud/accounts").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test account\",\"provider\":\"AWS\",\"externalAccountId\":\"" + AWS_ACCOUNT + "\","
                                + "\"authMode\":\"ASSUME_ROLE\",\"roleArn\":\"arn:aws:iam::123456789012:role/SentinelCoreReadOnly\","
                                + "\"regions\":[\"us-east-1\"]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"data\":\\{\"id\":(\\d+).*", "$1");
    }

    private Long scanAndGetFindingId(String accountId) throws Exception {
        mockMvc.perform(post("/api/cloud/accounts/" + accountId + "/scans").header("Authorization", analyst))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.findingsObserved").value(1));
        List<CloudFinding> findings = findingRepository.findByCloudAccountId(Long.valueOf(accountId));
        assertEquals(1, findings.size());
        return findings.get(0).getId();
    }

    private String[] requestRemediation(Long findingId, String key) throws Exception {
        String body = mockMvc.perform(post("/api/cloud/remediations").header("Authorization", requester)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"findingId\":" + findingId + ",\"actionType\":\"S3_BLOCK_PUBLIC_ACCESS\","
                                + "\"justification\":\"Bucket must not be public\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();
        return new String[]{
                body.replaceAll(".*\"data\":\\{\"id\":(\\d+).*", "$1"),
                body.replaceAll(".*\"requestHash\":\"([a-f0-9]{64})\".*", "$1")};
    }

    @Test
    void fullWorkflowFromScanToVerifiedRemediation() throws Exception {
        String accountId = registerAccount();

        mockMvc.perform(post("/api/cloud/accounts/" + accountId + "/verify").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.connectionStatus").value("CONNECTED"));

        Long findingId = scanAndGetFindingId(accountId);
        CloudFinding first = findingRepository.findById(findingId).orElseThrow();

        // Re-scan is idempotent: same finding row, first detection time preserved
        scanAndGetFindingId(accountId);
        CloudFinding again = findingRepository.findById(findingId).orElseThrow();
        assertEquals(first.getFirstDetectedAt(), again.getFirstDetectedAt());

        mockMvc.perform(get("/api/cloud/accounts/" + accountId + "/resources").header("Authorization", analyst))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content", hasSize(1)));

        String key = "wf-" + UUID.randomUUID();
        String[] created = requestRemediation(findingId, key);
        String id = created[0];
        String hash = created[1];

        // Same idempotency key returns the same request instead of creating a duplicate
        assertEquals(id, requestRemediation(findingId, key)[0]);
        // A second active remediation for the same finding is refused
        mockMvc.perform(post("/api/cloud/remediations").header("Authorization", requester)
                        .header("Idempotency-Key", "wf-other-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"findingId\":" + findingId + ",\"actionType\":\"S3_BLOCK_PUBLIC_ACCESS\"}"))
                .andExpect(status().isConflict());

        // Separation of duties: the requester holds REMEDIATION_APPROVE but cannot approve their own request
        mockMvc.perform(post("/api/cloud/remediations/" + id + "/approve").header("Authorization", requester)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"requestHash\":\"" + hash + "\"}"))
                .andExpect(status().isForbidden());
        // Approval is bound to the exact request hash
        mockMvc.perform(post("/api/cloud/remediations/" + id + "/approve").header("Authorization", approver)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"requestHash\":\"" + "f".repeat(64) + "\"}"))
                .andExpect(status().isConflict());
        // Cannot execute before approval
        mockMvc.perform(post("/api/cloud/remediations/" + id + "/execute").header("Authorization", requester)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmResourceId\":\"public-bucket\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/cloud/remediations/" + id + "/preview").header("Authorization", approver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previewType").value("LOCAL_VALIDATION"))
                .andExpect(jsonPath("$.data.currentStateAvailable").value(true));

        mockMvc.perform(post("/api/cloud/remediations/" + id + "/approve").header("Authorization", approver)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"requestHash\":\"" + hash + "\",\"comment\":\"ok\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"));

        // Confirmation must name the target
        mockMvc.perform(post("/api/cloud/remediations/" + id + "/execute").header("Authorization", requester)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmResourceId\":\"some-other-bucket\"}"))
                .andExpect(status().isBadRequest());
        verify(s3, never()).putPublicAccessBlock(any(PutPublicAccessBlockRequest.class));

        when(s3.putPublicAccessBlock(any(PutPublicAccessBlockRequest.class))).thenAnswer(inv -> {
            blocked.set(true);
            return PutPublicAccessBlockResponse.builder().build();
        });

        mockMvc.perform(post("/api/cloud/remediations/" + id + "/execute").header("Authorization", requester)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmResourceId\":\"public-bucket\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"))
                .andExpect(jsonPath("$.data.executionAttempts").value(1));

        CloudFinding resolved = findingRepository.findById(findingId).orElseThrow();
        assertEquals(CloudFinding.Status.RESOLVED, resolved.getStatus());
        assertTrue(resolved.getResolutionReason().contains("Verified by post-remediation check"));

        // Duplicate execution is refused and the provider was called exactly once
        mockMvc.perform(post("/api/cloud/remediations/" + id + "/execute").header("Authorization", requester)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmResourceId\":\"public-bucket\"}"))
                .andExpect(status().isConflict());
        verify(s3, times(1)).putPublicAccessBlock(any(PutPublicAccessBlockRequest.class));

        for (String action : List.of("REMEDIATION_REQUESTED", "REMEDIATION_SOD_VIOLATION", "REMEDIATION_APPROVED",
                "REMEDIATION_EXECUTION_STARTED", "REMEDIATION_VERIFIED", "CLOUD_SCAN_COMPLETED")) {
            assertTrue(auditLogRepository.findAll().stream().anyMatch(l -> action.equals(l.getAction())),
                    "missing audit event " + action);
        }
    }

    @Test
    void uncertainExecutionIsNotTreatedAsSuccess() throws Exception {
        String accountId = registerAccount();
        Long findingId = scanAndGetFindingId(accountId);
        String[] created = requestRemediation(findingId, "wf-timeout-" + UUID.randomUUID());

        mockMvc.perform(post("/api/cloud/remediations/" + created[0] + "/approve").header("Authorization", approver)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"requestHash\":\"" + created[1] + "\"}"))
                .andExpect(status().isOk());

        // The call times out and the change did not actually take effect
        when(s3.putPublicAccessBlock(any(PutPublicAccessBlockRequest.class)))
                .thenThrow(ApiCallTimeoutException.builder().message("timeout").build());

        mockMvc.perform(post("/api/cloud/remediations/" + created[0] + "/execute").header("Authorization", requester)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmResourceId\":\"public-bucket\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFICATION_FAILED"));

        assertEquals(CloudFinding.Status.OPEN, findingRepository.findById(findingId).orElseThrow().getStatus(),
                "a finding is never resolved without verified resource state");
    }

    @Test
    void definitiveFailureLeavesFindingOpenAndAllowsRetry() throws Exception {
        String accountId = registerAccount();
        Long findingId = scanAndGetFindingId(accountId);
        String[] created = requestRemediation(findingId, "wf-denied-" + UUID.randomUUID());
        mockMvc.perform(post("/api/cloud/remediations/" + created[0] + "/approve").header("Authorization", approver)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"requestHash\":\"" + created[1] + "\"}"))
                .andExpect(status().isOk());

        when(s3.putPublicAccessBlock(any(PutPublicAccessBlockRequest.class))).thenThrow(S3Exception.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDenied").build()).statusCode(403).build());

        mockMvc.perform(post("/api/cloud/remediations/" + created[0] + "/execute").header("Authorization", requester)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmResourceId\":\"public-bucket\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.executionMessage").value(org.hamcrest.Matchers.containsString("ACCESS_DENIED")));
        assertEquals(CloudFinding.Status.OPEN, findingRepository.findById(findingId).orElseThrow().getStatus());

        // After fixing permissions, the same approval can be retried
        reset(s3);
        setUpS3AfterReset();
        when(s3.putPublicAccessBlock(any(PutPublicAccessBlockRequest.class))).thenAnswer(inv -> {
            blocked.set(true);
            return PutPublicAccessBlockResponse.builder().build();
        });
        mockMvc.perform(post("/api/cloud/remediations/" + created[0] + "/execute").header("Authorization", requester)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmResourceId\":\"public-bucket\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"))
                .andExpect(jsonPath("$.data.executionAttempts").value(2));
    }

    @Test
    void resolvedFindingIsResolvedByCompleteScanAndReopensWhenConditionReturns() throws Exception {
        String accountId = registerAccount();
        Long findingId = scanAndGetFindingId(accountId);

        blocked.set(true); // condition fixed outside the platform
        mockMvc.perform(post("/api/cloud/accounts/" + accountId + "/scans").header("Authorization", analyst))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.findingsResolved").value(1));
        assertEquals(CloudFinding.Status.RESOLVED, findingRepository.findById(findingId).orElseThrow().getStatus());

        blocked.set(false); // condition reappears
        mockMvc.perform(post("/api/cloud/accounts/" + accountId + "/scans").header("Authorization", analyst))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.findingsReopened").value(1));
        CloudFinding reopened = findingRepository.findById(findingId).orElseThrow();
        assertEquals(CloudFinding.Status.OPEN, reopened.getStatus());
        assertEquals(1, reopened.getReopenCount());
    }

    @Test
    void incompleteScanDoesNotResolveFindings() throws Exception {
        String accountId = registerAccount();
        Long findingId = scanAndGetFindingId(accountId);

        when(s3.listBuckets(any(ListBucketsRequest.class))).thenThrow(S3Exception.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDenied").build()).statusCode(403).build());

        mockMvc.perform(post("/api/cloud/accounts/" + accountId + "/scans").header("Authorization", analyst))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PARTIAL"))
                .andExpect(jsonPath("$.data.findingsResolved").value(0));
        assertEquals(CloudFinding.Status.OPEN, findingRepository.findById(findingId).orElseThrow().getStatus());
    }

    private void setUpS3AfterReset() {
        when(s3.listBuckets(any(ListBucketsRequest.class))).thenReturn(ListBucketsResponse.builder()
                .buckets(Bucket.builder().name("public-bucket").bucketRegion("us-east-1").build()).build());
        when(s3.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class))).thenAnswer(inv -> {
            if (!blocked.get()) {
                throw S3Exception.builder().awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("NoSuchPublicAccessBlockConfiguration").build()).statusCode(404).build();
            }
            return GetPublicAccessBlockResponse.builder().publicAccessBlockConfiguration(
                    PublicAccessBlockConfiguration.builder().blockPublicAcls(true).ignorePublicAcls(true)
                            .blockPublicPolicy(true).restrictPublicBuckets(true).build()).build();
        });
    }
}
