package com.pyrosense.notification.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.notification.application.port.out.NotificationEventPublisherPort;
import com.pyrosense.notification.config.NotificationKafkaProperties;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.event.IntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaNotificationEventPublisher implements NotificationEventPublisherPort {

    private static final String SOURCE_SERVICE = "pyrosense-notification-service";
    private static final Logger log = LoggerFactory.getLogger(KafkaNotificationEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaNotificationEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                            ObjectMapper objectMapper,
                                            NotificationKafkaProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = properties.outputTopic();
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            IntegrationEvent integrationEvent = IntegrationEvent.builder()
                    .eventId(event.eventId())
                    .eventType(event.eventType())
                    .version(1)
                    .occurredAt(event.occurredAt())
                    .sourceService(SOURCE_SERVICE)
                    .causationId(event.eventId().toString())
                    .payload(payload)
                    .build();

            kafkaTemplate.send(topic, event.eventId().toString(), integrationEvent)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event {} to {}: {}",
                                    event.eventType(), topic, ex.getMessage(), ex);
                        } else {
                            log.info("Published event={} eventId={} to topic={}",
                                    event.eventType(), event.eventId(), topic);
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to serialize event: {} eventId={}", event.eventType(), event.eventId(), e);
        }
    }
}
