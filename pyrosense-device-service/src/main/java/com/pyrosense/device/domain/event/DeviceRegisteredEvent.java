package com.pyrosense.device.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;

import java.time.Instant;
import java.util.UUID;

public record DeviceRegisteredEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId,
        String serialNumber
) implements DomainEvent {

    @Override
    public String eventType() {
        return "device.registered";
    }
}
