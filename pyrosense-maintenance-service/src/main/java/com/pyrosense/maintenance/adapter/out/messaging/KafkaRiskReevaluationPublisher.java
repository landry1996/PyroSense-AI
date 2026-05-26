package com.pyrosense.maintenance.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.maintenance.application.port.out.RiskScoreReevaluationPublisherPort;
import com.pyrosense.shared.event.IntegrationEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class KafkaRiskReevaluationPublisher implements RiskScoreReevaluationPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaRiskReevaluationPublisher.class);
    private static final String TOPIC = "risk-reevaluation-requests";

    private final KafkaTemplate<String, IntegrationEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaRiskReevaluationPublisher(KafkaTemplate<String, IntegrationEvent> kafkaTemplate,
                                          ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void requestReevaluation(TenantId tenantId, DeviceId deviceId, UUID interventionId) {
        try {
            var payload = Map.of(
                    "tenantId", tenantId.value().toString(),
                    "deviceId", deviceId.value().toString(),
                    "interventionId", interventionId.toString(),
                    "reason", "INTERVENTION_COMPLETED"
            );
            String json = objectMapper.writeValueAsString(payload);
            var event = new IntegrationEvent(
                    UUID.randomUUID(),
                    "maintenance.risk_reevaluation.requested",
                    Instant.now(),
                    "pyrosense-maintenance-service",
                    json
            );
            kafkaTemplate.send(TOPIC, tenantId.value().toString(), event);
            log.debug("Published risk reevaluation request for device {} after intervention {}",
                    deviceId.value(), interventionId);
        } catch (Exception e) {
            log.error("Failed to publish risk reevaluation request for intervention {}", interventionId, e);
        }
    }
}
