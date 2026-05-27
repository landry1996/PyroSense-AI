package com.pyrosense.ingestion.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record RealDeviceTelemetryReceivedEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId,
        TenantId tenantId,
        Instant readingTimestamp,
        String firmwareVersion,
        double signalQuality,
        long sequenceNumber
) implements DomainEvent {

    @Override
    public String eventType() {
        return "ingestion.real_device.telemetry.received";
    }
}
