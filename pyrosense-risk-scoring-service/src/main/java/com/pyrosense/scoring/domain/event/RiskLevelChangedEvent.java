package com.pyrosense.scoring.domain.event;

import com.pyrosense.scoring.domain.model.RiskLevel;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;

import java.time.Instant;
import java.util.UUID;

public record RiskLevelChangedEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId,
        RiskLevel previousLevel,
        RiskLevel newLevel,
        int score
) implements DomainEvent {
    @Override
    public String eventType() {
        return "scoring.risk-level.changed";
    }
}
