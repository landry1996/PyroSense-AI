package com.pyrosense.analysis.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.analysis.application.port.out.AnalysisEventPublisherPort;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.event.IntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaAnalysisEventPublisher implements AnalysisEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaAnalysisEventPublisher.class);

    private final KafkaTemplate<String, IntegrationEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaAnalysisEventPublisher(KafkaTemplate<String, IntegrationEvent> kafkaTemplate,
                                       ObjectMapper objectMapper,
                                       @Value("${pyrosense.analysis.kafka.output-topic:analysis-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            IntegrationEvent integrationEvent = IntegrationEvent.of(
                    event.eventType(), "signal-analysis-service", payload);
            kafkaTemplate.send(topic, integrationEvent);
            log.debug("Published event: type={}", event.eventType());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", e.getMessage());
        }
    }
}
