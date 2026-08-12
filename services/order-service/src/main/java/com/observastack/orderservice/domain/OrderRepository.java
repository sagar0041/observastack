package com.observastack.orderservice.domain;

import java.util.Optional;

/**
 * Persistence port for {@link Order} aggregates, owned by the domain and
 * implemented by the infrastructure layer.
 */
public interface OrderRepository {

    /**
     * Persists an order, inserting or updating as appropriate.
     *
     * @param order the order to persist; must not be null
     * @return the persisted order, never null
     */
    Order save(Order order);

    /**
     * Looks up an order by identity.
     *
     * @param id the order's identity; must not be null
     * @return the matching order, or empty if none exists
     */
    Optional<Order> findById(OrderId id);
}
