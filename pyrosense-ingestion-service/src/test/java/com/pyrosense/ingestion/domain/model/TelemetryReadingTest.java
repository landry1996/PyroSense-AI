package com.pyrosense.ingestion.domain.model;

import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class TelemetryReadingTest {

    private TelemetryReading createReading(int microArcCount, double temperature, double hfNoise) {
        return TelemetryReading.builder()
                .id(UUID.randomUUID())
                .deviceId(DeviceId.generate())
                .tenantId(TenantId.generate())
                .timestamp(Instant.now())
                .samplingWindowMs(1000)
                .rmsCurrent(12.0)
                .rmsVoltage(230.0)
                .activePower(2800.0)
                .reactivePower(300.0)
                .powerFactor(0.94)
                .thd(4.5)
                .temperatureCelsius(temperature)
                .hfNoiseLevel(hfNoise)
                .microArcCount(microArcCount)
                .transientCount(0)
                .firmwareVersion("1.0.0")
                .payloadHash("abc123")
                .ingestedAt(Instant.now())
                .build();
    }

    @Test
    void shouldDetectMicroArcActivity() {
        var reading = createReading(3, 35.0, 0.1);
        assertThat(reading.hasMicroArcActivity()).isTrue();
    }

    @Test
    void shouldNotDetectMicroArcActivityWhenZero() {
        var reading = createReading(0, 35.0, 0.1);
        assertThat(reading.hasMicroArcActivity()).isFalse();
    }

    @Test
    void shouldDetectHighTemperature() {
        var reading = createReading(0, 75.0, 0.1);
        assertThat(reading.hasHighTemperature(60.0)).isTrue();
    }

    @Test
    void shouldNotDetectHighTemperatureWhenBelowThreshold() {
        var reading = createReading(0, 40.0, 0.1);
        assertThat(reading.hasHighTemperature(60.0)).isFalse();
    }

    @Test
    void shouldDetectHighHfNoise() {
        var reading = createReading(0, 35.0, 0.8);
        assertThat(reading.hasHighHfNoise(0.5)).isTrue();
    }

    @Test
    void shouldRejectNullDeviceId() {
        assertThatThrownBy(() -> TelemetryReading.builder()
                .id(UUID.randomUUID())
                .deviceId(null)
                .tenantId(TenantId.generate())
                .timestamp(Instant.now())
                .payloadHash("hash")
                .ingestedAt(Instant.now())
                .build()
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullTimestamp() {
        assertThatThrownBy(() -> TelemetryReading.builder()
                .id(UUID.randomUUID())
                .deviceId(DeviceId.generate())
                .tenantId(TenantId.generate())
                .timestamp(null)
                .payloadHash("hash")
                .ingestedAt(Instant.now())
                .build()
        ).isInstanceOf(NullPointerException.class);
    }
}
