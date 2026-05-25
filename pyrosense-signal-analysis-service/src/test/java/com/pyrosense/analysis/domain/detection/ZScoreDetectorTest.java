package com.pyrosense.analysis.domain.detection;

import com.pyrosense.analysis.domain.model.*;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ZScoreDetectorTest {

    private final DeviceId deviceId = DeviceId.generate();
    private final TenantId tenantId = TenantId.generate();
    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");
    private final DetectionThresholds thresholds = DetectionThresholds.defaults();

    @Test
    void shouldDetectAnomalyWhenZScoreExceedsThreshold() {
        BaselineProfile baseline = createBaseline(SignalFeature.THD, 5.0, 1.0);

        SignalWindow window = createWindow(Map.of(SignalFeature.THD, 12.0));

        List<SignalAnomaly> anomalies = ZScoreDetector.detect(window, baseline, thresholds, now);

        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.getFirst().feature()).isEqualTo(SignalFeature.THD);
        assertThat(anomalies.getFirst().type()).isEqualTo(AnomalyType.THD_ABNORMAL);
        assertThat(anomalies.getFirst().deviationSigma()).isEqualTo(7.0);
    }

    @Test
    void shouldNotDetectWhenWithinNormalRange() {
        BaselineProfile baseline = createBaseline(SignalFeature.THD, 5.0, 1.0);

        SignalWindow window = createWindow(Map.of(SignalFeature.THD, 6.5));

        List<SignalAnomaly> anomalies = ZScoreDetector.detect(window, baseline, thresholds, now);

        assertThat(anomalies).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenBaselineNotReady() {
        Map<SignalFeature, StatisticalRange> stats = new EnumMap<>(SignalFeature.class);
        stats.put(SignalFeature.THD, new StatisticalRange(5.0, 1.0, 0, 10, 2, 4, 5, 6, 8, 50));
        BaselineProfile baseline = new BaselineProfile(UUID.randomUUID(), deviceId, stats, 10,
                now, now, 100);

        SignalWindow window = createWindow(Map.of(SignalFeature.THD, 50.0));

        List<SignalAnomaly> anomalies = ZScoreDetector.detect(window, baseline, thresholds, now);

        assertThat(anomalies).isEmpty();
    }

    @Test
    void shouldDetectMultipleFeatureAnomalies() {
        Map<SignalFeature, StatisticalRange> stats = new EnumMap<>(SignalFeature.class);
        stats.put(SignalFeature.THD, new StatisticalRange(5.0, 1.0, 0, 10, 2, 4, 5, 6, 8, 1000));
        stats.put(SignalFeature.TEMPERATURE, new StatisticalRange(40.0, 5.0, 20, 60, 30, 35, 40, 45, 50, 1000));
        BaselineProfile baseline = new BaselineProfile(UUID.randomUUID(), deviceId, stats, 1000, now, now, 100);

        SignalWindow window = createWindow(Map.of(
                SignalFeature.THD, 15.0,
                SignalFeature.TEMPERATURE, 65.0));

        List<SignalAnomaly> anomalies = ZScoreDetector.detect(window, baseline, thresholds, now);

        assertThat(anomalies).hasSize(2);
    }

    @Test
    void shouldComputeConfidenceAboveHalf() {
        BaselineProfile baseline = createBaseline(SignalFeature.THD, 5.0, 1.0);
        SignalWindow window = createWindow(Map.of(SignalFeature.THD, 9.0));

        List<SignalAnomaly> anomalies = ZScoreDetector.detect(window, baseline, thresholds, now);

        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.getFirst().confidence()).isBetween(0.5, 1.0);
    }

    private BaselineProfile createBaseline(SignalFeature feature, double mean, double stdDev) {
        Map<SignalFeature, StatisticalRange> stats = new EnumMap<>(SignalFeature.class);
        stats.put(feature, new StatisticalRange(mean, stdDev, mean - 3 * stdDev, mean + 3 * stdDev,
                mean - 1.6 * stdDev, mean - 0.67 * stdDev, mean, mean + 0.67 * stdDev, mean + 1.6 * stdDev, 1000));
        return new BaselineProfile(UUID.randomUUID(), deviceId, stats, 1000, now, now, 100);
    }

    private SignalWindow createWindow(Map<SignalFeature, Double> values) {
        var sample = new SignalWindow.SignalSample(now, values);
        return new SignalWindow(deviceId, tenantId, List.of(sample));
    }
}
