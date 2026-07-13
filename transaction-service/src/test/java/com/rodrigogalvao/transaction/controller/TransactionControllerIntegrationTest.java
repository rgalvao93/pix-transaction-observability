package com.rodrigogalvao.transaction.controller;

import tools.jackson.databind.ObjectMapper;
import com.rodrigogalvao.transaction.dto.TransactionRequest;
import com.rodrigogalvao.transaction.model.TransactionType;
import com.rodrigogalvao.transaction.partner.ExternalPartnerClient;
import com.rodrigogalvao.transaction.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private ExternalPartnerClient partnerClient;

    private TransactionRequest validRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setTransactionId("txn-int-1");
        request.setType(TransactionType.CASH_IN);
        request.setAmount(BigDecimal.valueOf(100));
        request.setAccountId("acc-1");
        return request;
    }

    @Test
    void returns200WhenTransactionIsValidAndAuthenticated() throws Exception {
        when(partnerClient.verifyBalance("acc-1"))
                .thenReturn(new ExternalPartnerClient.VerifyBalanceResult("acc-1", BigDecimal.ZERO, true));
        when(partnerClient.transfer("acc-1", BigDecimal.valueOf(100), TransactionType.CASH_IN))
                .thenReturn(new ExternalPartnerClient.TransferResult(true, "ref-1"));

        String token = jwtTokenProvider.generateToken("test-client");

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"));
    }

    @Test
    void returns401WhenAuthorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returns401WhenTokenIsInvalid() throws Exception {
        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returns400WhenAmountIsNotPositive() throws Exception {
        String token = jwtTokenProvider.generateToken("test-client");
        TransactionRequest request = validRequest();
        request.setAmount(BigDecimal.ZERO);

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void returns400WhenTypeIsInvalidEnumValue() throws Exception {
        String token = jwtTokenProvider.generateToken("test-client");
        String payload = "{\"transactionId\":\"txn-int-2\",\"type\":\"FOO\",\"amount\":100,\"accountId\":\"acc-1\"}";

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}
