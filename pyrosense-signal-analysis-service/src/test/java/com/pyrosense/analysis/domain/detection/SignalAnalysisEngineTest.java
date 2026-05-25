package com.pyrosense.analysis.domain.detection;

import com.pyrosense.analysis.domain.model.*;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class SignalAnalysisEngineTest {

    private final DeviceId deviceId = DeviceId.generate();
    private final TenantId tenantId = TenantId.generate();
    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");
    private final SignalAnalysisEngine engine = new SignalAnalysisEngine(DetectionThresholds.defaults());

    @Test
    void shouldReturnNoAnomalyForNormalSignals() {
        Map<SignalFeature, StatisticalRange> stats = new EnumMap<>(SignalFeature.class);
        stats.put(SignalFeature.THD, new StatisticalRange(5.0, 1.0, 3.0, 7.0, 3.5, 4.3, 5.0, 5.7, 6.5, 1000));
        stats.put(SignalFeature.TEMPERATURE, new StatisticalRange(40.0, 5.0, 30.0, 50.0, 32.0, 37.0, 40.0, 43.0, 48.0, 1000));
        BaselineProfile baseline = new BaselineProfile(UUID.randomUUID(), deviceId, stats, 1000, now, now, 100);

        var sample = new SignalWindow.SignalSample(now, Map.of(
                SignalFeature.THD, 5.5,
                SignalFeature.TEMPERATURE, 42.0,
                SignalFeature.MICRO_ARC_COUNT, 0.0));
        var window = new SignalWindow(deviceId, tenantId, List.of(sample));

        AnalysisResult result = engine.analyze(window, baseline, now);

        assertThat(result.hasAnomalies()).isFalse();
        assertThat(result.aggregateRiskScore()).isEqualTo(0.0);
        assertThat(result.baselineAvailable()).isTrue();
    }

    @Test
    void shouldDetectMultipleAnomalies() {
        Map<SignalFeature, StatisticalRange> stats = new EnumMap<>(SignalFeature.class);
        stats.put(SignalFeature.THD, new StatisticalRange(4.0, 0.5, 3.0, 5.0, 3.2, 3.7, 4.0, 4.3, 4.8, 1000));
        stats.put(SignalFeature.TEMPERATURE, new StatisticalRange(40.0, 3.0, 30.0, 50.0, 35.0, 38.0, 40.0, 42.0, 46.0, 1000));
        BaselineProfile baseline = new BaselineProfile(UUID.randomUUID(), deviceId, stats, 1000, now, now, 100);

        List<SignalWindow.SignalSample> samples = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            samples.add(new SignalWindow.SignalSample(now.plusSeconds(i * 60), Map.of(
                    SignalFeature.THD, 12.0,
                    SignalFeature.TEMPERATURE, 90.0,
                    SignalFeature.MICRO_ARC_COUNT, 3.0)));
        }
        var window = new SignalWindow(deviceId, tenantId, samples);

        AnalysisResult result = engine.analyze(window, baseline, now);

        assertThat(result.hasAnomalies()).isTrue();
        assertThat(result.anomalyCount()).isGreaterThanOrEqualTo(2);
        assertThat(result.aggregateRiskScore()).isGreaterThan(0);
    }

    @Test
    void shouldWorkWithoutBaseline() {
        List<SignalWindow.SignalSample> samples = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            samples.add(new SignalWindow.SignalSample(now.plusSeconds(i * 60), Map.of(
                    SignalFeature.MICRO_ARC_COUNT, 5.0,
                    SignalFeature.TEMPERATURE, 90.0,
                    SignalFeature.THD, 12.0)));
        }
        var window = new SignalWindow(deviceId, tenantId, samples);

        AnalysisResult result = engine.analyze(window, null, now);

        assertThat(result.hasAnomalies()).isTrue();
        assertThat(result.baselineAvailable()).isFalse();
    }

    @Test
    void shouldDeduplicateByFeature() {
        Map<SignalFeature, StatisticalRange> stats = new EnumMap<>(SignalFeature.class);
        stats.put(SignalFeature.THD, new StatisticalRange(4.0, 0.5, 3.0, 5.0, 3.2, 3.7, 4.0, 4.3, 4.8, 1000));
        BaselineProfile baseline = new BaselineProfile(UUID.randomUUID(), deviceId, stats, 1000, now, now, 100);

        var sample = new SignalWindow.SignalSample(now, Map.of(SignalFeature.THD, 15.0));
        var window = new SignalWindow(deviceId, tenantId, List.of(sample));

        AnalysisResult result = engine.analyze(window, baseline, now);

        long thdAnomalies = result.anomalies().stream()
                .filter(a -> a.feature() == SignalFeature.THD).count();
        assertThat(thdAnomalies).isEqualTo(1);
    }

    @Test
    void shouldCapRiskScoreAt100() {
        Map<SignalFeature, StatisticalRange> stats = new EnumMap<>(SignalFeature.class);
        for (SignalFeature f : SignalFeature.values()) {
            stats.put(f, new StatisticalRange(1.0, 0.1, 0, 2, 0.5, 0.8, 1.0, 1.2, 1.5, 1000));
        }
        BaselineProfile baseline = new BaselineProfile(UUID.randomUUID(), deviceId, stats, 1000, now, now, 100);

        List<SignalWindow.SignalSample> samples = new ArrayList<>();
        Map<SignalFeature, Double> extreme = new EnumMap<>(SignalFeature.class);
        for (SignalFeature f : SignalFeature.values()) {
            extreme.put(f, 100.0);
        }
        for (int i = 0; i < 5; i++) {
            samples.add(new SignalWindow.SignalSample(now.plusSeconds(i * 60), extreme));
        }
        var window = new SignalWindow(deviceId, tenantId, samples);

        AnalysisResult result = engine.analyze(window, baseline, now);

        assertThat(result.aggregateRiskScore()).isLessThanOrEqualTo(100.0);
    }
}
