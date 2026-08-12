package com.observastack.orderservice.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A non-negative monetary amount with exactly two decimal places.
 *
 * <p>Scale is normalised to 2 on construction so that two amounts
 * representing the same value always compare equal ({@code "5"} and
 * {@code "5.00"} are the same {@link Money}, even though the underlying
 * {@link BigDecimal#equals} would say otherwise). Amounts with more than
 * two decimal places are rejected rather than silently rounded, since
 * that would hide a caller bug.
 *
 * @param amount the monetary amount; must not be null, negative, or carry
 *               more than two decimal places
 */
public record Money(BigDecimal amount) {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("amount must not have more than 2 decimal places");
        }
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    /**
     * The zero amount, {@code 0.00}.
     *
     * @return zero money, never null
     */
    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    /**
     * Wraps a {@link BigDecimal} as {@link Money}.
     *
     * @param amount the amount to wrap; must not be null, negative, or carry
     *               more than two decimal places
     * @return the wrapped amount, never null
     */
    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    /**
     * Adds another amount to this one.
     *
     * @param other the amount to add; must not be null
     * @return the sum, never null
     */
    public Money add(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        return new Money(this.amount.add(other.amount));
    }

    /**
     * Multiplies this amount by a non-negative whole-number factor.
     *
     * @param factor the factor to multiply by; must not be negative
     * @return the product, never null
     * @throws IllegalArgumentException if {@code factor} is negative
     */
    public Money multiply(int factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("factor must not be negative");
        }
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)));
    }
}
