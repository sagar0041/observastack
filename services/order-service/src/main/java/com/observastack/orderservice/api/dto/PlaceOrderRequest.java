package com.observastack.orderservice.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request body for {@code POST /orders}.
 *
 * <p>Validated by Bean Validation before the controller method runs;
 * deeper business invariants (a SKU's exact format, a price's scale) are
 * still enforced by the domain layer, since this layer only checks
 * shape, not business meaning.
 *
 * @param customerId the ordering customer's id; must not be null
 * @param lineItems  the items being ordered; must not be null or empty
 */
public record PlaceOrderRequest(
        @NotNull UUID customerId,
        @NotEmpty @Valid List<LineItemRequest> lineItems) {

    /**
     * One requested line item.
     *
     * @param sku       stock keeping unit; must not be blank
     * @param quantity  units requested; must be positive
     * @param unitPrice price per unit, in decimal currency units; must not
     *                  be null or negative
     */
    public record LineItemRequest(
            @NotBlank String sku,
            @Positive int quantity,
            @NotNull @DecimalMin("0.00") BigDecimal unitPrice) {}
}
