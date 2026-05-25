package com.pyrosense.analysis.domain.event;

import com.pyrosense.analysis.domain.model.SignalAnomaly;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SignalAnomalyDetectedEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId,
        TenantId tenantId,
        List<SignalAnomaly> anomalies,
        double aggregateRiskScore
) implements DomainEvent {
    @Override
    public String eventType() {
        return "analysis.signal-anomaly.detected";
    }
}
