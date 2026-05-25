package com.pyrosense.analysis.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.analysis.application.port.in.AnalyzeSignalUseCase;
import com.pyrosense.analysis.application.port.in.AnalyzeSignalUseCase.AnalyzeTelemetryWindowCommand;
import com.pyrosense.analysis.application.port.in.BuildBaselineUseCase;
import com.pyrosense.analysis.application.port.in.BuildBaselineUseCase.BuildBaselineCommand;
import com.pyrosense.analysis.domain.model.SignalFeature;
import com.pyrosense.analysis.domain.model.SignalWindow;
import com.pyrosense.analysis.domain.model.SignalWindow.SignalSample;
import com.pyrosense.shared.event.IntegrationEvent;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class KafkaTelemetryEventListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaTelemetryEventListener.class);

    private final AnalyzeSignalUseCase analyzeSignalUseCase;
    private final BuildBaselineUseCase buildBaselineUseCase;
    private final ObjectMapper objectMapper;
    private final Counter eventsReceived;
    private final Counter eventsProcessed;
    private final Counter eventsFailed;
    private final Counter anomaliesDetected;
    private final Timer analysisTimer;

    public KafkaTelemetryEventListener(AnalyzeSignalUseCase analyzeSignalUseCase,
                                        BuildBaselineUseCase buildBaselineUseCase,
                                        ObjectMapper objectMapper,
                                        MeterRegistry registry) {
        this.analyzeSignalUseCase = analyzeSignalUseCase;
        this.buildBaselineUseCase = buildBaselineUseCase;
        this.objectMapper = objectMapper;
        this.eventsReceived = Counter.builder("pyrosense.analysis.events.received").register(registry);
        this.eventsProcessed = Counter.builder("pyrosense.analysis.events.processed").register(registry);
        this.eventsFailed = Counter.builder("pyrosense.analysis.events.failed").register(registry);
        this.anomaliesDetected = Counter.builder("pyrosense.anomalies.detected").register(registry);
        this.analysisTimer = Timer.builder("pyrosense.analysis.processing.duration").register(registry);
    }

    @KafkaListener(topics = "${pyrosense.analysis.kafka.topic:telemetry-events}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onTelemetryEvent(IntegrationEvent event) {
        eventsReceived.increment();

        if (!"ingestion.telemetry.received".equals(event.eventType())) {
            return;
        }

        analysisTimer.record(() -> {
            try {
                SignalWindow window = parseWindow(event);
                if (window == null) return;

                buildBaselineUseCase.buildOrUpdate(
                        new BuildBaselineCommand(window.deviceId(), window));

                var result = analyzeSignalUseCase.analyze(new AnalyzeTelemetryWindowCommand(window));

                eventsProcessed.increment();
                if (result != null && result.hasAnomalies()) {
                    anomaliesDetected.increment(result.anomalyCount());
                }
            } catch (Exception e) {
                eventsFailed.increment();
                log.error("Failed to process telemetry event: {}", e.getMessage(), e);
            }
        });
    }

    private SignalWindow parseWindow(IntegrationEvent event) {
        try {
            JsonNode node = objectMapper.readTree(event.payload());

            DeviceId deviceId = DeviceId.from(node.get("deviceId").asText());
            TenantId tenantId = TenantId.from(node.get("tenantId").asText());
            Instant timestamp = Instant.parse(node.get("timestamp").asText());

            Map<SignalFeature, Double> values = new EnumMap<>(SignalFeature.class);
            mapIfPresent(node, "rmsCurrent", SignalFeature.RMS_CURRENT, values);
            mapIfPresent(node, "rmsVoltage", SignalFeature.RMS_VOLTAGE, values);
            mapIfPresent(node, "activePower", SignalFeature.ACTIVE_POWER, values);
            mapIfPresent(node, "reactivePower", SignalFeature.REACTIVE_POWER, values);
            mapIfPresent(node, "powerFactor", SignalFeature.POWER_FACTOR, values);
            mapIfPresent(node, "thd", SignalFeature.THD, values);
            mapIfPresent(node, "temperatureCelsius", SignalFeature.TEMPERATURE, values);
            mapIfPresent(node, "hfNoiseLevel", SignalFeature.HF_NOISE, values);
            mapIfPresent(node, "microArcCount", SignalFeature.MICRO_ARC_COUNT, values);
            mapIfPresent(node, "transientCount", SignalFeature.TRANSIENT_COUNT, values);

            SignalSample sample = new SignalSample(timestamp, values);
            return new SignalWindow(deviceId, tenantId, List.of(sample));
        } catch (Exception e) {
            log.warn("Failed to parse telemetry event payload: {}", e.getMessage());
            return null;
        }
    }

    private void mapIfPresent(JsonNode node, String field, SignalFeature feature,
                              Map<SignalFeature, Double> values) {
        if (node.has(field) && !node.get(field).isNull()) {
            values.put(feature, node.get(field).asDouble());
        }
    }
}
