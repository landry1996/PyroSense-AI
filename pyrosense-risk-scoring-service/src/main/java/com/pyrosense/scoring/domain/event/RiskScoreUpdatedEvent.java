package com.pyrosense.scoring.domain.event;

import com.pyrosense.scoring.domain.model.RiskLevel;
import com.pyrosense.scoring.domain.model.RiskTrend;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;

import java.time.Instant;
import java.util.UUID;

public record RiskScoreUpdatedEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId,
        int score,
        RiskLevel level,
        RiskTrend trend
) implements DomainEvent {
    @Override
    public String eventType() {
        return "scoring.risk-score.updated";
    }
}
