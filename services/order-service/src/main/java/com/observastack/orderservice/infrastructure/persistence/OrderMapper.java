package com.observastack.orderservice.infrastructure.persistence;

import com.observastack.orderservice.domain.CustomerId;
import com.observastack.orderservice.domain.Money;
import com.observastack.orderservice.domain.Order;
import com.observastack.orderservice.domain.OrderId;
import com.observastack.orderservice.domain.OrderLineItem;
import com.observastack.orderservice.domain.OrderStatus;
import com.observastack.orderservice.domain.Sku;
import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.Mapper;

/**
 * Converts between the {@code Order} aggregate and its persisted
 * {@link OrderEntity} representation.
 *
 * <p>The top-level {@code Order}/{@code OrderEntity} conversion is
 * written by hand rather than generated: {@link Order} has no public
 * constructor or setters — see {@link Order#reconstruct} — which is
 * deliberate, and MapStruct's usual bean-mapping strategy doesn't apply
 * to an aggregate that refuses to expose a mutation surface. The
 * line-item and value-object conversions below are ordinary bean
 * mappings and are left to the annotation processor.
 */
@Mapper(componentModel = "spring")
public abstract class OrderMapper {

    /**
     * Converts a domain order to its persisted form.
     *
     * @param order the order to convert; must not be null
     * @return the equivalent entity, never null
     */
    public OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setId(order.id().value());
        entity.setCustomerId(order.customerId().value());
        entity.setStatus(toEntity(order.status()));
        entity.setCreatedAt(order.createdAt());
        entity.setPlacedAt(order.placedAt());
        entity.setCancelledAt(order.cancelledAt());
        entity.setLineItems(order.lineItems().stream().map(this::toEntity).toList());
        return entity;
    }

    /**
     * Converts a persisted order back to its domain form.
     *
     * @param entity the entity to convert; must not be null
     * @return the equivalent domain order, never null
     */
    public Order toDomain(OrderEntity entity) {
        List<OrderLineItem> lineItems = entity.getLineItems().stream().map(this::toDomain).toList();
        return Order.reconstruct(
                new OrderId(entity.getId()),
                new CustomerId(entity.getCustomerId()),
                lineItems,
                toDomain(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getPlacedAt(),
                entity.getCancelledAt());
    }

    abstract OrderLineItemEmbeddable toEntity(OrderLineItem lineItem);

    abstract OrderLineItem toDomain(OrderLineItemEmbeddable embeddable);

    OrderStatusEntity toEntity(OrderStatus status) {
        return OrderStatusEntity.valueOf(status.name());
    }

    OrderStatus toDomain(OrderStatusEntity status) {
        return OrderStatus.valueOf(status.name());
    }

    String map(Sku sku) {
        return sku.value();
    }

    Sku map(String sku) {
        return new Sku(sku);
    }

    BigDecimal map(Money money) {
        return money.amount();
    }

    Money map(BigDecimal amount) {
        return Money.of(amount);
    }
}
