package com.rodrigogalvao.transaction.partner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Applies partner HTTP timeouts to the auto-configured RestClient.Builder
 * (via a customizer, not a replacement bean) so the observation/tracing
 * instrumentation stays on the builder (Fase 6).
 */
@Configuration
public class PartnerClientConfig {

    @Bean
    public RestClientCustomizer partnerTimeoutsRestClientCustomizer(
            @Value("${partner.connect-timeout-ms:1000}") int connectTimeoutMs,
            @Value("${partner.read-timeout-ms:2000}") int readTimeoutMs) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        return builder -> builder.requestFactory(requestFactory);
    }
}
