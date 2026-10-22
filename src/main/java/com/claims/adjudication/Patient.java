package com.claims.adjudication;

import java.util.Objects;

/**
 * Represents a patient who may have healthcare claims.
 */
public record Patient(String patientId, String name, String familyId) {

    public Patient(String patientId, String name) {
        this(patientId, name, null);
    }

    public Patient {
        Objects.requireNonNull(patientId, "Patient ID cannot be null");
        Objects.requireNonNull(name, "Patient name cannot be null");

        if (patientId.isBlank()) {
            throw new IllegalArgumentException("Patient ID cannot be blank");
        }

        if (name.isBlank()) {
            throw new IllegalArgumentException("Patient name cannot be blank");
        }

        patientId = patientId.trim();
        name = name.trim();

        if (familyId != null) {
            if (familyId.isBlank()) {
                throw new IllegalArgumentException(
                        "Family ID cannot be blank when provided");
            }
            familyId = familyId.trim();
        }
    }
}
