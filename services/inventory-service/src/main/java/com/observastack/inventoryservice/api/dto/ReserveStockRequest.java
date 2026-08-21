package com.observastack.inventoryservice.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Request body for {@code POST /reservations}.
 *
 * @param orderId the order this reservation is for; must not be null
 * @param lines   the SKUs and quantities to reserve; must not be null or empty
 */
public record ReserveStockRequest(@NotNull UUID orderId, @NotEmpty @Valid List<LineRequest> lines) {

    /**
     * @param sku      stock keeping unit; must not be blank
     * @param quantity units requested; must be positive
     */
    public record LineRequest(@NotBlank @Size(max = 64) String sku, @Positive int quantity) {}
}
