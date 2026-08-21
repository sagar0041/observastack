package com.observastack.inventoryservice.infrastructure.persistence;

import com.observastack.inventoryservice.domain.DuplicateReservationException;
import com.observastack.inventoryservice.domain.OrderId;
import com.observastack.inventoryservice.domain.Reservation;
import com.observastack.inventoryservice.domain.ReservationNotFoundException;
import com.observastack.inventoryservice.domain.ReservationRepository;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/**
 * JPA-backed implementation of the domain-owned {@link ReservationRepository}
 * port.
 */
@Repository
public class ReservationRepositoryImpl implements ReservationRepository {

    /**
     * Matches the {@code uniqueConstraintName} given to the reservations
     * table's unique constraint on {@code order_id} in the Liquibase
     * changelog — see {@code OrderRepositoryImpl}'s equivalent in
     * order-service for why the constraint name is what's matched on.
     */
    private static final String ORDER_ID_CONSTRAINT_NAME = "uq_reservations_order_id";

    private final ReservationJpaRepository jpaRepository;
    private final ReservationMapper mapper;

    public ReservationRepositoryImpl(ReservationJpaRepository jpaRepository, ReservationMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Reservation save(Reservation reservation) {
        try {
            ReservationEntity saved = jpaRepository.saveAndFlush(mapper.toEntity(reservation));
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            if (isOrderIdConflict(e)) {
                throw new DuplicateReservationException(reservation.orderId(), e);
            }
            throw e;
        }
    }

    @Override
    public void update(Reservation reservation) {
        ReservationEntity entity = jpaRepository
                .findById(reservation.id().value())
                .orElseThrow(() -> new ReservationNotFoundException(reservation.orderId()));
        mapper.updateEntity(reservation, entity);
        jpaRepository.flush();
    }

    @Override
    public Optional<Reservation> findByOrderId(OrderId orderId) {
        return jpaRepository.findByOrderId(orderId.value()).map(mapper::toDomain);
    }

    private boolean isOrderIdConflict(DataIntegrityViolationException e) {
        Throwable cause = e.getMostSpecificCause();
        return cause.getMessage() != null && cause.getMessage().contains(ORDER_ID_CONSTRAINT_NAME);
    }
}
