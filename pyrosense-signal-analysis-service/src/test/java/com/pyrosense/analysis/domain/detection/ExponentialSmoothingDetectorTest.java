package com.pyrosense.analysis.domain.detection;

import com.pyrosense.analysis.domain.model.*;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class ExponentialSmoothingDetectorTest {

    private final DeviceId deviceId = DeviceId.generate();
    private final TenantId tenantId = TenantId.generate();
    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");
    private final DetectionThresholds thresholds = DetectionThresholds.defaults();

    @Test
    void shouldDetectHfNoiseAnomaly() {
        Map<SignalFeature, StatisticalRange> stats = new EnumMap<>(SignalFeature.class);
        stats.put(SignalFeature.HF_NOISE, new StatisticalRange(-60.0, 5.0, -80.0, -40.0,
                -68.0, -63.0, -60.0, -57.0, -52.0, 1000));
        BaselineProfile baseline = new BaselineProfile(UUID.randomUUID(), deviceId, stats, 1000, now, now, 100);

        List<SignalWindow.SignalSample> samples = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            samples.add(new SignalWindow.SignalSample(now.plusSeconds(i * 60),
                    Map.of(SignalFeature.HF_NOISE, -30.0)));
        }
        var window = new SignalWindow(deviceId, tenantId, samples);

        List<SignalAnomaly> anomalies = ExponentialSmoothingDetector.detect(window, baseline, thresholds, now);

        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.getFirst().type()).isEqualTo(AnomalyType.HF_NOISE_ELEVATED);
    }

    @Test
    void shouldNotDetectWhenNoBaseline() {
        List<SignalWindow.SignalSample> samples = List.of(
                new SignalWindow.SignalSample(now, Map.of(SignalFeature.HF_NOISE, -30.0)),
                new SignalWindow.SignalSample(now.plusSeconds(60), Map.of(SignalFeature.HF_NOISE, -30.0)),
                new SignalWindow.SignalSample(now.plusSeconds(120), Map.of(SignalFeature.HF_NOISE, -30.0))
        );
        var window = new SignalWindow(deviceId, tenantId, samples);

        List<SignalAnomaly> anomalies = ExponentialSmoothingDetector.detect(window, null, thresholds, now);

        assertThat(anomalies).isEmpty();
    }

    @Test
    void shouldNotDetectWhenValuesNormal() {
        Map<SignalFeature, StatisticalRange> stats = new EnumMap<>(SignalFeature.class);
        stats.put(SignalFeature.HF_NOISE, new StatisticalRange(-60.0, 5.0, -80.0, -40.0,
                -68.0, -63.0, -60.0, -57.0, -52.0, 1000));
        BaselineProfile baseline = new BaselineProfile(UUID.randomUUID(), deviceId, stats, 1000, now, now, 100);

        List<SignalWindow.SignalSample> samples = List.of(
                new SignalWindow.SignalSample(now, Map.of(SignalFeature.HF_NOISE, -58.0)),
                new SignalWindow.SignalSample(now.plusSeconds(60), Map.of(SignalFeature.HF_NOISE, -61.0)),
                new SignalWindow.SignalSample(now.plusSeconds(120), Map.of(SignalFeature.HF_NOISE, -59.0))
        );
        var window = new SignalWindow(deviceId, tenantId, samples);

        List<SignalAnomaly> anomalies = ExponentialSmoothingDetector.detect(window, baseline, thresholds, now);

        assertThat(anomalies).isEmpty();
    }

    @Test
    void shouldDetectTransientCountAnomaly() {
        Map<SignalFeature, StatisticalRange> stats = new EnumMap<>(SignalFeature.class);
        stats.put(SignalFeature.TRANSIENT_COUNT, new StatisticalRange(2.0, 1.0, 0.0, 5.0,
                0.5, 1.0, 2.0, 3.0, 4.0, 1000));
        BaselineProfile baseline = new BaselineProfile(UUID.randomUUID(), deviceId, stats, 1000, now, now, 100);

        List<SignalWindow.SignalSample> samples = List.of(
                new SignalWindow.SignalSample(now, Map.of(SignalFeature.TRANSIENT_COUNT, 15.0)),
                new SignalWindow.SignalSample(now.plusSeconds(60), Map.of(SignalFeature.TRANSIENT_COUNT, 18.0)),
                new SignalWindow.SignalSample(now.plusSeconds(120), Map.of(SignalFeature.TRANSIENT_COUNT, 20.0))
        );
        var window = new SignalWindow(deviceId, tenantId, samples);

        List<SignalAnomaly> anomalies = ExponentialSmoothingDetector.detect(window, baseline, thresholds, now);

        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.getFirst().type()).isEqualTo(AnomalyType.TRANSIENT_ABNORMAL);
    }
}
