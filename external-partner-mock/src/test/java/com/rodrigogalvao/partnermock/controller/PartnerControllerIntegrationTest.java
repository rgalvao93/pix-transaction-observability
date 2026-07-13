package com.rodrigogalvao.partnermock.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PartnerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private String validToken() {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.builder()
                .subject("transaction-service")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }

    @Test
    void verifyBalanceReturns200WithValidToken() throws Exception {
        mockMvc.perform(post("/partner/verify-balance")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"acc-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void verifyBalanceReturns401WithoutToken() throws Exception {
        mockMvc.perform(post("/partner/verify-balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"acc-123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void transferReturns200WithValidToken() throws Exception {
        mockMvc.perform(post("/partner/transfer")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"acc-123\",\"amount\":50,\"type\":\"CASH_IN\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void transferReturns400ForInvalidType() throws Exception {
        mockMvc.perform(post("/partner/transfer")
                        .header("Authorization", "Bearer " + validToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"acc-123\",\"amount\":50,\"type\":\"FOO\"}"))
                .andExpect(status().isBadRequest());
    }
}
