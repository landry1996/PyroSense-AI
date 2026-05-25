package com.pyrosense.notification.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.notification.application.port.in.SendNotificationUseCase;
import com.pyrosense.notification.application.port.in.SendNotificationUseCase.DispatchAlertNotificationCommand;
import com.pyrosense.shared.event.IntegrationEvent;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.valueobject.AlertSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KafkaAlertEventListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaAlertEventListener.class);

    private final SendNotificationUseCase sendUseCase;
    private final ObjectMapper objectMapper;

    public KafkaAlertEventListener(SendNotificationUseCase sendUseCase, ObjectMapper objectMapper) {
        this.sendUseCase = sendUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${pyrosense.notification.kafka.alerting-topic:alerting-events}",
            groupId = "${spring.kafka.consumer.group-id:notification-group}")
    public void onAlertEvent(IntegrationEvent event) {
        if (!"alerting.alert.created".equals(event.eventType())) {
            return;
        }

        try {
            JsonNode payload = objectMapper.readTree(event.payload());
            String alertId = extractField(payload, "alertId");
            String tenantId = extractField(payload, "tenantId");
            String deviceId = extractField(payload, "deviceId");
            String severity = extractField(payload, "severity");
            String alertType = extractField(payload, "type");
            String occurredAt = extractField(payload, "occurredAt");

            var command = new DispatchAlertNotificationCommand(
                    new TenantId(UUID.fromString(tenantId)),
                    alertId,
                    deviceId,
                    AlertSeverity.valueOf(severity),
                    alertType != null ? alertType : "UNKNOWN",
                    occurredAt != null ? occurredAt : event.occurredAt().toString()
            );

            sendUseCase.dispatchForAlert(command);
            log.debug("Processed alert event: alertId={} severity={}", alertId, severity);
        } catch (Exception e) {
            log.error("Failed to process alert event: {}", e.getMessage(), e);
        }
    }

    private String extractField(JsonNode node, String fieldName) {
        if (node.has(fieldName)) {
            JsonNode field = node.get(fieldName);
            if (field.isObject() && field.has("value")) {
                return field.get("value").asText();
            }
            return field.asText();
        }
        return null;
    }
}
