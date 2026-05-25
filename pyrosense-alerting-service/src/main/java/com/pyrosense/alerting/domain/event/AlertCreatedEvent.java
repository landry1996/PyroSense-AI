package com.pyrosense.alerting.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.valueobject.AlertSeverity;
import com.pyrosense.alerting.domain.model.AlertType;

import java.time.Instant;
import java.util.UUID;

public record AlertCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        AlertId alertId,
        TenantId tenantId,
        DeviceId deviceId,
        AlertSeverity severity,
        AlertType type
) implements DomainEvent {
    @Override
    public String eventType() {
        return "alerting.alert.created";
    }
}
