package com.pyrosense.analysis.domain.detection;

import com.pyrosense.analysis.domain.model.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class MicroArcPatternDetector {

    private MicroArcPatternDetector() {}

    public static List<SignalAnomaly> detect(SignalWindow window, DetectionThresholds thresholds, Instant now) {
        double[] arcCounts = window.valuesFor(SignalFeature.MICRO_ARC_COUNT);

        int recurrentSamples = 0;
        for (double count : arcCounts) {
            if (count > 0) recurrentSamples++;
        }

        if (recurrentSamples >= thresholds.microArcRecurrenceThreshold()) {
            double totalArcs = 0;
            for (double c : arcCounts) totalArcs += c;
            double avgArcs = totalArcs / arcCounts.length;

            double confidence = Math.min((double) recurrentSamples / arcCounts.length, 1.0);

            return List.of(new SignalAnomaly(
                    UUID.randomUUID(),
                    window.deviceId(),
                    SignalFeature.MICRO_ARC_COUNT,
                    AnomalyType.MICRO_ARC_RECURRENT,
                    avgArcs,
                    0.0,
                    recurrentSamples,
                    confidence,
                    now
            ));
        }
        return List.of();
    }
}
