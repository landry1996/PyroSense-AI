package com.pyrosense.dashboard.domain.model;

import java.time.Instant;

public record DeviceTechnicalHealth(
        String deviceId,
        String serialNumber,
        String firmwareVersion,
        String hardwareRevision,
        String connectivity,
        Instant lastHeartbeat,
        long uptimeSeconds,
        double signalQuality,
        double dataQualityScore,
        String dataQualityGrade,
        Double batteryPercent,
        Double deviceTemperature,
        long clockDriftMs,
        int sequenceGaps,
        int rejectedTelemetryCount,
        String status
) {}
