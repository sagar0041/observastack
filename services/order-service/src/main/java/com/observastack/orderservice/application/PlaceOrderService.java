package com.observastack.orderservice.application;

import com.observastack.orderservice.domain.CustomerId;
import com.observastack.orderservice.domain.EmptyOrderException;
import com.observastack.orderservice.domain.Money;
import com.observastack.orderservice.domain.Order;
import com.observastack.orderservice.domain.OrderLineItem;
import com.observastack.orderservice.domain.OrderRepository;
import com.observastack.orderservice.domain.Sku;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Places an order: builds it from a {@link PlaceOrderCommand}, transitions
 * it to {@code PLACED}, and persists it — all within one transaction.
 */
@Service
public class PlaceOrderService {

    private final OrderRepository orderRepository;
    private final Clock clock;

    /**
     * @param orderRepository port used to persist the placed order; must not be null
     * @param clock           clock used to stamp the order's timestamps; must not be null
     */
    public PlaceOrderService(OrderRepository orderRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    /**
     * Places an order for the given customer and line items.
     *
     * @param command the customer and line items to place an order for; must not be null
     * @return the placed, persisted order, never null
     * @throws EmptyOrderException      if {@code command} has no line items
     * @throws IllegalArgumentException if any line item's SKU, quantity, or
     *                                  price is invalid
     */
    @Transactional
    public Order placeOrder(PlaceOrderCommand command) {
        CustomerId customerId = new CustomerId(command.customerId());
        List<OrderLineItem> lineItems = command.lineItems().stream()
                .map(item -> new OrderLineItem(new Sku(item.sku()), item.quantity(), Money.of(item.unitPrice())))
                .toList();

        Order order = Order.create(customerId, lineItems, clock);
        order.place(clock);

        return orderRepository.save(order);
    }
}
