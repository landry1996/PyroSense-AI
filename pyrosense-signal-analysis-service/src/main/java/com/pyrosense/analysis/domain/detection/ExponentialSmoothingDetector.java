package com.pyrosense.analysis.domain.detection;

import com.pyrosense.analysis.domain.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ExponentialSmoothingDetector {

    private ExponentialSmoothingDetector() {}

    public static List<SignalAnomaly> detect(SignalWindow window, BaselineProfile baseline,
                                             DetectionThresholds thresholds, Instant now) {
        if (baseline == null || !baseline.isReady()) return List.of();

        List<SignalAnomaly> anomalies = new ArrayList<>();
        double alpha = thresholds.exponentialSmoothingAlpha();

        for (SignalFeature feature : List.of(SignalFeature.HF_NOISE, SignalFeature.TRANSIENT_COUNT)) {
            if (!baseline.hasFeature(feature)) continue;

            double[] values = window.valuesFor(feature);
            if (values.length < 3) continue;

            double smoothed = values[0];
            for (int i = 1; i < values.length; i++) {
                smoothed = alpha * values[i] + (1 - alpha) * smoothed;
            }

            StatisticalRange stats = baseline.statsFor(feature);
            double deviation = stats.zScore(smoothed);

            if (Math.abs(deviation) > thresholds.zScoreThreshold()) {
                AnomalyType type = feature == SignalFeature.HF_NOISE
                        ? AnomalyType.HF_NOISE_ELEVATED
                        : AnomalyType.TRANSIENT_ABNORMAL;
                double confidence = Math.min(0.5 + (Math.abs(deviation) - thresholds.zScoreThreshold()) / 6.0, 1.0);

                anomalies.add(new SignalAnomaly(
                        UUID.randomUUID(), window.deviceId(), feature, type,
                        smoothed, stats.mean(), deviation, confidence, now
                ));
            }
        }
        return anomalies;
    }
}
