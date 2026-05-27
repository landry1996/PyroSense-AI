package com.pyrosense.ingestion.domain.event;

import com.pyrosense.ingestion.domain.model.DeviceClockDrift;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record DeviceClockDriftDetectedEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId,
        TenantId tenantId,
        Duration drift,
        DeviceClockDrift.DriftSeverity severity
) implements DomainEvent {

    @Override
    public String eventType() {
        return "ingestion.device.clock_drift_detected";
    }
}
