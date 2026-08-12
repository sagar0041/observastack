package com.observastack.orderservice.domain;

/**
 * Thrown when an {@link Order} is constructed with line items priced in
 * more than one currency.
 *
 * <p>An order is placed, paid for, and refunded as a single monetary
 * total — mixing currencies within it would make {@link Order#totalPrice}
 * meaningless. This invariant is enforced in the constructor, alongside
 * the empty-line-items check.
 */
public class MixedCurrencyException extends RuntimeException {

    /**
     * @param orderId the identity the offending order would have had
     */
    public MixedCurrencyException(OrderId orderId) {
        super("order " + orderId.value() + " has line items priced in more than one currency");
    }
}
