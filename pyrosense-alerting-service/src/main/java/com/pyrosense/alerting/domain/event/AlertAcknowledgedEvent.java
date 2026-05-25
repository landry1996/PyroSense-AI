package com.pyrosense.alerting.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.UserId;

import java.time.Instant;
import java.util.UUID;

public record AlertAcknowledgedEvent(
        UUID eventId,
        Instant occurredAt,
        AlertId alertId,
        UserId acknowledgedBy
) implements DomainEvent {
    @Override
    public String eventType() {
        return "alerting.alert.acknowledged";
    }
}
