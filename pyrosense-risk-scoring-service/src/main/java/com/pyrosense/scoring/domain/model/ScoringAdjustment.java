package com.pyrosense.scoring.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ScoringAdjustment(
        UUID id,
        String anomalyType,
        AdjustmentType type,
        double previousValue,
        double newValue,
        double delta,
        AdjustmentMode mode,
        String reason,
        Instant appliedAt
) {
    public enum AdjustmentType {
        CONFIDENCE_BOOST,
        CONFIDENCE_REDUCTION,
        WEIGHT_INCREASE,
        WEIGHT_DECREASE
    }

    public enum AdjustmentMode {
        APPLIED,
        SUGGESTION_ONLY
    }

    public static ScoringAdjustment confidenceBoost(String anomalyType, double previous, double boost, String reason) {
        double newVal = Math.min(previous + boost, 1.0);
        return new ScoringAdjustment(UUID.randomUUID(), anomalyType,
                AdjustmentType.CONFIDENCE_BOOST, previous, newVal, boost,
                AdjustmentMode.SUGGESTION_ONLY, reason, Instant.now());
    }

    public static ScoringAdjustment confidenceReduction(String anomalyType, double previous, double reduction, String reason) {
        double newVal = Math.max(previous - reduction, 0.0);
        return new ScoringAdjustment(UUID.randomUUID(), anomalyType,
                AdjustmentType.CONFIDENCE_REDUCTION, previous, newVal, -reduction,
                AdjustmentMode.SUGGESTION_ONLY, reason, Instant.now());
    }

    public boolean isApplied() {
        return mode == AdjustmentMode.APPLIED;
    }

    public boolean isSuggestionOnly() {
        return mode == AdjustmentMode.SUGGESTION_ONLY;
    }

    public ScoringAdjustment withAppliedMode() {
        return new ScoringAdjustment(id, anomalyType, type, previousValue, newValue, delta,
                AdjustmentMode.APPLIED, reason, appliedAt);
    }
}
