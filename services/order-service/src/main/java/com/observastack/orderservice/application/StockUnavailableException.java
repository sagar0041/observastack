package com.observastack.orderservice.application;

import com.observastack.orderservice.domain.OrderId;

/**
 * Thrown when inventory-service could not reserve the stock an order
 * needs — a SKU is unknown, or doesn't have enough available.
 *
 * <p>This is a business outcome {@link PlaceOrderService} reacts to by
 * cancelling the order rather than placing it, not a technical failure.
 * A genuinely unexpected failure talking to inventory-service (it's
 * down, timed out, or returned a 5xx) is deliberately a different,
 * unchecked exception type that propagates instead — conflating "we
 * asked and were told no" with "we couldn't even ask" would silently
 * turn an infrastructure outage into what looks to the customer like an
 * out-of-stock order.
 */
public class StockUnavailableException extends RuntimeException {

    /**
     * @param orderId the order that couldn't be fully reserved
     * @param cause   the underlying rejection from inventory-service
     */
    public StockUnavailableException(OrderId orderId, Throwable cause) {
        super("inventory could not reserve stock for order " + orderId.value(), cause);
    }
}
