package com.observastack.orderservice.domain;

import java.util.Optional;

/**
 * Persistence port for {@link Order} aggregates, owned by the domain and
 * implemented by the infrastructure layer.
 *
 * <p>{@link #save} and {@link #update} are deliberately separate rather
 * than one polymorphic "save or update" method: they have different
 * preconditions (a brand-new identity vs. an already-persisted one) and
 * different costs (an insert needs no lookup first; an update does), and
 * naming them separately makes each call site's intent explicit instead
 * of leaving it to be inferred from context.
 */
public interface OrderRepository {

    /**
     * Persists a brand-new order.
     *
     * <p>The caller guarantees the order doesn't already exist — this is
     * an insert, not an upsert. Used by order placement.
     *
     * @param order the order to persist; must not be null
     * @return the persisted order, never null
     * @throws DuplicateIdempotencyKeyException if another order was placed
     *                                           concurrently with the same
     *                                           {@link IdempotencyKey}
     */
    Order save(Order order);

    /**
     * Persists changes to an order that has already been saved.
     *
     * @param order the order to persist, with its identity unchanged
     *              since it was first saved; must not be null
     * @return the updated order, never null
     * @throws OrderNotFoundException if no order with this identity has
     *                                been saved yet
     */
    Order update(Order order);

    /**
     * Looks up an order by identity.
     *
     * @param id the order's identity; must not be null
     * @return the matching order, or empty if none exists
     */
    Optional<Order> findById(OrderId id);

    /**
     * Looks up an order by the idempotency key it was placed with.
     *
     * @param idempotencyKey the key to look up; must not be null
     * @return the matching order, or empty if no order was placed with
     *         this key
     */
    Optional<Order> findByIdempotencyKey(IdempotencyKey idempotencyKey);
}
