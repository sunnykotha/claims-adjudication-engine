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

        Money patientCostShare;
        if (amountAfterDeductible.isZero()) {
            patientCostShare = Money.zero();
        } else if (claim.policy().copayAmount() != null) {
            patientCostShare = claim.policy().copayAmount()
                    .compareTo(amountAfterDeductible) < 0
                    ? claim.policy().copayAmount()
                    : amountAfterDeductible;
        } else {
            BigDecimal applicableCoinsuranceRate =
                    !claim.provider().inNetwork()
                            && claim.policy().outOfNetworkCoinsuranceRate() != null
                            ? claim.policy().outOfNetworkCoinsuranceRate()
                            : claim.policy().coinsuranceRate();

            patientCostShare = new Money(
                    amountAfterDeductible.amount()
                            .multiply(applicableCoinsuranceRate));
        }

        Money patientResponsibility = deductibleApplied.add(patientCostShare);

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
        return adjudicateWithAccumulator(
                claim, previouslySubmittedClaims, initialAccumulator).results();
    }
    public BatchAdjudicationResult adjudicateWithAccumulator(
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

        boolean hasExistingAccumulatorAmounts =
                !initialAccumulator.deductibleMet().isZero()
                || !initialAccumulator.outOfPocketMet().isZero()
                || !initialAccumulator.familyDeductibleMet().isZero();

        if (initialAccumulator.serviceYear() == null
                && hasExistingAccumulatorAmounts) {
            throw new IllegalArgumentException(
                    "A populated accumulator must include serviceYear "
                            + "for batch adjudication");
        }

        List<AdjudicationResult> results = new ArrayList<>();
        Accumulator currentAccumulator = initialAccumulator;
        Integer previousServiceYear = initialAccumulator.serviceYear();
        List<Claim> claimsToCheck = new ArrayList<>(previouslySubmittedClaims);
        List<ClaimLine> processedLines = new ArrayList<>();

        for (ClaimLine line : claim.lines()) {
            int serviceYear = line.serviceDate().getYear();

            if (previousServiceYear != null
                    && previousServiceYear != serviceYear) {
                currentAccumulator = new Accumulator(
                        Money.zero(),
                        Money.zero(),
                        Money.zero(),
                        serviceYear);
            } else if (currentAccumulator.serviceYear() != null
                    && currentAccumulator.serviceYear() != serviceYear) {
                currentAccumulator = new Accumulator(
                        Money.zero(),
                        Money.zero(),
                        Money.zero(),
                        serviceYear);
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

                Money deductibleCredited =
                        deductibleApplied.compareTo(result.patientResponsibility()) < 0
                                ? deductibleApplied
                                : result.patientResponsibility();
                currentAccumulator = new Accumulator(
                        currentAccumulator.deductibleMet().add(deductibleCredited),
                        currentAccumulator.outOfPocketMet()
                                .add(result.patientResponsibility()),
                        currentAccumulator.familyDeductibleMet().add(deductibleCredited),
                        serviceYear);
            }
        }

        return new BatchAdjudicationResult(List.copyOf(results), currentAccumulator);
    }
    public Money calculateRetroactiveRecovery(
            AdjudicationResult previouslyPaidResult,
            AdjudicationResult revisedResult) {

        Objects.requireNonNull(
                previouslyPaidResult,
                "previouslyPaidResult must not be null");
        Objects.requireNonNull(
                revisedResult,
                "revisedResult must not be null");

        ClaimLine previousLine = previouslyPaidResult.claimLine();
        ClaimLine revisedLine = revisedResult.claimLine();

        if (!previousLine.lineId().equals(revisedLine.lineId())
                || !previousLine.serviceDate().equals(revisedLine.serviceDate())
                || !previousLine.procedureCode().equals(revisedLine.procedureCode())) {
            throw new IllegalArgumentException(
                    "Both results must refer to the same claim line");
        }

        if (previouslyPaidResult.isDenied()
                || previouslyPaidResult.insurerPayment().isZero()) {
            return Money.zero();
        }

        if (revisedResult.denialReason() == DenialReason.INELIGIBLE_MEMBER
                || revisedResult.denialReason() == DenialReason.POLICY_TERMINATED) {
            return previouslyPaidResult.insurerPayment();
        }

        return Money.zero();
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
