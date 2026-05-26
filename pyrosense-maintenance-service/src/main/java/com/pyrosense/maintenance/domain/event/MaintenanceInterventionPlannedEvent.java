package com.pyrosense.maintenance.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record MaintenanceInterventionPlannedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID interventionId,
        TenantId tenantId,
        Instant scheduledAt
) implements DomainEvent {
    @Override
    public String eventType() {
        return "maintenance.intervention.planned";
    }
}
