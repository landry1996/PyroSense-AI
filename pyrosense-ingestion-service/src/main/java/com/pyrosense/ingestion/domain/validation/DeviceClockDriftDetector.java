package com.pyrosense.ingestion.domain.validation;

import com.pyrosense.ingestion.domain.model.DeviceClockDrift;

import java.time.Instant;

public final class DeviceClockDriftDetector {

    private DeviceClockDriftDetector() {}

    public static DeviceClockDrift detect(String deviceId, Instant deviceTimestamp) {
        Instant serverTimestamp = Instant.now();
        return DeviceClockDrift.compute(deviceId, deviceTimestamp, serverTimestamp);
    }

    public static DeviceClockDrift detect(String deviceId, Instant deviceTimestamp, Instant serverTimestamp) {
        return DeviceClockDrift.compute(deviceId, deviceTimestamp, serverTimestamp);
    }
}
