package com.pyrosense.maintenance.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record MaintenanceInterventionStartedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID interventionId,
        TenantId tenantId,
        DeviceId deviceId
) implements DomainEvent {
    @Override
    public String eventType() {
        return "maintenance.intervention.started";
    }
}
