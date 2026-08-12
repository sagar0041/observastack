package com.observastack.orderservice.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request body for {@code POST /orders}.
 *
 * <p>Validated by Bean Validation before the controller method runs;
 * deeper business invariants (a SKU's exact format) are still enforced
 * by the domain layer, since this layer only checks shape, not business
 * meaning. The size limits here exist so an oversized SKU or price
 * fails as a 400 here rather than as an unhandled 500 when it no longer
 * fits the {@code orders}/{@code order_line_items} column definitions.
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
     * @param sku       stock keeping unit; must not be blank, and at most
     *                  64 characters (matches the {@code sku} column)
     * @param quantity  units requested; must be positive
     * @param unitPrice price per unit, in decimal currency units; must not
     *                  be null or negative, and must fit the {@code
     *                  numeric(12,2)} {@code unit_price} column
     */
    public record LineItemRequest(
            @NotBlank @Size(max = 64) String sku,
            @Positive int quantity,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice) {}
}
