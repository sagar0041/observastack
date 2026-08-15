package com.observastack.orderservice.application;

import com.observastack.orderservice.domain.Order;
import com.observastack.orderservice.domain.OrderId;
import com.observastack.orderservice.domain.OrderNotFoundException;
import com.observastack.orderservice.domain.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Looks up previously placed orders.
 */
@Service
public class GetOrderService {

    private final OrderRepository orderRepository;

    /**
     * @param orderRepository port used to look up orders; must not be null
     */
    public GetOrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Looks up an order by id.
     *
     * @param id the order's identity; must not be null
     * @return the matching order, never null
     * @throws OrderNotFoundException if no order with that id exists
     */
    @Transactional(readOnly = true)
    public Order getById(OrderId id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
