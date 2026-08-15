package com.observastack.orderservice.infrastructure.persistence;

import com.observastack.orderservice.domain.CustomerId;
import com.observastack.orderservice.domain.IdempotencyKey;
import com.observastack.orderservice.domain.Money;
import com.observastack.orderservice.domain.Order;
import com.observastack.orderservice.domain.OrderId;
import com.observastack.orderservice.domain.OrderLineItem;
import com.observastack.orderservice.domain.OrderStatus;
import com.observastack.orderservice.domain.Sku;
import java.util.Currency;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converts between the {@code Order} aggregate and its persisted
 * {@link OrderEntity} representation.
 *
 * <p>The top-level {@code Order}/{@code OrderEntity} conversion is
 * written by hand rather than generated: {@link Order} has no public
 * constructor or setters — see {@link Order#reconstruct} — which is
 * deliberate, and MapStruct's usual bean-mapping strategy doesn't apply
 * to an aggregate that refuses to expose a mutation surface.
 *
 * <p>The line-item conversion below is generated. {@code sku} and
 * {@code quantity} map directly; {@code unitPrice}/{@code currency} are
 * two flat columns standing in for one {@link Money} value, so
 * {@link #toEntity(OrderLineItem)} pulls the amount and currency out via
 * nested-property {@code source} paths, and {@link #toDomain(OrderLineItemEmbeddable)}
 * reassembles them with a small expression — the one place a plain
 * property mapping can't express what's needed, since combining two
 * source fields into one target value isn't something a 1:1 mapping can
 * say declaratively.
 */
@Mapper(componentModel = "spring", imports = {Money.class, Currency.class})
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
        entity.setIdempotencyKey(order.idempotencyKey().value());
        entity.setStatus(toEntity(order.status()));
        entity.setCreatedAt(order.createdAt());
        entity.setPlacedAt(order.placedAt());
        entity.setCancelledAt(order.cancelledAt());
        entity.setLineItems(order.lineItems().stream().map(this::toEntity).toList());
        return entity;
    }

    /**
     * Applies an order's mutable state onto an already-persisted entity,
     * in place.
     *
     * <p>Only the fields {@link Order#place} and {@link Order#cancel} can
     * actually change after creation — identity, customer, idempotency
     * key, and line items are fixed for the life of the order — so this
     * only touches those three, rather than rebuilding the whole entity.
     *
     * @param order  the order holding the new state; must not be null
     * @param entity the managed entity to update; must not be null
     */
    public void updateEntity(Order order, OrderEntity entity) {
        entity.setStatus(toEntity(order.status()));
        entity.setPlacedAt(order.placedAt());
        entity.setCancelledAt(order.cancelledAt());
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
                new IdempotencyKey(entity.getIdempotencyKey()),
                lineItems,
                toDomain(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getPlacedAt(),
                entity.getCancelledAt());
    }

    @Mapping(target = "unitPrice", source = "unitPrice.amount")
    @Mapping(target = "currency", source = "unitPrice.currency")
    abstract OrderLineItemEmbeddable toEntity(OrderLineItem lineItem);

    @Mapping(target = "unitPrice", expression = "java(Money.of(embeddable.getUnitPrice(), Currency.getInstance(embeddable.getCurrency())))")
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

    String map(Currency currency) {
        return currency.getCurrencyCode();
    }
}
