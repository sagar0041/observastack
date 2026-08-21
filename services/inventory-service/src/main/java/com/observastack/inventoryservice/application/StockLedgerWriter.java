package com.observastack.inventoryservice.application;

import com.observastack.inventoryservice.domain.ConcurrentStockUpdateException;
import com.observastack.inventoryservice.domain.OrderId;
import com.observastack.inventoryservice.domain.Reservation;
import com.observastack.inventoryservice.domain.ReservationLine;
import com.observastack.inventoryservice.domain.ReservationNotFoundException;
import com.observastack.inventoryservice.domain.ReservationRepository;
import com.observastack.inventoryservice.domain.StockItem;
import com.observastack.inventoryservice.domain.StockItemNotFoundException;
import com.observastack.inventoryservice.domain.StockItemRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies a reserve or release as one transaction, re-reading every
 * {@link StockItem} it touches fresh within that transaction.
 *
 * <p>This is a separate bean from {@link ReserveStockService}/
 * {@link ReleaseStockService} — not a stylistic choice. Those services
 * retry {@link #reserve}/{@link #release} on {@link ConcurrentStockUpdateException},
 * and a retry has to run in a brand-new transaction against current
 * data, not keep reusing one a conflict already doomed. Spring's
 * {@code @Transactional} proxy only intercepts calls that arrive from
 * outside the bean; calling this method on {@code this} from within the
 * same class would silently keep reusing the enclosing transaction
 * instead of starting a fresh one for each attempt (order-service's M2
 * review round hit exactly this while adding order cancellation) — so
 * the retry loop lives in a different bean that calls this one through
 * its proxy on every attempt.
 */
@Service
class StockLedgerWriter {

    private final StockItemRepository stockItemRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    StockLedgerWriter(StockItemRepository stockItemRepository, ReservationRepository reservationRepository, Clock clock) {
        this.stockItemRepository = stockItemRepository;
        this.reservationRepository = reservationRepository;
        this.clock = clock;
    }

    @Transactional
    Reservation reserve(OrderId orderId, List<ReservationLine> lines) {
        for (ReservationLine line : lines) {
            StockItem item = stockItemRepository
                    .findBySku(line.sku())
                    .orElseThrow(() -> new StockItemNotFoundException(line.sku()));
            item.reserve(line.quantity());
            stockItemRepository.update(item);
        }
        Reservation reservation = Reservation.create(orderId, lines, clock);
        return reservationRepository.save(reservation);
    }

    @Transactional
    void release(OrderId orderId) {
        Reservation reservation =
                reservationRepository.findByOrderId(orderId).orElseThrow(() -> new ReservationNotFoundException(orderId));
        reservation.release(clock);
        for (ReservationLine line : reservation.lines()) {
            StockItem item = stockItemRepository
                    .findBySku(line.sku())
                    .orElseThrow(() -> new StockItemNotFoundException(line.sku()));
            item.release(line.quantity());
            stockItemRepository.update(item);
        }
        reservationRepository.update(reservation);
    }
}
