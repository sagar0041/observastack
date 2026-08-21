package com.observastack.inventoryservice.application;

/**
 * Input to {@link CreateStockItemService#create}.
 *
 * @param sku      stock keeping unit to stock; must not be blank
 * @param quantity initial available quantity; must not be negative
 */
public record CreateStockItemCommand(String sku, int quantity) {}
