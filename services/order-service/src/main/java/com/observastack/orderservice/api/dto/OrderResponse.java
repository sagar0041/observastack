package com.observastack.orderservice.api.dto;

import com.observastack.orderservice.domain.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response body for {@code POST /orders} and {@code GET /orders/{id}}.
 */
public record OrderResponse(
        UUID id,
        UUID customerId,
        String status,
        List<LineItemResponse> lineItems,
        BigDecimal totalPrice,
        Instant createdAt,
        Instant placedAt,
        Instant cancelledAt) {

    /**
     * One line item as rendered in a response.
     */
    public record LineItemResponse(String sku, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}

    /**
     * Builds a response from a domain order.
     *
     * @param order the order to render; must not be null
     * @return the response body, never null
     */
    public static OrderResponse from(Order order) {
        List<LineItemResponse> items = order.lineItems().stream()
                .map(li -> new LineItemResponse(
                        li.sku().value(), li.quantity(), li.unitPrice().amount(), li.lineTotal().amount()))
                .toList();
        return new OrderResponse(
                order.id().value(),
                order.customerId().value(),
                order.status().name(),
                items,
                order.totalPrice().amount(),
                order.createdAt(),
                order.placedAt(),
                order.cancelledAt());
    }
}
