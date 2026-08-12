package com.observastack.orderservice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    private static CustomerId aCustomerId() {
        return new CustomerId(UUID.randomUUID());
    }

    private static IdempotencyKey anIdempotencyKey() {
        return new IdempotencyKey(UUID.randomUUID().toString());
    }

    private static List<OrderLineItem> aLineItem() {
        return List.of(new OrderLineItem(new Sku("WIDGET-1"), 2, Money.of(new BigDecimal("9.99"), USD)));
    }

    @Test
    void create_succeeds_withAtLeastOneLineItem() {
        Order order = Order.create(aCustomerId(), anIdempotencyKey(), aLineItem(), CLOCK);

        assertThat(order.id()).isNotNull();
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.createdAt()).isEqualTo(NOW);
        assertThat(order.placedAt()).isNull();
        assertThat(order.cancelledAt()).isNull();
    }

    @Test
    void create_throwsEmptyOrder_whenLineItemsIsEmpty() {
        assertThatExceptionOfType(EmptyOrderException.class)
                .isThrownBy(() -> Order.create(aCustomerId(), anIdempotencyKey(), List.of(), CLOCK));
    }

    @Test
    void create_throwsEmptyOrder_whenLineItemsIsNull() {
        assertThatExceptionOfType(EmptyOrderException.class)
                .isThrownBy(() -> Order.create(aCustomerId(), anIdempotencyKey(), null, CLOCK));
    }

    @Test
    void create_throwsNullPointer_whenCustomerIdIsNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> Order.create(null, anIdempotencyKey(), aLineItem(), CLOCK));
    }

    @Test
    void create_throwsNullPointer_whenIdempotencyKeyIsNull() {
        assertThatNullPointerException().isThrownBy(() -> Order.create(aCustomerId(), null, aLineItem(), CLOCK));
    }

    @Test
    void create_throwsMixedCurrency_whenLineItemsDisagreeOnCurrency() {
        List<OrderLineItem> items = List.of(
                new OrderLineItem(new Sku("WIDGET-1"), 1, Money.of(new BigDecimal("9.99"), USD)),
                new OrderLineItem(new Sku("WIDGET-2"), 1, Money.of(new BigDecimal("9.99"), EUR)));

        assertThatExceptionOfType(MixedCurrencyException.class)
                .isThrownBy(() -> Order.create(aCustomerId(), anIdempotencyKey(), items, CLOCK));
    }

    @Test
    void currency_returnsTheLineItemsSharedCurrency() {
        Order order = Order.create(aCustomerId(), anIdempotencyKey(), aLineItem(), CLOCK);

        assertThat(order.currency()).isEqualTo(USD);
    }

    @Test
    void lineItems_isImmutableAndDefensivelyCopied() {
        List<OrderLineItem> mutable = new ArrayList<>(aLineItem());
        Order order = Order.create(aCustomerId(), anIdempotencyKey(), mutable, CLOCK);

        mutable.add(new OrderLineItem(new Sku("EXTRA"), 1, Money.of(BigDecimal.ONE, USD)));

        assertThat(order.lineItems()).hasSize(1);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> order.lineItems().add(new OrderLineItem(new Sku("X"), 1, Money.of(BigDecimal.ONE, USD))));
    }

    @Test
    void place_transitionsToPlaced_whenCreated() {
        Order order = Order.create(aCustomerId(), anIdempotencyKey(), aLineItem(), CLOCK);

        order.place(CLOCK);

        assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.placedAt()).isEqualTo(NOW);
    }

    @Test
    void place_throwsIllegalOrderState_whenAlreadyPlaced() {
        Order order = Order.create(aCustomerId(), anIdempotencyKey(), aLineItem(), CLOCK);
        order.place(CLOCK);

        assertThatExceptionOfType(IllegalOrderStateException.class).isThrownBy(() -> order.place(CLOCK));
    }

    @Test
    void place_throwsIllegalOrderState_whenCancelled() {
        Order order = Order.create(aCustomerId(), anIdempotencyKey(), aLineItem(), CLOCK);
        order.cancel(CLOCK);

        assertThatExceptionOfType(IllegalOrderStateException.class).isThrownBy(() -> order.place(CLOCK));
    }

    @Test
    void cancel_transitionsToCancelled_fromCreated() {
        Order order = Order.create(aCustomerId(), anIdempotencyKey(), aLineItem(), CLOCK);

        order.cancel(CLOCK);

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.cancelledAt()).isEqualTo(NOW);
    }

    @Test
    void cancel_transitionsToCancelled_fromPlaced() {
        Order order = Order.create(aCustomerId(), anIdempotencyKey(), aLineItem(), CLOCK);
        order.place(CLOCK);

        order.cancel(CLOCK);

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.cancelledAt()).isEqualTo(NOW);
    }

    @Test
    void cancel_throwsIllegalOrderState_whenAlreadyCancelled() {
        Order order = Order.create(aCustomerId(), anIdempotencyKey(), aLineItem(), CLOCK);
        order.cancel(CLOCK);

        assertThatExceptionOfType(IllegalOrderStateException.class).isThrownBy(() -> order.cancel(CLOCK));
    }

    @Test
    void totalPrice_sumsAllLineItemTotals() {
        List<OrderLineItem> items = List.of(
                new OrderLineItem(new Sku("WIDGET-1"), 2, Money.of(new BigDecimal("9.99"), USD)),
                new OrderLineItem(new Sku("WIDGET-2"), 1, Money.of(new BigDecimal("5.00"), USD)));
        Order order = Order.create(aCustomerId(), anIdempotencyKey(), items, CLOCK);

        assertThat(order.totalPrice()).isEqualTo(Money.of(new BigDecimal("24.98"), USD));
    }

    @Test
    void equals_comparesByIdentityOnly() {
        Order order = Order.create(aCustomerId(), anIdempotencyKey(), aLineItem(), CLOCK);
        Order reconstructed = Order.reconstruct(
                order.id(),
                new CustomerId(UUID.randomUUID()), // deliberately different field value
                anIdempotencyKey(),
                aLineItem(),
                OrderStatus.PLACED,
                NOW,
                NOW,
                null);

        assertThat(order).isEqualTo(reconstructed);
        assertThat(order).hasSameHashCodeAs(reconstructed);
    }
}
