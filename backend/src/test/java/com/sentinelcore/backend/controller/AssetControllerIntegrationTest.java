package com.sentinelcore.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AssetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetAssetsWithoutToken_Returns401() throws Exception {
        // Unauthenticated requests are rejected with 401 (previously the default 403 entry point)
        mockMvc.perform(get("/api/assets")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
