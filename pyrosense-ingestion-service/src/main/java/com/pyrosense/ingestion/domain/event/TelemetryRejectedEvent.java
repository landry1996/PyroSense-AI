package com.pyrosense.ingestion.domain.event;

import com.pyrosense.ingestion.domain.model.IngestionRejectionReason;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record TelemetryRejectedEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId,
        TenantId tenantId,
        IngestionRejectionReason reason,
        String details
) implements DomainEvent {

    @Override
    public String eventType() {
        return "ingestion.telemetry.rejected";
    }
}
