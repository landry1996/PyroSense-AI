package com.pyrosense.ingestion.domain.model.quality;

public enum SignalQualityLevel {
    EXCELLENT(0.9, 1.0),
    GOOD(0.7, 0.9),
    DEGRADED(0.5, 0.7),
    POOR(0.3, 0.5),
    CRITICAL(0.0, 0.3);

    private final double lowerBound;
    private final double upperBound;

    SignalQualityLevel(double lowerBound, double upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public static SignalQualityLevel fromScore(double score) {
        for (var level : values()) {
            if (score >= level.lowerBound && score < level.upperBound) {
                return level;
            }
        }
        return score >= 1.0 ? EXCELLENT : CRITICAL;
    }

    public boolean isTrustworthy() {
        return this == EXCELLENT || this == GOOD;
    }

    public boolean requiresReview() {
        return this == POOR || this == CRITICAL;
    }
}
