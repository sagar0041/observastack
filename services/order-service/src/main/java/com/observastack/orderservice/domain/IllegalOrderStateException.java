package com.observastack.orderservice.domain;

/**
 * Thrown when an {@link Order} is asked to transition to a state its
 * current {@link OrderStatus} does not allow — for example, placing an
 * order that has already been cancelled.
 */
public class IllegalOrderStateException extends RuntimeException {

    /**
     * @param orderId   the order that was asked to transition
     * @param current   its status at the time of the attempt
     * @param attempted the status the caller tried to move it to
     */
    public IllegalOrderStateException(OrderId orderId, OrderStatus current, OrderStatus attempted) {
        super("order " + orderId.value() + " cannot transition from " + current + " to " + attempted);
    }
}
