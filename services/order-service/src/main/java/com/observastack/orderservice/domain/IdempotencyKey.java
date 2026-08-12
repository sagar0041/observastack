package com.observastack.orderservice.domain;

/**
 * A client-supplied key that makes order placement safe to retry.
 *
 * <p>The order service treats a duplicate {@link #value} as "this is the
 * same placement request arriving again," not "place another order" —
 * see {@link OrderRepository#findByIdempotencyKey} and
 * {@link DuplicateIdempotencyKeyException}. Without this, a client retry
 * after a dropped response (a timeout, a lost connection) would create a
 * second, distinct order for what the customer experienced as one
 * checkout.
 *
 * @param value the idempotency key text, typically a client-generated
 *              UUID; must not be null or blank
 */
public record IdempotencyKey(String value) {

    public IdempotencyKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
