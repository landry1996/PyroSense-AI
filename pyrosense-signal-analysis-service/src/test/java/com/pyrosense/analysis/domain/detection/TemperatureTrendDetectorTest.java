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

class TemperatureTrendDetectorTest {

    private final DeviceId deviceId = DeviceId.generate();
    private final TenantId tenantId = TenantId.generate();
    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");
    private final DetectionThresholds thresholds = DetectionThresholds.defaults();

    @Test
    void shouldDetectTemperatureAboveMax() {
        var sample = new SignalWindow.SignalSample(now, Map.of(SignalFeature.TEMPERATURE, 90.0));
        var window = new SignalWindow(deviceId, tenantId, List.of(sample));

        List<SignalAnomaly> anomalies = TemperatureTrendDetector.detect(window, thresholds, now);

        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.getFirst().type()).isEqualTo(AnomalyType.TEMPERATURE_RISING);
        assertThat(anomalies.getFirst().currentValue()).isEqualTo(90.0);
    }

    @Test
    void shouldDetectRapidTemperatureRise() {
        List<SignalWindow.SignalSample> samples = new ArrayList<>();
        Instant start = Instant.parse("2025-01-15T09:00:00Z");
        samples.add(new SignalWindow.SignalSample(start, Map.of(SignalFeature.TEMPERATURE, 40.0)));
        samples.add(new SignalWindow.SignalSample(start.plusSeconds(3600), Map.of(SignalFeature.TEMPERATURE, 50.0)));
        var window = new SignalWindow(deviceId, tenantId, samples);

        List<SignalAnomaly> anomalies = TemperatureTrendDetector.detect(window, thresholds, now);

        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.getFirst().type()).isEqualTo(AnomalyType.TEMPERATURE_RISING);
    }

    @Test
    void shouldNotDetectNormalTemperature() {
        List<SignalWindow.SignalSample> samples = new ArrayList<>();
        Instant start = Instant.parse("2025-01-15T09:00:00Z");
        samples.add(new SignalWindow.SignalSample(start, Map.of(SignalFeature.TEMPERATURE, 40.0)));
        samples.add(new SignalWindow.SignalSample(start.plusSeconds(3600), Map.of(SignalFeature.TEMPERATURE, 42.0)));
        var window = new SignalWindow(deviceId, tenantId, samples);

        List<SignalAnomaly> anomalies = TemperatureTrendDetector.detect(window, thresholds, now);

        assertThat(anomalies).isEmpty();
    }

    @Test
    void shouldNotDetectWithSingleSample() {
        var sample = new SignalWindow.SignalSample(now, Map.of(SignalFeature.TEMPERATURE, 50.0));
        var window = new SignalWindow(deviceId, tenantId, List.of(sample));

        List<SignalAnomaly> anomalies = TemperatureTrendDetector.detect(window, thresholds, now);

        assertThat(anomalies).isEmpty();
    }
}
