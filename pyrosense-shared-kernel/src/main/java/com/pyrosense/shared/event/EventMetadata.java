package com.pyrosense.shared.event;

import java.util.UUID;

public record EventMetadata(
        String correlationId,
        String causationId,
        String tenantId,
        String sourceService
) {
    public static EventMetadata from(IntegrationEvent event) {
        return new EventMetadata(
                event.correlationId(),
                event.eventId().toString(),
                event.tenantId(),
                event.sourceService()
        );
    }

    public static EventMetadata withCorrelation(String correlationId, String tenantId, String sourceService) {
        return new EventMetadata(correlationId, UUID.randomUUID().toString(), tenantId, sourceService);
    }
}
