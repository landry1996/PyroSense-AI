package com.pyrosense.analysis.domain.model;

public record DetectionThresholds(
        double zScoreThreshold,
        double thdMaxPercent,
        double temperatureMaxCelsius,
        double temperatureRiseRatePerHour,
        int microArcRecurrenceThreshold,
        int transientCountThreshold,
        double hfNoiseMaxDb,
        double powerFactorMin,
        double driftPercentThreshold,
        double exponentialSmoothingAlpha,
        int baselineMinimumSamples
) {
    public DetectionThresholds {
        if (zScoreThreshold <= 0) throw new IllegalArgumentException("zScoreThreshold must be positive");
        if (exponentialSmoothingAlpha <= 0 || exponentialSmoothingAlpha >= 1)
            throw new IllegalArgumentException("alpha must be in (0,1)");
        if (baselineMinimumSamples <= 0) throw new IllegalArgumentException("minimumSamples must be positive");
    }

    public static DetectionThresholds defaults() {
        return new DetectionThresholds(
                3.0,
                8.0,
                85.0,
                5.0,
                3,
                10,
                -40.0,
                0.85,
                15.0,
                0.3,
                100
        );
    }
}
