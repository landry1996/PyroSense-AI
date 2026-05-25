package com.pyrosense.analysis.domain.detection;

import com.pyrosense.analysis.domain.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SignalAnalysisEngine {

    private final DetectionThresholds thresholds;

    public SignalAnalysisEngine(DetectionThresholds thresholds) {
        this.thresholds = thresholds;
    }

    public AnalysisResult analyze(SignalWindow window, BaselineProfile baseline, Instant now) {
        List<SignalAnomaly> allAnomalies = new ArrayList<>();

        allAnomalies.addAll(MicroArcPatternDetector.detect(window, thresholds, now));
        allAnomalies.addAll(TemperatureTrendDetector.detect(window, thresholds, now));
        allAnomalies.addAll(ThdDriftDetector.detect(window, baseline, thresholds, now));

        if (baseline != null && baseline.isReady()) {
            allAnomalies.addAll(ZScoreDetector.detect(window, baseline, thresholds, now));
            allAnomalies.addAll(ExponentialSmoothingDetector.detect(window, baseline, thresholds, now));
        }

        List<SignalAnomaly> deduplicated = deduplicateByFeature(allAnomalies);
        double riskScore = computeAggregateRisk(deduplicated);

        return new AnalysisResult(
                UUID.randomUUID(),
                window.deviceId(),
                now,
                deduplicated,
                riskScore,
                baseline != null && baseline.isReady()
        );
    }

    private List<SignalAnomaly> deduplicateByFeature(List<SignalAnomaly> anomalies) {
        var byFeature = new java.util.EnumMap<SignalFeature, SignalAnomaly>(SignalFeature.class);
        for (SignalAnomaly a : anomalies) {
            byFeature.merge(a.feature(), a, (existing, incoming) ->
                    incoming.confidence() > existing.confidence() ? incoming : existing);
        }
        return new ArrayList<>(byFeature.values());
    }

    private double computeAggregateRisk(List<SignalAnomaly> anomalies) {
        if (anomalies.isEmpty()) return 0;
        double totalWeight = 0;
        for (SignalAnomaly a : anomalies) {
            totalWeight += a.weightedScore();
        }
        return Math.min(totalWeight * 100.0, 100.0);
    }
}
