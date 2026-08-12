package com.observastack.orderservice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void constructor_throwsIllegalArgument_whenAmountIsNegative() {
        assertThatIllegalArgumentException().isThrownBy(() -> Money.of(new BigDecimal("-0.01")));
    }

    @Test
    void constructor_throwsIllegalArgument_whenScaleExceedsTwoDecimalPlaces() {
        assertThatIllegalArgumentException().isThrownBy(() -> Money.of(new BigDecimal("1.999")));
    }

    @Test
    void constructor_padsScaleToTwoDecimalPlaces() {
        assertThat(Money.of(new BigDecimal("5")).amount()).isEqualByComparingTo("5.00");
    }

    @Test
    void add_sumsAmounts() {
        Money sum = Money.of(new BigDecimal("2.50")).add(Money.of(new BigDecimal("1.25")));

        assertThat(sum).isEqualTo(Money.of(new BigDecimal("3.75")));
    }

    @Test
    void multiply_throwsIllegalArgument_whenFactorIsNegative() {
        assertThatIllegalArgumentException().isThrownBy(() -> Money.of(BigDecimal.ONE).multiply(-1));
    }
}
