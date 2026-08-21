package com.observastack.orderservice.application;

import com.observastack.orderservice.domain.CustomerId;
import com.observastack.orderservice.domain.DuplicateIdempotencyKeyException;
import com.observastack.orderservice.domain.EmptyOrderException;
import com.observastack.orderservice.domain.IdempotencyKey;
import com.observastack.orderservice.domain.MixedCurrencyException;
import com.observastack.orderservice.domain.Money;
import com.observastack.orderservice.domain.Order;
import com.observastack.orderservice.domain.OrderLineItem;
import com.observastack.orderservice.domain.OrderRepository;
import com.observastack.orderservice.domain.Sku;
import java.time.Clock;
import java.util.Currency;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Places an order: builds it from a {@link PlaceOrderCommand}, reserves
 * its stock with inventory-service, transitions it to {@code PLACED} or
 * {@code CANCELLED} depending on how that went, and persists it — all
 * within one transaction.
 *
 * <p>Idempotent by {@link IdempotencyKey}: a retried request with a key
 * that already has an order returns that order unchanged rather than
 * placing a second one. See {@link DuplicateIdempotencyKeyException} for
 * how a genuine race between two identical concurrent requests is
 * handled.
 *
 * <p>The reservation call happens inside this method's transaction,
 * which means a slow inventory-service holds this transaction — and the
 * database connection backing it — open for as long as the call takes.
 * {@link com.observastack.orderservice.infrastructure.client.InventoryClientConfig}
 * bounds that with a short timeout; a design that avoided the coupling
 * entirely (an outbox, a saga) is a bigger change than this milestone's
 * scope of "call it synchronously" asks for.
 */
@Service
public class PlaceOrderService {

    private final OrderRepository orderRepository;
    private final InventoryPort inventoryPort;
    private final Clock clock;

    /**
     * @param orderRepository port used to persist the placed order; must not be null
     * @param inventoryPort   port used to reserve stock for the order; must not be null
     * @param clock           clock used to stamp the order's timestamps; must not be null
     */
    public PlaceOrderService(OrderRepository orderRepository, InventoryPort inventoryPort, Clock clock) {
        this.orderRepository = orderRepository;
        this.inventoryPort = inventoryPort;
        this.clock = clock;
    }

    /**
     * Places an order for the given customer and line items, or returns
     * the order already placed under this command's idempotency key.
     *
     * <p>If inventory can't reserve the stock this order needs, the order
     * is still created and persisted — as {@code CANCELLED} rather than
     * {@code PLACED} — so there's a durable record of the attempt instead
     * of it silently vanishing.
     *
     * @param command the customer, line items, and idempotency key to place
     *                an order for; must not be null
     * @return the resulting order, {@code PLACED} or {@code CANCELLED}, never null
     * @throws EmptyOrderException      if {@code command} has no line items
     * @throws MixedCurrencyException   if line items disagree on currency
     *                                   (can't happen today — {@code currency}
     *                                   is one value applied to every line —
     *                                   but stays a real, checked invariant
     *                                   rather than an assumption)
     * @throws IllegalArgumentException if any line item's SKU, quantity, or
     *                                  price is invalid, or the currency
     *                                  code isn't a recognised ISO 4217 code
     */
    @Transactional
    public Order placeOrder(PlaceOrderCommand command) {
        IdempotencyKey idempotencyKey = new IdempotencyKey(command.idempotencyKey());
        return orderRepository.findByIdempotencyKey(idempotencyKey).orElseGet(() -> placeNewOrder(command, idempotencyKey));
    }

    private Order placeNewOrder(PlaceOrderCommand command, IdempotencyKey idempotencyKey) {
        CustomerId customerId = new CustomerId(command.customerId());
        Currency currency = Currency.getInstance(command.currency());
        List<OrderLineItem> lineItems = command.lineItems().stream()
                .map(item -> new OrderLineItem(new Sku(item.sku()), item.quantity(), Money.of(item.unitPrice(), currency)))
                .toList();

        Order order = Order.create(customerId, idempotencyKey, lineItems, clock);

        try {
            inventoryPort.reserve(order.id(), toInventoryLineItems(lineItems));
            order.place(clock);
        } catch (StockUnavailableException e) {
            order.cancel(clock);
        }

        return orderRepository.save(order);
    }

    private static List<InventoryPort.LineItem> toInventoryLineItems(List<OrderLineItem> lineItems) {
        return lineItems.stream().map(item -> new InventoryPort.LineItem(item.sku(), item.quantity())).toList();
    }
}
