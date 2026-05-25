package com.pyrosense.maintenance.domain.event;

import com.pyrosense.maintenance.domain.model.InterventionPriority;
import com.pyrosense.maintenance.domain.model.InterventionType;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record MaintenanceInterventionCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID interventionId,
        TenantId tenantId,
        AlertId sourceAlertId,
        DeviceId deviceId,
        InterventionType type,
        InterventionPriority priority
) implements DomainEvent {
    @Override
    public String eventType() {
        return "maintenance.intervention.created";
    }
}
