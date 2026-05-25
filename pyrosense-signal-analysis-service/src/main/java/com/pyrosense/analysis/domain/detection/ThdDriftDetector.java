package com.pyrosense.analysis.domain.detection;

import com.pyrosense.analysis.domain.model.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ThdDriftDetector {

    private ThdDriftDetector() {}

    public static List<SignalAnomaly> detect(SignalWindow window, BaselineProfile baseline,
                                             DetectionThresholds thresholds, Instant now) {
        double[] thdValues = window.valuesFor(SignalFeature.THD);
        double latestThd = thdValues[thdValues.length - 1];

        if (latestThd > thresholds.thdMaxPercent()) {
            double confidence = Math.min(latestThd / (thresholds.thdMaxPercent() * 2), 1.0);
            return List.of(new SignalAnomaly(
                    UUID.randomUUID(), window.deviceId(), SignalFeature.THD,
                    AnomalyType.THD_ABNORMAL, latestThd, thresholds.thdMaxPercent(),
                    (latestThd - thresholds.thdMaxPercent()) / 2.0, confidence, now
            ));
        }

        if (baseline != null && baseline.isReady() && baseline.hasFeature(SignalFeature.THD)) {
            StatisticalRange stats = baseline.statsFor(SignalFeature.THD);
            double movingAvg = computeMovingAverage(thdValues);
            double driftPercent = stats.mean() > 0
                    ? ((movingAvg - stats.mean()) / stats.mean()) * 100.0
                    : 0;

            if (driftPercent > thresholds.driftPercentThreshold()) {
                double confidence = Math.min(driftPercent / (thresholds.driftPercentThreshold() * 2), 1.0);
                return List.of(new SignalAnomaly(
                        UUID.randomUUID(), window.deviceId(), SignalFeature.THD,
                        AnomalyType.BASELINE_DRIFT, movingAvg, stats.mean(),
                        stats.zScore(movingAvg), confidence, now
                ));
            }
        }

        return List.of();
    }

    private static double computeMovingAverage(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }
}
