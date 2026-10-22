package com.claims.adjudication;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Represents a monetary amount with exactly two decimal places.
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(amount, "Money amount cannot be null");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public static Money zero() {
        return Money.of("0.00");
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "Other money cannot be null");
        return new Money(amount.add(other.amount));
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "Other money cannot be null");
        return new Money(amount.subtract(other.amount));
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "Other money cannot be null");
        return amount.compareTo(other.amount);
    }
}