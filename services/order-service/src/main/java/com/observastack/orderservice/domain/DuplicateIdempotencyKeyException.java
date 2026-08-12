package com.observastack.orderservice.domain;

/**
 * Thrown when two orders are placed concurrently with the same
 * {@link IdempotencyKey}.
 *
 * <p>{@link OrderRepository#findByIdempotencyKey} is checked before a
 * new order is built, which handles the common case — a client retrying
 * a dropped response some time later. This exception covers the rarer
 * race where two requests carrying the same key reach the database at
 * nearly the same instant: the database's unique constraint is the
 * actual source of truth, and the loser of that race gets this
 * exception rather than a corrupted duplicate row. The caller should
 * treat it as "retry the read" — {@link OrderRepository#findByIdempotencyKey}
 * will now return the winner's order.
 */
public class DuplicateIdempotencyKeyException extends RuntimeException {

    /**
     * @param idempotencyKey the key that was already in use
     * @param cause          the underlying persistence failure
     */
    public DuplicateIdempotencyKeyException(IdempotencyKey idempotencyKey, Throwable cause) {
        super("an order with idempotency key " + idempotencyKey.value() + " was placed concurrently; retry to fetch it", cause);
    }
}
