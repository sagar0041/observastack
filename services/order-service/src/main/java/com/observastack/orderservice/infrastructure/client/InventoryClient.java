package com.observastack.orderservice.infrastructure.client;

import com.observastack.orderservice.application.InventoryPort;
import com.observastack.orderservice.application.StockUnavailableException;
import com.observastack.orderservice.domain.OrderId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * HTTP-backed implementation of {@link InventoryPort}, calling
 * inventory-service's {@code /reservations} API synchronously.
 *
 * <p>Only {@code 404} and {@code 409} from a reservation attempt are
 * translated to {@link StockUnavailableException} — those are the shapes
 * of "inventory looked at this and said no." A {@code 400} would mean
 * this client sent something malformed (a bug here, not a business
 * outcome), and anything else — a 5xx, a timeout, a connection failure —
 * is left to propagate as the unchecked exception it already is, so
 * {@link com.observastack.orderservice.application.PlaceOrderService}
 * can't mistake "inventory is unreachable" for "inventory said no."
 */
@Component
class InventoryClient implements InventoryPort {

    private final RestClient restClient;

    InventoryClient(RestClient inventoryRestClient) {
        this.restClient = inventoryRestClient;
    }

    @Override
    public void reserve(OrderId orderId, List<LineItem> lines) {
        ReserveRequest request = new ReserveRequest(
                orderId.value(),
                lines.stream().map(line -> new ReserveRequest.Line(line.sku().value(), line.quantity())).toList());
        try {
            restClient.post().uri("/reservations").body(request).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT || e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new StockUnavailableException(orderId, e);
            }
            throw e;
        }
    }

    @Override
    public void release(OrderId orderId) {
        try {
            restClient.post().uri("/reservations/{orderId}/release", orderId.value()).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            // No reservation to release means the goal — this order isn't
            // holding stock — is already true. Not a failure.
        }
    }

    private record ReserveRequest(UUID orderId, List<Line> lines) {
        private record Line(String sku, int quantity) {}
    }
}
