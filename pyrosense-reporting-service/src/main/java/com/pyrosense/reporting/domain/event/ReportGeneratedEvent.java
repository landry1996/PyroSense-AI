package com.pyrosense.reporting.domain.event;

import com.pyrosense.reporting.domain.model.ReportType;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record ReportGeneratedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID reportId,
        String reportNumber,
        TenantId tenantId,
        BuildingId buildingId,
        ReportType type
) implements DomainEvent {
    @Override
    public String eventType() {
        return "reporting.report.generated";
    }
}
