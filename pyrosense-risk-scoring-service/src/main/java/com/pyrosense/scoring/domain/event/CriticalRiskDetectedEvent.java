package com.pyrosense.scoring.domain.event;

import com.pyrosense.scoring.domain.model.RiskFactor;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CriticalRiskDetectedEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId,
        ElectricalPanelId panelId,
        int score,
        Duration estimatedIncidentWindow,
        List<RiskFactor> topFactors,
        String recommendation
) implements DomainEvent {
    @Override
    public String eventType() {
        return "scoring.critical-risk.detected";
    }
}
