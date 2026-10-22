package com.claims.adjudication;

import java.util.Objects;

/**
 * Represents a healthcare provider submitting claims.
 */
public record Provider(
        String providerId,
        String name,
        boolean inNetwork) {

    public Provider(String providerId, String name) {
        this(providerId, name, true);
    }

    public Provider {
        Objects.requireNonNull(providerId, "Provider ID cannot be null");
        Objects.requireNonNull(name, "Provider name cannot be null");

        if (providerId.isBlank()) {
            throw new IllegalArgumentException("Provider ID cannot be blank");
        }

        if (name.isBlank()) {
            throw new IllegalArgumentException("Provider name cannot be blank");
        }

        providerId = providerId.trim();
        name = name.trim();
    }
}