package com.observastack.inventoryservice.application;

import com.observastack.inventoryservice.domain.ConcurrentStockUpdateException;
import com.observastack.inventoryservice.domain.IllegalReservationStateException;
import com.observastack.inventoryservice.domain.OrderId;
import com.observastack.inventoryservice.domain.ReservationNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Releases the reservation for an order, crediting its lines back to
 * available stock. Retries on {@link ConcurrentStockUpdateException} for
 * the same reason {@link ReserveStockService} does.
 */
@Service
public class ReleaseStockService {

    // Same reasoning as ReserveStockService's ceiling: retries here are
    // cheap, and contention losing a few rounds in a row isn't evidence
    // of anything but bad luck.
    private static final int MAX_ATTEMPTS = 10;

    private final StockLedgerWriter writer;

    ReleaseStockService(StockLedgerWriter writer) {
        this.writer = writer;
    }

    /**
     * Releases the reservation made for the given order.
     *
     * @param orderId the order whose reservation should be released; must not be null
     * @throws ReservationNotFoundException     if no reservation exists for this order
     * @throws IllegalReservationStateException if the reservation was already released
     * @throws ConcurrentStockUpdateException   if stock kept changing out from
     *                                          under this attempt for {@value #MAX_ATTEMPTS}
     *                                          tries in a row
     */
    public void release(OrderId orderId) {
        ConcurrentStockUpdateException lastConflict = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                writer.release(orderId);
                return;
            } catch (ConcurrentStockUpdateException e) {
                lastConflict = e;
            }
        }
        throw lastConflict;
    }
}
