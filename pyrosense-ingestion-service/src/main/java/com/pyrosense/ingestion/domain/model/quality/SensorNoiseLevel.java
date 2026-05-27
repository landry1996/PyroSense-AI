package com.pyrosense.ingestion.domain.model.quality;

public record SensorNoiseLevel(
        double hfNoiseAverage,
        double hfNoiseMax,
        double currentStdDev,
        double voltageStdDev
) {
    private static final double HF_NOISE_THRESHOLD = 0.6;
    private static final double CURRENT_NOISE_THRESHOLD = 0.5;
    private static final double VOLTAGE_NOISE_THRESHOLD = 2.0;

    public boolean isExcessive() {
        return hfNoiseAverage > HF_NOISE_THRESHOLD
                || currentStdDev > CURRENT_NOISE_THRESHOLD
                || voltageStdDev > VOLTAGE_NOISE_THRESHOLD;
    }

    public String describe() {
        if (hfNoiseAverage > HF_NOISE_THRESHOLD) {
            return "HF noise average %.2f exceeds threshold %.2f".formatted(hfNoiseAverage, HF_NOISE_THRESHOLD);
        }
        if (currentStdDev > CURRENT_NOISE_THRESHOLD) {
            return "Current stddev %.3fA exceeds threshold %.3fA".formatted(currentStdDev, CURRENT_NOISE_THRESHOLD);
        }
        if (voltageStdDev > VOLTAGE_NOISE_THRESHOLD) {
            return "Voltage stddev %.2fV exceeds threshold %.2fV".formatted(voltageStdDev, VOLTAGE_NOISE_THRESHOLD);
        }
        return "Noise within acceptable range";
    }
}
