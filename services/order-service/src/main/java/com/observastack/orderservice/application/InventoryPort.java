package com.observastack.orderservice.application;

import com.observastack.orderservice.domain.OrderId;
import com.observastack.orderservice.domain.Sku;
import java.util.List;

/**
 * Application-level port to inventory-service's synchronous stock
 * reservation API.
 *
 * <p>Owned by the application layer, not the domain: the domain layer
 * has no notion that inventory even exists — placing an order and
 * reserving stock for it are two separate services' concerns, and this
 * is the seam between them. {@code infrastructure.client.InventoryClient}
 * is the only implementation, and the only place an HTTP call happens.
 */
public interface InventoryPort {

    /**
     * Reserves stock for every line on an order, as one atomic
     * reservation on inventory-service's side.
     *
     * @param orderId the order to reserve stock for; must not be null
     * @param lines   the SKUs and quantities to reserve; must not be
     *                null or empty
     * @throws StockUnavailableException if inventory could not honour the
     *                                   reservation — a SKU is unknown or
     *                                   doesn't have enough available.
     *                                   This is a business outcome the
     *                                   caller is expected to handle, not
     *                                   an unexpected failure.
     */
    void reserve(OrderId orderId, List<LineItem> lines);

    /**
     * Releases the reservation made for an order, if any, crediting its
     * stock back. A no-op if inventory has no reservation for this order
     * (e.g. one was never made) — the goal, "this order isn't holding any
     * stock," is already true in that case.
     *
     * @param orderId the order whose reservation should be released; must not be null
     */
    void release(OrderId orderId);

    /**
     * One SKU-and-quantity entry to reserve.
     *
     * @param sku      the stock keeping unit; must not be null
     * @param quantity units requested; must be positive
     */
    record LineItem(Sku sku, int quantity) {}
}
