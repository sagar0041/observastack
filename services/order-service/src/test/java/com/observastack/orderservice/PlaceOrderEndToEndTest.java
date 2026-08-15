package com.observastack.orderservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.observastack.orderservice.api.dto.ErrorResponse;
import com.observastack.orderservice.api.dto.OrderResponse;
import com.observastack.orderservice.api.dto.PlaceOrderRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises order placement end to end: real HTTP request, real Spring
 * context, real PostgreSQL migrated by the actual Liquibase changelog —
 * nothing mocked or substituted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PlaceOrderEndToEndTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.4");

    @Autowired
    private TestRestTemplate restTemplate;

    private static PlaceOrderRequest aRequest(UUID customerId) {
        return new PlaceOrderRequest(
                customerId, "USD", List.of(new PlaceOrderRequest.LineItemRequest("WIDGET-1", 2, new BigDecimal("9.99"))));
    }

    private ResponseEntity<OrderResponse> place(String idempotencyKey, PlaceOrderRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        return restTemplate.exchange("/orders", HttpMethod.POST, new HttpEntity<>(request, headers), OrderResponse.class);
    }

    private ResponseEntity<ErrorResponse> placeExpectingError(String idempotencyKey, PlaceOrderRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        return restTemplate.exchange("/orders", HttpMethod.POST, new HttpEntity<>(request, headers), ErrorResponse.class);
    }

    @Test
    void placeOrder_persistsOrder_andIsRetrievableById() {
        UUID customerId = UUID.randomUUID();

        ResponseEntity<OrderResponse> placeResponse = place(UUID.randomUUID().toString(), aRequest(customerId));

        assertThat(placeResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(placeResponse.getHeaders().getLocation()).isNotNull();
        OrderResponse placed = placeResponse.getBody();
        assertThat(placed).isNotNull();
        assertThat(placed.status()).isEqualTo("PLACED");
        assertThat(placed.customerId()).isEqualTo(customerId);
        assertThat(placed.currency()).isEqualTo("USD");
        assertThat(placed.totalPrice()).isEqualByComparingTo("19.98");
        assertThat(placed.placedAt()).isNotNull();

        ResponseEntity<OrderResponse> getResponse =
                restTemplate.getForEntity("/orders/" + placed.id(), OrderResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        OrderResponse fetched = getResponse.getBody();
        assertThat(fetched).isNotNull();
        assertThat(fetched.id()).isEqualTo(placed.id());
        assertThat(fetched.lineItems()).hasSize(1);
        assertThat(fetched.lineItems().get(0).sku()).isEqualTo("WIDGET-1");
    }

    @Test
    void placeOrder_returnsTheSameOrder_whenIdempotencyKeyIsReplayed() {
        String idempotencyKey = UUID.randomUUID().toString();
        PlaceOrderRequest request = aRequest(UUID.randomUUID());

        ResponseEntity<OrderResponse> first = place(idempotencyKey, request);
        ResponseEntity<OrderResponse> replay = place(idempotencyKey, request);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(replay.getBody()).isNotNull();
        assertThat(replay.getBody().id()).isEqualTo(first.getBody().id());
    }

    @Test
    void placeOrder_returnsBadRequest_whenIdempotencyKeyHeaderIsMissing() {
        ResponseEntity<ErrorResponse> response =
                restTemplate.postForEntity("/orders", aRequest(UUID.randomUUID()), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void cancelOrder_transitionsToCancelled() {
        OrderResponse placed = place(UUID.randomUUID().toString(), aRequest(UUID.randomUUID())).getBody();

        ResponseEntity<OrderResponse> cancelResponse =
                restTemplate.postForEntity("/orders/" + placed.id() + "/cancel", null, OrderResponse.class);

        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelResponse.getBody()).isNotNull();
        assertThat(cancelResponse.getBody().status()).isEqualTo("CANCELLED");

        ResponseEntity<OrderResponse> getResponse =
                restTemplate.getForEntity("/orders/" + placed.id(), OrderResponse.class);
        assertThat(getResponse.getBody().status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelOrder_returnsNotFound_whenOrderDoesNotExist() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/orders/" + UUID.randomUUID() + "/cancel", null, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getOrder_returnsNotFound_whenOrderDoesNotExist() {
        ResponseEntity<ErrorResponse> response =
                restTemplate.getForEntity("/orders/" + UUID.randomUUID(), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void placeOrder_returnsBadRequest_whenLineItemsIsEmpty() {
        PlaceOrderRequest request = new PlaceOrderRequest(UUID.randomUUID(), "USD", List.of());

        ResponseEntity<ErrorResponse> response = placeExpectingError(UUID.randomUUID().toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void placeOrder_returnsBadRequest_whenSkuExceedsColumnLength() {
        String tooLong = "X".repeat(65); // orders.sku is varchar(64)
        PlaceOrderRequest request = new PlaceOrderRequest(
                UUID.randomUUID(), "USD", List.of(new PlaceOrderRequest.LineItemRequest(tooLong, 1, BigDecimal.ONE)));

        ResponseEntity<ErrorResponse> response = placeExpectingError(UUID.randomUUID().toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void placeOrder_returnsBadRequest_whenUnitPriceExceedsColumnPrecision() {
        // unit_price is numeric(12,2): 10 integer digits is the max it holds.
        BigDecimal tooLarge = new BigDecimal("99999999999.99");
        PlaceOrderRequest request = new PlaceOrderRequest(
                UUID.randomUUID(), "USD", List.of(new PlaceOrderRequest.LineItemRequest("WIDGET-1", 1, tooLarge)));

        ResponseEntity<ErrorResponse> response = placeExpectingError(UUID.randomUUID().toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void placeOrder_returnsBadRequest_whenCurrencyIsNotThreeLetters() {
        PlaceOrderRequest request = new PlaceOrderRequest(
                UUID.randomUUID(), "US", List.of(new PlaceOrderRequest.LineItemRequest("WIDGET-1", 1, BigDecimal.ONE)));

        ResponseEntity<ErrorResponse> response = placeExpectingError(UUID.randomUUID().toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
