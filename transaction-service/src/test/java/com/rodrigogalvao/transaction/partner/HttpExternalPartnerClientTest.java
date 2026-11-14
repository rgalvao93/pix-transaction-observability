package com.rodrigogalvao.transaction.partner;

import com.rodrigogalvao.transaction.security.JwtTokenProvider;
import com.rodrigogalvao.transaction.model.TransactionType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpExternalPartnerClientTest {

    private static final String BASE_URL = "http://localhost:8081";
    private static final JwtTokenProvider JWT_TOKEN_PROVIDER =
            new JwtTokenProvider("test-only-secret-at-least-32-bytes-long-1234567890", 60_000);

    private HttpExternalPartnerClient buildClient(RestClient.Builder builder, int maxAttempts) {
        return new HttpExternalPartnerClient(builder, JWT_TOKEN_PROVIDER, new SimpleMeterRegistry(), BASE_URL,
                maxAttempts, 10);
    }

    @Test
    void retriesOnFailureAndSucceedsOnLaterAttempt() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpExternalPartnerClient client = buildClient(builder, 3);

        server.expect(requestTo(BASE_URL + "/partner/verify-balance")).andRespond(withServerError());
        server.expect(requestTo(BASE_URL + "/partner/verify-balance")).andRespond(withServerError());
        server.expect(requestTo(BASE_URL + "/partner/verify-balance"))
                .andRespond(withSuccess("{\"accountId\":\"acc-1\",\"balance\":100.0,\"available\":true}",
                        MediaType.APPLICATION_JSON));

        ExternalPartnerClient.VerifyBalanceResult result = client.verifyBalance("acc-1");

        assertThat(result.available()).isTrue();
        server.verify();
    }

    @Test
    void retriesTransferAndSucceedsOnLaterAttempt() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpExternalPartnerClient client = buildClient(builder, 3);

        server.expect(requestTo(BASE_URL + "/partner/transfer")).andRespond(withServerError());
        server.expect(requestTo(BASE_URL + "/partner/transfer"))
                .andRespond(withSuccess("{\"success\":true,\"partnerReference\":\"ref-9\"}",
                        MediaType.APPLICATION_JSON));

        ExternalPartnerClient.TransferResult result =
                client.transfer("acc-1", java.math.BigDecimal.valueOf(100), TransactionType.CASH_OUT);

        assertThat(result.success()).isTrue();
        assertThat(result.partnerReference()).isEqualTo("ref-9");
        server.verify();
    }

    @Test
    void throwsExternalPartnerExceptionAfterExhaustingRetries() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpExternalPartnerClient client = buildClient(builder, 2);

        server.expect(requestTo(BASE_URL + "/partner/verify-balance")).andRespond(withServerError());
        server.expect(requestTo(BASE_URL + "/partner/verify-balance")).andRespond(withServerError());

        assertThatThrownBy(() -> client.verifyBalance("acc-1"))
                .isInstanceOf(ExternalPartnerException.class);
        server.verify();
    }
}
