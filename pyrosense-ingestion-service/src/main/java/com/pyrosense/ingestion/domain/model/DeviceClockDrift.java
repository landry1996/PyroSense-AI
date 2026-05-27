package com.pyrosense.ingestion.domain.model;

import java.time.Duration;
import java.time.Instant;

public record DeviceClockDrift(
        String deviceId,
        Instant deviceTimestamp,
        Instant serverTimestamp,
        Duration drift,
        DriftSeverity severity
) {

    public enum DriftSeverity {
        NONE,
        MINOR,
        MODERATE,
        SEVERE
    }

    private static final Duration MINOR_THRESHOLD = Duration.ofSeconds(30);
    private static final Duration MODERATE_THRESHOLD = Duration.ofMinutes(2);
    private static final Duration SEVERE_THRESHOLD = Duration.ofMinutes(5);

    public static DeviceClockDrift compute(String deviceId, Instant deviceTimestamp, Instant serverTimestamp) {
        Duration drift = Duration.between(deviceTimestamp, serverTimestamp).abs();
        DriftSeverity severity;
        if (drift.compareTo(SEVERE_THRESHOLD) >= 0) {
            severity = DriftSeverity.SEVERE;
        } else if (drift.compareTo(MODERATE_THRESHOLD) >= 0) {
            severity = DriftSeverity.MODERATE;
        } else if (drift.compareTo(MINOR_THRESHOLD) >= 0) {
            severity = DriftSeverity.MINOR;
        } else {
            severity = DriftSeverity.NONE;
        }
        return new DeviceClockDrift(deviceId, deviceTimestamp, serverTimestamp, drift, severity);
    }

    public boolean requiresAlert() {
        return severity == DriftSeverity.MODERATE || severity == DriftSeverity.SEVERE;
    }
}
