package com.pyrosense.device.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.device.application.port.out.DeviceEventPublisherPort;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.event.IntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KafkaDeviceEventPublisher implements DeviceEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaDeviceEventPublisher.class);
    private static final String TOPIC = "device-events";
    private static final String SOURCE = "device-service";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaDeviceEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                     ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            try {
                String payload = objectMapper.writeValueAsString(event);
                var integrationEvent = new IntegrationEvent(
                        event.eventId(),
                        event.eventType(),
                        event.occurredAt(),
                        SOURCE,
                        payload
                );
                String message = objectMapper.writeValueAsString(integrationEvent);
                kafkaTemplate.send(TOPIC, event.eventId().toString(), message);
                log.debug("Published event: {} [{}]", event.eventType(), event.eventId());
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize event: {}", event.eventType(), e);
            }
        }
    }
}
