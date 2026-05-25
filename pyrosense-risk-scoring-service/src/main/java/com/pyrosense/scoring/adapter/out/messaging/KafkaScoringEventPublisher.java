package com.pyrosense.scoring.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.scoring.application.port.out.ScoringEventPublisherPort;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.event.IntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaScoringEventPublisher implements ScoringEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaScoringEventPublisher.class);

    private final KafkaTemplate<String, IntegrationEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaScoringEventPublisher(KafkaTemplate<String, IntegrationEvent> kafkaTemplate,
                                      ObjectMapper objectMapper,
                                      @Value("${pyrosense.scoring.kafka.output-topic:scoring-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            IntegrationEvent integration = IntegrationEvent.of(
                    event.eventType(), "risk-scoring-service", payload);
            kafkaTemplate.send(topic, integration)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event to {}: {}", topic, ex.getMessage(), ex);
                        }
                    });
            log.debug("Published: type={}", event.eventType());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize scoring event: {}", e.getMessage());
        }
    }
}
