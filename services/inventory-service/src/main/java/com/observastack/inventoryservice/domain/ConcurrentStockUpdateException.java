package com.observastack.inventoryservice.domain;

/**
 * Thrown by {@link StockItemRepository#update} when a {@link StockItem}
 * was modified by another transaction between when it was read and when
 * this update was attempted.
 *
 * <p>This is the domain-level shape of what the infrastructure layer
 * detects as a JPA optimistic-locking conflict — the interface stays
 * free of any JPA type. The caller (the application layer's retry loop)
 * is expected to re-read the stock item and try again against current
 * state, not treat this as a final failure.
 */
public class ConcurrentStockUpdateException extends RuntimeException {

    /**
     * @param sku the SKU whose stock item was concurrently modified
     */
    public ConcurrentStockUpdateException(Sku sku) {
        super("stock item " + sku.value() + " was concurrently modified");
    }
}
