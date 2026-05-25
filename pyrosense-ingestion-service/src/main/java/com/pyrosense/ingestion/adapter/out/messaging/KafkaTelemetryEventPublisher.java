package com.pyrosense.ingestion.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.ingestion.application.port.out.TelemetryEventPublisherPort;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.event.IntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaTelemetryEventPublisher implements TelemetryEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaTelemetryEventPublisher.class);
    private static final String TELEMETRY_TOPIC = "telemetry-events";
    private static final String DLQ_TOPIC = "ingestion-dlq";
    private static final String SOURCE = "ingestion-service";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaTelemetryEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                         ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            var integrationEvent = new IntegrationEvent(
                    event.eventId(), event.eventType(), event.occurredAt(), SOURCE, payload);
            String message = objectMapper.writeValueAsString(integrationEvent);
            kafkaTemplate.send(TELEMETRY_TOPIC, event.eventId().toString(), message);
            log.debug("Published event: {} [{}]", event.eventType(), event.eventId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", event.eventType(), e);
        }
    }

    @Override
    public void publishToDlq(String topic, String key, String payload, String reason) {
        try {
            var dlqMessage = objectMapper.writeValueAsString(new DlqEntry(key, payload, reason));
            kafkaTemplate.send(DLQ_TOPIC, key, dlqMessage);
            log.warn("Sent to DLQ: key={} reason={}", key, reason);
        } catch (JsonProcessingException e) {
            log.error("Failed to write to DLQ: key={}", key, e);
        }
    }

    private record DlqEntry(String key, String payload, String reason) {}
}
