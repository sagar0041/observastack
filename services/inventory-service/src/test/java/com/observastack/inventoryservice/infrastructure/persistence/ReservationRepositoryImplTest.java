package com.observastack.inventoryservice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.observastack.inventoryservice.domain.DuplicateReservationException;
import com.observastack.inventoryservice.domain.OrderId;
import com.observastack.inventoryservice.domain.Reservation;
import com.observastack.inventoryservice.domain.ReservationLine;
import com.observastack.inventoryservice.domain.Sku;
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
 * Exercises {@link ReservationRepositoryImpl} against a real PostgreSQL
 * instance, migrated by the service's actual Liquibase changelog.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ReservationRepositoryImplTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.4");

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Autowired
    private ReservationJpaRepository jpaRepository;

    private ReservationRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ReservationRepositoryImpl(jpaRepository, new ReservationMapperImpl());
    }

    private static List<ReservationLine> aLine() {
        return List.of(new ReservationLine(new Sku("WIDGET-1"), 2));
    }

    @Test
    void save_thenFindByOrderId_returnsAnEquivalentReservation() {
        OrderId orderId = new OrderId(UUID.randomUUID());
        Reservation reservation = Reservation.create(orderId, aLine(), CLOCK);

        Reservation saved = repository.save(reservation);
        Optional<Reservation> found = repository.findByOrderId(orderId);

        assertThat(found).isPresent();
        Reservation loaded = found.orElseThrow();
        assertThat(loaded.id()).isEqualTo(saved.id());
        assertThat(loaded.orderId()).isEqualTo(orderId);
        assertThat(loaded.reservedAt()).isEqualTo(NOW);
        assertThat(loaded.releasedAt()).isNull();
        assertThat(loaded.lines()).containsExactly(new ReservationLine(new Sku("WIDGET-1"), 2));
    }

    @Test
    void findByOrderId_returnsEmpty_whenReservationDoesNotExist() {
        assertThat(repository.findByOrderId(new OrderId(UUID.randomUUID()))).isEmpty();
    }

    @Test
    void save_throwsDuplicateReservation_whenOrderIdAlreadyUsed() {
        OrderId orderId = new OrderId(UUID.randomUUID());
        repository.save(Reservation.create(orderId, aLine(), CLOCK));

        assertThatExceptionOfType(DuplicateReservationException.class)
                .isThrownBy(() -> repository.save(Reservation.create(orderId, aLine(), CLOCK)));
    }

    @Test
    void update_persistsReleasedAt() {
        OrderId orderId = new OrderId(UUID.randomUUID());
        Reservation saved = repository.save(Reservation.create(orderId, aLine(), CLOCK));

        saved.release(CLOCK);
        repository.update(saved);

        Reservation reloaded = repository.findByOrderId(orderId).orElseThrow();
        assertThat(reloaded.releasedAt()).isEqualTo(NOW);
        assertThat(reloaded.isReleased()).isTrue();
    }
}
