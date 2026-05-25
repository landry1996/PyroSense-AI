package com.pyrosense.maintenance.domain.event;

import com.pyrosense.maintenance.domain.model.InterventionResult;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record ElectricalDefectConfirmedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID interventionId,
        TenantId tenantId,
        AlertId sourceAlertId,
        DeviceId deviceId,
        InterventionResult result,
        String diagnosticObservations
) implements DomainEvent {
    @Override
    public String eventType() {
        return "maintenance.defect.confirmed";
    }
}
