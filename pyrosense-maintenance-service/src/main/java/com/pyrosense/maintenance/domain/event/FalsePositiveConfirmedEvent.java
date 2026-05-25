package com.pyrosense.maintenance.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record FalsePositiveConfirmedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID interventionId,
        TenantId tenantId,
        AlertId sourceAlertId,
        DeviceId deviceId,
        String diagnosticObservations
) implements DomainEvent {
    @Override
    public String eventType() {
        return "maintenance.false_positive.confirmed";
    }
}
