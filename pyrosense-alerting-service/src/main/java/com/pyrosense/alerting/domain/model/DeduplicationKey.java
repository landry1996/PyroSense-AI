package com.pyrosense.alerting.domain.model;

import com.pyrosense.shared.id.DeviceId;

import java.util.Objects;

public record DeduplicationKey(
        DeviceId deviceId,
        AlertType type
) {
    public DeduplicationKey {
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(type);
    }

    public String toStringKey() {
        return deviceId.value().toString() + ":" + type.name();
    }

    public static DeduplicationKey from(String key) {
        String[] parts = key.split(":");
        return new DeduplicationKey(DeviceId.from(parts[0]), AlertType.valueOf(parts[1]));
    }
}
