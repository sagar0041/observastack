package com.observastack.orderservice.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * A non-negative monetary amount in a specific currency, with exactly
 * two decimal places.
 *
 * <p>Scale is normalised to 2 on construction so that two amounts
 * representing the same value always compare equal ({@code "5"} and
 * {@code "5.00"} are the same {@link Money}, even though the underlying
 * {@link BigDecimal#equals} would say otherwise). Amounts with more than
 * two decimal places are rejected rather than silently rounded, since
 * that would hide a caller bug.
 *
 * <p>{@link #add} refuses to combine amounts in different currencies —
 * adding USD to EUR is not a smaller number, it's a different question.
 *
 * @param amount   the monetary amount; must not be null, negative, or
 *                 carry more than two decimal places
 * @param currency the amount's currency; must not be null
 */
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("amount must not have more than 2 decimal places");
        }
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    /**
     * The zero amount, {@code 0.00}, in the given currency.
     *
     * @param currency the currency of the zero amount; must not be null
     * @return zero money, never null
     */
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    /**
     * Wraps a {@link BigDecimal} and {@link Currency} as {@link Money}.
     *
     * @param amount   the amount to wrap; must not be null, negative, or
     *                 carry more than two decimal places
     * @param currency the amount's currency; must not be null
     * @return the wrapped amount, never null
     */
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    /**
     * Adds another amount in the same currency to this one.
     *
     * @param other the amount to add; must not be null, and must be in
     *              the same currency as this amount
     * @return the sum, never null
     * @throws IllegalArgumentException if {@code other} is in a different
     *                                  currency
     */
    public Money add(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "cannot add " + other.currency.getCurrencyCode() + " to " + this.currency.getCurrencyCode());
        }
        return new Money(this.amount.add(other.amount), this.currency);
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
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }
}
