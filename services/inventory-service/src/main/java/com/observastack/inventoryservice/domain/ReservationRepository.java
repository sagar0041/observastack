package com.observastack.inventoryservice.domain;

import java.util.Optional;

/**
 * Persistence port for {@link Reservation}, owned by the domain and
 * implemented by the infrastructure layer.
 */
public interface ReservationRepository {

    /**
     * Persists a brand-new reservation.
     *
     * @param reservation the reservation to persist; must not be null
     * @return the persisted reservation, never null
     * @throws DuplicateReservationException if another reservation was
     *                                       created concurrently for the
     *                                       same {@link OrderId}
     */
    Reservation save(Reservation reservation);

    /**
     * Persists changes to a reservation that already exists (in
     * practice, only {@link Reservation#release}).
     *
     * @param reservation the reservation to persist; must not be null
     * @throws ReservationNotFoundException if no reservation with this
     *                                      identity has been saved yet
     */
    void update(Reservation reservation);

    /**
     * Looks up a reservation by the order it was made for.
     *
     * @param orderId the order id to look up; must not be null
     * @return the matching reservation, or empty if none exists
     */
    Optional<Reservation> findByOrderId(OrderId orderId);
}
