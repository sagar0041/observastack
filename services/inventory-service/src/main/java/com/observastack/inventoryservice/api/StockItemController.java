package com.observastack.inventoryservice.api;

import com.observastack.inventoryservice.api.dto.CreateStockItemRequest;
import com.observastack.inventoryservice.api.dto.StockItemResponse;
import com.observastack.inventoryservice.application.CreateStockItemCommand;
import com.observastack.inventoryservice.application.CreateStockItemService;
import com.observastack.inventoryservice.application.GetStockItemService;
import com.observastack.inventoryservice.domain.Sku;
import com.observastack.inventoryservice.domain.StockItem;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for stocking and looking up SKUs.
 */
@RestController
@RequestMapping("/stock-items")
public class StockItemController {

    private final CreateStockItemService createStockItemService;
    private final GetStockItemService getStockItemService;

    /**
     * @param createStockItemService use case backing {@code POST /stock-items}; must not be null
     * @param getStockItemService    use case backing {@code GET /stock-items/{sku}}; must not be null
     */
    public StockItemController(CreateStockItemService createStockItemService, GetStockItemService getStockItemService) {
        this.createStockItemService = createStockItemService;
        this.getStockItemService = getStockItemService;
    }

    /**
     * Stocks a new SKU.
     *
     * @param request the SKU and initial quantity; must be valid per its own constraints
     * @return 201 Created, with a {@code Location} header and the stock item
     */
    @PostMapping
    public ResponseEntity<StockItemResponse> createStockItem(@Valid @RequestBody CreateStockItemRequest request) {
        StockItem stockItem =
                createStockItemService.create(new CreateStockItemCommand(request.sku(), request.quantity()));
        return ResponseEntity.created(URI.create("/stock-items/" + stockItem.sku().value()))
                .body(StockItemResponse.from(stockItem));
    }

    /**
     * Looks up a stock item by SKU.
     *
     * @param sku the SKU to look up
     * @return 200 OK with the stock item
     */
    @GetMapping("/{sku}")
    public StockItemResponse getStockItem(@PathVariable String sku) {
        StockItem stockItem = getStockItemService.getBySku(new Sku(sku));
        return StockItemResponse.from(stockItem);
    }
}
