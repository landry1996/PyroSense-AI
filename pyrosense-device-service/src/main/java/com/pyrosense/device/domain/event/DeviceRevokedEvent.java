package com.pyrosense.device.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;

import java.time.Instant;
import java.util.UUID;

public record DeviceRevokedEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId,
        String reason
) implements DomainEvent {

    @Override
    public String eventType() {
        return "device.revoked";
    }
}
