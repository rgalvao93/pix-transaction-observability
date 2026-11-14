package com.rodrigogalvao.transaction.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADR-010: the dev-only token endpoint can be disabled via
 * {@code security.dev-token-enabled=false} (env {@code SECURITY_DEV_TOKEN_ENABLED}).
 * Any real deployment must turn it off — see docs/DEPLOY.md.
 */
@SpringBootTest(properties = "security.dev-token-enabled=false")
@AutoConfigureMockMvc
class DevTokenEndpointDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returns404WhenDevTokenEndpointIsDisabled() throws Exception {
        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"test-client\"}"))
                .andExpect(status().isNotFound());
    }
}
