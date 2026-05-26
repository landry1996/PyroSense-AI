package com.pyrosense.maintenance.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.maintenance.application.port.in.CreateInterventionUseCase;
import com.pyrosense.maintenance.application.port.in.CreateInterventionUseCase.CreateInterventionCommand;
import com.pyrosense.maintenance.application.port.out.AuditLogPort;
import com.pyrosense.maintenance.application.port.out.MaintenanceEventPublisherPort;
import com.pyrosense.maintenance.application.port.out.RecommendationRepositoryPort;
import com.pyrosense.maintenance.domain.event.RecommendationCreatedEvent;
import com.pyrosense.maintenance.domain.model.InterventionPriority;
import com.pyrosense.maintenance.domain.model.InterventionPriorityPolicy;
import com.pyrosense.maintenance.domain.model.InterventionRecommendation;
import com.pyrosense.maintenance.domain.model.InterventionType;
import com.pyrosense.shared.event.IntegrationEvent;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class KafkaAlertEventListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaAlertEventListener.class);

    private final CreateInterventionUseCase createInterventionUseCase;
    private final RecommendationRepositoryPort recommendationRepository;
    private final MaintenanceEventPublisherPort eventPublisher;
    private final AuditLogPort auditLog;
    private final ObjectMapper objectMapper;
    private final InterventionPriorityPolicy priorityPolicy = new InterventionPriorityPolicy();

    public KafkaAlertEventListener(CreateInterventionUseCase createInterventionUseCase,
                                   RecommendationRepositoryPort recommendationRepository,
                                   MaintenanceEventPublisherPort eventPublisher,
                                   AuditLogPort auditLog,
                                   ObjectMapper objectMapper) {
        this.createInterventionUseCase = createInterventionUseCase;
        this.recommendationRepository = recommendationRepository;
        this.eventPublisher = eventPublisher;
        this.auditLog = auditLog;
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

            if ("CRITICAL".equalsIgnoreCase(severity)) {
                handleCriticalAlert(payload, severity);
            } else if ("WARNING".equalsIgnoreCase(severity)) {
                handleWarningAlert(payload, severity);
            }
        } catch (Exception e) {
            log.error("Failed to process alert event: {}", event.eventId(), e);
        }
    }

    private void handleCriticalAlert(JsonNode payload, String severity) {
        TenantId tenantId = extractTenantId(payload);
        AlertId alertId = extractAlertId(payload);

        // Idempotence: skip if already processed
        if (recommendationRepository.findByAlertId(alertId).isPresent()) {
            log.debug("Alert {} already has a recommendation, skipping", alertId.value());
            return;
        }

        var command = new CreateInterventionCommand(
                tenantId,
                alertId,
                extractDeviceId(payload),
                severity,
                payload.path("type").asText(),
                buildDescription(severity, payload.path("type").asText())
        );

        try {
            createInterventionUseCase.createFromAlert(command);
            log.info("Auto-created URGENT intervention for CRITICAL alert: {}", alertId.value());
            auditLog.log("AUTO_INTERVENTION_CREATED", tenantId,
                    "CRITICAL alert %s → auto-created URGENT intervention".formatted(alertId.value()));
        } catch (Exception e) {
            log.warn("Could not auto-create intervention for alert {}: {}", alertId.value(), e.getMessage());
        }
    }

    private void handleWarningAlert(JsonNode payload, String severity) {
        TenantId tenantId = extractTenantId(payload);
        AlertId alertId = extractAlertId(payload);
        DeviceId deviceId = extractDeviceId(payload);

        // Idempotence: skip if already processed
        if (recommendationRepository.findByAlertId(alertId).isPresent()) {
            log.debug("Alert {} already has a recommendation, skipping", alertId.value());
            return;
        }

        Integer riskScore = payload.has("riskScore") ? payload.path("riskScore").asInt() : null;
        InterventionPriority priority = priorityPolicy.determineFromAlert(severity, riskScore);
        InterventionType type = priorityPolicy.determineTypeFromAlert(severity);
        Duration sla = priorityPolicy.determineSlaDeadline(priority);

        var recommendation = new InterventionRecommendation(
                UUID.randomUUID(), tenantId, alertId, deviceId,
                type, priority,
                buildDescription(severity, payload.path("type").asText()),
                sla
        );

        recommendationRepository.save(recommendation);

        eventPublisher.publish(new RecommendationCreatedEvent(
                UUID.randomUUID(), ClockProvider.now(),
                recommendation.getId(), tenantId, alertId,
                type, priority, sla));

        log.info("Created recommendation {} for WARNING alert: {}", recommendation.getId(), alertId.value());
        auditLog.log("RECOMMENDATION_CREATED", tenantId,
                "WARNING alert %s → recommendation %s (priority=%s, SLA=%s)".formatted(
                        alertId.value(), recommendation.getId(), priority, sla));
    }

    private TenantId extractTenantId(JsonNode payload) {
        return new TenantId(UUID.fromString(payload.path("tenantId").path("value").asText()));
    }

    private AlertId extractAlertId(JsonNode payload) {
        return new AlertId(UUID.fromString(payload.path("alertId").path("value").asText()));
    }

    private DeviceId extractDeviceId(JsonNode payload) {
        return new DeviceId(UUID.fromString(payload.path("deviceId").path("value").asText()));
    }

    private String buildDescription(String severity, String alertType) {
        return "Auto-generated intervention for %s alert [%s]".formatted(severity, alertType);
    }
}
