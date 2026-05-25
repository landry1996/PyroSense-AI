package com.pyrosense.ingestion.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record HeartbeatReceivedEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId,
        TenantId tenantId,
        String firmwareVersion
) implements DomainEvent {

    @Override
    public String eventType() {
        return "ingestion.heartbeat.received";
    }
}
