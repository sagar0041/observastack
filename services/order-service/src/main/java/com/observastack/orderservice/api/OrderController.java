package com.observastack.orderservice.api;

import com.observastack.orderservice.api.dto.OrderResponse;
import com.observastack.orderservice.api.dto.PlaceOrderRequest;
import com.observastack.orderservice.application.GetOrderService;
import com.observastack.orderservice.application.PlaceOrderCommand;
import com.observastack.orderservice.application.PlaceOrderService;
import com.observastack.orderservice.domain.Order;
import com.observastack.orderservice.domain.OrderId;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for placing and retrieving orders.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final PlaceOrderService placeOrderService;
    private final GetOrderService getOrderService;

    /**
     * @param placeOrderService use case backing {@code POST /orders}; must not be null
     * @param getOrderService   use case backing {@code GET /orders/{id}}; must not be null
     */
    public OrderController(PlaceOrderService placeOrderService, GetOrderService getOrderService) {
        this.placeOrderService = placeOrderService;
        this.getOrderService = getOrderService;
    }

    /**
     * Places a new order.
     *
     * @param request the order to place; must be valid per its own constraints
     * @return 201 Created, with a {@code Location} header and the placed order
     */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        List<PlaceOrderCommand.LineItem> items = request.lineItems().stream()
                .map(li -> new PlaceOrderCommand.LineItem(li.sku(), li.quantity(), li.unitPrice()))
                .toList();
        Order order = placeOrderService.placeOrder(new PlaceOrderCommand(request.customerId(), items));

        return ResponseEntity.created(URI.create("/orders/" + order.id().value()))
                .body(OrderResponse.from(order));
    }

    /**
     * Looks up an order by id.
     *
     * @param id the order's id
     * @return 200 OK with the order
     */
    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable UUID id) {
        Order order = getOrderService.getById(new OrderId(id));
        return OrderResponse.from(order);
    }
}
