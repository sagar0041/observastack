package com.observastack.inventoryservice.domain;

import java.util.Objects;

/**
 * One SKU-and-quantity entry within a {@link Reservation}.
 *
 * @param sku      the stock keeping unit reserved; must not be null
 * @param quantity units reserved; must be positive
 */
public record ReservationLine(Sku sku, int quantity) {

    public ReservationLine {
        Objects.requireNonNull(sku, "sku must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
