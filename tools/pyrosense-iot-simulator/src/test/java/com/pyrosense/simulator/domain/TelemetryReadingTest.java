package com.pyrosense.simulator.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TelemetryReadingTest {

    @Test
    void shouldBuildWithDefaults() {
        TelemetryReading reading = TelemetryReading.builder()
                .tenantId("tenant-1")
                .deviceId("device-1")
                .build();

        assertThat(reading.tenantId()).isEqualTo("tenant-1");
        assertThat(reading.deviceId()).isEqualTo("device-1");
        assertThat(reading.rmsCurrent()).isEqualTo(10.0);
        assertThat(reading.rmsVoltage()).isEqualTo(230.0);
        assertThat(reading.powerFactor()).isEqualTo(0.95);
    }

    @Test
    void shouldBuildWithCustomValues() {
        Instant now = Instant.now();
        TelemetryReading reading = TelemetryReading.builder()
                .tenantId("t1")
                .buildingId("b1")
                .panelId("p1")
                .circuitId("c1")
                .deviceId("d1")
                .timestamp(now)
                .rmsCurrent(15.5)
                .rmsVoltage(225.0)
                .activePower(3000.0)
                .reactivePower(300.0)
                .powerFactor(0.90)
                .thd(5.0)
                .temperatureCelsius(35.0)
                .hfNoiseLevel(0.3)
                .microArcCount(5)
                .transientCount(3)
                .build();

        assertThat(reading.tenantId()).isEqualTo("t1");
        assertThat(reading.buildingId()).isEqualTo("b1");
        assertThat(reading.panelId()).isEqualTo("p1");
        assertThat(reading.circuitId()).isEqualTo("c1");
        assertThat(reading.deviceId()).isEqualTo("d1");
        assertThat(reading.timestamp()).isEqualTo(now);
        assertThat(reading.rmsCurrent()).isEqualTo(15.5);
        assertThat(reading.rmsVoltage()).isEqualTo(225.0);
        assertThat(reading.activePower()).isEqualTo(3000.0);
        assertThat(reading.reactivePower()).isEqualTo(300.0);
        assertThat(reading.powerFactor()).isEqualTo(0.90);
        assertThat(reading.thd()).isEqualTo(5.0);
        assertThat(reading.temperatureCelsius()).isEqualTo(35.0);
        assertThat(reading.hfNoiseLevel()).isEqualTo(0.3);
        assertThat(reading.microArcCount()).isEqualTo(5);
        assertThat(reading.transientCount()).isEqualTo(3);
    }
}
