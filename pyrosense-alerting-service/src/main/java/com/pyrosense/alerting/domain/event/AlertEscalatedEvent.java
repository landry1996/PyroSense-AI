package com.pyrosense.alerting.domain.event;

import com.pyrosense.alerting.domain.model.EscalationLevel;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.valueobject.AlertSeverity;

import java.time.Instant;
import java.util.UUID;

public record AlertEscalatedEvent(
        UUID eventId,
        Instant occurredAt,
        AlertId alertId,
        EscalationLevel previousLevel,
        EscalationLevel newLevel,
        AlertSeverity severity
) implements DomainEvent {
    @Override
    public String eventType() {
        return "alerting.alert.escalated";
    }
}
