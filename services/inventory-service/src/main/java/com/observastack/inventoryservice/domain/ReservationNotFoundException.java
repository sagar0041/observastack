package com.observastack.inventoryservice.domain;

/**
 * Thrown when no {@link Reservation} exists for a given {@link OrderId}.
 */
public class ReservationNotFoundException extends RuntimeException {

    /**
     * @param orderId the order id that has no reservation
     */
    public ReservationNotFoundException(OrderId orderId) {
        super("no reservation for order " + orderId.value());
    }
}
