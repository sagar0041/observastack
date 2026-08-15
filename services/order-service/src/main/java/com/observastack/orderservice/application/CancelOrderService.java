package com.observastack.orderservice.application;

import com.observastack.orderservice.domain.IllegalOrderStateException;
import com.observastack.orderservice.domain.Order;
import com.observastack.orderservice.domain.OrderId;
import com.observastack.orderservice.domain.OrderNotFoundException;
import com.observastack.orderservice.domain.OrderRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancels a previously placed order.
 */
@Service
public class CancelOrderService {

    private final OrderRepository orderRepository;
    private final Clock clock;

    /**
     * @param orderRepository port used to load and persist the order; must not be null
     * @param clock           clock used to stamp {@code cancelledAt}; must not be null
     */
    public CancelOrderService(OrderRepository orderRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    /**
     * Cancels the order with the given id.
     *
     * @param id the order's identity; must not be null
     * @return the cancelled order, never null
     * @throws OrderNotFoundException     if no order with that id exists
     * @throws IllegalOrderStateException if the order is already cancelled
     */
    @Transactional
    public Order cancel(OrderId id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
        order.cancel(clock);
        return orderRepository.update(order);
    }
}
