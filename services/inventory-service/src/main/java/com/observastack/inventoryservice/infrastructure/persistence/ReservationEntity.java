package com.observastack.inventoryservice.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * JPA entity persisting a domain {@code Reservation}.
 *
 * <p>Implements {@link Persistable} for the same app-assigned-id reason
 * as {@code StockItemEntity} and order-service's {@code OrderEntity}.
 *
 * <p>{@code lines} is fetched eagerly, against the JPA default: a
 * repository port is a domain-owned abstraction, and the domain layer
 * has no notion of a Hibernate session — a {@code Reservation} handed
 * back by {@link ReservationRepositoryImpl} needs to be fully usable
 * whether or not the caller happens to still be inside a transaction.
 * {@code findByOrderId} is called exactly that way, non-transactionally,
 * from {@code ReserveStockService}'s idempotency check — lazy loading
 * there throws {@code LazyInitializationException} once the implicit
 * per-call transaction backing the query has already closed.
 */
@Entity
@Table(name = "reservations")
public class ReservationEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Transient
    private boolean isNew = true;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reservation_lines", joinColumns = @JoinColumn(name = "reservation_id"))
    @OrderColumn(name = "line_number")
    private List<ReservationLineEmbeddable> lines = new ArrayList<>();

    protected ReservationEntity() {
        // required by JPA
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(Instant reservedAt) {
        this.reservedAt = reservedAt;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
    }

    public List<ReservationLineEmbeddable> getLines() {
        return lines;
    }

    public void setLines(List<ReservationLineEmbeddable> lines) {
        this.lines = lines;
    }
}
