package com.observastack.inventoryservice.domain;

/**
 * Thrown when a {@link Reservation} is asked to release stock it has
 * already released.
 */
public class IllegalReservationStateException extends RuntimeException {

    /**
     * @param id the reservation that was already released
     */
    public IllegalReservationStateException(ReservationId id) {
        super("reservation " + id.value() + " has already been released");
    }
}
