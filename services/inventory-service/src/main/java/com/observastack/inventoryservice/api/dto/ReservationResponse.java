package com.observastack.inventoryservice.api.dto;

import com.observastack.inventoryservice.domain.Reservation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response body for {@code POST /reservations}.
 */
public record ReservationResponse(
        UUID id, UUID orderId, List<LineResponse> lines, Instant reservedAt, Instant releasedAt) {

    /**
     * One reserved line as rendered in a response.
     */
    public record LineResponse(String sku, int quantity) {}

    /**
     * @param reservation the reservation to render; must not be null
     * @return the response body, never null
     */
    public static ReservationResponse from(Reservation reservation) {
        List<LineResponse> lines =
                reservation.lines().stream().map(line -> new LineResponse(line.sku().value(), line.quantity())).toList();
        return new ReservationResponse(
                reservation.id().value(), reservation.orderId().value(), lines, reservation.reservedAt(), reservation.releasedAt());
    }
}
