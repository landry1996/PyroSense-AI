package com.pyrosense.alerting.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.alerting.application.port.in.CreateAlertUseCase;
import com.pyrosense.alerting.application.port.in.CreateAlertUseCase.CreateAlertCommand;
import com.pyrosense.alerting.domain.model.AlertType;
import com.pyrosense.shared.event.IntegrationEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.valueobject.AlertSeverity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KafkaScoringEventListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaScoringEventListener.class);

    private final CreateAlertUseCase createAlertUseCase;
    private final ObjectMapper objectMapper;
    private final Counter eventsReceived;
    private final Counter alertsCreated;

    public KafkaScoringEventListener(CreateAlertUseCase createAlertUseCase,
                                      ObjectMapper objectMapper,
                                      MeterRegistry registry) {
        this.createAlertUseCase = createAlertUseCase;
        this.objectMapper = objectMapper;
        this.eventsReceived = Counter.builder("alerting.events.received").register(registry);
        this.alertsCreated = Counter.builder("alerting.alerts.created").register(registry);
    }

    @KafkaListener(topics = "${pyrosense.alerting.kafka.scoring-topic:scoring-events}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onScoringEvent(IntegrationEvent event) {
        eventsReceived.increment();
        try {
            switch (event.eventType()) {
                case "scoring.critical-risk.detected" -> handleCriticalRisk(event);
                case "scoring.risk-level.changed" -> handleRiskLevelChanged(event);
                default -> { }
            }
        } catch (Exception e) {
            log.error("Failed to process scoring event {}: {}", event.eventId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${pyrosense.alerting.kafka.analysis-topic:analysis-events}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onAnalysisEvent(IntegrationEvent event) {
        eventsReceived.increment();
        try {
            if ("analysis.signal-anomaly.detected".equals(event.eventType())) {
                handleAnomalyDetected(event);
            }
        } catch (Exception e) {
            log.error("Failed to process analysis event {}: {}", event.eventId(), e.getMessage(), e);
        }
    }

    private void handleCriticalRisk(IntegrationEvent event) throws Exception {
        JsonNode node = objectMapper.readTree(event.payload());
        DeviceId deviceId = DeviceId.from(node.get("deviceId").asText());
        TenantId tenantId = extractTenantId(node);
        int score = node.get("score").asInt();

        CreateAlertCommand command = new CreateAlertCommand(
                tenantId, deviceId, AlertType.CRITICAL_RISK_SCORE, AlertSeverity.CRITICAL,
                "Score de risque critique: " + score + "/100",
                "Le dispositif " + deviceId + " a atteint un score de risque critique de " + score
                        + ". Intervention immédiate recommandée."
        );
        createAlertUseCase.create(command);
        alertsCreated.increment();
    }

    private void handleRiskLevelChanged(IntegrationEvent event) throws Exception {
        JsonNode node = objectMapper.readTree(event.payload());
        DeviceId deviceId = DeviceId.from(node.get("deviceId").asText());
        TenantId tenantId = extractTenantId(node);
        String newLevel = node.get("newLevel").asText();

        if ("HIGH".equals(newLevel)) {
            CreateAlertCommand command = new CreateAlertCommand(
                    tenantId, deviceId, AlertType.HIGH_RISK_SCORE, AlertSeverity.WARNING,
                    "Score de risque élevé détecté",
                    "Le dispositif " + deviceId + " est passé au niveau de risque HIGH. Surveillance renforcée nécessaire."
            );
            createAlertUseCase.create(command);
            alertsCreated.increment();
        }
    }

    private void handleAnomalyDetected(IntegrationEvent event) throws Exception {
        JsonNode node = objectMapper.readTree(event.payload());
        DeviceId deviceId = DeviceId.from(node.get("deviceId").asText());
        TenantId tenantId = extractTenantId(node);

        if (node.has("anomalies") && node.get("anomalies").isArray()) {
            for (JsonNode anomaly : node.get("anomalies")) {
                String typeStr = anomaly.get("type").asText();
                AlertType alertType = mapAnomalyToAlertType(typeStr);
                if (alertType != null) {
                    double confidence = anomaly.get("confidence").asDouble();
                    if (confidence >= 0.8) {
                        CreateAlertCommand command = new CreateAlertCommand(
                                tenantId, deviceId, alertType, alertType.defaultSeverity(),
                                formatTitle(alertType, confidence),
                                formatDescription(alertType, deviceId, confidence)
                        );
                        createAlertUseCase.create(command);
                        alertsCreated.increment();
                    }
                }
            }
        }
    }

    private AlertType mapAnomalyToAlertType(String anomalyType) {
        return switch (anomalyType) {
            case "MICRO_ARC_RECURRENT" -> AlertType.MICRO_ARC_DETECTED;
            case "THD_ABNORMAL" -> AlertType.HARMONIC_DISTORTION;
            case "TEMPERATURE_RISING" -> AlertType.OVERHEATING;
            case "TRANSIENT_ABNORMAL" -> AlertType.ABNORMAL_TRANSIENT;
            case "HF_NOISE_ELEVATED" -> AlertType.INSULATION_DEGRADATION;
            default -> null;
        };
    }

    private TenantId extractTenantId(JsonNode node) {
        if (node.has("tenantId") && !node.get("tenantId").isNull()) {
            return new TenantId(UUID.fromString(node.get("tenantId").asText()));
        }
        return new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    private String formatTitle(AlertType type, double confidence) {
        return switch (type) {
            case MICRO_ARC_DETECTED -> "Arc électrique détecté (confiance: " + (int)(confidence * 100) + "%)";
            case OVERHEATING -> "Surchauffe détectée (confiance: " + (int)(confidence * 100) + "%)";
            case HARMONIC_DISTORTION -> "Distorsion harmonique anormale";
            case ABNORMAL_TRANSIENT -> "Transitoire anormal détecté";
            case INSULATION_DEGRADATION -> "Dégradation d'isolation suspectée";
            default -> type.name() + " détecté";
        };
    }

    private String formatDescription(AlertType type, DeviceId deviceId, double confidence) {
        return "Anomalie de type " + type.name() + " détectée sur le dispositif " + deviceId
                + " avec un niveau de confiance de " + (int)(confidence * 100) + "%.";
    }
}
