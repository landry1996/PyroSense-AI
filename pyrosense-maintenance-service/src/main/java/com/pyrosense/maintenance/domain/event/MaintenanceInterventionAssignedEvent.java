package com.pyrosense.maintenance.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

import java.time.Instant;
import java.util.UUID;

public record MaintenanceInterventionAssignedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID interventionId,
        TenantId tenantId,
        UserId electricianId,
        Instant scheduledAt
) implements DomainEvent {
    @Override
    public String eventType() {
        return "maintenance.intervention.assigned";
    }
}
