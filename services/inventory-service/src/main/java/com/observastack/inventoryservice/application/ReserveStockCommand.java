package com.observastack.inventoryservice.application;

import java.util.List;
import java.util.UUID;

/**
 * Input to {@link ReserveStockService#reserve}.
 *
 * <p>Carries plain values rather than domain types, for the same reason
 * as order-service's {@code PlaceOrderCommand}: the use case, not the
 * API layer, is responsible for turning them into validated domain
 * objects.
 *
 * @param orderId the order this reservation is for; must not be null
 * @param lines   the SKUs and quantities to reserve; must not be null or
 *                empty
 */
public record ReserveStockCommand(UUID orderId, List<Line> lines) {

    /**
     * @param sku      stock keeping unit; must not be blank
     * @param quantity units requested; must be positive
     */
    public record Line(String sku, int quantity) {}
}
