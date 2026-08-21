package com.observastack.inventoryservice.domain;

/**
 * Thrown when a reservation asks for more units of a SKU than are
 * currently available.
 *
 * <p>Reservation is optimistic: the caller may receive this exception
 * even after a successful availability check, because concurrent
 * reservations are resolved at commit time — see
 * {@link ConcurrentStockUpdateException}, which the application layer
 * retries against fresh state, potentially surfacing this exception for
 * real on a later attempt even though an earlier attempt's read looked
 * sufficient.
 */
public class InsufficientStockException extends RuntimeException {

    /**
     * @param sku       the SKU that couldn't be fully reserved
     * @param requested units requested
     * @param available units actually available at the time of the attempt
     */
    public InsufficientStockException(Sku sku, int requested, int available) {
        super("insufficient stock for " + sku.value() + ": requested " + requested + ", available " + available);
    }
}
