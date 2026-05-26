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

@Component
public class KafkaMaintenanceEventPublisher implements MaintenanceEventPublisherPort {

    private static final String SOURCE_SERVICE = "pyrosense-maintenance-service";
    private static final Logger log = LoggerFactory.getLogger(KafkaMaintenanceEventPublisher.class);

    private final KafkaTemplate<String, IntegrationEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaMaintenanceEventPublisher(KafkaTemplate<String, IntegrationEvent> kafkaTemplate,
                                          ObjectMapper objectMapper,
                                          @Value("${pyrosense.maintenance.kafka.output-topic:pyrosense.maintenance.events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(DomainEvent event) {
        publish(event, null, null);
    }

    public void publish(DomainEvent event, String correlationId, String tenantId) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            IntegrationEvent integrationEvent = IntegrationEvent.builder()
                    .eventId(event.eventId())
                    .eventType(event.eventType())
                    .version(1)
                    .occurredAt(event.occurredAt())
                    .sourceService(SOURCE_SERVICE)
                    .tenantId(tenantId)
                    .correlationId(correlationId)
                    .causationId(event.eventId().toString())
                    .payload(payload)
                    .build();

            kafkaTemplate.send(topic, event.eventId().toString(), integrationEvent)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event {} to {}: {}",
                                    event.eventType(), topic, ex.getMessage(), ex);
                        } else {
                            log.info("Published event={} eventId={} to topic={} partition={} offset={}",
                                    event.eventType(), event.eventId(), topic,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to serialize event: {} eventId={}", event.eventType(), event.eventId(), e);
        }
    }
}
