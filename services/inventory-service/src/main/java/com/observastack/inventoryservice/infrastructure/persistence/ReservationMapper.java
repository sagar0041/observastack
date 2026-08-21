package com.observastack.inventoryservice.infrastructure.persistence;

import com.observastack.inventoryservice.domain.OrderId;
import com.observastack.inventoryservice.domain.Reservation;
import com.observastack.inventoryservice.domain.ReservationId;
import com.observastack.inventoryservice.domain.ReservationLine;
import com.observastack.inventoryservice.domain.Sku;
import java.util.List;
import org.mapstruct.Mapper;

/**
 * Converts between the {@code Reservation} aggregate and its persisted
 * {@link ReservationEntity} representation.
 *
 * <p>The top-level conversion is hand-written, as with order-service's
 * {@code OrderMapper}: {@link Reservation} has no public constructor.
 * The line conversion is a plain MapStruct mapping — {@code sku} and
 * {@code quantity} both map directly, with no value object like
 * {@code Money} to reassemble, so there's nothing here MapStruct can't
 * generate on its own.
 */
@Mapper(componentModel = "spring")
public abstract class ReservationMapper {

    /**
     * @param reservation the reservation to convert; must not be null
     * @return the equivalent entity, never null
     */
    public ReservationEntity toEntity(Reservation reservation) {
        ReservationEntity entity = new ReservationEntity();
        entity.setId(reservation.id().value());
        entity.setOrderId(reservation.orderId().value());
        entity.setReservedAt(reservation.reservedAt());
        entity.setReleasedAt(reservation.releasedAt());
        entity.setLines(reservation.lines().stream().map(this::toEntity).toList());
        return entity;
    }

    /**
     * Applies a reservation's mutable state — only {@code releasedAt} can
     * ever change after creation — onto an already-persisted entity, in
     * place.
     *
     * @param reservation the reservation holding the new state; must not be null
     * @param entity      the managed entity to update; must not be null
     */
    public void updateEntity(Reservation reservation, ReservationEntity entity) {
        entity.setReleasedAt(reservation.releasedAt());
    }

    /**
     * @param entity the entity to convert; must not be null
     * @return the equivalent domain reservation, never null
     */
    public Reservation toDomain(ReservationEntity entity) {
        List<ReservationLine> lines = entity.getLines().stream().map(this::toDomain).toList();
        return Reservation.reconstruct(
                new ReservationId(entity.getId()),
                new OrderId(entity.getOrderId()),
                lines,
                entity.getReservedAt(),
                entity.getReleasedAt());
    }

    abstract ReservationLineEmbeddable toEntity(ReservationLine line);

    abstract ReservationLine toDomain(ReservationLineEmbeddable embeddable);

    String map(Sku sku) {
        return sku.value();
    }

    Sku map(String sku) {
        return new Sku(sku);
    }
}
