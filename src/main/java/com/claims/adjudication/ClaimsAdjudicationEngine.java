package com.claims.adjudication;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClaimsAdjudicationEngine {

    private final int timelyFilingDays;

    public ClaimsAdjudicationEngine() {
        this(90);
    }

    public ClaimsAdjudicationEngine(int timelyFilingDays) {
        if (timelyFilingDays < 0) {
            throw new IllegalArgumentException(
                    "Timely filing days must not be negative");
        }
        this.timelyFilingDays = timelyFilingDays;
    }

    public AdjudicationResult adjudicate(Claim claim, ClaimLine line) {
        return adjudicate(claim, line, List.of(), Accumulator.zero());
    }

    public AdjudicationResult adjudicate(
            Claim claim,
            ClaimLine line,
            List<Claim> previouslySubmittedClaims) {
        return adjudicate(
                claim, line, previouslySubmittedClaims, Accumulator.zero());
    }

    public AdjudicationResult adjudicate(
            Claim claim,
            ClaimLine line,
            Accumulator accumulator) {
        return adjudicate(claim, line, List.of(), accumulator);
    }

    public AdjudicationResult adjudicate(
            Claim claim,
            ClaimLine line,
            List<Claim> previouslySubmittedClaims,
            Accumulator accumulator) {

        Objects.requireNonNull(claim, "claim must not be null");
        Objects.requireNonNull(line, "line must not be null");
        Objects.requireNonNull(
                previouslySubmittedClaims,
                "previouslySubmittedClaims must not be null");
        Objects.requireNonNull(accumulator, "accumulator must not be null");

        if (!claim.lines().contains(line)) {
            throw new IllegalArgumentException(
                    "Claim line must belong to the supplied claim");
        }

        if (!claim.policy().coversServiceDate(line.serviceDate())) {
            return denied(line, DenialReason.INELIGIBLE_MEMBER,
                    "Service date is outside the policy coverage dates");
        }

        if (!claim.policy().coversProcedure(line.procedureCode())) {
            return denied(line, DenialReason.SERVICE_NOT_COVERED,
                    "Procedure code is not covered by the policy");
        }

        if (claim.policy().requiresReferral(line.procedureCode())
                && !line.referralObtained()) {
            return denied(line, DenialReason.REFERRAL_REQUIRED,
                    "Required referral was not obtained");
        }

        if (claim.policy().requiresPriorAuthorization(line.procedureCode())
                && !line.priorAuthorizationObtained()) {
            return denied(line, DenialReason.PRIOR_AUTHORIZATION_REQUIRED,
                    "Required prior authorization was not obtained");
        }

        for (Claim previousClaim : previouslySubmittedClaims) {
            Objects.requireNonNull(
                    previousClaim,
                    "Previously submitted claims must not contain null");

            for (ClaimLine previousLine : previousClaim.lines()) {
                boolean duplicate =
                        previousClaim.patient().patientId()
                                .equals(claim.patient().patientId())
                        && previousClaim.provider().providerId()
                                .equals(claim.provider().providerId())
                        && previousLine.procedureCode()
                                .equals(line.procedureCode())
                        && previousLine.serviceDate()
                                .equals(line.serviceDate());

                if (duplicate) {
                    return denied(line, DenialReason.DUPLICATE_CLAIM,
                            "A claim with the same patient, provider, procedure code, "
                                    + "and service date was previously submitted");
                }
            }
        }

        long daysAfterService = ChronoUnit.DAYS.between(
                line.serviceDate(), claim.submissionDate());

        if (daysAfterService > timelyFilingDays) {
            return denied(line, DenialReason.TIMELY_FILING_EXCEEDED,
                    "Claim was submitted more than "
                            + timelyFilingDays
                            + " days after the service date");
        }

        Money allowedAmount;
        if (claim.provider().inNetwork()) {
            allowedAmount = line.contractedRate();
        } else {
            allowedAmount = new Money(
                    line.usualAndCustomaryAmount().amount()
                            .multiply(new BigDecimal("0.60")));
        }

        Money deductibleRemaining = claim.policy().annualDeductible()
                .subtract(accumulator.deductibleMet());
        if (deductibleRemaining.isNegative()) {
            deductibleRemaining = Money.zero();
        }

        if (claim.policy().familyDeductible() != null) {
            Money familyDeductibleRemaining = claim.policy().familyDeductible()
                    .subtract(accumulator.familyDeductibleMet());

            if (familyDeductibleRemaining.isNegative()) {
                familyDeductibleRemaining = Money.zero();
            }

            if (familyDeductibleRemaining.compareTo(deductibleRemaining) < 0) {
                deductibleRemaining = familyDeductibleRemaining;
            }
        }

        Money deductibleApplied = allowedAmount.compareTo(deductibleRemaining) < 0
                ? allowedAmount
                : deductibleRemaining;

        Money amountAfterDeductible = allowedAmount.subtract(deductibleApplied);
        Money coinsurance = new Money(
                amountAfterDeductible.amount()
                        .multiply(claim.policy().coinsuranceRate()));

        Money patientResponsibility = deductibleApplied.add(coinsurance);

        if (claim.policy().outOfPocketMaximum() != null) {
            Money outOfPocketRemaining = claim.policy().outOfPocketMaximum()
                    .subtract(accumulator.outOfPocketMet());

            if (outOfPocketRemaining.isNegative()) {
                outOfPocketRemaining = Money.zero();
            }

            if (patientResponsibility.compareTo(outOfPocketRemaining) > 0) {
                patientResponsibility = outOfPocketRemaining;
            }
        }

        Money insurerPayment = allowedAmount.subtract(patientResponsibility);

        return new AdjudicationResult(
                line,
                allowedAmount,
                insurerPayment,
                patientResponsibility,
                null,
                List.of(
                        "Eligibility check passed",
                        "Covered-service check passed",
                        "Referral check passed",
                        "Prior-authorization check passed",
                        "Duplicate check passed",
                        "Timely-filing check passed",
                        "Network allowed-amount check passed",
                        "Deductible and coinsurance calculated"));
    }

    public List<AdjudicationResult> adjudicate(
            Claim claim,
            List<Claim> previouslySubmittedClaims,
            Accumulator initialAccumulator) {

        Objects.requireNonNull(claim, "claim must not be null");
        Objects.requireNonNull(
                previouslySubmittedClaims,
                "previouslySubmittedClaims must not be null");
        Objects.requireNonNull(
                initialAccumulator,
                "initialAccumulator must not be null");

        List<AdjudicationResult> results = new ArrayList<>();
        Accumulator currentAccumulator = initialAccumulator;
        Integer previousServiceYear = null;
        List<Claim> claimsToCheck = new ArrayList<>(previouslySubmittedClaims);
        List<ClaimLine> processedLines = new ArrayList<>();

        for (ClaimLine line : claim.lines()) {
            int serviceYear = line.serviceDate().getYear();

            if (previousServiceYear != null
                    && previousServiceYear != serviceYear) {
                currentAccumulator = new Accumulator(
                        Money.zero(),
                        currentAccumulator.outOfPocketMet(),
                        Money.zero());
            }

            previousServiceYear = serviceYear;

            AdjudicationResult result = adjudicate(
                    claim, line, claimsToCheck, currentAccumulator);
            results.add(result);

            processedLines.add(line);
            claimsToCheck.add(new Claim(
                    claim.claimId(),
                    claim.patient(),
                    claim.policy(),
                    claim.provider(),
                    claim.submissionDate(),
                    List.of(line)));

            if (!result.isDenied()) {
                Money deductibleRemaining = claim.policy().annualDeductible()
                        .subtract(currentAccumulator.deductibleMet());

                if (deductibleRemaining.isNegative()) {
                    deductibleRemaining = Money.zero();
                }

                if (claim.policy().familyDeductible() != null) {
                    Money familyDeductibleRemaining =
                            claim.policy().familyDeductible()
                                    .subtract(currentAccumulator.familyDeductibleMet());

                    if (familyDeductibleRemaining.isNegative()) {
                        familyDeductibleRemaining = Money.zero();
                    }

                    if (familyDeductibleRemaining.compareTo(deductibleRemaining) < 0) {
                        deductibleRemaining = familyDeductibleRemaining;
                    }
                }

                Money deductibleApplied =
                        result.allowedAmount().compareTo(deductibleRemaining) < 0
                                ? result.allowedAmount()
                                : deductibleRemaining;

                currentAccumulator = new Accumulator(
                        currentAccumulator.deductibleMet().add(deductibleApplied),
                        currentAccumulator.outOfPocketMet()
                                .add(result.patientResponsibility()),
                        currentAccumulator.familyDeductibleMet().add(deductibleApplied));
            }
        }

        return List.copyOf(results);
    }
    private AdjudicationResult denied(
            ClaimLine line,
            DenialReason reason,
            String explanation) {
        return new AdjudicationResult(
                line,
                Money.zero(),
                Money.zero(),
                Money.zero(),
                reason,
                List.of(explanation));
    }
}