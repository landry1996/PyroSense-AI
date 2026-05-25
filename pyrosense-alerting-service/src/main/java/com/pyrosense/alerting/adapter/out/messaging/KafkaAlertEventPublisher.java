package com.pyrosense.alerting.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.alerting.application.port.out.AlertEventPublisherPort;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.event.IntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KafkaAlertEventPublisher implements AlertEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaAlertEventPublisher.class);

    private final KafkaTemplate<String, IntegrationEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaAlertEventPublisher(KafkaTemplate<String, IntegrationEvent> kafkaTemplate,
                                     ObjectMapper objectMapper,
                                     @Value("${pyrosense.alerting.kafka.output-topic:alerting-events}") String topic) {
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
                    "pyrosense-alerting-service",
                    payload
            );
            kafkaTemplate.send(topic, integrationEvent)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event to {}: {}", topic, ex.getMessage(), ex);
                        }
                    });
            log.debug("Published event: {} to topic: {}", event.eventType(), topic);
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event.eventType(), e);
        }
    }
}
