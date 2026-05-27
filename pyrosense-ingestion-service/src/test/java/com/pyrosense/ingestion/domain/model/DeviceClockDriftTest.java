package com.pyrosense.ingestion.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class DeviceClockDriftTest {

    @Test
    @DisplayName("no drift when timestamps are close")
    void noDrift() {
        Instant server = Instant.now();
        Instant device = server.minusSeconds(5);
        var drift = DeviceClockDrift.compute("device-1", device, server);

        assertThat(drift.severity()).isEqualTo(DeviceClockDrift.DriftSeverity.NONE);
        assertThat(drift.requiresAlert()).isFalse();
        assertThat(drift.drift()).isLessThan(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("minor drift between 30s and 2min")
    void minorDrift() {
        Instant server = Instant.now();
        Instant device = server.minusSeconds(60);
        var drift = DeviceClockDrift.compute("device-1", device, server);

        assertThat(drift.severity()).isEqualTo(DeviceClockDrift.DriftSeverity.MINOR);
        assertThat(drift.requiresAlert()).isFalse();
    }

    @Test
    @DisplayName("moderate drift triggers alert")
    void moderateDrift() {
        Instant server = Instant.now();
        Instant device = server.minusSeconds(180);
        var drift = DeviceClockDrift.compute("device-1", device, server);

        assertThat(drift.severity()).isEqualTo(DeviceClockDrift.DriftSeverity.MODERATE);
        assertThat(drift.requiresAlert()).isTrue();
    }

    @Test
    @DisplayName("severe drift triggers alert")
    void severeDrift() {
        Instant server = Instant.now();
        Instant device = server.minusSeconds(400);
        var drift = DeviceClockDrift.compute("device-1", device, server);

        assertThat(drift.severity()).isEqualTo(DeviceClockDrift.DriftSeverity.SEVERE);
        assertThat(drift.requiresAlert()).isTrue();
    }

    @Test
    @DisplayName("future device timestamp also counts as drift")
    void futureTimestamp() {
        Instant server = Instant.now();
        Instant device = server.plusSeconds(350);
        var drift = DeviceClockDrift.compute("device-1", device, server);

        assertThat(drift.severity()).isEqualTo(DeviceClockDrift.DriftSeverity.SEVERE);
        assertThat(drift.requiresAlert()).isTrue();
    }
}
