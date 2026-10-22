package com.claims.adjudication;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ClaimLineTest {

    private ClaimLine createLine() {
        return new ClaimLine(
                "LINE001",
                LocalDate.of(2026, 10, 15),
                "99213",
                Money.of("250.00")
        );
    }

    @Test
    void createsClaimLineWithValidDetails() {
        ClaimLine line = createLine();

        assertEquals("LINE001", line.lineId());
        assertEquals(LocalDate.of(2026, 10, 15), line.serviceDate());
        assertEquals("99213", line.procedureCode());
        assertEquals(Money.of("250.00"), line.billedAmount());
    }

    @Test
    void trimsLineId() {
        ClaimLine line = new ClaimLine(
                " LINE001 ",
                LocalDate.of(2026, 10, 15),
                "99213",
                Money.of("250.00")
        );

        assertEquals("LINE001", line.lineId());
    }

    @Test
    void normalizesProcedureCode() {
        ClaimLine line = new ClaimLine(
                "LINE002",
                LocalDate.of(2026, 10, 15),
                " abc123 ",
                Money.of("100.00")
        );

        assertEquals("ABC123", line.procedureCode());
    }

    @Test
    void acceptsZeroBilledAmount() {
        ClaimLine line = new ClaimLine(
                "LINE003",
                LocalDate.of(2026, 10, 15),
                "99213",
                Money.zero()
        );

        assertTrue(line.billedAmount().isZero());
    }

    @Test
    void rejectsNegativeBilledAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                new ClaimLine(
                        "LINE004",
                        LocalDate.of(2026, 10, 15),
                        "99213",
                        Money.of("-1.00")
                ));
    }

    @Test
    void rejectsBlankLineId() {
        assertThrows(IllegalArgumentException.class, () ->
                new ClaimLine(
                        " ",
                        LocalDate.of(2026, 10, 15),
                        "99213",
                        Money.of("100.00")
                ));
    }

    @Test
    void rejectsBlankProcedureCode() {
        assertThrows(IllegalArgumentException.class, () ->
                new ClaimLine(
                        "LINE005",
                        LocalDate.of(2026, 10, 15),
                        " ",
                        Money.of("100.00")
                ));
    }

    @Test
    void rejectsNullRequiredFields() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () ->
                        new ClaimLine(
                                null,
                                LocalDate.of(2026, 10, 15),
                                "99213",
                                Money.of("100.00"))),

                () -> assertThrows(NullPointerException.class, () ->
                        new ClaimLine(
                                "LINE006",
                                null,
                                "99213",
                                Money.of("100.00"))),

                () -> assertThrows(NullPointerException.class, () ->
                        new ClaimLine(
                                "LINE007",
                                LocalDate.of(2026, 10, 15),
                                null,
                                Money.of("100.00"))),

                () -> assertThrows(NullPointerException.class, () ->
                        new ClaimLine(
                                "LINE008",
                                LocalDate.of(2026, 10, 15),
                                "99213",
                                null))
        );
    }
}