package com.observastack.orderservice.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * An order placed by a customer for one or more line items.
 *
 * <p>Invariants are enforced at construction and on every state
 * transition: an order always has at least one line item, all priced in
 * the same currency, and its {@link OrderStatus} only moves along the
 * transitions allowed by {@link #place} and {@link #cancel}. There is no
 * setter — once built, an {@link Order} cannot be pushed into a state
 * its own rules wouldn't allow it to reach on its own.
 */
public final class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private final IdempotencyKey idempotencyKey;
    private final List<OrderLineItem> lineItems;
    private final Instant createdAt;
    private OrderStatus status;
    private Instant placedAt;
    private Instant cancelledAt;

    private Order(
            OrderId id,
            CustomerId customerId,
            IdempotencyKey idempotencyKey,
            List<OrderLineItem> lineItems,
            OrderStatus status,
            Instant createdAt,
            Instant placedAt,
            Instant cancelledAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        if (lineItems == null || lineItems.isEmpty()) {
            throw new EmptyOrderException(id);
        }
        this.lineItems = List.copyOf(lineItems);
        Currency currency = this.lineItems.get(0).unitPrice().currency();
        if (this.lineItems.stream().anyMatch(item -> !item.unitPrice().currency().equals(currency))) {
            throw new MixedCurrencyException(id);
        }
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.placedAt = placedAt;
        this.cancelledAt = cancelledAt;
    }

    /**
     * Creates a new order in the {@link OrderStatus#CREATED} state, with a
     * freshly generated identity.
     *
     * @param customerId     the customer the order belongs to; must not be null
     * @param idempotencyKey key that makes placing this order safe to retry; must not be null
     * @param lineItems      the items being ordered; must not be null or empty, and must
     *                       all be priced in the same currency
     * @param clock          clock used to stamp {@code createdAt}; must not be null
     * @return a new order, never null
     * @throws EmptyOrderException   if {@code lineItems} is null or empty
     * @throws MixedCurrencyException if {@code lineItems} mixes currencies
     */
    public static Order create(
            CustomerId customerId, IdempotencyKey idempotencyKey, List<OrderLineItem> lineItems, Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        return new Order(
                OrderId.newId(),
                customerId,
                idempotencyKey,
                lineItems,
                OrderStatus.CREATED,
                Instant.now(clock),
                null,
                null);
    }

    /**
     * Rebuilds an order from previously persisted state.
     *
     * <p>Unlike {@link #create}, this does not generate a new identity or
     * default the status — it restores an order exactly as it was saved.
     * Used exclusively by the persistence mapper; application code should
     * never need to call this directly.
     *
     * @param id             the order's identity; must not be null
     * @param customerId     the customer the order belongs to; must not be null
     * @param idempotencyKey key the order was originally placed with; must not be null
     * @param lineItems      the persisted items; must not be null or empty
     * @param status         the persisted lifecycle state; must not be null
     * @param createdAt      when the order was created; must not be null
     * @param placedAt       when the order was placed, or null if never placed
     * @param cancelledAt    when the order was cancelled, or null if never cancelled
     * @return the reconstructed order, never null
     * @throws EmptyOrderException    if {@code lineItems} is null or empty
     * @throws MixedCurrencyException if {@code lineItems} mixes currencies
     */
    public static Order reconstruct(
            OrderId id,
            CustomerId customerId,
            IdempotencyKey idempotencyKey,
            List<OrderLineItem> lineItems,
            OrderStatus status,
            Instant createdAt,
            Instant placedAt,
            Instant cancelledAt) {
        return new Order(id, customerId, idempotencyKey, lineItems, status, createdAt, placedAt, cancelledAt);
    }

    /**
     * Places the order, committing it as a real business transaction.
     *
     * @param clock clock used to stamp {@code placedAt}; must not be null
     * @throws IllegalOrderStateException if the order is not currently
     *                                    {@link OrderStatus#CREATED}
     */
    public void place(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        if (status != OrderStatus.CREATED) {
            throw new IllegalOrderStateException(id, status, OrderStatus.PLACED);
        }
        this.status = OrderStatus.PLACED;
        this.placedAt = Instant.now(clock);
    }

    /**
     * Cancels the order.
     *
     * <p>Allowed from either {@link OrderStatus#CREATED} or
     * {@link OrderStatus#PLACED} — cancellation is the terminal path out
     * of both. Not allowed once the order is already cancelled.
     *
     * @param clock clock used to stamp {@code cancelledAt}; must not be null
     * @throws IllegalOrderStateException if the order is already
     *                                    {@link OrderStatus#CANCELLED}
     */
    public void cancel(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalOrderStateException(id, status, OrderStatus.CANCELLED);
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = Instant.now(clock);
    }

    /**
     * Sums the line totals of every item on the order.
     *
     * <p>Safe to reduce without a currency-aware seed value: the
     * constructor already guarantees every line item shares one
     * currency, and {@link #lineItems} is never empty.
     *
     * @return the order's total price, never null
     */
    public Money totalPrice() {
        return lineItems.stream().map(OrderLineItem::lineTotal).reduce(Money::add).orElseThrow();
    }

    /**
     * The currency every line item is priced in.
     *
     * @return the order's currency, never null
     */
    public Currency currency() {
        return lineItems.get(0).unitPrice().currency();
    }

    public OrderId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public IdempotencyKey idempotencyKey() {
        return idempotencyKey;
    }

    /**
     * The order's line items.
     *
     * @return an immutable, defensively-copied list of line items; never
     *         null or empty
     */
    public List<OrderLineItem> lineItems() {
        return lineItems;
    }

    public OrderStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    /**
     * @return when the order was placed, or null if it never has been
     */
    public Instant placedAt() {
        return placedAt;
    }

    /**
     * @return when the order was cancelled, or null if it never has been
     */
    public Instant cancelledAt() {
        return cancelledAt;
    }

    /**
     * Orders are compared by identity, not by field values — two
     * {@link Order} instances with the same {@link OrderId} are the same
     * order even if one holds staler field values than the other.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Order other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
