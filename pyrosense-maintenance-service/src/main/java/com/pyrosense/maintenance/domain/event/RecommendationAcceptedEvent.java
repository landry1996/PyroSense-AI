package com.pyrosense.maintenance.domain.event;

import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record RecommendationAcceptedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID recommendationId,
        TenantId tenantId,
        AlertId alertId,
        UUID interventionId
) implements DomainEvent {
    @Override
    public String eventType() {
        return "maintenance.recommendation.accepted";
    }
}
