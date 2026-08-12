package com.observastack.orderservice.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifies an {@link Order} uniquely and permanently.
 *
 * @param value the underlying UUID; must not be null
 */
public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "value must not be null");
    }

    /**
     * Generates a new, random order identity.
     *
     * @return a freshly generated {@link OrderId}, never null
     */
    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }
}
