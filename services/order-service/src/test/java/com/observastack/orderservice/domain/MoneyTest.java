package com.observastack.orderservice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void constructor_throwsIllegalArgument_whenAmountIsNegative() {
        assertThatIllegalArgumentException().isThrownBy(() -> Money.of(new BigDecimal("-0.01"), USD));
    }

    @Test
    void constructor_throwsIllegalArgument_whenScaleExceedsTwoDecimalPlaces() {
        assertThatIllegalArgumentException().isThrownBy(() -> Money.of(new BigDecimal("1.999"), USD));
    }

    @Test
    void constructor_padsScaleToTwoDecimalPlaces() {
        assertThat(Money.of(new BigDecimal("5"), USD).amount()).isEqualByComparingTo("5.00");
    }

    @Test
    void add_sumsAmounts_whenSameCurrency() {
        Money sum = Money.of(new BigDecimal("2.50"), USD).add(Money.of(new BigDecimal("1.25"), USD));

        assertThat(sum).isEqualTo(Money.of(new BigDecimal("3.75"), USD));
    }

    @Test
    void add_throwsIllegalArgument_whenCurrenciesDiffer() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Money.of(BigDecimal.TEN, USD).add(Money.of(BigDecimal.ONE, EUR)));
    }

    @Test
    void multiply_throwsIllegalArgument_whenFactorIsNegative() {
        assertThatIllegalArgumentException().isThrownBy(() -> Money.of(BigDecimal.ONE, USD).multiply(-1));
    }
}
