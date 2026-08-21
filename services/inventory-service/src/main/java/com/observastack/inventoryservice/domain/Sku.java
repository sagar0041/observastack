package com.observastack.inventoryservice.domain;

/**
 * A stock keeping unit identifying a line of stock.
 *
 * <p>Inventory owns the authoritative notion of what a SKU is stocked
 * as; order-service carries its own, independently-validated {@code Sku}
 * for the same text — the two services share no code, only a
 * convention, which is deliberate: each bounded context defines its own
 * types rather than depending on a shared kernel.
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
