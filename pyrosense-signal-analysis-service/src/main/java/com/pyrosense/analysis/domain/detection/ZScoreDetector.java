package com.pyrosense.analysis.domain.detection;

import com.pyrosense.analysis.domain.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ZScoreDetector {

    private ZScoreDetector() {}

    public static List<SignalAnomaly> detect(SignalWindow window, BaselineProfile baseline,
                                             DetectionThresholds thresholds, Instant now) {
        if (!baseline.isReady()) return List.of();

        List<SignalAnomaly> anomalies = new ArrayList<>();

        for (SignalFeature feature : SignalFeature.values()) {
            if (!baseline.hasFeature(feature)) continue;

            StatisticalRange stats = baseline.statsFor(feature);
            double currentValue = window.latestValueFor(feature);
            double zScore = stats.zScore(currentValue);

            if (Math.abs(zScore) > thresholds.zScoreThreshold()) {
                AnomalyType type = mapFeatureToAnomalyType(feature);
                double confidence = computeConfidence(zScore, thresholds.zScoreThreshold());

                anomalies.add(new SignalAnomaly(
                        UUID.randomUUID(),
                        window.deviceId(),
                        feature,
                        type,
                        currentValue,
                        stats.mean(),
                        zScore,
                        confidence,
                        now
                ));
            }
        }
        return anomalies;
    }

    private static AnomalyType mapFeatureToAnomalyType(SignalFeature feature) {
        return switch (feature) {
            case THD -> AnomalyType.THD_ABNORMAL;
            case MICRO_ARC_COUNT -> AnomalyType.MICRO_ARC_RECURRENT;
            case TEMPERATURE -> AnomalyType.TEMPERATURE_RISING;
            case TRANSIENT_COUNT -> AnomalyType.TRANSIENT_ABNORMAL;
            case HF_NOISE -> AnomalyType.HF_NOISE_ELEVATED;
            case POWER_FACTOR -> AnomalyType.POWER_FACTOR_DEGRADED;
            default -> AnomalyType.BASELINE_DRIFT;
        };
    }

    private static double computeConfidence(double zScore, double threshold) {
        double excess = Math.abs(zScore) - threshold;
        return Math.min(0.5 + (excess / (2 * threshold)) * 0.5, 1.0);
    }
}
