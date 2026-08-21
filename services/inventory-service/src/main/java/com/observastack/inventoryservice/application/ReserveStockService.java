package com.observastack.inventoryservice.application;

import com.observastack.inventoryservice.domain.ConcurrentStockUpdateException;
import com.observastack.inventoryservice.domain.DuplicateReservationException;
import com.observastack.inventoryservice.domain.EmptyReservationException;
import com.observastack.inventoryservice.domain.InsufficientStockException;
import com.observastack.inventoryservice.domain.OrderId;
import com.observastack.inventoryservice.domain.Reservation;
import com.observastack.inventoryservice.domain.ReservationLine;
import com.observastack.inventoryservice.domain.ReservationRepository;
import com.observastack.inventoryservice.domain.Sku;
import com.observastack.inventoryservice.domain.StockItemNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Reserves stock for an order: every line succeeds or none do, and a
 * retried request for an order that already has a reservation gets that
 * reservation back rather than a second attempt.
 *
 * <p>Reservation is optimistic: a request that read the stock item as
 * available can still fail here, because concurrent reservations are
 * resolved when each write actually lands, not by the earlier read. On
 * a {@link ConcurrentStockUpdateException} — another transaction changed
 * a stock item between when this attempt read it and tried to write —
 * this retries against fresh state up to {@link #MAX_ATTEMPTS} times
 * before giving up, since the conflict alone doesn't mean stock is
 * actually insufficient, only that the first read was stale.
 */
@Service
public class ReserveStockService {

    // Deliberately generous: each retry is a fast, cheap round trip (a
    // fresh read plus one conditional write), and under heavy contention
    // on one SKU, a thread can lose several consecutive races to other
    // threads without it meaning stock actually ran out. A low ceiling
    // here would turn "briefly unlucky" into "wrongly rejected."
    private static final int MAX_ATTEMPTS = 10;

    private final ReservationRepository reservationRepository;
    private final StockLedgerWriter writer;

    /**
     * @param reservationRepository port used to check for an existing reservation; must not be null
     * @param writer                applies the reservation as one retryable transaction; must not be null
     */
    ReserveStockService(ReservationRepository reservationRepository, StockLedgerWriter writer) {
        this.reservationRepository = reservationRepository;
        this.writer = writer;
    }

    /**
     * Reserves stock for the given order, or returns the reservation
     * already made for it.
     *
     * @param command the order and lines to reserve; must not be null
     * @return the reservation, never null
     * @throws EmptyReservationException      if {@code command} has no lines
     * @throws StockItemNotFoundException     if a requested SKU isn't stocked at all
     * @throws InsufficientStockException     if a requested SKU doesn't have enough available
     * @throws DuplicateReservationException  if another reservation was made
     *                                        concurrently for the same order
     * @throws ConcurrentStockUpdateException if stock kept changing out from
     *                                        under this attempt for {@value #MAX_ATTEMPTS}
     *                                        tries in a row
     */
    public Reservation reserve(ReserveStockCommand command) {
        OrderId orderId = new OrderId(command.orderId());
        return reservationRepository.findByOrderId(orderId).orElseGet(() -> reserveNew(orderId, command));
    }

    private Reservation reserveNew(OrderId orderId, ReserveStockCommand command) {
        List<ReservationLine> lines = aggregateBySku(command.lines());

        ConcurrentStockUpdateException lastConflict = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return writer.reserve(orderId, lines);
            } catch (ConcurrentStockUpdateException e) {
                lastConflict = e;
            }
        }
        throw lastConflict;
    }

    /**
     * Combines multiple lines for the same SKU into one before reserving.
     *
     * <p>Without this, two lines for the same SKU would each read the
     * item's current quantity independently and reserve against that same
     * stale snapshot — the second read wouldn't see the first line's
     * in-memory decrement — so both could appear to succeed against stock
     * that isn't really there twice over. Reserving once per distinct SKU,
     * for the summed quantity, closes that gap.
     */
    private static List<ReservationLine> aggregateBySku(List<ReserveStockCommand.Line> lines) {
        return lines.stream()
                .collect(Collectors.groupingBy(
                        line -> new Sku(line.sku()), Collectors.summingInt(ReserveStockCommand.Line::quantity)))
                .entrySet()
                .stream()
                .map(entry -> new ReservationLine(entry.getKey(), entry.getValue()))
                .toList();
    }
}
