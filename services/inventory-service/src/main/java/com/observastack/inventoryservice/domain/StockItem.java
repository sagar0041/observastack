package com.observastack.inventoryservice.domain;

import java.util.Objects;

/**
 * The stock ledger entry for one SKU: how many units are currently
 * available to reserve.
 *
 * <p>{@link #reserve} and {@link #release} are the only ways to change
 * {@link #availableQuantity} — there is no setter, so the quantity can
 * never go negative except by a bug in one of those two methods, not by
 * a caller pushing an arbitrary value in.
 */
public final class StockItem {

    private final Sku sku;
    private int availableQuantity;

    private StockItem(Sku sku, int availableQuantity) {
        this.sku = Objects.requireNonNull(sku, "sku must not be null");
        if (availableQuantity < 0) {
            throw new IllegalArgumentException("availableQuantity must not be negative");
        }
        this.availableQuantity = availableQuantity;
    }

    /**
     * Builds a stock item with the given available quantity — used both
     * to record a newly-stocked SKU and to restore one from persistence;
     * the two cases have no divergent behaviour, unlike an {@code Order},
     * so there's no separate {@code create}/{@code reconstruct} split.
     *
     * @param sku               the SKU this entry tracks; must not be null
     * @param availableQuantity units currently available; must not be negative
     * @return the stock item, never null
     */
    public static StockItem of(Sku sku, int availableQuantity) {
        return new StockItem(sku, availableQuantity);
    }

    /**
     * Reserves units of this SKU, decrementing what's available.
     *
     * @param quantity units to reserve; must be positive
     * @throws InsufficientStockException if {@code quantity} exceeds what's
     *                                    currently available
     * @throws IllegalArgumentException   if {@code quantity} is not positive
     */
    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (quantity > availableQuantity) {
            throw new InsufficientStockException(sku, quantity, availableQuantity);
        }
        availableQuantity -= quantity;
    }

    /**
     * Releases previously-reserved units of this SKU back to available
     * stock.
     *
     * @param quantity units to release; must be positive
     * @throws IllegalArgumentException if {@code quantity} is not positive
     */
    public void release(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        availableQuantity += quantity;
    }

    public Sku sku() {
        return sku;
    }

    public int availableQuantity() {
        return availableQuantity;
    }
}
