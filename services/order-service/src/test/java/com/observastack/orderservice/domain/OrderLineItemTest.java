package com.observastack.orderservice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderLineItemTest {

    @Test
    void constructor_throwsIllegalArgument_whenQuantityIsZero() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OrderLineItem(new Sku("SKU-1"), 0, Money.of(BigDecimal.ONE)));
    }

    @Test
    void constructor_throwsIllegalArgument_whenQuantityIsNegative() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OrderLineItem(new Sku("SKU-1"), -1, Money.of(BigDecimal.ONE)));
    }

    @Test
    void constructor_throwsNullPointer_whenSkuIsNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new OrderLineItem(null, 1, Money.of(BigDecimal.ONE)));
    }

    @Test
    void lineTotal_multipliesUnitPriceByQuantity() {
        OrderLineItem item = new OrderLineItem(new Sku("SKU-1"), 3, Money.of(new BigDecimal("2.50")));

        assertThat(item.lineTotal()).isEqualTo(Money.of(new BigDecimal("7.50")));
    }
}
