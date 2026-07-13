package com.rodrigogalvao.transaction.partner;

import com.rodrigogalvao.transaction.model.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class HttpExternalPartnerClient implements ExternalPartnerClient {

    private static final Logger log = LoggerFactory.getLogger(HttpExternalPartnerClient.class);

    private final RestClient restClient;
    private final int maxAttempts;
    private final Duration retryBackoff;

    public HttpExternalPartnerClient(
            RestClient.Builder restClientBuilder,
            @Value("${partner.base-url:http://localhost:8081}") String baseUrl,
            @Value("${partner.retry.max-attempts:3}") int maxAttempts,
            @Value("${partner.retry.backoff-ms:200}") long retryBackoffMs) {

        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.maxAttempts = maxAttempts;
        this.retryBackoff = Duration.ofMillis(retryBackoffMs);
    }

    @Override
    public VerifyBalanceResult verifyBalance(String accountId) {
        return executeWithRetry("verify-balance", () -> restClient.post()
                .uri("/partner/verify-balance")
                .body(Map.of("accountId", accountId))
                .retrieve()
                .body(VerifyBalanceResult.class));
    }

    @Override
    public TransferResult transfer(String accountId, BigDecimal amount, TransactionType type) {
        return executeWithRetry("transfer", () -> restClient.post()
                .uri("/partner/transfer")
                .body(Map.of("accountId", accountId, "amount", amount, "type", type.name()))
                .retrieve()
                .body(TransferResult.class));
    }

    private <T> T executeWithRetry(String operation, Supplier<T> call) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.get();
            } catch (RestClientException ex) {
                lastError = ex;
                log.warn("Partner call '{}' failed on attempt {}/{}: {}", operation, attempt, maxAttempts, ex.getMessage());
                if (attempt < maxAttempts) {
                    sleep(retryBackoff.toMillis() * attempt);
                }
            }
        }
        throw new ExternalPartnerException("Partner call '" + operation + "' failed after " + maxAttempts + " attempts", lastError);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
