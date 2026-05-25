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

class ThdDriftDetectorTest {

    private final DeviceId deviceId = DeviceId.generate();
    private final TenantId tenantId = TenantId.generate();
    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");
    private final DetectionThresholds thresholds = DetectionThresholds.defaults();

    @Test
    void shouldDetectThdAboveAbsoluteMax() {
        var sample = new SignalWindow.SignalSample(now, Map.of(SignalFeature.THD, 12.0));
        var window = new SignalWindow(deviceId, tenantId, List.of(sample));

        List<SignalAnomaly> anomalies = ThdDriftDetector.detect(window, null, thresholds, now);

        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.getFirst().type()).isEqualTo(AnomalyType.THD_ABNORMAL);
    }

    @Test
    void shouldDetectDriftFromBaseline() {
        Map<SignalFeature, StatisticalRange> stats = new EnumMap<>(SignalFeature.class);
        stats.put(SignalFeature.THD, new StatisticalRange(4.0, 0.5, 3.0, 5.0, 3.2, 3.7, 4.0, 4.3, 4.8, 1000));
        BaselineProfile baseline = new BaselineProfile(UUID.randomUUID(), deviceId, stats, 1000, now, now, 100);

        var s1 = new SignalWindow.SignalSample(now, Map.of(SignalFeature.THD, 5.0));
        var s2 = new SignalWindow.SignalSample(now.plusSeconds(60), Map.of(SignalFeature.THD, 5.2));
        var s3 = new SignalWindow.SignalSample(now.plusSeconds(120), Map.of(SignalFeature.THD, 5.5));
        var window = new SignalWindow(deviceId, tenantId, List.of(s1, s2, s3));

        List<SignalAnomaly> anomalies = ThdDriftDetector.detect(window, baseline, thresholds, now);

        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.getFirst().type()).isEqualTo(AnomalyType.BASELINE_DRIFT);
    }

    @Test
    void shouldNotDetectWhenThdNormal() {
        Map<SignalFeature, StatisticalRange> stats = new EnumMap<>(SignalFeature.class);
        stats.put(SignalFeature.THD, new StatisticalRange(4.0, 0.5, 3.0, 5.0, 3.2, 3.7, 4.0, 4.3, 4.8, 1000));
        BaselineProfile baseline = new BaselineProfile(UUID.randomUUID(), deviceId, stats, 1000, now, now, 100);

        var sample = new SignalWindow.SignalSample(now, Map.of(SignalFeature.THD, 4.2));
        var window = new SignalWindow(deviceId, tenantId, List.of(sample));

        List<SignalAnomaly> anomalies = ThdDriftDetector.detect(window, baseline, thresholds, now);

        assertThat(anomalies).isEmpty();
    }

    @Test
    void shouldNotDetectDriftWithoutBaseline() {
        var sample = new SignalWindow.SignalSample(now, Map.of(SignalFeature.THD, 6.0));
        var window = new SignalWindow(deviceId, tenantId, List.of(sample));

        List<SignalAnomaly> anomalies = ThdDriftDetector.detect(window, null, thresholds, now);

        assertThat(anomalies).isEmpty();
    }
}
