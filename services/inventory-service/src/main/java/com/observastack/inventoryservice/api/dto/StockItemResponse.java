package com.observastack.inventoryservice.api.dto;

import com.observastack.inventoryservice.domain.StockItem;

/**
 * Response body for {@code POST /stock-items} and {@code GET /stock-items/{sku}}.
 */
public record StockItemResponse(String sku, int availableQuantity) {

    /**
     * @param stockItem the stock item to render; must not be null
     * @return the response body, never null
     */
    public static StockItemResponse from(StockItem stockItem) {
        return new StockItemResponse(stockItem.sku().value(), stockItem.availableQuantity());
    }
}
