package com.pyrosense.ingestion.domain.model;

import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.Objects;

public record DeviceHeartbeat(
        DeviceId deviceId,
        TenantId tenantId,
        Instant timestamp,
        String firmwareVersion,
        double uptimeHours,
        double memoryUsagePercent,
        double cpuTemperatureCelsius
) {
    public DeviceHeartbeat {
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(timestamp);
    }
}
