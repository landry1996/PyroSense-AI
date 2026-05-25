package com.pyrosense.analysis.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;

import java.time.Instant;
import java.util.UUID;

public record BaselineBuiltEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId,
        int sampleCount
) implements DomainEvent {
    @Override
    public String eventType() {
        return "analysis.baseline.built";
    }
}
