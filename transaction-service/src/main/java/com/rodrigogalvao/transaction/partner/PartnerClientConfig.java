package com.rodrigogalvao.transaction.partner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class PartnerClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder(
            @Value("${partner.connect-timeout-ms:1000}") int connectTimeoutMs,
            @Value("${partner.read-timeout-ms:2000}") int readTimeoutMs) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        return RestClient.builder().requestFactory(requestFactory);
    }
}
