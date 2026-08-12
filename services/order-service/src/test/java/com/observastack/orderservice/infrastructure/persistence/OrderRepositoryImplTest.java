package com.observastack.orderservice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.observastack.orderservice.domain.CustomerId;
import com.observastack.orderservice.domain.Money;
import com.observastack.orderservice.domain.Order;
import com.observastack.orderservice.domain.OrderId;
import com.observastack.orderservice.domain.OrderLineItem;
import com.observastack.orderservice.domain.OrderStatus;
import com.observastack.orderservice.domain.Sku;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises {@link OrderRepositoryImpl} against a real PostgreSQL instance,
 * migrated by the service's actual Liquibase changelog.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OrderRepositoryImplTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.4");

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Autowired
    private OrderJpaRepository jpaRepository;

    private OrderRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        // Instantiated directly rather than via Spring: @DataJpaTest's slice
        // doesn't scan plain @Repository beans, only JPA infrastructure, and
        // the generated OrderMapperImpl has no dependencies of its own to
        // wire, so there's nothing Spring would add here.
        repository = new OrderRepositoryImpl(jpaRepository, new OrderMapperImpl());
    }

    private static Order aPlacedOrder() {
        Order order = Order.create(
                new CustomerId(UUID.randomUUID()),
                List.of(new OrderLineItem(new Sku("WIDGET-1"), 2, Money.of(new BigDecimal("9.99")))),
                CLOCK);
        order.place(CLOCK);
        return order;
    }

    @Test
    void save_thenFindById_returnsAnEquivalentOrder() {
        Order order = aPlacedOrder();

        Order saved = repository.save(order);
        Optional<Order> found = repository.findById(saved.id());

        assertThat(found).isPresent();
        Order loaded = found.orElseThrow();
        assertThat(loaded.id()).isEqualTo(order.id());
        assertThat(loaded.customerId()).isEqualTo(order.customerId());
        assertThat(loaded.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(loaded.createdAt()).isEqualTo(NOW);
        assertThat(loaded.placedAt()).isEqualTo(NOW);
        assertThat(loaded.cancelledAt()).isNull();
        assertThat(loaded.lineItems())
                .containsExactly(new OrderLineItem(new Sku("WIDGET-1"), 2, Money.of(new BigDecimal("9.99"))));
    }

    @Test
    void findById_returnsEmpty_whenOrderDoesNotExist() {
        assertThat(repository.findById(OrderId.newId())).isEmpty();
    }

    @Test
    void save_persistsMultipleLineItemsInOrder() {
        Order order = Order.create(
                new CustomerId(UUID.randomUUID()),
                List.of(
                        new OrderLineItem(new Sku("WIDGET-1"), 2, Money.of(new BigDecimal("9.99"))),
                        new OrderLineItem(new Sku("WIDGET-2"), 1, Money.of(new BigDecimal("5.00")))),
                CLOCK);

        Order saved = repository.save(order);
        Order loaded = repository.findById(saved.id()).orElseThrow();

        assertThat(loaded.lineItems()).containsExactly(
                new OrderLineItem(new Sku("WIDGET-1"), 2, Money.of(new BigDecimal("9.99"))),
                new OrderLineItem(new Sku("WIDGET-2"), 1, Money.of(new BigDecimal("5.00"))));
    }
}
