package com.pyrosense.scoring.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.scoring.application.port.in.CalculateRiskUseCase;
import com.pyrosense.scoring.application.port.in.CalculateRiskUseCase.CalculateRiskCommand;
import com.pyrosense.scoring.domain.model.AnomalyInput;
import com.pyrosense.shared.event.IntegrationEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class KafkaAnomalyEventListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaAnomalyEventListener.class);

    private final CalculateRiskUseCase calculateRiskUseCase;
    private final ObjectMapper objectMapper;
    private final Counter eventsReceived;
    private final Counter eventsProcessed;
    private final Timer scoringTimer;

    public KafkaAnomalyEventListener(CalculateRiskUseCase calculateRiskUseCase,
                                      ObjectMapper objectMapper,
                                      MeterRegistry registry) {
        this.calculateRiskUseCase = calculateRiskUseCase;
        this.objectMapper = objectMapper;
        this.eventsReceived = Counter.builder("pyrosense.scoring.events.received").register(registry);
        this.eventsProcessed = Counter.builder("pyrosense.risk.score.updated").register(registry);
        this.scoringTimer = Timer.builder("pyrosense.scoring.calculation.duration").register(registry);
    }

    @KafkaListener(topics = "${pyrosense.scoring.kafka.topic:analysis-events}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onAnalysisEvent(IntegrationEvent event) {
        eventsReceived.increment();

        if (!"analysis.signal-anomaly.detected".equals(event.eventType())) return;

        scoringTimer.record(() -> {
            try {
                CalculateRiskCommand command = parseCommand(event);
                if (command != null) {
                    calculateRiskUseCase.calculate(command);
                    eventsProcessed.increment();
                }
            } catch (Exception e) {
                log.error("Failed to process analysis event: {}", e.getMessage(), e);
            }
        });
    }

    private CalculateRiskCommand parseCommand(IntegrationEvent event) {
        try {
            JsonNode node = objectMapper.readTree(event.payload());

            DeviceId deviceId = DeviceId.from(node.get("deviceId").asText());
            ElectricalPanelId panelId = node.has("panelId") && !node.get("panelId").isNull()
                    ? new ElectricalPanelId(UUID.fromString(node.get("panelId").asText()))
                    : null;
            UUID circuitId = node.has("circuitId") && !node.get("circuitId").isNull()
                    ? UUID.fromString(node.get("circuitId").asText())
                    : null;

            List<AnomalyInput> anomalies = new ArrayList<>();
            if (node.has("anomalies") && node.get("anomalies").isArray()) {
                for (JsonNode anomalyNode : node.get("anomalies")) {
                    anomalies.add(new AnomalyInput(
                            deviceId,
                            anomalyNode.get("type").asText(),
                            anomalyNode.get("confidence").asDouble(),
                            anomalyNode.has("deviationSigma") ? anomalyNode.get("deviationSigma").asDouble() : 0,
                            Instant.parse(anomalyNode.get("detectedAt").asText())
                    ));
                }
            }

            double riskScore = node.has("aggregateRiskScore") ? node.get("aggregateRiskScore").asDouble() : 0;

            return new CalculateRiskCommand(deviceId, panelId, circuitId, anomalies, true, riskScore > 0);
        } catch (Exception e) {
            log.warn("Failed to parse analysis event: {}", e.getMessage());
            return null;
        }
    }
}
