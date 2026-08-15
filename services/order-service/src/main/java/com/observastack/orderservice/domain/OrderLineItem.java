package com.observastack.orderservice.domain;

import java.util.Objects;

/**
 * A single requested item on an {@link Order}: a SKU, a quantity, and the
 * price agreed for one unit at the time of ordering.
 *
 * <p>A line item has no identity of its own — two line items with the
 * same SKU, quantity, and price are interchangeable — so it is modelled
 * as a value, not an entity.
 *
 * @param sku       the stock keeping unit ordered; must not be null
 * @param quantity  units requested; must be positive
 * @param unitPrice price agreed per unit; must not be null
 */
public record OrderLineItem(Sku sku, int quantity, Money unitPrice) {

    public OrderLineItem {
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }

    /**
     * The total price for this line: {@code unitPrice * quantity}.
     *
     * @return the line total, never null
     */
    public Money lineTotal() {
        return unitPrice.multiply(quantity);
    }
}
