package com.pyrosense.shared.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Envelope for events crossing bounded context boundaries via Kafka.
 * Contains serialized payload for loose coupling between services.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IntegrationEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String source,
        String payload
) {

    public IntegrationEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
    }

    public static IntegrationEvent of(String eventType, String source, String payload) {
        return new IntegrationEvent(UUID.randomUUID(), eventType, Instant.now(), source, payload);
    }
}
