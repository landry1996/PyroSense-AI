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
        int version,
        Instant occurredAt,
        String sourceService,
        String tenantId,
        String correlationId,
        String causationId,
        String payload
) {

    public IntegrationEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(sourceService, "sourceService must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        if (correlationId == null) correlationId = eventId.toString();
        if (causationId == null) causationId = eventId.toString();
        if (tenantId == null) tenantId = "unknown";
    }

    /**
     * @deprecated Use the builder or full constructor instead.
     */
    @Deprecated
    public IntegrationEvent(UUID eventId, String eventType, Instant occurredAt,
                            String source, String payload) {
        this(eventId, eventType, 1, occurredAt, source, null, null, null, payload);
    }

    public static IntegrationEvent of(String eventType, String source, String payload) {
        return new IntegrationEvent(UUID.randomUUID(), eventType, 1, Instant.now(),
                source, null, null, null, payload);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID eventId = UUID.randomUUID();
        private String eventType;
        private int version = 1;
        private Instant occurredAt = Instant.now();
        private String sourceService;
        private String tenantId;
        private String correlationId;
        private String causationId;
        private String payload;

        public Builder eventId(UUID eventId) { this.eventId = eventId; return this; }
        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        public Builder version(int version) { this.version = version; return this; }
        public Builder occurredAt(Instant occurredAt) { this.occurredAt = occurredAt; return this; }
        public Builder sourceService(String sourceService) { this.sourceService = sourceService; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public Builder causationId(String causationId) { this.causationId = causationId; return this; }
        public Builder payload(String payload) { this.payload = payload; return this; }

        public IntegrationEvent build() {
            return new IntegrationEvent(eventId, eventType, version, occurredAt,
                    sourceService, tenantId, correlationId, causationId, payload);
        }
    }
}
