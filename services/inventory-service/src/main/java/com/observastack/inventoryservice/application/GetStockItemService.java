package com.observastack.inventoryservice.application;

import com.observastack.inventoryservice.domain.Sku;
import com.observastack.inventoryservice.domain.StockItem;
import com.observastack.inventoryservice.domain.StockItemNotFoundException;
import com.observastack.inventoryservice.domain.StockItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Looks up a stock item by SKU.
 */
@Service
public class GetStockItemService {

    private final StockItemRepository stockItemRepository;

    public GetStockItemService(StockItemRepository stockItemRepository) {
        this.stockItemRepository = stockItemRepository;
    }

    /**
     * @param sku the SKU to look up; must not be null
     * @return the matching stock item, never null
     * @throws StockItemNotFoundException if no stock item exists for this SKU
     */
    @Transactional(readOnly = true)
    public StockItem getBySku(Sku sku) {
        return stockItemRepository.findBySku(sku).orElseThrow(() -> new StockItemNotFoundException(sku));
    }
}
