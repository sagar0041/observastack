package com.observastack.inventoryservice.domain;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class ReservationLineTest {

    @Test
    void constructor_throwsIllegalArgument_whenQuantityIsNotPositive() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ReservationLine(new Sku("SKU-1"), 0));
    }

    @Test
    void constructor_throwsNullPointer_whenSkuIsNull() {
        assertThatNullPointerException().isThrownBy(() -> new ReservationLine(null, 1));
    }
}
