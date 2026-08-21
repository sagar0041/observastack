package com.observastack.inventoryservice.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /stock-items}.
 *
 * @param sku      stock keeping unit to stock; must not be blank, and at
 *                 most 64 characters (matches the {@code sku} column)
 * @param quantity initial available quantity; must not be negative
 */
public record CreateStockItemRequest(@NotBlank @Size(max = 64) String sku, @PositiveOrZero int quantity) {}
