package com.observastack.orderservice.infrastructure.persistence;

import com.observastack.orderservice.domain.DuplicateIdempotencyKeyException;
import com.observastack.orderservice.domain.IdempotencyKey;
import com.observastack.orderservice.domain.Order;
import com.observastack.orderservice.domain.OrderId;
import com.observastack.orderservice.domain.OrderNotFoundException;
import com.observastack.orderservice.domain.OrderRepository;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/**
 * JPA-backed implementation of the domain-owned {@link OrderRepository} port.
 */
@Repository
public class OrderRepositoryImpl implements OrderRepository {

    /**
     * Matches the {@code uniqueConstraintName} given to the idempotency
     * key's unique constraint in the Liquibase changelog. Postgres
     * includes the constraint name in the driver error it raises on
     * violation, which is how {@link #save} tells a duplicate-key
     * conflict apart from any other constraint failure.
     */
    private static final String IDEMPOTENCY_KEY_CONSTRAINT_NAME = "uq_orders_idempotency_key";

    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    /**
     * @param jpaRepository Spring Data repository doing the actual persistence; must not be null
     * @param mapper        converts between {@link Order} and {@link OrderEntity}; must not be null
     */
    public OrderRepositoryImpl(OrderJpaRepository jpaRepository, OrderMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Order save(Order order) {
        try {
            // saveAndFlush, not save: plain save() just queues the insert
            // in the persistence context — Hibernate wouldn't actually
            // send it, and so wouldn't hit the unique constraint, until
            // some later flush (the transaction's commit, or whatever
            // query happens to trigger one next). By then this catch
            // block is long gone, so the conflict would surface as a
            // raw, untranslated failure somewhere else entirely. Forcing
            // the flush here keeps the constraint check inside this
            // try block, where it's actually being handled.
            OrderEntity saved = jpaRepository.saveAndFlush(mapper.toEntity(order));
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            if (isIdempotencyKeyConflict(e)) {
                throw new DuplicateIdempotencyKeyException(order.idempotencyKey(), e);
            }
            throw e;
        }
    }

    @Override
    public Order update(Order order) {
        OrderEntity entity = jpaRepository
                .findById(order.id().value())
                .orElseThrow(() -> new OrderNotFoundException(order.id()));
        // entity is already managed by this persistence context (findById
        // just loaded it), so mutating it via the mapper is enough —
        // Hibernate's dirty checking will write the change back on its
        // own. Calling save() here, on an entity Persistable already
        // reports as not-new, would route through merge() instead, which
        // — since source and target would be the very same managed
        // instance — breaks on the lineItems collection. flush() just
        // forces the already-tracked change to execute now.
        mapper.updateEntity(order, entity);
        jpaRepository.flush();
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Order> findByIdempotencyKey(IdempotencyKey idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey.value()).map(mapper::toDomain);
    }

    private boolean isIdempotencyKeyConflict(DataIntegrityViolationException e) {
        Throwable cause = e.getMostSpecificCause();
        return cause.getMessage() != null && cause.getMessage().contains(IDEMPOTENCY_KEY_CONSTRAINT_NAME);
    }
}
