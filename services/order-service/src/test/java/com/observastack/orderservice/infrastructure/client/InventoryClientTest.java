package com.observastack.orderservice.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.observastack.orderservice.application.InventoryPort;
import com.observastack.orderservice.application.StockUnavailableException;
import com.observastack.orderservice.domain.OrderId;
import com.observastack.orderservice.domain.Sku;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

/**
 * Verifies {@link InventoryClient} translates inventory-service's HTTP
 * responses the way {@link InventoryPort} promises, in isolation from a
 * real inventory-service process.
 *
 * <p>Uses {@link MockRestServiceServer} bound to the same
 * {@link RestClient.Builder} the real bean is built from, so these tests
 * exercise the actual request the client sends and the actual response
 * handling — only the network hop itself is stubbed. This is the "HTTP
 * translation correctness" test promised in {@link
 * com.observastack.orderservice.PlaceOrderEndToEndTest}'s class Javadoc;
 * that test covers order-service's reaction to a reservation outcome via
 * {@link com.observastack.orderservice.FakeInventoryPort} — this one
 * covers how that outcome gets decided from a real HTTP response.
 */
class InventoryClientTest {

    private static final String BASE_URL = "http://inventory-service.test";

    private MockRestServiceServer server;
    private InventoryClient client;

    private void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new InventoryClient(builder.build());
    }

    private static OrderId anOrderId() {
        return OrderId.newId();
    }

    private static List<InventoryPort.LineItem> aLine() {
        return List.of(new InventoryPort.LineItem(new Sku("WIDGET-1"), 2));
    }

    @Test
    void reserve_succeeds_onCreated() {
        setUp();
        OrderId orderId = anOrderId();
        server.expect(requestTo(BASE_URL + "/reservations"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED));

        client.reserve(orderId, aLine());

        server.verify();
    }

    @Test
    void reserve_throwsStockUnavailable_onConflict() {
        setUp();
        OrderId orderId = anOrderId();
        server.expect(requestTo(BASE_URL + "/reservations"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"insufficient stock\"}"));

        assertThatThrownBy(() -> client.reserve(orderId, aLine()))
                .isInstanceOf(StockUnavailableException.class)
                .hasCauseInstanceOf(HttpClientErrorException.class);

        server.verify();
    }

    @Test
    void reserve_throwsStockUnavailable_onNotFound() {
        setUp();
        OrderId orderId = anOrderId();
        server.expect(requestTo(BASE_URL + "/reservations"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.reserve(orderId, aLine())).isInstanceOf(StockUnavailableException.class);

        server.verify();
    }

    @Test
    void reserve_propagates_onServerError() {
        setUp();
        OrderId orderId = anOrderId();
        server.expect(requestTo(BASE_URL + "/reservations"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.reserve(orderId, aLine()))
                .isInstanceOf(HttpServerErrorException.class)
                .isNotInstanceOf(StockUnavailableException.class);

        server.verify();
    }

    @Test
    void release_succeeds_onNoContent() {
        setUp();
        OrderId orderId = anOrderId();
        server.expect(requestTo(BASE_URL + "/reservations/" + orderId.value() + "/release"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.release(orderId);

        server.verify();
    }

    @Test
    void release_isNoOp_onNotFound() {
        setUp();
        OrderId orderId = anOrderId();
        server.expect(requestTo(BASE_URL + "/reservations/" + orderId.value() + "/release"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        client.release(orderId);

        server.verify();
    }

    @Test
    void reserve_sendsOrderIdAndLines_inRequestBody() {
        setUp();
        OrderId orderId = anOrderId();
        server.expect(requestTo(BASE_URL + "/reservations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content()
                        .string(allOf(
                                containsString(orderId.value().toString()),
                                containsString("WIDGET-1"),
                                containsString("2"))))
                .andRespond(withSuccess());

        client.reserve(orderId, aLine());

        server.verify();
    }
}
