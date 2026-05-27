package com.pyrosense.ingestion.domain.event;

import com.pyrosense.ingestion.domain.model.SignalQualityScore;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record LowSignalQualityDetectedEvent(
        UUID eventId,
        Instant occurredAt,
        DeviceId deviceId,
        TenantId tenantId,
        double signalQuality,
        SignalQualityScore.QualityLevel qualityLevel
) implements DomainEvent {

    @Override
    public String eventType() {
        return "ingestion.device.low_signal_quality";
    }
}
