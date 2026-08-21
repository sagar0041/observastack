package com.observastack.inventoryservice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class StockItemTest {

    private static Sku aSku() {
        return new Sku("WIDGET-1");
    }

    @Test
    void of_succeeds_withNonNegativeQuantity() {
        StockItem item = StockItem.of(aSku(), 10);

        assertThat(item.availableQuantity()).isEqualTo(10);
    }

    @Test
    void of_throwsIllegalArgument_whenQuantityIsNegative() {
        assertThatIllegalArgumentException().isThrownBy(() -> StockItem.of(aSku(), -1));
    }

    @Test
    void reserve_decrementsAvailableQuantity() {
        StockItem item = StockItem.of(aSku(), 10);

        item.reserve(4);

        assertThat(item.availableQuantity()).isEqualTo(6);
    }

    @Test
    void reserve_throwsInsufficientStock_whenQuantityExceedsAvailable() {
        StockItem item = StockItem.of(aSku(), 3);

        assertThatExceptionOfType(InsufficientStockException.class).isThrownBy(() -> item.reserve(4));
        assertThat(item.availableQuantity()).isEqualTo(3); // unchanged on failure
    }

    @Test
    void reserve_succeeds_whenQuantityExactlyMatchesAvailable() {
        StockItem item = StockItem.of(aSku(), 3);

        item.reserve(3);

        assertThat(item.availableQuantity()).isZero();
    }

    @Test
    void reserve_throwsIllegalArgument_whenQuantityIsNotPositive() {
        StockItem item = StockItem.of(aSku(), 10);

        assertThatIllegalArgumentException().isThrownBy(() -> item.reserve(0));
    }

    @Test
    void release_incrementsAvailableQuantity() {
        StockItem item = StockItem.of(aSku(), 6);

        item.release(4);

        assertThat(item.availableQuantity()).isEqualTo(10);
    }

    @Test
    void release_throwsIllegalArgument_whenQuantityIsNotPositive() {
        StockItem item = StockItem.of(aSku(), 10);

        assertThatIllegalArgumentException().isThrownBy(() -> item.release(0));
    }
}
