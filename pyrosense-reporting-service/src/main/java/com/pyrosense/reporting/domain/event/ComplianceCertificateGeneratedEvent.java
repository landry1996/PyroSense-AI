package com.pyrosense.reporting.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record ComplianceCertificateGeneratedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID reportId,
        String reportNumber,
        TenantId tenantId,
        BuildingId buildingId,
        Instant periodStart,
        Instant periodEnd,
        String signatureHash
) implements DomainEvent {
    @Override
    public String eventType() {
        return "reporting.compliance_certificate.generated";
    }
}
