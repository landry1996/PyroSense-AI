package com.pyrosense.ingestion.domain.validation;

import com.pyrosense.ingestion.domain.model.SignalQualityScore;

import java.util.ArrayList;
import java.util.List;

public final class TelemetryQualityValidator {

    private static final double MIN_SIGNAL_QUALITY = 0.3;
    private static final double VOLTAGE_NOISE_THRESHOLD = 0.05;
    private static final double CURRENT_NOISE_THRESHOLD = 0.02;

    private TelemetryQualityValidator() {}

    public record QualityAssessment(
            SignalQualityScore signalScore,
            List<String> warnings,
            boolean acceptable
    ) {}

    public static QualityAssessment assess(double signalQuality,
                                            double rmsVoltage,
                                            double rmsCurrent,
                                            double hfNoiseLevel,
                                            int samplingWindowMs) {
        List<String> warnings = new ArrayList<>();
        SignalQualityScore score = SignalQualityScore.fromRawValue(signalQuality);

        if (score.isBelowMinimum()) {
            warnings.add("Signal quality below minimum threshold: " + signalQuality);
        }

        if (hfNoiseLevel > 0.8) {
            warnings.add("High HF noise level: " + hfNoiseLevel);
        }

        if (rmsVoltage > 0 && rmsVoltage < VOLTAGE_NOISE_THRESHOLD) {
            warnings.add("Voltage reading near noise floor: " + rmsVoltage);
        }

        if (rmsCurrent > 0 && rmsCurrent < CURRENT_NOISE_THRESHOLD) {
            warnings.add("Current reading near noise floor: " + rmsCurrent);
        }

        if (samplingWindowMs < 100) {
            warnings.add("Sampling window unusually short: " + samplingWindowMs + "ms");
        }

        boolean acceptable = signalQuality >= MIN_SIGNAL_QUALITY;
        return new QualityAssessment(score, warnings, acceptable);
    }
}
