package com.observastack.orderservice.domain;

/**
 * A stock keeping unit identifying what is being ordered.
 *
 * <p>The order service treats a SKU as an opaque, validated string — it
 * has no opinion on whether the SKU actually exists or is in stock. That
 * check belongs to the inventory service (M3).
 *
 * @param value the SKU text; must not be null or blank
 */
public record Sku(String value) {

    public Sku {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
