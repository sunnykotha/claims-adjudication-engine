package com.claims.adjudication;

import java.util.List;
import java.util.Objects;

public record AdjudicationResult(
        ClaimLine claimLine,
        Money allowedAmount,
        Money insurerPayment,
        Money patientResponsibility,
        DenialReason denialReason,
        List<String> ruleExplanations) {

    public AdjudicationResult {
        Objects.requireNonNull(claimLine, "claimLine must not be null");
        Objects.requireNonNull(allowedAmount, "allowedAmount must not be null");
        Objects.requireNonNull(insurerPayment, "insurerPayment must not be null");
        Objects.requireNonNull(patientResponsibility,
                "patientResponsibility must not be null");
        Objects.requireNonNull(ruleExplanations,
                "ruleExplanations must not be null");

        if (allowedAmount.isNegative()
                || insurerPayment.isNegative()
                || patientResponsibility.isNegative()) {
            throw new IllegalArgumentException(
                    "Financial amounts must not be negative");
        }

        if (ruleExplanations.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Rule explanations must not contain null values");
        }

        ruleExplanations = List.copyOf(ruleExplanations);

        if (denialReason != null
                && (!insurerPayment.isZero()
                || !patientResponsibility.isZero())) {
            throw new IllegalArgumentException(
                    "Denied results must not assign payment or patient responsibility");
        }
    }

    public boolean isDenied() {
        return denialReason != null;
    }
}
