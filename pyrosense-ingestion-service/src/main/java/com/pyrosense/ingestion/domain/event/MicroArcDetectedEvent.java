package com.pyrosense.ingestion.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record MicroArcDetectedEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId,
        TenantId tenantId,
        int microArcCount,
        double hfNoiseLevel,
        Instant readingTimestamp
) implements DomainEvent {

    @Override
    public String eventType() {
        return "ingestion.micro_arc.detected";
    }
}
