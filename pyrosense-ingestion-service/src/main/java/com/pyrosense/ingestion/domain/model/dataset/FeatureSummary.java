package com.pyrosense.ingestion.domain.model.dataset;

public record FeatureSummary(
        double rmsVoltageAvg,
        double rmsVoltageSd,
        double rmsCurrentAvg,
        double rmsCurrentSd,
        double temperatureAvg,
        double temperatureSd,
        double thdAvg,
        double thdMax,
        double hfNoiseAvg,
        double hfNoiseMax,
        int microArcCount,
        double powerFactorAvg,
        int readingCount,
        double signalQualityAvg
) {
    public boolean hasAnomalousSignals() {
        return microArcCount > 0 || thdMax > 8.0 || hfNoiseMax > 0.8;
    }

    public boolean isSufficientData() {
        return readingCount >= 100;
    }
}
