package com.claims.adjudication;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/**
 * Represents one healthcare service billed on a claim.
 */
public record ClaimLine(
        String lineId,
        LocalDate serviceDate,
        String procedureCode,
        Money billedAmount,
        boolean referralObtained,
        boolean priorAuthorizationObtained,
        Money contractedRate,
        Money usualAndCustomaryAmount) {

    public ClaimLine(
            String lineId,
            LocalDate serviceDate,
            String procedureCode,
            Money billedAmount) {
        this(lineId, serviceDate, procedureCode, billedAmount, true, true,
                billedAmount, billedAmount);
    }

    public ClaimLine(
            String lineId,
            LocalDate serviceDate,
            String procedureCode,
            Money billedAmount,
            boolean referralObtained,
            boolean priorAuthorizationObtained) {
        this(lineId, serviceDate, procedureCode, billedAmount,
                referralObtained, priorAuthorizationObtained,
                billedAmount, billedAmount);
    }

    public ClaimLine {
        Objects.requireNonNull(lineId, "Line ID cannot be null");
        Objects.requireNonNull(serviceDate, "Service date cannot be null");
        Objects.requireNonNull(procedureCode, "Procedure code cannot be null");
        Objects.requireNonNull(billedAmount, "Billed amount cannot be null");
        Objects.requireNonNull(contractedRate, "Contracted rate cannot be null");
        Objects.requireNonNull(
                usualAndCustomaryAmount,
                "Usual-and-customary amount cannot be null");

        if (lineId.isBlank()) {
            throw new IllegalArgumentException("Line ID cannot be blank");
        }

        if (procedureCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Procedure code cannot be blank");
        }

        if (billedAmount.isNegative()
                || contractedRate.isNegative()
                || usualAndCustomaryAmount.isNegative()) {
            throw new IllegalArgumentException("Amounts cannot be negative");
        }

        lineId = lineId.trim();
        procedureCode = procedureCode.trim().toUpperCase(Locale.ROOT);
    }
}