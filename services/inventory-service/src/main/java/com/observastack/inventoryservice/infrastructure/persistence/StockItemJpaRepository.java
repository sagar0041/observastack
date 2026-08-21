package com.observastack.inventoryservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository backing {@link StockItemRepositoryImpl}.
 */
public interface StockItemJpaRepository extends JpaRepository<StockItemEntity, String> {}
