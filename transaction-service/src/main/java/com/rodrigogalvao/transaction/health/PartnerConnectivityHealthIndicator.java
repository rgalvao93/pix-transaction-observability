package com.rodrigogalvao.transaction.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Readiness signal: pings external-partner-mock's own /actuator/health directly
 * (no auth, no business-flow retry) so a down partner doesn't block on the
 * transaction retry/backoff policy just to answer a health check.
 */
@Component("partnerConnectivity")
public class PartnerConnectivityHealthIndicator implements HealthIndicator {

    private final RestClient restClient;

    public PartnerConnectivityHealthIndicator(
            RestClient.Builder restClientBuilder,
            @Value("${partner.base-url:http://localhost:8081}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Health health() {
        try {
            restClient.get().uri("/actuator/health").retrieve().toBodilessEntity();
            return Health.up().build();
        } catch (RestClientException ex) {
            return Health.down().withDetail("error", ex.getMessage()).build();
        }
    }
}
