package com.observastack.orderservice;

import com.observastack.orderservice.application.InventoryPort;
import com.observastack.orderservice.application.StockUnavailableException;
import com.observastack.orderservice.domain.OrderId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory {@link InventoryPort} test double.
 *
 * <p>A hand-written fake rather than a mocking-framework stub, to match
 * how the rest of this codebase tests: real behaviour you can read,
 * not recorded expectations. It exists because inventory-service is a
 * genuinely separate process reached over HTTP — the one seam in this
 * codebase where standing up the real thing for every test is the wrong
 * trade, not because this project mocks internals.
 */
class FakeInventoryPort implements InventoryPort {

    private boolean stockAvailable = true;
    private final Set<UUID> reservedOrderIds = new HashSet<>();
    private final Set<UUID> releasedOrderIds = new HashSet<>();

    void setStockAvailable(boolean stockAvailable) {
        this.stockAvailable = stockAvailable;
    }

    @Override
    public void reserve(OrderId orderId, List<LineItem> lines) {
        if (!stockAvailable) {
            throw new StockUnavailableException(orderId, null);
        }
        reservedOrderIds.add(orderId.value());
    }

    @Override
    public void release(OrderId orderId) {
        releasedOrderIds.add(orderId.value());
    }

    boolean wasReserved(UUID orderId) {
        return reservedOrderIds.contains(orderId);
    }

    boolean wasReleased(UUID orderId) {
        return releasedOrderIds.contains(orderId);
    }
}
