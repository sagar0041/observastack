package com.observastack.orderservice.infrastructure.persistence;

/**
 * Persisted representation of {@code com.observastack.orderservice.domain.OrderStatus}.
 *
 * <p>Kept as a separate type rather than reusing the domain enum
 * directly, so the persistence layer has no compile-time dependency on
 * the domain layer's lifecycle representation — {@link OrderMapper} is
 * the only place the two are related.
 */
public enum OrderStatusEntity {
    CREATED,
    PLACED,
    CANCELLED
}
