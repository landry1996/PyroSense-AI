package com.pyrosense.device.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;

import java.time.Instant;
import java.util.UUID;

public record DeviceActivatedEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId
) implements DomainEvent {

    @Override
    public String eventType() {
        return "device.activated";
    }
}
