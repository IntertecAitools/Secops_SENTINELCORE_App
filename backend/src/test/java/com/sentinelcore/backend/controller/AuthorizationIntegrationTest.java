package com.sentinelcore.backend.controller;

import com.sentinelcore.backend.entity.Role;
import com.sentinelcore.backend.entity.User;
import com.sentinelcore.backend.repository.AuditLogRepository;
import com.sentinelcore.backend.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthorizationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void anonymousRequestsAreRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/alerts")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
        mockMvc.perform(get("/api/cloud/accounts")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/assets/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void basicUserCannotReadOrMutateSecurityData() throws Exception {
        String token = bearer(Role.USER);
        mockMvc.perform(get("/api/assets").header("Authorization", token)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/vulnerabilities").header("Authorization", token)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard/stats").header("Authorization", token)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/alerts").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\",\"severity\":\"LOW\",\"status\":\"OPEN\"}"))
                .andExpect(status().isForbidden());
        // Profile remains available to every authenticated user
        mockMvc.perform(get("/api/users/profile").header("Authorization", token)).andExpect(status().isOk());
    }

    @Test
    void viewPermissionDoesNotGrantMutation() throws Exception {
        String auditor = bearer(Role.AUDITOR);
        mockMvc.perform(get("/api/alerts").header("Authorization", auditor)).andExpect(status().isOk());
        mockMvc.perform(delete("/api/alerts/12345").header("Authorization", auditor)).andExpect(status().isForbidden());

        String assetManager = bearer(Role.ASSET_MANAGER);
        mockMvc.perform(delete("/api/incidents/12345").header("Authorization", assetManager)).andExpect(status().isForbidden());
    }

    @Test
    void auditLogsRequireAuditPermission() throws Exception {
        mockMvc.perform(get("/api/audit-logs").header("Authorization", bearer(Role.AUDITOR))).andExpect(status().isOk());
        mockMvc.perform(get("/api/audit-logs").header("Authorization", bearer(Role.SECURITY_ANALYST)))
                .andExpect(status().isForbidden());
        assertTrue(auditLogRepository.findAll().stream().anyMatch(l -> "ACCESS_DENIED".equals(l.getAction())),
                "denied authorization attempts are audited");
    }

    @Test
    void disabledUsersTokenIsRejected() throws Exception {
        User user = createUser(Role.AUDITOR);
        String token = bearer(user);
        user.setEnabled(false);
        userRepository.save(user);

        mockMvc.perform(get("/api/alerts").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @Test
    void forgedOrGarbageTokensAreRejected() throws Exception {
        mockMvc.perform(get("/api/alerts").header("Authorization", "Bearer not-a-jwt")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/alerts").header("Authorization",
                        "Bearer eyJhbGciOiJub25lIn0.eyJzdWIiOiJzdXBlcmFkbWluIn0."))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicRegistrationIsDisabledAndCannotGrantRoles() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Mallory\",\"username\":\"mallory\",\"email\":\"m@test.invalid\","
                                + "\"password\":\"Password12345\",\"role\":\"SUPER_ADMIN\"}"))
                .andExpect(status().isForbidden());
        assertTrue(userRepository.findByUsername("mallory").isEmpty());
    }

    @Test
    void adminCannotGrantSuperAdminOrChangeOwnRole() throws Exception {
        User admin = createUser(Role.ADMIN);
        User target = createUser(Role.USER);
        String token = bearer(admin);

        mockMvc.perform(put("/api/users/" + target.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"T\",\"email\":\"" + target.getEmail() + "\",\"role\":\"SUPER_ADMIN\",\"enabled\":true}"))
                .andExpect(status().isForbidden());
        assertEquals(Role.USER, userRepository.findById(target.getId()).orElseThrow().getRole());

        mockMvc.perform(put("/api/users/" + admin.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"A\",\"email\":\"" + admin.getEmail() + "\",\"role\":\"AUDITOR\",\"enabled\":true}"))
                .andExpect(status().isForbidden());

        // Ordinary role change within the admin's authority still works
        mockMvc.perform(put("/api/users/" + target.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"T\",\"email\":\"" + target.getEmail() + "\",\"role\":\"AUDITOR\",\"enabled\":true}"))
                .andExpect(status().isOk());
        assertEquals(Role.AUDITOR, userRepository.findById(target.getId()).orElseThrow().getRole());
    }

    @Test
    void adminCannotManageSuperAdminAccounts() throws Exception {
        User superAdmin = createUser(Role.SUPER_ADMIN);
        String adminToken = bearer(Role.ADMIN);
        mockMvc.perform(put("/api/users/" + superAdmin.getId() + "/toggle-status").header("Authorization", adminToken))
                .andExpect(status().isForbidden());
        assertTrue(userRepository.findById(superAdmin.getId()).orElseThrow().getEnabled());
    }

    @Test
    void passwordsAreNeverWrittenToAuditLog() throws Exception {
        String superAdmin = bearer(Role.SUPER_ADMIN);
        User target = createUser(Role.USER);
        String secret = "Unique-Reset-Passw0rd-7731";

        mockMvc.perform(put("/api/users/" + target.getId() + "/reset-password").header("Authorization", superAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"newPassword\":\"" + secret + "\"}"))
                .andExpect(status().isOk());

        assertTrue(auditLogRepository.findAll().stream()
                .noneMatch(l -> l.getDetails() != null && l.getDetails().contains(secret)));
        assertTrue(auditLogRepository.findAll().stream().anyMatch(l -> "USER_PASSWORD_RESET".equals(l.getAction())));
    }

    @Test
    void failedLoginsReturnGenericErrorAndAreThrottled() throws Exception {
        User user = createUser(Role.AUDITOR);
        String body = "{\"username\":\"" + user.getUsername() + "\",\"password\":\"wrong-password\"}";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }
        // Even the correct password is refused while locked
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user.getUsername() + "\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isTooManyRequests());
        assertTrue(auditLogRepository.findAll().stream().anyMatch(l -> "LOGIN_FAILED".equals(l.getAction())));
    }

    @Test
    void unknownUserGetsSameResponseAsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"no-such-user-xyz\",\"password\":\"whatever-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void successfulLoginIssuesWorkingToken() throws Exception {
        User user = createUser(Role.AUDITOR);
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user.getUsername() + "\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = response.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        mockMvc.perform(get("/api/alerts").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    @Test
    void invalidSortParameterIsBadRequestNotServerError() throws Exception {
        mockMvc.perform(get("/api/alerts").param("sortBy", "noSuchField").header("Authorization", bearer(Role.AUDITOR)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void incidentManagerCanListUsersForAssignment() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", bearer(Role.INCIDENT_MANAGER)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users").header("Authorization", bearer(Role.AUDITOR)))
                .andExpect(status().isForbidden());
    }
}
