package com.observastack.inventoryservice.domain;

/**
 * Thrown when two reservation attempts for the same {@link OrderId} race
 * each other to the database.
 *
 * <p>{@link ReservationRepository#findByOrderId} is checked before a new
 * reservation is built, which handles the common case of a retried
 * request. This exception covers the rarer case where two requests for
 * the same order reach the database at nearly the same instant — the
 * database's unique constraint on {@code order_id} is the actual source
 * of truth, and the loser of that race gets this exception rather than a
 * duplicate reservation. The caller should retry the read.
 */
public class DuplicateReservationException extends RuntimeException {

    /**
     * @param orderId the order id that already has a reservation
     * @param cause   the underlying persistence failure
     */
    public DuplicateReservationException(OrderId orderId, Throwable cause) {
        super("a reservation for order " + orderId.value() + " was created concurrently; retry to fetch it", cause);
    }
}
