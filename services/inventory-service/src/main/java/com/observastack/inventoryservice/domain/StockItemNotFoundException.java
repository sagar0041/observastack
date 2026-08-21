package com.observastack.inventoryservice.domain;

/**
 * Thrown when no {@link StockItem} exists for a given {@link Sku} — the
 * SKU isn't stocked at all, as distinct from {@link InsufficientStockException}
 * where it's stocked but out.
 */
public class StockItemNotFoundException extends RuntimeException {

    /**
     * @param sku the SKU that has no stock item
     */
    public StockItemNotFoundException(Sku sku) {
        super("no stock item for sku " + sku.value());
    }
}
