package com.pyrosense.ingestion.domain.validation;

import com.pyrosense.ingestion.domain.model.DeviceClockDrift;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class DeviceClockDriftDetectorTest {

    @Test
    @DisplayName("detect with explicit server timestamp")
    void detectWithServerTimestamp() {
        Instant server = Instant.parse("2026-05-27T10:00:00Z");
        Instant device = Instant.parse("2026-05-27T09:57:00Z");

        var drift = DeviceClockDriftDetector.detect("dev-001", device, server);

        assertThat(drift.deviceId()).isEqualTo("dev-001");
        assertThat(drift.severity()).isEqualTo(DeviceClockDrift.DriftSeverity.MODERATE);
        assertThat(drift.requiresAlert()).isTrue();
    }

    @Test
    @DisplayName("detect with current time uses now()")
    void detectWithCurrentTime() {
        Instant device = Instant.now().minusSeconds(10);
        var drift = DeviceClockDriftDetector.detect("dev-002", device);

        assertThat(drift.deviceId()).isEqualTo("dev-002");
        assertThat(drift.severity()).isEqualTo(DeviceClockDrift.DriftSeverity.NONE);
    }
}
