package com.pyrosense.notification.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.notification.adapter.in.websocket.WebSocketEventBroadcaster;
import com.pyrosense.notification.application.port.in.ProcessNotificationEventUseCase;
import com.pyrosense.notification.application.port.in.ProcessNotificationEventUseCase.*;
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

    private final ProcessNotificationEventUseCase eventUseCase;
    private final ObjectMapper objectMapper;
    private final WebSocketEventBroadcaster wsBroadcaster;

    public KafkaAlertEventListener(ProcessNotificationEventUseCase eventUseCase,
                                   ObjectMapper objectMapper,
                                   WebSocketEventBroadcaster wsBroadcaster) {
        this.eventUseCase = eventUseCase;
        this.objectMapper = objectMapper;
        this.wsBroadcaster = wsBroadcaster;
    }

    @KafkaListener(topics = "${pyrosense.notification.kafka.alerting-topic:alerting-events}",
            groupId = "${spring.kafka.consumer.group-id:notification-group}")
    public void onAlertEvent(IntegrationEvent event) {
        broadcastAndProcess(event);
    }

    @KafkaListener(topics = "${pyrosense.notification.kafka.scoring-topic:scoring-events}",
            groupId = "${spring.kafka.consumer.group-id:notification-group}")
    public void onScoringEvent(IntegrationEvent event) {
        broadcastAndProcess(event);
    }

    @KafkaListener(topics = "${pyrosense.notification.kafka.maintenance-topic:maintenance-events}",
            groupId = "${spring.kafka.consumer.group-id:notification-group}")
    public void onMaintenanceEvent(IntegrationEvent event) {
        broadcastAndProcess(event);
    }

    @KafkaListener(topics = "${pyrosense.notification.kafka.reporting-topic:reporting-events}",
            groupId = "${spring.kafka.consumer.group-id:notification-group}")
    public void onReportingEvent(IntegrationEvent event) {
        broadcastAndProcess(event);
    }

    @KafkaListener(topics = "${pyrosense.notification.kafka.device-topic:device-events}",
            groupId = "${spring.kafka.consumer.group-id:notification-group}")
    public void onDeviceEvent(IntegrationEvent event) {
        broadcastAndProcess(event);
    }

    private void broadcastAndProcess(IntegrationEvent event) {
        String tenantId = extractTenantId(event);
        wsBroadcaster.broadcastFromKafkaEvent(event.eventType(), tenantId, event.payload());

        try {
            processEvent(event);
        } catch (Exception e) {
            log.error("Failed to process event type={}: {}", event.eventType(), e.getMessage(), e);
        }
    }

    private void processEvent(IntegrationEvent event) throws Exception {
        JsonNode payload = objectMapper.readTree(event.payload());
        String eventType = event.eventType();

        switch (eventType) {
            case "alerting.alert.created" -> processAlertCreated(payload);
            case "alerting.alert.escalated" -> processAlertEscalated(payload);
            case "scoring.critical.risk.detected" -> processCriticalRisk(payload);
            case "maintenance.intervention.created" -> processInterventionCreated(payload);
            case "maintenance.intervention.assigned" -> processInterventionAssigned(payload);
            case "maintenance.intervention.completed" -> processInterventionCompleted(payload);
            case "reporting.report.generated" -> processReportGenerated(payload);
            case "device.offline.detected" -> processDeviceOffline(payload);
            default -> log.debug("Ignoring unhandled event type: {}", eventType);
        }
    }

    private void processAlertCreated(JsonNode payload) {
        eventUseCase.processAlertCreated(new AlertEventCommand(
                parseTenantId(payload),
                field(payload, "alertId"),
                field(payload, "deviceId"),
                parseSeverity(payload),
                fieldOrDefault(payload, "type", "UNKNOWN"),
                fieldOrDefault(payload, "occurredAt", "")
        ));
    }

    private void processAlertEscalated(JsonNode payload) {
        eventUseCase.processAlertEscalated(new AlertEventCommand(
                parseTenantId(payload),
                field(payload, "alertId"),
                field(payload, "deviceId"),
                parseSeverity(payload),
                fieldOrDefault(payload, "type", "UNKNOWN"),
                fieldOrDefault(payload, "occurredAt", "")
        ));
    }

    private void processCriticalRisk(JsonNode payload) {
        eventUseCase.processCriticalRiskDetected(new CriticalRiskCommand(
                parseTenantId(payload),
                field(payload, "deviceId"),
                fieldOrDefault(payload, "buildingId", ""),
                payload.has("riskScore") ? payload.get("riskScore").asDouble() : 0,
                fieldOrDefault(payload, "occurredAt", "")
        ));
    }

    private void processInterventionCreated(JsonNode payload) {
        eventUseCase.processInterventionCreated(new InterventionEventCommand(
                parseTenantId(payload),
                field(payload, "interventionId"),
                fieldOrDefault(payload, "buildingId", ""),
                fieldOrDefault(payload, "type", "PREVENTIVE"),
                fieldOrDefault(payload, "occurredAt", "")
        ));
    }

    private void processInterventionAssigned(JsonNode payload) {
        eventUseCase.processInterventionAssigned(new InterventionAssignedCommand(
                parseTenantId(payload),
                field(payload, "interventionId"),
                fieldOrDefault(payload, "buildingId", ""),
                fieldOrDefault(payload, "assigneeId", ""),
                fieldOrDefault(payload, "type", "PREVENTIVE"),
                fieldOrDefault(payload, "occurredAt", "")
        ));
    }

    private void processInterventionCompleted(JsonNode payload) {
        eventUseCase.processInterventionCompleted(new InterventionEventCommand(
                parseTenantId(payload),
                field(payload, "interventionId"),
                fieldOrDefault(payload, "buildingId", ""),
                fieldOrDefault(payload, "type", "PREVENTIVE"),
                fieldOrDefault(payload, "occurredAt", "")
        ));
    }

    private void processReportGenerated(JsonNode payload) {
        eventUseCase.processReportGenerated(new ReportGeneratedCommand(
                parseTenantId(payload),
                field(payload, "reportId"),
                fieldOrDefault(payload, "reportNumber", ""),
                fieldOrDefault(payload, "type", "MONTHLY_HEALTH"),
                fieldOrDefault(payload, "buildingId", ""),
                fieldOrDefault(payload, "occurredAt", "")
        ));
    }

    private void processDeviceOffline(JsonNode payload) {
        eventUseCase.processDeviceOffline(new DeviceOfflineCommand(
                parseTenantId(payload),
                field(payload, "deviceId"),
                fieldOrDefault(payload, "buildingId", ""),
                fieldOrDefault(payload, "occurredAt", "")
        ));
    }

    private TenantId parseTenantId(JsonNode payload) {
        String raw = field(payload, "tenantId");
        return new TenantId(UUID.fromString(raw));
    }

    private AlertSeverity parseSeverity(JsonNode payload) {
        String severity = fieldOrDefault(payload, "severity", "WARNING");
        try {
            return AlertSeverity.valueOf(severity);
        } catch (IllegalArgumentException e) {
            return AlertSeverity.WARNING;
        }
    }

    private String extractTenantId(IntegrationEvent event) {
        try {
            JsonNode node = objectMapper.readTree(event.payload());
            return field(node, "tenantId");
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String field(JsonNode node, String fieldName) {
        if (node.has(fieldName)) {
            JsonNode f = node.get(fieldName);
            if (f.isObject() && f.has("value")) {
                return f.get("value").asText();
            }
            return f.asText();
        }
        return null;
    }

    private String fieldOrDefault(JsonNode node, String fieldName, String defaultValue) {
        String value = field(node, fieldName);
        return value != null ? value : defaultValue;
    }
}
