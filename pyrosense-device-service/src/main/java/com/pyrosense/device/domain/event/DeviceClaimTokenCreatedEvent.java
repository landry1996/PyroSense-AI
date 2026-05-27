package com.pyrosense.device.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record DeviceClaimTokenCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId,
        TenantId tenantId,
        Instant expiresAt
) implements DomainEvent {

    @Override
    public String eventType() {
        return "device.claim_token.created";
    }
}
