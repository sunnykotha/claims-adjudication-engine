package com.claims.adjudication;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClaimTest {

    private Patient patient() {
        return new Patient("P001", "John Smith");
    }

    private Policy policy() {
        return new Policy(
                "POL001",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Money.of("1000.00"),
                new BigDecimal("0.20"),
                Set.of("99213", "80053")
        );
    }

    private Provider provider() {
        return new Provider("PR001", "City Hospital");
    }

    private ClaimLine line(String id, String code, String amount) {
        return new ClaimLine(
                id,
                LocalDate.of(2026, 10, 15),
                code,
                Money.of(amount)
        );
    }

    private Claim createClaim() {
        return new Claim(
                "CLM001",
                patient(),
                policy(),
                provider(),
                LocalDate.of(2026, 10, 20),
                List.of(
                        line("L001", "99213", "250.00"),
                        line("L002", "80053", "100.00")
                )
        );
    }

    @Test
    void createsClaimWithValidDetails() {
        Claim claim = createClaim();

        assertEquals("CLM001", claim.claimId());
        assertEquals("P001", claim.patient().patientId());
        assertEquals("POL001", claim.policy().policyId());
        assertEquals("PR001", claim.provider().providerId());
        assertEquals(LocalDate.of(2026, 10, 20), claim.submissionDate());
        assertEquals(2, claim.lines().size());
    }

    @Test
    void preservesClaimLineOrder() {
        Claim claim = createClaim();

        assertEquals("L001", claim.lines().get(0).lineId());
        assertEquals("L002", claim.lines().get(1).lineId());
    }

    @Test
    void makesClaimLinesImmutable() {
        Claim claim = createClaim();

        assertThrows(UnsupportedOperationException.class,
                () -> claim.lines().add(line("L003", "99213", "50.00")));
    }

    @Test
    void defensivelyCopiesInputLines() {
        List<ClaimLine> mutableLines = new ArrayList<>();
        mutableLines.add(line("L001", "99213", "250.00"));

        Claim claim = new Claim(
                "CLM002",
                patient(),
                policy(),
                provider(),
                LocalDate.of(2026, 10, 20),
                mutableLines
        );

        mutableLines.add(line("L002", "80053", "100.00"));

        assertEquals(1, claim.lines().size());
    }

    @Test
    void rejectsEmptyClaim() {
        assertThrows(IllegalArgumentException.class, () ->
                new Claim(
                        "CLM003",
                        patient(),
                        policy(),
                        provider(),
                        LocalDate.of(2026, 10, 20),
                        List.of()
                ));
    }

    @Test
    void rejectsDuplicateLineIds() {
        assertThrows(IllegalArgumentException.class, () ->
                new Claim(
                        "CLM004",
                        patient(),
                        policy(),
                        provider(),
                        LocalDate.of(2026, 10, 20),
                        List.of(
                                line("L001", "99213", "100.00"),
                                line("L001", "80053", "200.00")
                        )
                ));
    }

    @Test
    void rejectsBlankClaimId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Claim(
                        " ",
                        patient(),
                        policy(),
                        provider(),
                        LocalDate.of(2026, 10, 20),
                        List.of(line("L001", "99213", "100.00"))
                ));
    }

    @Test
    void rejectsNullRequiredFields() {
        ClaimLine validLine = line("L001", "99213", "100.00");
        LocalDate submissionDate = LocalDate.of(2026, 10, 20);

        assertAll(
                () -> assertThrows(NullPointerException.class, () ->
                        new Claim(null, patient(), policy(), provider(),
                                submissionDate, List.of(validLine))),

                () -> assertThrows(NullPointerException.class, () ->
                        new Claim("CLM005", null, policy(), provider(),
                                submissionDate, List.of(validLine))),

                () -> assertThrows(NullPointerException.class, () ->
                        new Claim("CLM006", patient(), null, provider(),
                                submissionDate, List.of(validLine))),

                () -> assertThrows(NullPointerException.class, () ->
                        new Claim("CLM007", patient(), policy(), null,
                                submissionDate, List.of(validLine))),

                () -> assertThrows(NullPointerException.class, () ->
                        new Claim("CLM008", patient(), policy(), provider(),
                                null, List.of(validLine))),

                () -> assertThrows(NullPointerException.class, () ->
                        new Claim("CLM009", patient(), policy(), provider(),
                                submissionDate, null))
        );
    }
}