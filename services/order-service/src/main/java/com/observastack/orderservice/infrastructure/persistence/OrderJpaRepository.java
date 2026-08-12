package com.observastack.orderservice.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository backing {@link OrderRepositoryImpl}.
 */
public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {}
