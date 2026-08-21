package com.observastack.inventoryservice.application;

import com.observastack.inventoryservice.domain.Sku;
import com.observastack.inventoryservice.domain.StockItem;
import com.observastack.inventoryservice.domain.StockItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stocks a new SKU with an initial available quantity.
 *
 * <p>This is intentionally the only way stock enters the system for M3 —
 * there's no restock/adjustment API. The milestone this belongs to is
 * about reservation and its concurrency handling; a full inventory
 * management surface (restocking, adjustments, low-stock thresholds)
 * isn't part of that and would be scope creep with nothing here to
 * exercise it.
 */
@Service
public class CreateStockItemService {

    private final StockItemRepository stockItemRepository;

    public CreateStockItemService(StockItemRepository stockItemRepository) {
        this.stockItemRepository = stockItemRepository;
    }

    /**
     * Creates a new stock item.
     *
     * @param command the SKU and initial quantity to stock; must not be null
     * @return the created stock item, never null
     * @throws IllegalArgumentException if the SKU is blank or quantity is negative
     */
    @Transactional
    public StockItem create(CreateStockItemCommand command) {
        StockItem stockItem = StockItem.of(new Sku(command.sku()), command.quantity());
        return stockItemRepository.save(stockItem);
    }
}
