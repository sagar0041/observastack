package com.observastack.orderservice.application;

import com.observastack.orderservice.domain.IllegalOrderStateException;
import com.observastack.orderservice.domain.Order;
import com.observastack.orderservice.domain.OrderId;
import com.observastack.orderservice.domain.OrderNotFoundException;
import com.observastack.orderservice.domain.OrderRepository;
import com.observastack.orderservice.domain.OrderStatus;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancels a previously placed order.
 */
@Service
public class CancelOrderService {

    private final OrderRepository orderRepository;
    private final InventoryPort inventoryPort;
    private final Clock clock;

    /**
     * @param orderRepository port used to load and persist the order; must not be null
     * @param inventoryPort   port used to release the order's reservation, if it has one; must not be null
     * @param clock           clock used to stamp {@code cancelledAt}; must not be null
     */
    public CancelOrderService(OrderRepository orderRepository, InventoryPort inventoryPort, Clock clock) {
        this.orderRepository = orderRepository;
        this.inventoryPort = inventoryPort;
        this.clock = clock;
    }

    /**
     * Cancels the order with the given id.
     *
     * <p>Only a {@code PLACED} order ever has a live reservation — one
     * that failed to place was already cancelled without ever reserving
     * anything, see {@link PlaceOrderService} — so inventory is only
     * called for that case.
     *
     * @param id the order's identity; must not be null
     * @return the cancelled order, never null
     * @throws OrderNotFoundException     if no order with that id exists
     * @throws IllegalOrderStateException if the order is already cancelled
     */
    @Transactional
    public Order cancel(OrderId id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
        boolean hadReservation = order.status() == OrderStatus.PLACED;

        order.cancel(clock);
        if (hadReservation) {
            inventoryPort.release(id);
        }

        return orderRepository.update(order);
    }
}
