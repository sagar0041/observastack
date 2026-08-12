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
 * @param idempotencyKey client-supplied key that makes this placement
 *                       request safe to retry; must not be null or blank
 * @param customerId     the ordering customer's identity; must not be null
 * @param currency       ISO 4217 currency code every line item is priced
 *                       in; must not be null
 * @param lineItems      the items being ordered; must not be null or empty
 */
public record PlaceOrderCommand(String idempotencyKey, UUID customerId, String currency, List<LineItem> lineItems) {

    /**
     * One requested line item.
     *
     * @param sku       stock keeping unit; must not be blank
     * @param quantity  units requested; must be positive
     * @param unitPrice price per unit; must not be null or negative
     */
    public record LineItem(String sku, int quantity, BigDecimal unitPrice) {}
}
