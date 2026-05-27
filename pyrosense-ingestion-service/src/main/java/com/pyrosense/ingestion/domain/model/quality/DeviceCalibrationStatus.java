package com.pyrosense.ingestion.domain.model.quality;

import java.time.Duration;
import java.time.Instant;

public record DeviceCalibrationStatus(
        boolean calibrated,
        Instant lastCalibrationDate,
        String calibrationMethod
) {
    private static final Duration MAX_CALIBRATION_AGE = Duration.ofDays(365);

    public static DeviceCalibrationStatus unknown() {
        return new DeviceCalibrationStatus(false, null, null);
    }

    public static DeviceCalibrationStatus calibrated(Instant date, String method) {
        return new DeviceCalibrationStatus(true, date, method);
    }

    public boolean isExpired() {
        if (!calibrated || lastCalibrationDate == null) return true;
        return Duration.between(lastCalibrationDate, Instant.now()).compareTo(MAX_CALIBRATION_AGE) > 0;
    }

    public boolean isValid() {
        return calibrated && !isExpired();
    }
}
