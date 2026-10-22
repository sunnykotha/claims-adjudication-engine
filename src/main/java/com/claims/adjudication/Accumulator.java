package com.claims.adjudication;

import java.util.Objects;

public record Accumulator(
        Money deductibleMet,
        Money outOfPocketMet,
        Money familyDeductibleMet) {

    public Accumulator(Money deductibleMet, Money outOfPocketMet) {
        this(deductibleMet, outOfPocketMet, deductibleMet);
    }

    public Accumulator {
        Objects.requireNonNull(deductibleMet, "deductibleMet must not be null");
        Objects.requireNonNull(outOfPocketMet, "outOfPocketMet must not be null");
        Objects.requireNonNull(
                familyDeductibleMet, "familyDeductibleMet must not be null");

        if (deductibleMet.isNegative()) {
            throw new IllegalArgumentException("deductibleMet must not be negative");
        }

        if (outOfPocketMet.isNegative()) {
            throw new IllegalArgumentException("outOfPocketMet must not be negative");
        }

        if (familyDeductibleMet.isNegative()) {
            throw new IllegalArgumentException(
                    "familyDeductibleMet must not be negative");
        }
    }

    public static Accumulator zero() {
        return new Accumulator(Money.zero(), Money.zero(), Money.zero());
    }

    public Accumulator addDeductible(Money amount) {
        Objects.requireNonNull(amount, "amount must not be null");

        if (amount.isNegative()) {
            throw new IllegalArgumentException("amount must not be negative");
        }

        return new Accumulator(
                deductibleMet.add(amount),
                outOfPocketMet.add(amount),
                familyDeductibleMet.add(amount));
    }

    public Accumulator addOutOfPocket(Money amount) {
        Objects.requireNonNull(amount, "amount must not be null");

        if (amount.isNegative()) {
            throw new IllegalArgumentException("amount must not be negative");
        }

        return new Accumulator(
                deductibleMet,
                outOfPocketMet.add(amount),
                familyDeductibleMet);
    }
}