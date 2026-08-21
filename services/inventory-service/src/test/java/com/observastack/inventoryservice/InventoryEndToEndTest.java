package com.observastack.inventoryservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.observastack.inventoryservice.api.dto.CreateStockItemRequest;
import com.observastack.inventoryservice.api.dto.ErrorResponse;
import com.observastack.inventoryservice.api.dto.ReservationResponse;
import com.observastack.inventoryservice.api.dto.ReserveStockRequest;
import com.observastack.inventoryservice.api.dto.StockItemResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises stocking, reservation, and release end to end: real HTTP
 * requests, real Spring context, real PostgreSQL migrated by the actual
 * Liquibase changelog — nothing mocked or substituted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class InventoryEndToEndTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.4");

    @Autowired
    private TestRestTemplate restTemplate;

    private ResponseEntity<StockItemResponse> createStockItem(String sku, int quantity) {
        return restTemplate.postForEntity("/stock-items", new CreateStockItemRequest(sku, quantity), StockItemResponse.class);
    }

    private ResponseEntity<ReservationResponse> reserve(UUID orderId, String sku, int quantity) {
        var request = new ReserveStockRequest(orderId, List.of(new ReserveStockRequest.LineRequest(sku, quantity)));
        return restTemplate.postForEntity("/reservations", request, ReservationResponse.class);
    }

    @Test
    void createStockItem_thenGetStockItem_returnsIt() {
        String sku = "WIDGET-" + UUID.randomUUID();

        ResponseEntity<StockItemResponse> createResponse = createStockItem(sku, 20);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getHeaders().getLocation()).isNotNull();

        ResponseEntity<StockItemResponse> getResponse = restTemplate.getForEntity("/stock-items/" + sku, StockItemResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().availableQuantity()).isEqualTo(20);
    }

    @Test
    void getStockItem_returnsNotFound_whenSkuIsNotStocked() {
        ResponseEntity<ErrorResponse> response =
                restTemplate.getForEntity("/stock-items/NO-SUCH-SKU-" + UUID.randomUUID(), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void reserve_decrementsAvailableQuantity() {
        String sku = "WIDGET-" + UUID.randomUUID();
        createStockItem(sku, 10);

        ResponseEntity<ReservationResponse> reserveResponse = reserve(UUID.randomUUID(), sku, 4);

        assertThat(reserveResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reserveResponse.getBody()).isNotNull();
        assertThat(reserveResponse.getBody().lines()).containsExactly(new ReservationResponse.LineResponse(sku, 4));

        StockItemResponse stockItem =
                restTemplate.getForEntity("/stock-items/" + sku, StockItemResponse.class).getBody();
        assertThat(stockItem.availableQuantity()).isEqualTo(6);
    }

    @Test
    void reserve_returnsTheSameReservation_whenOrderIdIsReplayed() {
        String sku = "WIDGET-" + UUID.randomUUID();
        createStockItem(sku, 10);
        UUID orderId = UUID.randomUUID();

        ResponseEntity<ReservationResponse> first = reserve(orderId, sku, 3);
        ResponseEntity<ReservationResponse> replay = reserve(orderId, sku, 3);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(replay.getBody().id()).isEqualTo(first.getBody().id());

        // The replay must not have decremented stock a second time.
        StockItemResponse stockItem =
                restTemplate.getForEntity("/stock-items/" + sku, StockItemResponse.class).getBody();
        assertThat(stockItem.availableQuantity()).isEqualTo(7);
    }

    @Test
    void reserve_returnsConflict_whenStockIsInsufficient() {
        String sku = "WIDGET-" + UUID.randomUUID();
        createStockItem(sku, 2);

        var request = new ReserveStockRequest(
                UUID.randomUUID(), List.of(new ReserveStockRequest.LineRequest(sku, 5)));
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity("/reservations", request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        // Failing to reserve must not have touched available stock.
        StockItemResponse stockItem =
                restTemplate.getForEntity("/stock-items/" + sku, StockItemResponse.class).getBody();
        assertThat(stockItem.availableQuantity()).isEqualTo(2);
    }

    @Test
    void reserve_returnsNotFound_whenSkuIsNotStocked() {
        var request = new ReserveStockRequest(
                UUID.randomUUID(), List.of(new ReserveStockRequest.LineRequest("NO-SUCH-SKU-" + UUID.randomUUID(), 1)));

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity("/reservations", request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void reserve_combinesDuplicateSkuLines_intoOneDecrement() {
        String sku = "WIDGET-" + UUID.randomUUID();
        createStockItem(sku, 10);

        var request = new ReserveStockRequest(
                UUID.randomUUID(),
                List.of(new ReserveStockRequest.LineRequest(sku, 3), new ReserveStockRequest.LineRequest(sku, 2)));
        ResponseEntity<ReservationResponse> response = restTemplate.postForEntity("/reservations", request, ReservationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().lines()).containsExactly(new ReservationResponse.LineResponse(sku, 5));

        StockItemResponse stockItem =
                restTemplate.getForEntity("/stock-items/" + sku, StockItemResponse.class).getBody();
        assertThat(stockItem.availableQuantity()).isEqualTo(5);
    }

    @Test
    void release_creditsStockBack() {
        String sku = "WIDGET-" + UUID.randomUUID();
        createStockItem(sku, 10);
        UUID orderId = UUID.randomUUID();
        reserve(orderId, sku, 4);

        ResponseEntity<Void> releaseResponse =
                restTemplate.postForEntity("/reservations/" + orderId + "/release", null, Void.class);

        assertThat(releaseResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        StockItemResponse stockItem =
                restTemplate.getForEntity("/stock-items/" + sku, StockItemResponse.class).getBody();
        assertThat(stockItem.availableQuantity()).isEqualTo(10);
    }

    @Test
    void release_returnsBadRequest_whenAlreadyReleased() {
        String sku = "WIDGET-" + UUID.randomUUID();
        createStockItem(sku, 10);
        UUID orderId = UUID.randomUUID();
        reserve(orderId, sku, 4);
        restTemplate.postForEntity("/reservations/" + orderId + "/release", null, Void.class);

        ResponseEntity<ErrorResponse> response =
                restTemplate.postForEntity("/reservations/" + orderId + "/release", null, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void release_returnsNotFound_whenNoReservationForOrder() {
        ResponseEntity<ErrorResponse> response =
                restTemplate.postForEntity("/reservations/" + UUID.randomUUID() + "/release", null, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void reserve_returnsBadRequest_whenLinesIsEmpty() {
        var request = new ReserveStockRequest(UUID.randomUUID(), List.of());

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity("/reservations", request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
