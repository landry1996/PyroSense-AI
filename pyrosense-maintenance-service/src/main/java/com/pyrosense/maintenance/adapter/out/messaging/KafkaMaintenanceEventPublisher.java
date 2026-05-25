package com.pyrosense.maintenance.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.maintenance.application.port.out.MaintenanceEventPublisherPort;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.event.IntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KafkaMaintenanceEventPublisher implements MaintenanceEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaMaintenanceEventPublisher.class);

    private final KafkaTemplate<String, IntegrationEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaMaintenanceEventPublisher(KafkaTemplate<String, IntegrationEvent> kafkaTemplate,
                                          ObjectMapper objectMapper,
                                          @Value("${pyrosense.maintenance.kafka.output-topic:maintenance-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            IntegrationEvent integrationEvent = new IntegrationEvent(
                    UUID.randomUUID(),
                    event.eventType(),
                    event.occurredAt(),
                    "pyrosense-maintenance-service",
                    payload
            );
            kafkaTemplate.send(topic, integrationEvent);
            log.debug("Published event: {} to topic: {}", event.eventType(), topic);
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event.eventType(), e);
        }
    }
}
