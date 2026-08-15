package com.observastack.orderservice.domain;

/**
 * Thrown when an {@link Order} is constructed with no line items.
 *
 * <p>An order that orders nothing is not a valid order — this invariant
 * is enforced in the constructor, so it is impossible to observe an
 * {@link Order} instance that violates it.
 */
public class EmptyOrderException extends RuntimeException {

    /**
     * @param orderId the identity the offending order would have had
     */
    public EmptyOrderException(OrderId orderId) {
        super("order " + orderId.value() + " must have at least one line item");
    }
}
