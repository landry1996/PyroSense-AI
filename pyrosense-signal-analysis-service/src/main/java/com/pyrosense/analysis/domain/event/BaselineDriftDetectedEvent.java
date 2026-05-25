package com.pyrosense.analysis.domain.event;

import com.pyrosense.analysis.domain.model.SignalFeature;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;

import java.time.Instant;
import java.util.UUID;

public record BaselineDriftDetectedEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId,
        SignalFeature feature,
        double previousMean,
        double currentMean,
        double driftPercent
) implements DomainEvent {
    @Override
    public String eventType() {
        return "analysis.baseline-drift.detected";
    }
}
