package com.claims.adjudication;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PolicyTest {

    private Policy createPolicy() {
        return new Policy(
                "POL001",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Money.of("1000.00"),
                new BigDecimal("0.20"),
                Set.of("99213", "80053")
        );
    }

    @Test
    void createsPolicyWithValidDetails() {
        Policy policy = createPolicy();

        assertEquals("POL001", policy.policyId());
        assertEquals(Money.of("1000.00"), policy.annualDeductible());
        assertEquals(new BigDecimal("0.20"), policy.coinsuranceRate());
        assertEquals(2, policy.coveredProcedureCodes().size());
    }

    @Test
    void acceptsServiceOnEffectiveDate() {
        assertTrue(createPolicy().coversServiceDate(
                LocalDate.of(2026, 1, 1)));
    }

    @Test
    void acceptsServiceOnTerminationDate() {
        assertTrue(createPolicy().coversServiceDate(
                LocalDate.of(2026, 12, 31)));
    }

    @Test
    void rejectsServiceBeforeEffectiveDate() {
        assertFalse(createPolicy().coversServiceDate(
                LocalDate.of(2025, 12, 31)));
    }

    @Test
    void rejectsServiceAfterTerminationDate() {
        assertFalse(createPolicy().coversServiceDate(
                LocalDate.of(2027, 1, 1)));
    }

    @Test
    void recognizesCoveredProcedureCodesIgnoringCase() {
        Policy policy = createPolicy();

        assertTrue(policy.coversProcedure("99213"));
        assertTrue(policy.coversProcedure("80053"));
        assertTrue(policy.coversProcedure(" 99213 "));
        assertFalse(policy.coversProcedure("99999"));
    }

    @Test
    void normalizesProcedureCodesToUppercase() {
        Policy policy = new Policy(
                "POL002",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Money.zero(),
                new BigDecimal("0.10"),
                Set.of("abc123")
        );

        assertTrue(policy.coversProcedure("ABC123"));
        assertTrue(policy.coveredProcedureCodes().contains("ABC123"));
    }

    @Test
    void rejectsTerminationBeforeEffectiveDate() {
        assertThrows(IllegalArgumentException.class, () ->
                new Policy(
                        "POL003",
                        LocalDate.of(2026, 12, 31),
                        LocalDate.of(2026, 1, 1),
                        Money.zero(),
                        new BigDecimal("0.20"),
                        Set.of("99213")
                ));
    }

    @Test
    void rejectsNegativeDeductible() {
        assertThrows(IllegalArgumentException.class, () ->
                new Policy(
                        "POL004",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        Money.of("-1.00"),
                        new BigDecimal("0.20"),
                        Set.of("99213")
                ));
    }

    @Test
    void rejectsCoinsuranceRateOutsideZeroAndOne() {
        assertThrows(IllegalArgumentException.class, () ->
                new Policy(
                        "POL005",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        Money.zero(),
                        new BigDecimal("1.01"),
                        Set.of("99213")
                ));

        assertThrows(IllegalArgumentException.class, () ->
                new Policy(
                        "POL006",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        Money.zero(),
                        new BigDecimal("-0.01"),
                        Set.of("99213")
                ));
    }

    @Test
    void coveredProcedureCodesCannotBeModified() {
        Policy policy = createPolicy();

        assertThrows(UnsupportedOperationException.class, () ->
                policy.coveredProcedureCodes().add("99999"));
    }

    @Test
    void defensivelyCopiesCoveredProcedureCodes() {
        Set<String> codes = new HashSet<>(Set.of("99213"));

        Policy policy = new Policy(
                "POL007",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Money.zero(),
                new BigDecimal("0.20"),
                codes
        );

        codes.add("99999");

        assertFalse(policy.coversProcedure("99999"));
        assertTrue(policy.coversProcedure("99213"));
    }
    @Test
    void acceptsOptionalFamilyDeductible() {
        Policy policy = new Policy(
                "POL008",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Money.of("1000.00"),
                new BigDecimal("0.20"),
                Set.of("99213"),
                Set.of(),
                Set.of(),
                null,
                Money.of("2000.00"));

        assertEquals(Money.of("2000.00"), policy.familyDeductible());
    }

    @Test
    void rejectsNegativeFamilyDeductible() {
        assertThrows(IllegalArgumentException.class, () ->
                new Policy(
                        "POL009",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        Money.of("1000.00"),
                        new BigDecimal("0.20"),
                        Set.of("99213"),
                        Set.of(),
                        Set.of(),
                        null,
                        Money.of("-1.00")));
    }
}