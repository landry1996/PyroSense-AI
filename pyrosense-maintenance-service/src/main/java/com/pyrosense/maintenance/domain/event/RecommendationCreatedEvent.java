package com.pyrosense.maintenance.domain.event;

import com.pyrosense.maintenance.domain.model.InterventionPriority;
import com.pyrosense.maintenance.domain.model.InterventionType;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.TenantId;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record RecommendationCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID recommendationId,
        TenantId tenantId,
        AlertId alertId,
        InterventionType suggestedType,
        InterventionPriority suggestedPriority,
        Duration slaDeadline
) implements DomainEvent {
    @Override
    public String eventType() {
        return "maintenance.recommendation.created";
    }
}
