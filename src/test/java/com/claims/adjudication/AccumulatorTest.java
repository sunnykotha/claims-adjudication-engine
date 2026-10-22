package com.claims.adjudication;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccumulatorTest {

    @Test
    void zeroCreatesEmptyAccumulators() {
        Accumulator accumulator = Accumulator.zero();

        assertEquals(Money.of("0.00"), accumulator.deductibleMet());
        assertEquals(Money.of("0.00"), accumulator.outOfPocketMet());
    }

    @Test
    void constructorAcceptsExistingAmounts() {
        Accumulator accumulator = new Accumulator(
                Money.of("200.00"),
                Money.of("350.00"));

        assertEquals(Money.of("200.00"), accumulator.deductibleMet());
        assertEquals(Money.of("350.00"), accumulator.outOfPocketMet());
    }

    @Test
    void constructorRejectsNegativeDeductible() {
        assertThrows(IllegalArgumentException.class,
                () -> new Accumulator(
                        Money.of("-1.00"),
                        Money.zero()));
    }

    @Test
    void constructorRejectsNegativeOutOfPocketAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new Accumulator(
                        Money.zero(),
                        Money.of("-1.00")));
    }

    @Test
    void addDeductibleIncreasesBothAccumulators() {
        Accumulator initial = new Accumulator(
                Money.of("200.00"),
                Money.of("300.00"));

        Accumulator updated = initial.addDeductible(Money.of("100.00"));

        assertEquals(Money.of("300.00"), updated.deductibleMet());
        assertEquals(Money.of("400.00"), updated.outOfPocketMet());
    }

    @Test
    void addOutOfPocketChangesOnlyOutOfPocketAccumulator() {
        Accumulator initial = new Accumulator(
                Money.of("200.00"),
                Money.of("300.00"));

        Accumulator updated = initial.addOutOfPocket(Money.of("50.00"));

        assertEquals(Money.of("200.00"), updated.deductibleMet());
        assertEquals(Money.of("350.00"), updated.outOfPocketMet());
    }

    @Test
    void addDeductibleRejectsNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> Accumulator.zero().addDeductible(Money.of("-1.00")));
    }

    @Test
    void addOutOfPocketRejectsNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> Accumulator.zero().addOutOfPocket(Money.of("-1.00")));
    }

    @Test
    void addMethodsRejectNullAmounts() {
        Accumulator accumulator = Accumulator.zero();

        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> accumulator.addDeductible(null)),
                () -> assertThrows(NullPointerException.class,
                        () -> accumulator.addOutOfPocket(null))
        );
    }

    @Test
    void updatesDoNotMutateOriginalAccumulator() {
        Accumulator original = Accumulator.zero();

        Accumulator updated = original.addDeductible(Money.of("100.00"));

        assertEquals(Money.zero(), original.deductibleMet());
        assertEquals(Money.zero(), original.outOfPocketMet());
        assertEquals(Money.of("100.00"), updated.deductibleMet());
        assertEquals(Money.of("100.00"), updated.outOfPocketMet());
    }
    @Test
    void tracksFamilyDeductibleSeparately() {
        Accumulator accumulator = new Accumulator(
                Money.of("200.00"),
                Money.of("300.00"),
                Money.of("600.00"));

        assertEquals(Money.of("200.00"), accumulator.deductibleMet());
        assertEquals(Money.of("300.00"), accumulator.outOfPocketMet());
        assertEquals(Money.of("600.00"), accumulator.familyDeductibleMet());
    }

    @Test
    void addingDeductibleIncreasesFamilyDeductibleProgress() {
        Accumulator initial = new Accumulator(
                Money.of("200.00"),
                Money.of("300.00"),
                Money.of("600.00"));

        Accumulator updated = initial.addDeductible(Money.of("100.00"));

        assertEquals(Money.of("300.00"), updated.deductibleMet());
        assertEquals(Money.of("400.00"), updated.outOfPocketMet());
        assertEquals(Money.of("700.00"), updated.familyDeductibleMet());
    }
}