package com.pyrosense.ingestion.application.port.in;

import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;

public interface IngestHeartbeatUseCase {

    record HeartbeatCommand(
            DeviceId deviceId,
            TenantId tenantId,
            Instant timestamp,
            String firmwareVersion,
            double uptimeHours,
            double memoryUsagePercent,
            double cpuTemperatureCelsius
    ) {}

    void execute(HeartbeatCommand command);
}
