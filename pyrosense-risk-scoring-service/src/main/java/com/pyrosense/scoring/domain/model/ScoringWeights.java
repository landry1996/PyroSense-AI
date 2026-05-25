package com.pyrosense.scoring.domain.model;

public record ScoringWeights(
        double microArcWeight,
        double thdDriftWeight,
        double temperatureTrendWeight,
        double transientWeight,
        double hfNoiseWeight,
        double recencyDecayFactor,
        double repetitionBoostFactor,
        double deviceReliabilityWeight,
        int historyWindowDays,
        int recencyWindowHours
) {
    public ScoringWeights {
        if (microArcWeight < 0 || thdDriftWeight < 0 || temperatureTrendWeight < 0 ||
                transientWeight < 0 || hfNoiseWeight < 0) {
            throw new IllegalArgumentException("Weights must be non-negative");
        }
        if (recencyDecayFactor <= 0 || recencyDecayFactor > 1) {
            throw new IllegalArgumentException("recencyDecayFactor must be in (0,1]");
        }
        if (repetitionBoostFactor < 1) {
            throw new IllegalArgumentException("repetitionBoostFactor must be >= 1");
        }
    }

    public static ScoringWeights defaults() {
        return new ScoringWeights(0.30, 0.20, 0.20, 0.10, 0.10, 0.95, 1.3, 0.10, 24, 7);
    }
}
