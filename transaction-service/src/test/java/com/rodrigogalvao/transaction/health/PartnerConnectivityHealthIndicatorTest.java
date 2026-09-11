package com.rodrigogalvao.transaction.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PartnerConnectivityHealthIndicatorTest {

    private static final String BASE_URL = "http://localhost:8081";

    private PartnerConnectivityHealthIndicator buildIndicator(RestClient.Builder builder) {
        return new PartnerConnectivityHealthIndicator(builder, BASE_URL);
    }

    @Test
    void reportsUpWhenPartnerHealthEndpointResponds() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/actuator/health")).andRespond(withSuccess());

        Health health = buildIndicator(builder).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        server.verify();
    }

    @Test
    void reportsDownWhenPartnerHealthEndpointFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/actuator/health")).andRespond(withServerError());

        Health health = buildIndicator(builder).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        server.verify();
    }
}
