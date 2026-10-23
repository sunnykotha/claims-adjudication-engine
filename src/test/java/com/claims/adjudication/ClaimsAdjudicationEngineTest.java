package com.claims.adjudication;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClaimsAdjudicationEngineTest {

    private Policy policy(
            LocalDate effective,
            LocalDate termination,
            Set<String> coveredCodes) {

        return new Policy(
                "POL-1",
                effective,
                termination,
                Money.of("1000.00"),
                new BigDecimal("0.20"),
                coveredCodes);
    }

    private Claim claim(Policy policy, ClaimLine line) {
        return new Claim(
                "CLAIM-1",
                new Patient("PAT-1", "Test Patient"),
                policy,
                new Provider("PROV-1", "Test Provider"),
                LocalDate.of(2026, 4, 1),
                List.of(line));
    }

    private ClaimLine line(LocalDate serviceDate, String code) {
        return new ClaimLine(
                "LINE-1", serviceDate, code, Money.of("100.00"));
    }

    @Test
    void deniesServiceBeforePolicyEffectiveDate() {
        ClaimLine line = line(LocalDate.of(2025, 12, 31), "99213");
        Claim claim = claim(
                policy(LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        Set.of("99213")),
                line);

        AdjudicationResult result =
                new ClaimsAdjudicationEngine().adjudicate(claim, line);

        assertEquals(DenialReason.INELIGIBLE_MEMBER, result.denialReason());
        assertTrue(result.isDenied());
    }

    @Test
    void deniesServiceAfterPolicyTerminationDate() {
        ClaimLine line = line(LocalDate.of(2027, 1, 1), "99213");
        Claim claim = claim(
                policy(LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        Set.of("99213")),
                line);

        AdjudicationResult result =
                new ClaimsAdjudicationEngine().adjudicate(claim, line);

        assertEquals(DenialReason.INELIGIBLE_MEMBER, result.denialReason());
    }

    @Test
    void deniesProcedureNotCoveredByPolicy() {
        ClaimLine line = line(LocalDate.of(2026, 3, 1), "99999");
        Claim claim = claim(
                policy(LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        Set.of("99213")),
                line);

        AdjudicationResult result =
                new ClaimsAdjudicationEngine().adjudicate(claim, line);

        assertEquals(DenialReason.SERVICE_NOT_COVERED, result.denialReason());
    }

    @Test
    void acceptsEligibleCoveredServiceForFurtherAdjudication() {
        ClaimLine line = line(LocalDate.of(2026, 3, 1), "99213");
        Claim claim = claim(
                policy(LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        Set.of("99213")),
                line);

        AdjudicationResult result =
                new ClaimsAdjudicationEngine().adjudicate(claim, line);

        assertFalse(result.isDenied());
        assertNull(result.denialReason());
        assertEquals(
                List.of("Eligibility check passed", "Covered-service check passed", "Referral check passed", "Prior-authorization check passed", "Duplicate check passed", "Timely-filing check passed", "Network allowed-amount check passed", "Deductible and coinsurance calculated"),
                result.ruleExplanations());
    }

    @Test
    void deniesPreviouslySubmittedDuplicateClaimLine() {
        ClaimLine previousLine = line(LocalDate.of(2026, 3, 1), "99213");
        Claim previousClaim = claim(
                policy(LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31), Set.of("99213")),
                previousLine);

        ClaimLine newLine = line(LocalDate.of(2026, 3, 1), "99213");
        Claim newClaim = new Claim(
                "CLAIM-2",
                previousClaim.patient(),
                previousClaim.policy(),
                previousClaim.provider(),
                LocalDate.of(2026, 4, 2),
                List.of(newLine));

        AdjudicationResult result = new ClaimsAdjudicationEngine()
                .adjudicate(newClaim, newLine, List.of(previousClaim));

        assertEquals(DenialReason.DUPLICATE_CLAIM, result.denialReason());
    }

    @Test
    void doesNotTreatDifferentServiceDateAsDuplicate() {
        ClaimLine previousLine = line(LocalDate.of(2026, 3, 1), "99213");
        Claim previousClaim = claim(
                policy(LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31), Set.of("99213")),
                previousLine);

        ClaimLine newLine = line(LocalDate.of(2026, 3, 2), "99213");
        Claim newClaim = new Claim(
                "CLAIM-2",
                previousClaim.patient(),
                previousClaim.policy(),
                previousClaim.provider(),
                LocalDate.of(2026, 4, 2),
                List.of(newLine));

        AdjudicationResult result = new ClaimsAdjudicationEngine()
                .adjudicate(newClaim, newLine, List.of(previousClaim));

        assertFalse(result.isDenied());
    }

    @Test
    void acceptsClaimSubmittedWithinTimelyFilingLimit() {
        ClaimLine line = line(LocalDate.of(2026, 3, 10), "99213");
        Claim originalClaim = claim(
                policy(LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        Set.of("99213")),
                line);

        Claim submittedClaim = new Claim(
                "CLAIM-2",
                originalClaim.patient(),
                originalClaim.policy(),
                originalClaim.provider(),
                LocalDate.of(2026, 3, 30),
                List.of(line));

        AdjudicationResult result =
                new ClaimsAdjudicationEngine(30).adjudicate(submittedClaim, line);

        assertFalse(result.isDenied());
    }

    @Test
    void acceptsClaimSubmittedExactlyAtTimelyFilingLimit() {
        ClaimLine line = line(LocalDate.of(2026, 3, 1), "99213");
        Claim originalClaim = claim(
                policy(LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        Set.of("99213")),
                line);

        Claim submittedClaim = new Claim(
                "CLAIM-2",
                originalClaim.patient(),
                originalClaim.policy(),
                originalClaim.provider(),
                LocalDate.of(2026, 3, 31),
                List.of(line));

        AdjudicationResult result =
                new ClaimsAdjudicationEngine(30).adjudicate(submittedClaim, line);

        assertFalse(result.isDenied());
    }

    @Test
    void deniesClaimSubmittedBeyondTimelyFilingLimit() {
        ClaimLine line = line(LocalDate.of(2026, 3, 1), "99213");
        Claim originalClaim = claim(
                policy(LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        Set.of("99213")),
                line);

        Claim submittedClaim = new Claim(
                "CLAIM-2",
                originalClaim.patient(),
                originalClaim.policy(),
                originalClaim.provider(),
                LocalDate.of(2026, 4, 1),
                List.of(line));

        AdjudicationResult result =
                new ClaimsAdjudicationEngine(30).adjudicate(submittedClaim, line);

        assertEquals(
                DenialReason.TIMELY_FILING_EXCEEDED,
                result.denialReason());
        assertTrue(result.isDenied());
    }

    @Test
    void deniesServiceWhenRequiredReferralWasNotObtained() {
        ClaimLine line = new ClaimLine(
                "LINE-1",
                LocalDate.of(2026, 3, 1),
                "99213",
                Money.of("100.00"),
                false,
                true);

        Policy policy = new Policy(
                "POL-1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Money.of("1000.00"),
                new BigDecimal("0.20"),
                Set.of("99213"),
                Set.of("99213"),
                Set.of());

        Claim claim = claim(policy, line);

        AdjudicationResult result =
                new ClaimsAdjudicationEngine().adjudicate(claim, line);

        assertEquals(DenialReason.REFERRAL_REQUIRED, result.denialReason());
        assertTrue(result.isDenied());
    }

    @Test
    void deniesServiceWhenRequiredPriorAuthorizationWasNotObtained() {
        ClaimLine line = new ClaimLine(
                "LINE-1",
                LocalDate.of(2026, 3, 1),
                "99213",
                Money.of("100.00"),
                true,
                false);

        Policy policy = new Policy(
                "POL-1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Money.of("1000.00"),
                new BigDecimal("0.20"),
                Set.of("99213"),
                Set.of(),
                Set.of("99213"));

        Claim claim = claim(policy, line);

        AdjudicationResult result =
                new ClaimsAdjudicationEngine().adjudicate(claim, line);

        assertEquals(
                DenialReason.PRIOR_AUTHORIZATION_REQUIRED,
                result.denialReason());
        assertTrue(result.isDenied());
    }

    @Test
    void usesContractedRateForInNetworkProvider() {
        ClaimLine line = new ClaimLine(
                "LINE-1",
                LocalDate.of(2026, 3, 1),
                "99213",
                Money.of("100.00"),
                true,
                true,
                Money.of("80.00"),
                Money.of("120.00"));

        Policy policy = policy(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Set.of("99213"));

        Claim claim = new Claim(
                "CLAIM-1",
                new Patient("PAT-1", "Test Patient"),
                policy,
                new Provider("PROV-1", "Test Provider", true),
                LocalDate.of(2026, 4, 1),
                List.of(line));

        AdjudicationResult result =
                new ClaimsAdjudicationEngine().adjudicate(claim, line);

        assertEquals(Money.of("80.00"), result.allowedAmount());
    }

    @Test
    void usesSixtyPercentOfUsualAndCustomaryAmountOutOfNetwork() {
        ClaimLine line = new ClaimLine(
                "LINE-1",
                LocalDate.of(2026, 3, 1),
                "99213",
                Money.of("100.00"),
                true,
                true,
                Money.of("80.00"),
                Money.of("120.00"));

        Policy policy = policy(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Set.of("99213"));

        Claim claim = new Claim(
                "CLAIM-1",
                new Patient("PAT-1", "Test Patient"),
                policy,
                new Provider("PROV-1", "Test Provider", false),
                LocalDate.of(2026, 4, 1),
                List.of(line));

        AdjudicationResult result =
                new ClaimsAdjudicationEngine().adjudicate(claim, line);

        assertEquals(Money.of("72.00"), result.allowedAmount());
    }

    @Test
    void appliesFlatCopayInsteadOfCoinsuranceAfterDeductible() {
        ClaimLine line = new ClaimLine(
                "LINE-COPAY",
                LocalDate.of(2026, 3, 1),
                "99213",
                Money.of("200.00"),
                true,
                true,
                Money.of("200.00"),
                Money.of("200.00"));

        Policy policy = new Policy(
                "POL-COPAY",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Money.of("1000.00"),
                new BigDecimal("0.20"),
                Set.of("99213"),
                Set.of(),
                Set.of(),
                null,
                null,
                Money.of("25.00"),
                null);

        Claim claim = new Claim(
                "CLAIM-COPAY",
                new Patient("PAT-1", "Test Patient"),
                policy,
                new Provider("PROV-1", "Test Provider", true),
                LocalDate.of(2026, 4, 1),
                List.of(line));

        AdjudicationResult result = new ClaimsAdjudicationEngine()
                .adjudicate(claim, line,
                        new Accumulator(
                                Money.of("1000.00"),
                                Money.zero()));

        assertEquals(Money.of("200.00"), result.allowedAmount());
        assertEquals(Money.of("25.00"), result.patientResponsibility());
        assertEquals(Money.of("175.00"), result.insurerPayment());
        assertFalse(result.isDenied());
    }

    @Test
    void appliesConfiguredHigherOutOfNetworkCoinsuranceRate() {
        ClaimLine line = new ClaimLine(
                "LINE-OON",
                LocalDate.of(2026, 3, 1),
                "99213",
                Money.of("200.00"),
                true,
                true,
                Money.of("200.00"),
                Money.of("200.00"));

        Policy policy = new Policy(
                "POL-OON",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Money.of("1000.00"),
                new BigDecimal("0.20"),
                Set.of("99213"),
                Set.of(),
                Set.of(),
                null,
                null,
                null,
                new BigDecimal("0.40"));

        Claim claim = new Claim(
                "CLAIM-OON",
                new Patient("PAT-1", "Test Patient"),
                policy,
                new Provider("PROV-1", "Test Provider", false),
                LocalDate.of(2026, 4, 1),
                List.of(line));

        AdjudicationResult result = new ClaimsAdjudicationEngine()
                .adjudicate(claim, line,
                        new Accumulator(
                                Money.of("1000.00"),
                                Money.zero()));

        assertEquals(Money.of("120.00"), result.allowedAmount());
        assertEquals(Money.of("48.00"), result.patientResponsibility());
        assertEquals(Money.of("72.00"), result.insurerPayment());
        assertFalse(result.isDenied());
    }
    @Test
    void calculatesDeductibleAndCoinsuranceForWorkedExample() {
        ClaimLine line = new ClaimLine(
                "LINE-1",
                LocalDate.of(2026, 3, 1),
                "99213",
                Money.of("1200.00"),
                true,
                true,
                Money.of("1200.00"),
                Money.of("1200.00"));

        Policy policy = policy(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Set.of("99213"));

        Claim claim = new Claim(
                "CLAIM-1",
                new Patient("PAT-1", "Test Patient"),
                policy,
                new Provider("PROV-1", "Test Provider", true),
                LocalDate.of(2026, 4, 1),
                List.of(line));

        Accumulator accumulator = new Accumulator(
                Money.of("200.00"),
                Money.of("200.00"));

        AdjudicationResult result = new ClaimsAdjudicationEngine()
                .adjudicate(claim, line, accumulator);

        assertEquals(Money.of("1200.00"), result.allowedAmount());
        assertEquals(Money.of("320.00"), result.insurerPayment());
        assertEquals(Money.of("880.00"), result.patientResponsibility());
        assertFalse(result.isDenied());
    }

    @Test
    void usesFamilyDeductibleWhenFamilyLimitIsMet() {
        ClaimLine line = new ClaimLine(
                "LINE-FAMILY",
                LocalDate.of(2026, 3, 1),
                "99213",
                Money.of("1200.00"),
                true,
                true,
                Money.of("1200.00"),
                Money.of("1200.00"));

        Policy policy = new Policy(
                "POL-FAMILY",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Money.of("1000.00"),
                new BigDecimal("0.20"),
                Set.of("99213"),
                Set.of(),
                Set.of(),
                null,
                Money.of("2000.00"));

        Claim claim = new Claim(
                "CLAIM-FAMILY",
                new Patient("PAT-FAMILY", "Test Patient", "FAMILY-1"),
                policy,
                new Provider("PROV-FAMILY", "Test Provider", true),
                LocalDate.of(2026, 4, 1),
                List.of(line));

        Accumulator accumulator = new Accumulator(
                Money.of("200.00"),
                Money.of("200.00"),
                Money.of("2000.00"));

        AdjudicationResult result = new ClaimsAdjudicationEngine()
                .adjudicate(claim, line, accumulator);

        assertEquals(Money.of("240.00"), result.patientResponsibility());
        assertEquals(Money.of("960.00"), result.insurerPayment());
        assertFalse(result.isDenied());
    }
    @Test
    void capsPatientResponsibilityAtRemainingOutOfPocketMaximum() {
        ClaimLine line = new ClaimLine(
                "LINE-1",
                LocalDate.of(2026, 3, 1),
                "99213",
                Money.of("1200.00"),
                true,
                true,
                Money.of("1200.00"),
                Money.of("1200.00"));

        Policy policy = new Policy(
                "POL-1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Money.of("1000.00"),
                new BigDecimal("0.20"),
                Set.of("99213"),
                Set.of(),
                Set.of(),
                Money.of("500.00"));

        Claim claim = new Claim(
                "CLAIM-1",
                new Patient("PAT-1", "Test Patient"),
                policy,
                new Provider("PROV-1", "Test Provider", true),
                LocalDate.of(2026, 4, 1),
                List.of(line));

        Accumulator accumulator = new Accumulator(
                Money.of("200.00"),
                Money.of("200.00"));

        AdjudicationResult result = new ClaimsAdjudicationEngine()
                .adjudicate(claim, line, accumulator);

        assertEquals(Money.of("1200.00"), result.allowedAmount());
        assertEquals(Money.of("900.00"), result.insurerPayment());
        assertEquals(Money.of("300.00"), result.patientResponsibility());
        assertFalse(result.isDenied());
    }

    @Test
    void processesClaimLinesInOrderAndUpdatesAccumulators() {
        ClaimLine firstLine = new ClaimLine(
                "LINE-1",
                LocalDate.of(2026, 3, 1),
                "99213",
                Money.of("600.00"));

        ClaimLine secondLine = new ClaimLine(
                "LINE-2",
                LocalDate.of(2026, 3, 2),
                "99214",
                Money.of("600.00"));

        Policy policy = policy(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Set.of("99213", "99214"));

        Claim claim = new Claim(
                "CLAIM-1",
                new Patient("PAT-1", "Test Patient"),
                policy,
                new Provider("PROV-1", "Test Provider"),
                LocalDate.of(2026, 4, 1),
                List.of(firstLine, secondLine));

        List<AdjudicationResult> results = new ClaimsAdjudicationEngine()
                .adjudicate(claim, List.of(), Accumulator.zero());

        assertEquals(2, results.size());

        assertEquals(firstLine, results.get(0).claimLine());
        assertEquals(Money.of("600.00"), results.get(0).patientResponsibility());
        assertEquals(Money.of("0.00"), results.get(0).insurerPayment());

        assertEquals(secondLine, results.get(1).claimLine());
        assertEquals(Money.of("440.00"), results.get(1).patientResponsibility());
        assertEquals(Money.of("160.00"), results.get(1).insurerPayment());
    }

    @Test
    void updatesFamilyDeductibleAcrossClaimLines() {
        ClaimLine firstLine = new ClaimLine(
                "LINE-FAMILY-1",
                LocalDate.of(2026, 3, 1),
                "99213",
                Money.of("600.00"));
        ClaimLine secondLine = new ClaimLine(
                "LINE-FAMILY-2",
                LocalDate.of(2026, 3, 2),
                "99214",
                Money.of("600.00"));

        Policy policy = new Policy(
                "POL-FAMILY-BATCH",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Money.of("1000.00"),
                new BigDecimal("0.20"),
                Set.of("99213", "99214"),
                Set.of(),
                Set.of(),
                null,
                Money.of("1000.00"));

        Claim claim = new Claim(
                "CLAIM-FAMILY-BATCH",
                new Patient("PAT-FAMILY-BATCH", "Test Patient", "FAMILY-1"),
                policy,
                new Provider("PROV-FAMILY-BATCH", "Test Provider", true),
                LocalDate.of(2026, 4, 1),
                List.of(firstLine, secondLine));

        Accumulator initialAccumulator = new Accumulator(
                Money.of("0.00"),
                Money.of("0.00"),
                Money.of("500.00"));

        List<AdjudicationResult> results = new ClaimsAdjudicationEngine()
                .adjudicate(claim, List.of(), initialAccumulator);

        assertEquals(2, results.size());
        assertEquals(Money.of("520.00"), results.get(0).patientResponsibility());
        assertEquals(Money.of("80.00"), results.get(0).insurerPayment());
        assertEquals(Money.of("120.00"), results.get(1).patientResponsibility());
        assertEquals(Money.of("480.00"), results.get(1).insurerPayment());
    }

    @Test
    void resetsOutOfPocketMaximumWhenServiceYearChanges() {
        ClaimLine decemberLine = new ClaimLine(
                "LINE-1",
                LocalDate.of(2026, 12, 31),
                "99213",
                Money.of("600.00"));

        ClaimLine januaryLine = new ClaimLine(
                "LINE-2",
                LocalDate.of(2027, 1, 1),
                "99214",
                Money.of("600.00"));

        Policy policy = new Policy(
                "POL-1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 12, 31),
                Money.of("1000.00"),
                new BigDecimal("0.20"),
                Set.of("99213", "99214"),
                Set.of(),
                Set.of(),
                Money.of("1000.00"));

        Claim claim = new Claim(
                "CLAIM-1",
                new Patient("PAT-1", "Test Patient"),
                policy,
                new Provider("PROV-1", "Test Provider", true),
                LocalDate.of(2027, 1, 2),
                List.of(decemberLine, januaryLine));

        Accumulator startingAccumulator = new Accumulator(
                Money.of("0.00"),
                Money.of("900.00"));

        List<AdjudicationResult> results = new ClaimsAdjudicationEngine(90)
                .adjudicate(claim, List.of(), startingAccumulator);

        assertEquals(2, results.size());
        assertEquals(Money.of("100.00"),
                results.get(0).patientResponsibility());
        assertEquals(Money.of("600.00"),
                results.get(1).patientResponsibility());
    }
    @Test
    void resetsDeductibleWhenServiceYearChanges() {
        ClaimLine decemberLine = new ClaimLine(
                "LINE-1",
                LocalDate.of(2026, 12, 31),
                "99213",
                Money.of("600.00"));

        ClaimLine januaryLine = new ClaimLine(
                "LINE-2",
                LocalDate.of(2027, 1, 1),
                "99214",
                Money.of("600.00"));

        Policy policy = policy(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 12, 31),
                Set.of("99213", "99214"));

        Claim claim = new Claim(
                "CLAIM-1",
                new Patient("PAT-1", "Test Patient"),
                policy,
                new Provider("PROV-1", "Test Provider"),
                LocalDate.of(2027, 1, 2),
                List.of(decemberLine, januaryLine));

        List<AdjudicationResult> results = new ClaimsAdjudicationEngine(90)
                .adjudicate(claim, List.of(), Accumulator.zero());

        assertEquals(2, results.size());

        assertEquals(decemberLine, results.get(0).claimLine());
        assertFalse(results.get(0).isDenied());
        assertEquals(Money.of("600.00"),
                results.get(0).patientResponsibility());
        assertEquals(Money.of("0.00"),
                results.get(0).insurerPayment());

        assertEquals(januaryLine, results.get(1).claimLine());
        assertFalse(results.get(1).isDenied());
        assertEquals(Money.of("600.00"),
                results.get(1).patientResponsibility());
        assertEquals(Money.of("0.00"),
                results.get(1).insurerPayment());
    }

    @Test
    void deniesDuplicateLinesWithinTheSameClaim() {
        ClaimLine firstLine = new ClaimLine(
                "LINE-1",
                LocalDate.of(2026, 3, 1),
                "99213",
                Money.of("100.00"));

        ClaimLine duplicateLine = new ClaimLine(
                "LINE-2",
                LocalDate.of(2026, 3, 1),
                "99213",
                Money.of("100.00"));

        Policy policy = policy(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Set.of("99213"));

        Claim claim = new Claim(
                "CLAIM-1",
                new Patient("PAT-1", "Test Patient"),
                policy,
                new Provider("PROV-1", "Test Provider"),
                LocalDate.of(2026, 4, 1),
                List.of(firstLine, duplicateLine));

        List<AdjudicationResult> results = new ClaimsAdjudicationEngine()
                .adjudicate(claim, List.of(), Accumulator.zero());

        assertEquals(2, results.size());
        assertFalse(results.get(0).isDenied());
        assertTrue(results.get(1).isDenied());
        assertEquals(DenialReason.DUPLICATE_CLAIM,
                results.get(1).denialReason());
    }
    @Test
    void appliesSharedFamilyDeductibleMetByAnotherFamilyMember() {
        Policy familyPolicy = new Policy(
                "POL-FAMILY",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Money.of("1000.00"),
                new BigDecimal("0.20"),
                Set.of("99213"),
                Set.of(),
                Set.of(),
                null,
                Money.of("1000.00"));

        ClaimLine secondMemberLine = new ClaimLine(
                "LINE-MEMBER-2",
                LocalDate.of(2026, 3, 10),
                "99213",
                Money.of("600.00"));

        Claim secondMemberClaim = new Claim(
                "CLAIM-MEMBER-2",
                new Patient("PAT-2", "Second Family Member", "FAMILY-1"),
                familyPolicy,
                new Provider("PROV-1", "Test Provider"),
                LocalDate.of(2026, 4, 1),
                List.of(secondMemberLine));

        Accumulator sharedAccumulator = new Accumulator(
                Money.zero(),
                Money.zero(),
                Money.of("1000.00"));

        AdjudicationResult result = new ClaimsAdjudicationEngine()
                .adjudicate(
                        secondMemberClaim,
                        secondMemberLine,
                        sharedAccumulator);

        assertFalse(result.isDenied());
        assertEquals(Money.of("120.00"), result.patientResponsibility());
        assertEquals(Money.of("480.00"), result.insurerPayment());
    }
    @Test
    void retroactivePolicyTerminationMakesPreviouslyPayableServiceIneligible() {
        ClaimLine line = new ClaimLine(
                "LINE-RETRO",
                LocalDate.of(2026, 3, 1),
                "99213",
                Money.of("2000.00"));

        Policy originalPolicy = policy(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Set.of("99213"));

        Claim originalClaim = claim(originalPolicy, line);
        ClaimsAdjudicationEngine engine = new ClaimsAdjudicationEngine();

        AdjudicationResult originallyPaid = engine.adjudicate(
                originalClaim, line, Accumulator.zero());

        assertFalse(originallyPaid.isDenied());
        assertFalse(originallyPaid.insurerPayment().equals(Money.zero()));

        Policy retroactivelyTerminatedPolicy = policy(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 28),
                Set.of("99213"));

        Claim revisedClaim = claim(retroactivelyTerminatedPolicy, line);
        AdjudicationResult revisedResult = engine.adjudicate(
                revisedClaim, line, Accumulator.zero());

        assertTrue(revisedResult.isDenied());
        assertEquals(
                DenialReason.INELIGIBLE_MEMBER,
                revisedResult.denialReason());
        assertEquals(Money.zero(), revisedResult.insurerPayment());
        assertEquals(Money.zero(), revisedResult.patientResponsibility());
    }
}
