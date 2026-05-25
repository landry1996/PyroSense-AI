package com.pyrosense.ingestion.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record TelemetryReceivedEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId,
        TenantId tenantId,
        Instant readingTimestamp,
        double rmsCurrent,
        double rmsVoltage,
        double activePower,
        double powerFactor,
        double thd,
        double temperatureCelsius,
        double hfNoiseLevel,
        int microArcCount,
        int transientCount
) implements DomainEvent {

    @Override
    public String eventType() {
        return "ingestion.telemetry.received";
    }
}
