package com.observastack.inventoryservice.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * A reference to the order-service order a {@link Reservation} was made
 * for.
 *
 * <p>Inventory does not own order data — this is an opaque foreign
 * identity, carried only so a reservation can be looked up and released
 * by the order it belongs to.
 *
 * @param value the underlying UUID; must not be null
 */
public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
