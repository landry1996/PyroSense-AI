package com.pyrosense.analysis.domain.detection;

import com.pyrosense.analysis.domain.model.*;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class MicroArcPatternDetectorTest {

    private final DeviceId deviceId = DeviceId.generate();
    private final TenantId tenantId = TenantId.generate();
    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");
    private final DetectionThresholds thresholds = DetectionThresholds.defaults();

    @Test
    void shouldDetectRecurrentMicroArcs() {
        List<SignalWindow.SignalSample> samples = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            samples.add(new SignalWindow.SignalSample(
                    now.plusSeconds(i * 60),
                    Map.of(SignalFeature.MICRO_ARC_COUNT, 2.0)));
        }
        var window = new SignalWindow(deviceId, tenantId, samples);

        List<SignalAnomaly> anomalies = MicroArcPatternDetector.detect(window, thresholds, now);

        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.getFirst().type()).isEqualTo(AnomalyType.MICRO_ARC_RECURRENT);
        assertThat(anomalies.getFirst().confidence()).isEqualTo(1.0);
    }

    @Test
    void shouldNotDetectWhenBelowThreshold() {
        List<SignalWindow.SignalSample> samples = new ArrayList<>();
        samples.add(new SignalWindow.SignalSample(now, Map.of(SignalFeature.MICRO_ARC_COUNT, 1.0)));
        samples.add(new SignalWindow.SignalSample(now.plusSeconds(60), Map.of(SignalFeature.MICRO_ARC_COUNT, 0.0)));
        samples.add(new SignalWindow.SignalSample(now.plusSeconds(120), Map.of(SignalFeature.MICRO_ARC_COUNT, 1.0)));
        samples.add(new SignalWindow.SignalSample(now.plusSeconds(180), Map.of(SignalFeature.MICRO_ARC_COUNT, 0.0)));
        samples.add(new SignalWindow.SignalSample(now.plusSeconds(240), Map.of(SignalFeature.MICRO_ARC_COUNT, 0.0)));
        var window = new SignalWindow(deviceId, tenantId, samples);

        List<SignalAnomaly> anomalies = MicroArcPatternDetector.detect(window, thresholds, now);

        assertThat(anomalies).isEmpty();
    }

    @Test
    void shouldDetectWithExactlyThresholdRecurrences() {
        List<SignalWindow.SignalSample> samples = new ArrayList<>();
        samples.add(new SignalWindow.SignalSample(now, Map.of(SignalFeature.MICRO_ARC_COUNT, 1.0)));
        samples.add(new SignalWindow.SignalSample(now.plusSeconds(60), Map.of(SignalFeature.MICRO_ARC_COUNT, 0.0)));
        samples.add(new SignalWindow.SignalSample(now.plusSeconds(120), Map.of(SignalFeature.MICRO_ARC_COUNT, 1.0)));
        samples.add(new SignalWindow.SignalSample(now.plusSeconds(180), Map.of(SignalFeature.MICRO_ARC_COUNT, 0.0)));
        samples.add(new SignalWindow.SignalSample(now.plusSeconds(240), Map.of(SignalFeature.MICRO_ARC_COUNT, 1.0)));
        var window = new SignalWindow(deviceId, tenantId, samples);

        List<SignalAnomaly> anomalies = MicroArcPatternDetector.detect(window, thresholds, now);

        assertThat(anomalies).hasSize(1);
    }

    @Test
    void shouldCalculateCorrectAverage() {
        List<SignalWindow.SignalSample> samples = new ArrayList<>();
        samples.add(new SignalWindow.SignalSample(now, Map.of(SignalFeature.MICRO_ARC_COUNT, 3.0)));
        samples.add(new SignalWindow.SignalSample(now.plusSeconds(60), Map.of(SignalFeature.MICRO_ARC_COUNT, 5.0)));
        samples.add(new SignalWindow.SignalSample(now.plusSeconds(120), Map.of(SignalFeature.MICRO_ARC_COUNT, 7.0)));
        var window = new SignalWindow(deviceId, tenantId, samples);

        List<SignalAnomaly> anomalies = MicroArcPatternDetector.detect(window, thresholds, now);

        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.getFirst().currentValue()).isEqualTo(5.0);
    }
}
