package com.observastack.inventoryservice.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifies a {@link Reservation} uniquely and permanently.
 *
 * @param value the underlying UUID; must not be null
 */
public record ReservationId(UUID value) {

    public ReservationId {
        Objects.requireNonNull(value, "value must not be null");
    }

    /**
     * Generates a new, random reservation identity.
     *
     * @return a freshly generated {@link ReservationId}, never null
     */
    public static ReservationId newId() {
        return new ReservationId(UUID.randomUUID());
    }
}
