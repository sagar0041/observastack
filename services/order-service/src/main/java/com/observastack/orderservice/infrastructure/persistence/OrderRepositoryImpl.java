package com.observastack.orderservice.infrastructure.persistence;

import com.observastack.orderservice.domain.Order;
import com.observastack.orderservice.domain.OrderId;
import com.observastack.orderservice.domain.OrderRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * JPA-backed implementation of the domain-owned {@link OrderRepository} port.
 */
@Repository
public class OrderRepositoryImpl implements OrderRepository {

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
        OrderEntity saved = jpaRepository.save(mapper.toEntity(order));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }
}
