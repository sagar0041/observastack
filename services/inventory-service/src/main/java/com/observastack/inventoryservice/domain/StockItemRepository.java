package com.observastack.inventoryservice.domain;

import java.util.Optional;

/**
 * Persistence port for {@link StockItem}, owned by the domain and
 * implemented by the infrastructure layer.
 *
 * <p>{@link #save} and {@link #update} are separate for the same reason
 * as order-service's {@code OrderRepository}: an insert of a brand-new
 * SKU and an update to an existing one have different preconditions and
 * costs, and {@link #update}'s optimistic-concurrency contract only
 * makes sense for a row that's already there.
 */
public interface StockItemRepository {

    /**
     * Persists a brand-new stock item.
     *
     * @param stockItem the stock item to persist; must not be null, and
     *                  its SKU must not already have a stock item
     * @return the persisted stock item, never null
     */
    StockItem save(StockItem stockItem);

    /**
     * Persists changes to a stock item that already exists, using
     * optimistic concurrency control.
     *
     * @param stockItem the stock item to persist, as read and mutated by
     *                  the caller; must not be null
     * @throws ConcurrentStockUpdateException if the underlying row changed
     *                                        since {@code stockItem} was read
     * @throws StockItemNotFoundException     if no stock item exists for
     *                                        this SKU
     */
    void update(StockItem stockItem);

    /**
     * Looks up a stock item by SKU.
     *
     * @param sku the SKU to look up; must not be null
     * @return the matching stock item, or empty if none exists
     */
    Optional<StockItem> findBySku(Sku sku);
}
