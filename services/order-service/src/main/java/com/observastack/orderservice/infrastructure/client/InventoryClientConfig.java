package com.observastack.orderservice.infrastructure.client;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Supplies the {@link RestClient} {@link InventoryClient} calls
 * inventory-service through.
 *
 * <p>Short, fixed timeouts here are baseline hygiene for a synchronous
 * call on the order placement path — nothing should be able to hang
 * this request forever just because inventory-service stopped
 * responding. That's a different, smaller concern than the retry/circuit
 * breaker behaviour M9 is scoped to add once there's a track record of
 * how this call actually fails in practice to design against.
 */
@Configuration
public class InventoryClientConfig {

    @Bean
    RestClient inventoryRestClient(@Value("${inventory.base-url}") String baseUrl) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(2))
                .withReadTimeout(Duration.ofSeconds(5));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
