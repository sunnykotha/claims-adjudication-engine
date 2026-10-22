package com.claims.adjudication;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a submitted insurance claim containing one or more service lines.
 */
public record Claim(
        String claimId,
        Patient patient,
        Policy policy,
        Provider provider,
        LocalDate submissionDate,
        List<ClaimLine> lines) {

    public Claim {
        Objects.requireNonNull(claimId, "Claim ID cannot be null");
        Objects.requireNonNull(patient, "Patient cannot be null");
        Objects.requireNonNull(policy, "Policy cannot be null");
        Objects.requireNonNull(provider, "Provider cannot be null");
        Objects.requireNonNull(submissionDate, "Submission date cannot be null");
        Objects.requireNonNull(lines, "Claim lines cannot be null");

        if (claimId.isBlank()) {
            throw new IllegalArgumentException("Claim ID cannot be blank");
        }

        claimId = claimId.trim();
        lines = List.copyOf(lines);

        if (lines.isEmpty()) {
            throw new IllegalArgumentException(
                    "A claim must contain at least one line");
        }

        Set<String> lineIds = new HashSet<>();

        for (ClaimLine line : lines) {
            Objects.requireNonNull(line, "Claim line cannot be null");

            if (!lineIds.add(line.lineId())) {
                throw new IllegalArgumentException(
                        "Duplicate line ID within claim: " + line.lineId());
            }
        }
    }
}