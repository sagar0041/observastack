package com.observastack.inventoryservice.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A record of stock reserved, as one unit, for a single order.
 *
 * <p>A reservation covers every SKU an order needs in one place, so it
 * succeeds or fails atomically with the order it's for — see
 * {@link com.observastack.inventoryservice.application.ReserveStockService},
 * which builds one of these only after every line's stock has actually
 * been decremented. There is no setter; the only state change after
 * construction is {@link #release}.
 */
public final class Reservation {

    private final ReservationId id;
    private final OrderId orderId;
    private final List<ReservationLine> lines;
    private final Instant reservedAt;
    private Instant releasedAt;

    private Reservation(
            ReservationId id, OrderId orderId, List<ReservationLine> lines, Instant reservedAt, Instant releasedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        if (lines == null || lines.isEmpty()) {
            throw new EmptyReservationException(orderId);
        }
        this.lines = List.copyOf(lines);
        this.reservedAt = Objects.requireNonNull(reservedAt, "reservedAt must not be null");
        this.releasedAt = releasedAt;
    }

    /**
     * Creates a new, active reservation with a freshly generated identity.
     *
     * @param orderId the order this reservation is for; must not be null
     * @param lines   the SKUs and quantities reserved; must not be null or
     *                empty, and should already be aggregated to one entry
     *                per distinct SKU
     * @param clock   clock used to stamp {@code reservedAt}; must not be null
     * @return a new reservation, never null
     * @throws EmptyReservationException if {@code lines} is null or empty
     */
    public static Reservation create(OrderId orderId, List<ReservationLine> lines, Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        return new Reservation(ReservationId.newId(), orderId, lines, Instant.now(clock), null);
    }

    /**
     * Rebuilds a reservation from previously persisted state. Used
     * exclusively by the persistence mapper.
     *
     * @param id         the reservation's identity; must not be null
     * @param orderId    the order this reservation is for; must not be null
     * @param lines      the persisted lines; must not be null or empty
     * @param reservedAt when the reservation was made; must not be null
     * @param releasedAt when it was released, or null if still active
     * @return the reconstructed reservation, never null
     * @throws EmptyReservationException if {@code lines} is null or empty
     */
    public static Reservation reconstruct(
            ReservationId id, OrderId orderId, List<ReservationLine> lines, Instant reservedAt, Instant releasedAt) {
        return new Reservation(id, orderId, lines, reservedAt, releasedAt);
    }

    /**
     * Releases the reservation, returning its stock to available (the
     * caller is responsible for actually crediting each line back to its
     * {@link StockItem} — this only marks the reservation itself).
     *
     * @param clock clock used to stamp {@code releasedAt}; must not be null
     * @throws IllegalReservationStateException if already released
     */
    public void release(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        if (releasedAt != null) {
            throw new IllegalReservationStateException(id);
        }
        this.releasedAt = Instant.now(clock);
    }

    public ReservationId id() {
        return id;
    }

    public OrderId orderId() {
        return orderId;
    }

    public List<ReservationLine> lines() {
        return lines;
    }

    public Instant reservedAt() {
        return reservedAt;
    }

    /**
     * @return when the reservation was released, or null if still active
     */
    public Instant releasedAt() {
        return releasedAt;
    }

    public boolean isReleased() {
        return releasedAt != null;
    }
}
