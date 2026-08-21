package com.observastack.inventoryservice.api;

import com.observastack.inventoryservice.api.dto.ReservationResponse;
import com.observastack.inventoryservice.api.dto.ReserveStockRequest;
import com.observastack.inventoryservice.application.ReleaseStockService;
import com.observastack.inventoryservice.application.ReserveStockCommand;
import com.observastack.inventoryservice.application.ReserveStockService;
import com.observastack.inventoryservice.domain.OrderId;
import com.observastack.inventoryservice.domain.Reservation;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for reserving and releasing stock on behalf of orders.
 */
@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReserveStockService reserveStockService;
    private final ReleaseStockService releaseStockService;

    /**
     * @param reserveStockService use case backing {@code POST /reservations}; must not be null
     * @param releaseStockService use case backing {@code POST /reservations/{orderId}/release}; must not be null
     */
    public ReservationController(ReserveStockService reserveStockService, ReleaseStockService releaseStockService) {
        this.reserveStockService = reserveStockService;
        this.releaseStockService = releaseStockService;
    }

    /**
     * Reserves stock for an order, or returns the reservation already
     * made for it if this order id has been seen before.
     *
     * @param request the order and lines to reserve; must be valid per its own constraints
     * @return 201 Created, with a {@code Location} header and the reservation
     */
    @PostMapping
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReserveStockRequest request) {
        var lines = request.lines().stream()
                .map(line -> new ReserveStockCommand.Line(line.sku(), line.quantity()))
                .toList();
        Reservation reservation = reserveStockService.reserve(new ReserveStockCommand(request.orderId(), lines));

        return ResponseEntity.created(URI.create("/reservations/" + reservation.orderId().value()))
                .body(ReservationResponse.from(reservation));
    }

    /**
     * Releases the reservation made for an order, crediting its lines
     * back to available stock.
     *
     * @param orderId the order whose reservation should be released
     * @return 204 No Content
     */
    @PostMapping("/{orderId}/release")
    public ResponseEntity<Void> release(@PathVariable UUID orderId) {
        releaseStockService.release(new OrderId(orderId));
        return ResponseEntity.noContent().build();
    }
}
