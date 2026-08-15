package com.observastack.orderservice.domain;

/**
 * Thrown when no {@link Order} exists for a given {@link OrderId}.
 */
public class OrderNotFoundException extends RuntimeException {

    /**
     * @param orderId the identity that no order was found for
     */
    public OrderNotFoundException(OrderId orderId) {
        super("order " + orderId.value() + " not found");
    }
}
