package com.pyrosense.analysis.domain.model;

import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class SignalWindowTest {

    private final DeviceId deviceId = DeviceId.generate();
    private final TenantId tenantId = TenantId.generate();

    @Test
    void shouldCreateWindowWithSamples() {
        var sample = new SignalWindow.SignalSample(Instant.now(), Map.of(SignalFeature.THD, 5.0));
        var window = new SignalWindow(deviceId, tenantId, List.of(sample));

        assertThat(window.size()).isEqualTo(1);
        assertThat(window.deviceId()).isEqualTo(deviceId);
        assertThat(window.tenantId()).isEqualTo(tenantId);
    }

    @Test
    void shouldRejectEmptySamples() {
        assertThatThrownBy(() -> new SignalWindow(deviceId, tenantId, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnValuesForFeature() {
        var s1 = new SignalWindow.SignalSample(Instant.parse("2025-01-01T00:00:00Z"),
                Map.of(SignalFeature.THD, 4.0, SignalFeature.TEMPERATURE, 30.0));
        var s2 = new SignalWindow.SignalSample(Instant.parse("2025-01-01T00:01:00Z"),
                Map.of(SignalFeature.THD, 6.0, SignalFeature.TEMPERATURE, 35.0));
        var window = new SignalWindow(deviceId, tenantId, List.of(s1, s2));

        double[] thd = window.valuesFor(SignalFeature.THD);
        assertThat(thd).containsExactly(4.0, 6.0);

        assertThat(window.latestValueFor(SignalFeature.TEMPERATURE)).isEqualTo(35.0);
    }

    @Test
    void shouldReturnTimeRange() {
        Instant t1 = Instant.parse("2025-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2025-01-01T01:00:00Z");
        var s1 = new SignalWindow.SignalSample(t1, Map.of(SignalFeature.THD, 4.0));
        var s2 = new SignalWindow.SignalSample(t2, Map.of(SignalFeature.THD, 6.0));
        var window = new SignalWindow(deviceId, tenantId, List.of(s1, s2));

        assertThat(window.startTime()).isEqualTo(t1);
        assertThat(window.endTime()).isEqualTo(t2);
    }

    @Test
    void shouldReturnZeroForMissingFeature() {
        var sample = new SignalWindow.SignalSample(Instant.now(), Map.of(SignalFeature.THD, 5.0));
        var window = new SignalWindow(deviceId, tenantId, List.of(sample));

        assertThat(window.latestValueFor(SignalFeature.MICRO_ARC_COUNT)).isEqualTo(0.0);
    }
}
