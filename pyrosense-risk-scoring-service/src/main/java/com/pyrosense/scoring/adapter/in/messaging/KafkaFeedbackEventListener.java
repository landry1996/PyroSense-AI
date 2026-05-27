package com.pyrosense.scoring.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.scoring.application.port.in.ProcessFieldFeedbackUseCase;
import com.pyrosense.scoring.domain.model.FeedbackOutcome;
import com.pyrosense.shared.event.IntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KafkaFeedbackEventListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaFeedbackEventListener.class);

    private final ProcessFieldFeedbackUseCase feedbackUseCase;
    private final ObjectMapper objectMapper;

    public KafkaFeedbackEventListener(ProcessFieldFeedbackUseCase feedbackUseCase,
                                       ObjectMapper objectMapper) {
        this.feedbackUseCase = feedbackUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${pyrosense.scoring.kafka.feedback-topic:maintenance-events}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onMaintenanceEvent(IntegrationEvent event) {
        String eventType = event.eventType();
        if (!"maintenance.intervention.completed".equals(eventType)
                && !"maintenance.defect.confirmed".equals(eventType)
                && !"maintenance.false_positive.confirmed".equals(eventType)) {
            return;
        }

        try {
            var command = parseCommand(event);
            if (command != null) {
                feedbackUseCase.process(command);
            }
        } catch (Exception e) {
            log.error("Failed to process feedback event: type={} error={}", eventType, e.getMessage(), e);
        }
    }

    private ProcessFieldFeedbackUseCase.FieldFeedbackCommand parseCommand(IntegrationEvent event) {
        try {
            JsonNode node = objectMapper.readTree(event.payload());

            String deviceId = node.has("deviceId") ? node.get("deviceId").asText() : null;
            if (deviceId == null) return null;

            String tenantId = event.tenantId() != null ? event.tenantId() : "unknown";
            UUID alertId = node.has("alertId") && !node.get("alertId").isNull()
                    ? UUID.fromString(node.get("alertId").asText()) : null;
            UUID interventionId = node.has("interventionId") && !node.get("interventionId").isNull()
                    ? UUID.fromString(node.get("interventionId").asText()) : null;

            FeedbackOutcome outcome = mapOutcome(event.eventType(), node);
            String anomalyType = node.has("anomalyType") ? node.get("anomalyType").asText() : "UNKNOWN";
            double riskScore = node.has("riskScoreAtAlert") ? node.get("riskScoreAtAlert").asDouble() : 0;
            String comment = node.has("comment") ? node.get("comment").asText() : null;

            return new ProcessFieldFeedbackUseCase.FieldFeedbackCommand(
                    deviceId, tenantId, alertId, interventionId,
                    outcome, anomalyType, riskScore, "maintenance-service", comment);
        } catch (Exception e) {
            log.warn("Failed to parse feedback event: {}", e.getMessage());
            return null;
        }
    }

    private FeedbackOutcome mapOutcome(String eventType, JsonNode node) {
        if ("maintenance.false_positive.confirmed".equals(eventType)) {
            return FeedbackOutcome.FALSE_POSITIVE;
        }
        if ("maintenance.defect.confirmed".equals(eventType)) {
            return FeedbackOutcome.CONFIRMED_DEFECT;
        }

        String result = node.has("result") ? node.get("result").asText() : "";
        return switch (result) {
            case "DEFECT_CONFIRMED", "ELECTRICAL_DEFECT" -> FeedbackOutcome.CONFIRMED_DEFECT;
            case "FALSE_POSITIVE" -> FeedbackOutcome.FALSE_POSITIVE;
            case "INCONCLUSIVE" -> FeedbackOutcome.INCONCLUSIVE;
            case "NO_DEFECT_FOUND" -> FeedbackOutcome.NO_DEFECT_FOUND;
            default -> FeedbackOutcome.INCONCLUSIVE;
        };
    }
}
