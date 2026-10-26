package com.sentinelcore.backend.controller;

import com.sentinelcore.backend.cloud.CloudModels.AuthMode;
import com.sentinelcore.backend.cloud.CloudProvider;
import com.sentinelcore.backend.cloud.RemediationActionType;
import com.sentinelcore.backend.entity.CloudAccount;
import com.sentinelcore.backend.entity.CloudFinding;
import com.sentinelcore.backend.entity.Role;
import com.sentinelcore.backend.repository.CloudAccountRepository;
import com.sentinelcore.backend.repository.CloudFindingRepository;
import com.sentinelcore.backend.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cloud APIs with the default configuration (AWS integration disabled, execution disabled).
 * Fixture accounts and findings below are explicitly test data, created directly in the test database.
 */
class CloudTenantIsolationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CloudAccountRepository cloudAccountRepository;

    @Autowired
    private CloudFindingRepository findingRepository;

    private CloudAccount tenantAAccount;
    private CloudFinding tenantAFinding;
    private String tenantA;
    private String tenantB;

    @BeforeEach
    void setUp() {
        tenantA = "tenant-a-" + UUID.randomUUID().toString().substring(0, 8);
        tenantB = "tenant-b-" + UUID.randomUUID().toString().substring(0, 8);
        tenantAAccount = cloudAccountRepository.save(CloudAccount.builder()
                .tenantKey(tenantA)
                .name("Fixture account A")
                .provider(CloudProvider.AWS)
                .externalAccountId(String.valueOf(100000000000L + Math.abs(UUID.randomUUID().getMostSignificantBits() % 899999999999L)))
                .authMode(AuthMode.PLATFORM_DEFAULT)
                .createdBy("fixture")
                .build());
        tenantAFinding = findingRepository.save(CloudFinding.builder()
                .tenantKey(tenantA)
                .cloudAccount(tenantAAccount)
                .fingerprint(UUID.randomUUID().toString().replace("-", ""))
                .findingType("S3_PUBLIC_ACCESS_BLOCK_DISABLED")
                .severity("MEDIUM")
                .status(CloudFinding.Status.OPEN)
                .resourceType("s3:bucket")
                .resourceId("arn:aws:s3:::fixture-bucket")
                .region("us-east-1")
                .scope("s3")
                .title("Fixture finding")
                .supportedRemediation(RemediationActionType.S3_BLOCK_PUBLIC_ACCESS)
                .firstDetectedAt(LocalDateTime.now())
                .lastObservedAt(LocalDateTime.now())
                .build());
    }

    @Test
    void otherTenantCannotSeeOrUseAccount() throws Exception {
        String tenantBAdmin = bearer(createUser(Role.SUPER_ADMIN, tenantB));

        mockMvc.perform(get("/api/cloud/accounts").header("Authorization", tenantBAdmin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(0)));
        mockMvc.perform(get("/api/cloud/accounts/" + tenantAAccount.getId()).header("Authorization", tenantBAdmin))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/cloud/accounts/" + tenantAAccount.getId() + "/scans").header("Authorization", tenantBAdmin))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/cloud/accounts/" + tenantAAccount.getId() + "/verify").header("Authorization", tenantBAdmin))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/cloud/accounts/" + tenantAAccount.getId()).header("Authorization", tenantBAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"hijack\",\"provider\":\"AWS\",\"externalAccountId\":\""
                                + tenantAAccount.getExternalAccountId() + "\",\"authMode\":\"PLATFORM_DEFAULT\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void otherTenantCannotSeeFindingsOrRequestRemediation() throws Exception {
        String tenantBAdmin = bearer(createUser(Role.SUPER_ADMIN, tenantB));

        mockMvc.perform(get("/api/cloud/findings").header("Authorization", tenantBAdmin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content", hasSize(0)));
        mockMvc.perform(get("/api/cloud/findings/" + tenantAFinding.getId()).header("Authorization", tenantBAdmin))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/cloud/remediations").header("Authorization", tenantBAdmin)
                        .header("Idempotency-Key", "cross-tenant-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"findingId\":" + tenantAFinding.getId() + ",\"actionType\":\"S3_BLOCK_PUBLIC_ACCESS\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void sameTenantCanSeeFindingsAndRemediationsAreTenantScoped() throws Exception {
        String requester = bearer(createUser(Role.VULNERABILITY_MANAGER, tenantA));
        mockMvc.perform(get("/api/cloud/findings").header("Authorization", requester))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content", hasSize(1)));

        String body = mockMvc.perform(post("/api/cloud/remediations").header("Authorization", requester)
                        .header("Idempotency-Key", "tenant-a-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"findingId\":" + tenantAFinding.getId() + ",\"actionType\":\"S3_BLOCK_PUBLIC_ACCESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();
        String remediationId = body.replaceAll(".*\"data\":\\{\"id\":(\\d+).*", "$1");

        String tenantBAdmin = bearer(createUser(Role.SUPER_ADMIN, tenantB));
        mockMvc.perform(get("/api/cloud/remediations/" + remediationId).header("Authorization", tenantBAdmin))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/cloud/remediations/" + remediationId + "/approve").header("Authorization", tenantBAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestHash\":\"" + "0".repeat(64) + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cloudPermissionsAreEnforced() throws Exception {
        mockMvc.perform(get("/api/cloud/accounts").header("Authorization", bearer(Role.ASSET_MANAGER)))
                .andExpect(status().isForbidden());

        String auditor = bearer(Role.AUDITOR);
        mockMvc.perform(get("/api/cloud/accounts").header("Authorization", auditor)).andExpect(status().isOk());
        mockMvc.perform(post("/api/cloud/accounts").header("Authorization", auditor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"provider\":\"AWS\",\"externalAccountId\":\"123456789012\",\"authMode\":\"PLATFORM_DEFAULT\"}"))
                .andExpect(status().isForbidden());

        // Analysts can request but not approve or execute
        String analyst = bearer(createUser(Role.SECURITY_ANALYST, tenantA));
        mockMvc.perform(post("/api/cloud/remediations/1/approve").header("Authorization", analyst)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"requestHash\":\"" + "a".repeat(64) + "\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/cloud/remediations/1/execute").header("Authorization", analyst)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmResourceId\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountValidationAndUnsupportedProviders() throws Exception {
        String admin = bearer(createUser(Role.ADMIN, tenantA));

        mockMvc.perform(post("/api/cloud/accounts").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Azure\",\"provider\":\"AZURE\",\"externalAccountId\":\"sub-1\",\"authMode\":\"PLATFORM_DEFAULT\"}"))
                .andExpect(status().isNotImplemented());

        mockMvc.perform(post("/api/cloud/accounts").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad\",\"provider\":\"AWS\",\"externalAccountId\":\"12345\",\"authMode\":\"PLATFORM_DEFAULT\"}"))
                .andExpect(status().isBadRequest());

        // Role in a different account than the registered one is rejected
        mockMvc.perform(post("/api/cloud/accounts").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mismatch\",\"provider\":\"AWS\",\"externalAccountId\":\"111111111111\","
                                + "\"authMode\":\"ASSUME_ROLE\",\"roleArn\":\"arn:aws:iam::222222222222:role/x\"}"))
                .andExpect(status().isBadRequest());

        // ExternalId is accepted on write but never echoed back
        mockMvc.perform(post("/api/cloud/accounts").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Good\",\"provider\":\"AWS\",\"externalAccountId\":\"333333333333\","
                                + "\"authMode\":\"ASSUME_ROLE\",\"roleArn\":\"arn:aws:iam::333333333333:role/SentinelCoreReadOnly\","
                                + "\"externalId\":\"fixture-external-id\",\"regions\":[\"us-east-1\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.externalIdConfigured").value(true))
                .andExpect(jsonPath("$.data.externalId").doesNotExist())
                .andExpect(jsonPath("$.data.connectionStatus").value("NOT_VERIFIED"));
    }

    @Test
    void disabledIntegrationFailsSafely() throws Exception {
        String admin = bearer(createUser(Role.SUPER_ADMIN, tenantA));

        mockMvc.perform(post("/api/cloud/accounts/" + tenantAAccount.getId() + "/verify").header("Authorization", admin))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(post("/api/cloud/accounts/" + tenantAAccount.getId() + "/scans").header("Authorization", admin))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/cloud/capabilities").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.awsIntegrationEnabled").value(false))
                .andExpect(jsonPath("$.data.remediationExecutionEnabled").value(false))
                .andExpect(jsonPath("$.data.supportedProviders[0]").value("AWS"));
    }

    @Test
    void executionIsBlockedWhenDisabledEvenAfterApproval() throws Exception {
        String requester = bearer(createUser(Role.VULNERABILITY_MANAGER, tenantA));
        String approver = bearer(createUser(Role.SUPER_ADMIN, tenantA));

        String created = mockMvc.perform(post("/api/cloud/remediations").header("Authorization", requester)
                        .header("Idempotency-Key", "exec-disabled-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"findingId\":" + tenantAFinding.getId() + ",\"actionType\":\"S3_BLOCK_PUBLIC_ACCESS\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id = created.replaceAll(".*\"data\":\\{\"id\":(\\d+).*", "$1");
        String hash = created.replaceAll(".*\"requestHash\":\"([a-f0-9]{64})\".*", "$1");

        // Preview works without AWS and is labelled as local validation with unavailable state
        mockMvc.perform(get("/api/cloud/remediations/" + id + "/preview").header("Authorization", approver))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previewType").value("LOCAL_VALIDATION"))
                .andExpect(jsonPath("$.data.currentStateAvailable").value(false));

        mockMvc.perform(post("/api/cloud/remediations/" + id + "/approve").header("Authorization", approver)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"requestHash\":\"" + hash + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(post("/api/cloud/remediations/" + id + "/execute").header("Authorization", approver)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmResourceId\":\"fixture-bucket\"}"))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/cloud/remediations/" + id).header("Authorization", approver))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void remediationRequiresIdempotencyKeyAndAllowlistedActionForFinding() throws Exception {
        String requester = bearer(createUser(Role.VULNERABILITY_MANAGER, tenantA));
        mockMvc.perform(post("/api/cloud/remediations").header("Authorization", requester)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"findingId\":" + tenantAFinding.getId() + ",\"actionType\":\"S3_BLOCK_PUBLIC_ACCESS\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/cloud/remediations").header("Authorization", requester)
                        .header("Idempotency-Key", "bad-action-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"findingId\":" + tenantAFinding.getId() + ",\"actionType\":\"DELETE_EVERYTHING\"}"))
                .andExpect(status().isBadRequest());
    }
}
