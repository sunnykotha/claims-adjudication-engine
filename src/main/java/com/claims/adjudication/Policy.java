package com.claims.adjudication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents an insurance plan and its basic coverage rules.
 */
public record Policy(
        String policyId,
        LocalDate effectiveDate,
        LocalDate terminationDate,
        Money annualDeductible,
        BigDecimal coinsuranceRate,
        Set<String> coveredProcedureCodes,
        Set<String> referralRequiredProcedureCodes,
        Set<String> priorAuthorizationRequiredProcedureCodes,
        Money outOfPocketMaximum,
        Money familyDeductible,
        Money copayAmount,
        BigDecimal outOfNetworkCoinsuranceRate) {

    public Policy(
            String policyId,
            LocalDate effectiveDate,
            LocalDate terminationDate,
            Money annualDeductible,
            BigDecimal coinsuranceRate,
            Set<String> coveredProcedureCodes) {
        this(policyId, effectiveDate, terminationDate, annualDeductible,
                coinsuranceRate, coveredProcedureCodes, Set.of(), Set.of(),
                null, null, null, null);
    }

    public Policy(
            String policyId,
            LocalDate effectiveDate,
            LocalDate terminationDate,
            Money annualDeductible,
            BigDecimal coinsuranceRate,
            Set<String> coveredProcedureCodes,
            Set<String> referralRequiredProcedureCodes,
            Set<String> priorAuthorizationRequiredProcedureCodes) {
        this(policyId, effectiveDate, terminationDate, annualDeductible,
                coinsuranceRate, coveredProcedureCodes,
                referralRequiredProcedureCodes,
                priorAuthorizationRequiredProcedureCodes,
                null, null, null, null);
    }

    public Policy(
            String policyId,
            LocalDate effectiveDate,
            LocalDate terminationDate,
            Money annualDeductible,
            BigDecimal coinsuranceRate,
            Set<String> coveredProcedureCodes,
            Set<String> referralRequiredProcedureCodes,
            Set<String> priorAuthorizationRequiredProcedureCodes,
            Money outOfPocketMaximum) {
        this(policyId, effectiveDate, terminationDate, annualDeductible,
                coinsuranceRate, coveredProcedureCodes,
                referralRequiredProcedureCodes,
                priorAuthorizationRequiredProcedureCodes,
                outOfPocketMaximum, null, null, null);
    }

    /**
     * Preserves compatibility with the previous ten-argument constructor.
     */
    public Policy(
            String policyId,
            LocalDate effectiveDate,
            LocalDate terminationDate,
            Money annualDeductible,
            BigDecimal coinsuranceRate,
            Set<String> coveredProcedureCodes,
            Set<String> referralRequiredProcedureCodes,
            Set<String> priorAuthorizationRequiredProcedureCodes,
            Money outOfPocketMaximum,
            Money familyDeductible) {
        this(policyId, effectiveDate, terminationDate, annualDeductible,
                coinsuranceRate, coveredProcedureCodes,
                referralRequiredProcedureCodes,
                priorAuthorizationRequiredProcedureCodes,
                outOfPocketMaximum, familyDeductible, null, null);
    }

    public Policy {
        Objects.requireNonNull(policyId, "Policy ID cannot be null");
        Objects.requireNonNull(effectiveDate, "Effective date cannot be null");
        Objects.requireNonNull(terminationDate, "Termination date cannot be null");
        Objects.requireNonNull(annualDeductible, "Annual deductible cannot be null");
        Objects.requireNonNull(coinsuranceRate, "Coinsurance rate cannot be null");
        Objects.requireNonNull(coveredProcedureCodes,
                "Covered procedure codes cannot be null");
        Objects.requireNonNull(referralRequiredProcedureCodes,
                "Referral-required procedure codes cannot be null");
        Objects.requireNonNull(priorAuthorizationRequiredProcedureCodes,
                "Prior-authorization-required procedure codes cannot be null");

        if (policyId.isBlank()) {
            throw new IllegalArgumentException("Policy ID cannot be blank");
        }
        policyId = policyId.trim();

        if (terminationDate.isBefore(effectiveDate)) {
            throw new IllegalArgumentException(
                    "Termination date cannot be before effective date");
        }
        if (annualDeductible.isNegative()) {
            throw new IllegalArgumentException(
                    "Annual deductible cannot be negative");
        }
        if (outOfPocketMaximum != null && outOfPocketMaximum.isNegative()) {
            throw new IllegalArgumentException(
                    "Out-of-pocket maximum cannot be negative");
        }
        if (familyDeductible != null && familyDeductible.isNegative()) {
            throw new IllegalArgumentException(
                    "Family deductible cannot be negative");
        }
        if (copayAmount != null && copayAmount.isNegative()) {
            throw new IllegalArgumentException(
                    "Copay amount cannot be negative");
        }
        validateRate(coinsuranceRate, "Coinsurance rate");
        if (outOfNetworkCoinsuranceRate != null) {
            validateRate(outOfNetworkCoinsuranceRate,
                    "Out-of-network coinsurance rate");
            if (outOfNetworkCoinsuranceRate.compareTo(coinsuranceRate) <= 0) {
                throw new IllegalArgumentException(
                        "Out-of-network coinsurance rate must be greater "
                                + "than the in-network coinsurance rate");
            }
        }

        coveredProcedureCodes = normalizeCodes(coveredProcedureCodes);
        referralRequiredProcedureCodes =
                normalizeCodes(referralRequiredProcedureCodes);
        priorAuthorizationRequiredProcedureCodes =
                normalizeCodes(priorAuthorizationRequiredProcedureCodes);
    }

    private static void validateRate(BigDecimal rate, String fieldName) {
        if (rate.compareTo(BigDecimal.ZERO) < 0
                || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be between 0 and 1");
        }
    }

    private static Set<String> normalizeCodes(Set<String> codes) {
        Set<String> normalizedCodes = new TreeSet<>();
        for (String code : codes) {
            Objects.requireNonNull(code, "Procedure code cannot be null");
            if (code.isBlank()) {
                throw new IllegalArgumentException(
                        "Procedure code cannot be blank");
            }
            normalizedCodes.add(code.trim().toUpperCase(Locale.ROOT));
        }
        return Collections.unmodifiableSet(normalizedCodes);
    }

    public boolean coversServiceDate(LocalDate serviceDate) {
        Objects.requireNonNull(serviceDate, "Service date cannot be null");
        return !serviceDate.isBefore(effectiveDate)
                && !serviceDate.isAfter(terminationDate);
    }

    public boolean coversProcedure(String procedureCode) {
        Objects.requireNonNull(procedureCode, "Procedure code cannot be null");
        return coveredProcedureCodes.contains(normalizeCode(procedureCode));
    }

    public boolean requiresReferral(String procedureCode) {
        return referralRequiredProcedureCodes.contains(normalizeCode(procedureCode));
    }

    public boolean requiresPriorAuthorization(String procedureCode) {
        return priorAuthorizationRequiredProcedureCodes.contains(
                normalizeCode(procedureCode));
    }

    private static String normalizeCode(String procedureCode) {
        Objects.requireNonNull(procedureCode, "Procedure code cannot be null");
        return procedureCode.trim().toUpperCase(Locale.ROOT);
    }
}
