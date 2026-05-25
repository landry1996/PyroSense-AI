package com.pyrosense.maintenance.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.maintenance.application.port.in.CreateInterventionUseCase;
import com.pyrosense.maintenance.application.port.in.CreateInterventionUseCase.CreateInterventionCommand;
import com.pyrosense.shared.event.IntegrationEvent;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KafkaAlertEventListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaAlertEventListener.class);

    private final CreateInterventionUseCase createInterventionUseCase;
    private final ObjectMapper objectMapper;

    public KafkaAlertEventListener(CreateInterventionUseCase createInterventionUseCase,
                                   ObjectMapper objectMapper) {
        this.createInterventionUseCase = createInterventionUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${pyrosense.maintenance.kafka.alerting-topic:alerting-events}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onAlertEvent(IntegrationEvent event) {
        if (!"alerting.alert.created".equals(event.eventType())) {
            return;
        }

        try {
            JsonNode payload = objectMapper.readTree(event.payload());
            String severity = payload.path("severity").asText();

            if ("CRITICAL".equalsIgnoreCase(severity) || "WARNING".equalsIgnoreCase(severity)) {
                var command = new CreateInterventionCommand(
                        new TenantId(UUID.fromString(payload.path("tenantId").path("value").asText())),
                        new AlertId(UUID.fromString(payload.path("alertId").path("value").asText())),
                        new DeviceId(UUID.fromString(payload.path("deviceId").path("value").asText())),
                        severity,
                        payload.path("type").asText(),
                        buildDescription(severity, payload.path("type").asText())
                );
                createInterventionUseCase.createFromAlert(command);
                log.info("Auto-created intervention for {} alert: {}", severity,
                        payload.path("alertId").path("value").asText());
            }
        } catch (Exception e) {
            log.error("Failed to process alert event: {}", event.eventId(), e);
        }
    }

    private String buildDescription(String severity, String alertType) {
        return "Auto-generated intervention for %s alert [%s]".formatted(severity, alertType);
    }
}
