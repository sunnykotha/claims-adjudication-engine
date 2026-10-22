package com.claims.adjudication;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void createsMoneyWithTwoDecimalPlaces() {
        Money money = Money.of("10");

        assertEquals(new BigDecimal("10.00"), money.amount());
    }

    @Test
    void addsMoneyCorrectly() {
        Money first = Money.of("100.25");
        Money second = Money.of("50.50");

        assertEquals(Money.of("150.75"), first.add(second));
    }

    @Test
    void subtractsMoneyCorrectly() {
        Money total = Money.of("1200.00");
        Money deductible = Money.of("800.00");

        assertEquals(Money.of("400.00"), total.subtract(deductible));
    }

    @Test
    void roundsToTwoDecimalPlacesUsingHalfUp() {
        assertEquals(Money.of("10.13"), Money.of("10.125"));
        assertEquals(Money.of("10.12"), Money.of("10.124"));
    }

    @Test
    void comparesAmountsNumerically() {
        assertEquals(0, Money.of("10.00").compareTo(Money.of("10")));
        assertTrue(Money.of("20.00").compareTo(Money.of("10.00")) > 0);
        assertTrue(Money.of("5.00").compareTo(Money.of("10.00")) < 0);
    }

    @Test
    void identifiesZeroAndNegativeAmounts() {
        assertTrue(Money.zero().isZero());
        assertFalse(Money.zero().isNegative());
        assertTrue(Money.of("-1.00").isNegative());
    }

    @Test
    void rejectsNullAmounts() {
        assertThrows(
            NullPointerException.class,
            () -> new Money(null)
        );
    }
}