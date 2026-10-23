package com.claims.adjudication;

import java.util.List;
import java.util.Objects;

public record BatchAdjudicationResult(
        List<AdjudicationResult> results,
        Accumulator finalAccumulator) {

    public BatchAdjudicationResult {
        Objects.requireNonNull(results, "results must not be null");
        Objects.requireNonNull(
                finalAccumulator, "finalAccumulator must not be null");
        results = List.copyOf(results);
    }
}
