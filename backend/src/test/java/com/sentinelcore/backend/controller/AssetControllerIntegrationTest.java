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
    void testGetAssetsWithoutToken_Returns403() throws Exception {
        // Without authentication, accessing protected resource should return 403 Forbidden or 401 Unauthorized
        mockMvc.perform(get("/api/assets")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
