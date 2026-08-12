package com.observastack.orderservice.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Input to {@link PlaceOrderService#placeOrder}.
 *
 * <p>Carries plain values rather than domain types — translating those
 * values into validated domain objects ({@code Sku}, {@code Money}, ...)
 * is the use case's job, not the API layer's. This keeps the API layer's
 * request DTOs from leaking into the application layer, and vice versa.
 *
 * @param customerId the ordering customer's identity; must not be null
 * @param lineItems  the items being ordered; must not be null or empty
 */
public record PlaceOrderCommand(UUID customerId, List<LineItem> lineItems) {

    /**
     * One requested line item.
     *
     * @param sku       stock keeping unit; must not be blank
     * @param quantity  units requested; must be positive
     * @param unitPrice price per unit; must not be null or negative
     */
    public record LineItem(String sku, int quantity, BigDecimal unitPrice) {}
}
