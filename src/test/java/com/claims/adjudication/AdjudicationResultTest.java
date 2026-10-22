package com.claims.adjudication;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdjudicationResultTest {

    private ClaimLine sampleLine() {
        return new ClaimLine(
                "LINE-1",
                LocalDate.of(2026, 3, 10),
                "99213",
                Money.of("1200.00"));
    }

    @Test
    void createsApprovedResultWithoutDenialReason() {
        AdjudicationResult result = new AdjudicationResult(
                sampleLine(),
                Money.of("1200.00"),
                Money.of("320.00"),
                Money.of("880.00"),
                null,
                List.of("Deductible applied", "Coinsurance applied"));

        assertFalse(result.isDenied());
        assertNull(result.denialReason());
        assertEquals(Money.of("1200.00"), result.allowedAmount());
        assertEquals(Money.of("320.00"), result.insurerPayment());
        assertEquals(Money.of("880.00"), result.patientResponsibility());
    }

    @Test
    void createsDeniedResultWithZeroPayments() {
        AdjudicationResult result = new AdjudicationResult(
                sampleLine(),
                Money.zero(),
                Money.zero(),
                Money.zero(),
                DenialReason.SERVICE_NOT_COVERED,
                List.of("Procedure is not covered"));

        assertTrue(result.isDenied());
        assertEquals(DenialReason.SERVICE_NOT_COVERED, result.denialReason());
        assertEquals(Money.zero(), result.insurerPayment());
        assertEquals(Money.zero(), result.patientResponsibility());
    }

    @Test
    void explanationsAreDefensivelyCopied() {
        List<String> explanations = new ArrayList<>();
        explanations.add("Eligibility checked");

        AdjudicationResult result = new AdjudicationResult(
                sampleLine(),
                Money.of("100.00"),
                Money.of("80.00"),
                Money.of("20.00"),
                null,
                explanations);

        explanations.add("Unexpected later change");

        assertEquals(List.of("Eligibility checked"), result.ruleExplanations());
        assertThrows(UnsupportedOperationException.class,
                () -> result.ruleExplanations().add("Another explanation"));
    }

    @Test
    void rejectsNegativeFinancialAmounts() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AdjudicationResult(
                                sampleLine(), Money.of("-1.00"),
                                Money.zero(), Money.zero(), null, List.of())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AdjudicationResult(
                                sampleLine(), Money.zero(),
                                Money.of("-1.00"), Money.zero(), null, List.of())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AdjudicationResult(
                                sampleLine(), Money.zero(),
                                Money.zero(), Money.of("-1.00"), null, List.of()))
        );
    }

    @Test
    void rejectsPaymentAmountsOnDeniedResult() {
        assertThrows(IllegalArgumentException.class,
                () -> new AdjudicationResult(
                        sampleLine(),
                        Money.of("100.00"),
                        Money.of("80.00"),
                        Money.of("20.00"),
                        DenialReason.DUPLICATE_CLAIM,
                        List.of("Duplicate detected")));
    }

    @Test
    void rejectsNullRequiredFields() {
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> new AdjudicationResult(
                                null, Money.zero(), Money.zero(),
                                Money.zero(), null, List.of())),
                () -> assertThrows(NullPointerException.class,
                        () -> new AdjudicationResult(
                                sampleLine(), null, Money.zero(),
                                Money.zero(), null, List.of())),
                () -> assertThrows(NullPointerException.class,
                        () -> new AdjudicationResult(
                                sampleLine(), Money.zero(), Money.zero(),
                                Money.zero(), null, null))
        );
    }

    @Test
    void rejectsNullExplanationEntries() {
        List<String> explanations = new ArrayList<>();
        explanations.add(null);

        assertThrows(IllegalArgumentException.class,
                () -> new AdjudicationResult(
                        sampleLine(), Money.zero(), Money.zero(),
                        Money.zero(), null, explanations));
    }
}
