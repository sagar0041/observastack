package com.observastack.inventoryservice.infrastructure.persistence;

import com.observastack.inventoryservice.domain.Sku;
import com.observastack.inventoryservice.domain.StockItem;
import org.springframework.stereotype.Component;

/**
 * Converts between the {@code StockItem} aggregate and its persisted
 * {@link StockItemEntity} representation.
 *
 * <p>Hand-written rather than MapStruct, unlike most other mappers in
 * this codebase: {@link StockItem} is built via the static factory
 * {@code StockItem.of(...)}, not a public constructor MapStruct can
 * drive, and with only two fields there's nothing generation would save
 * over writing the two directions directly.
 */
@Component
public class StockItemMapper {

    /**
     * @param stockItem the stock item to convert; must not be null
     * @return the equivalent entity, never null
     */
    public StockItemEntity toEntity(StockItem stockItem) {
        StockItemEntity entity = new StockItemEntity();
        entity.setSku(stockItem.sku().value());
        entity.setAvailableQuantity(stockItem.availableQuantity());
        return entity;
    }

    /**
     * @param entity the entity to convert; must not be null
     * @return the equivalent domain stock item, never null
     */
    public StockItem toDomain(StockItemEntity entity) {
        return StockItem.of(new Sku(entity.getSku()), entity.getAvailableQuantity());
    }
}
