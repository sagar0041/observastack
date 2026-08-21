package com.observastack.inventoryservice.infrastructure.persistence;

import com.observastack.inventoryservice.domain.ConcurrentStockUpdateException;
import com.observastack.inventoryservice.domain.Sku;
import com.observastack.inventoryservice.domain.StockItem;
import com.observastack.inventoryservice.domain.StockItemNotFoundException;
import com.observastack.inventoryservice.domain.StockItemRepository;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

/**
 * JPA-backed implementation of the domain-owned {@link StockItemRepository}
 * port.
 *
 * <p>{@link #update} is where this milestone's concurrency handling
 * actually lives: it loads the entity JPA is already tracking for this
 * request, applies the new quantity to it, and forces an immediate
 * {@code flush()} rather than letting the write sit queued until commit.
 * That matters because a queued write wouldn't hit the database — and so
 * wouldn't trip the {@code @Version} check — until some later point this
 * method has already returned from, by which time nothing here could
 * still translate the failure into {@link ConcurrentStockUpdateException}
 * (order-service's M2 review round hit the identical bug the other way:
 * a queued insert that never actually ran the constraint check it was
 * supposed to be caught by).
 */
@Repository
public class StockItemRepositoryImpl implements StockItemRepository {

    private final StockItemJpaRepository jpaRepository;
    private final StockItemMapper mapper;

    public StockItemRepositoryImpl(StockItemJpaRepository jpaRepository, StockItemMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public StockItem save(StockItem stockItem) {
        StockItemEntity saved = jpaRepository.save(mapper.toEntity(stockItem));
        return mapper.toDomain(saved);
    }

    @Override
    public void update(StockItem stockItem) {
        StockItemEntity entity = jpaRepository
                .findById(stockItem.sku().value())
                .orElseThrow(() -> new StockItemNotFoundException(stockItem.sku()));
        entity.setAvailableQuantity(stockItem.availableQuantity());
        try {
            jpaRepository.flush();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConcurrentStockUpdateException(stockItem.sku());
        }
    }

    @Override
    public Optional<StockItem> findBySku(Sku sku) {
        return jpaRepository.findById(sku.value()).map(mapper::toDomain);
    }
}
