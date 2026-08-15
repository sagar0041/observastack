package com.observastack.orderservice.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifies the customer an {@link Order} belongs to.
 *
 * <p>The order service does not own customer data — this is a reference
 * to an identity that lives elsewhere, carried only so an order can be
 * attributed to whoever placed it.
 *
 * @param value the underlying UUID; must not be null
 */
public record CustomerId(UUID value) {

    public CustomerId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
