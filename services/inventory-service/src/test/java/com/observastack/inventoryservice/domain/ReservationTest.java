package com.observastack.inventoryservice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReservationTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static OrderId anOrderId() {
        return new OrderId(UUID.randomUUID());
    }

    private static List<ReservationLine> aLine() {
        return List.of(new ReservationLine(new Sku("WIDGET-1"), 2));
    }

    @Test
    void create_succeeds_withAtLeastOneLine() {
        Reservation reservation = Reservation.create(anOrderId(), aLine(), CLOCK);

        assertThat(reservation.id()).isNotNull();
        assertThat(reservation.reservedAt()).isEqualTo(NOW);
        assertThat(reservation.releasedAt()).isNull();
        assertThat(reservation.isReleased()).isFalse();
    }

    @Test
    void create_throwsEmptyReservation_whenLinesIsEmpty() {
        assertThatExceptionOfType(EmptyReservationException.class)
                .isThrownBy(() -> Reservation.create(anOrderId(), List.of(), CLOCK));
    }

    @Test
    void create_throwsEmptyReservation_whenLinesIsNull() {
        assertThatExceptionOfType(EmptyReservationException.class)
                .isThrownBy(() -> Reservation.create(anOrderId(), null, CLOCK));
    }

    @Test
    void create_throwsNullPointer_whenOrderIdIsNull() {
        assertThatNullPointerException().isThrownBy(() -> Reservation.create(null, aLine(), CLOCK));
    }

    @Test
    void release_marksReleasedAt() {
        Reservation reservation = Reservation.create(anOrderId(), aLine(), CLOCK);

        reservation.release(CLOCK);

        assertThat(reservation.releasedAt()).isEqualTo(NOW);
        assertThat(reservation.isReleased()).isTrue();
    }

    @Test
    void release_throwsIllegalReservationState_whenAlreadyReleased() {
        Reservation reservation = Reservation.create(anOrderId(), aLine(), CLOCK);
        reservation.release(CLOCK);

        assertThatExceptionOfType(IllegalReservationStateException.class).isThrownBy(() -> reservation.release(CLOCK));
    }

    @Test
    void lines_isImmutable() {
        Reservation reservation = Reservation.create(anOrderId(), aLine(), CLOCK);

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> reservation.lines().add(new ReservationLine(new Sku("X"), 1)));
    }
}
