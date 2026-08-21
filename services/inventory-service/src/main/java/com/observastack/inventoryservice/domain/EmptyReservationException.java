package com.observastack.inventoryservice.domain;

/**
 * Thrown when a {@link Reservation} is constructed with no lines.
 */
public class EmptyReservationException extends RuntimeException {

    /**
     * @param orderId the order the offending reservation would have been for
     */
    public EmptyReservationException(OrderId orderId) {
        super("reservation for order " + orderId.value() + " must have at least one line");
    }
}
