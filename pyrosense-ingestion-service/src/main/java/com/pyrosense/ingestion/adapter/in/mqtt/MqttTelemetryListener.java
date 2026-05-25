package com.pyrosense.ingestion.adapter.in.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.pyrosense.ingestion.adapter.in.rest.dto.HeartbeatPayload;
import com.pyrosense.ingestion.adapter.in.rest.dto.TelemetryPayload;
import com.pyrosense.ingestion.application.port.in.IngestHeartbeatUseCase;
import com.pyrosense.ingestion.application.port.in.IngestTelemetryUseCase;
import com.pyrosense.ingestion.application.port.out.TelemetryEventPublisherPort;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MqttTelemetryListener {

    private static final Logger log = LoggerFactory.getLogger(MqttTelemetryListener.class);
    private static final Pattern TOPIC_PATTERN = Pattern.compile("pyrosense/([^/]+)/([^/]+)/(telemetry|events|heartbeat)");
    private static final int MAX_PAYLOAD_SIZE = 8192;

    private final ExecutorService processingPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> { Thread t = new Thread(r, "mqtt-processor"); t.setDaemon(true); return t; });

    private final IngestTelemetryUseCase ingestTelemetry;
    private final IngestHeartbeatUseCase ingestHeartbeat;
    private final TelemetryEventPublisherPort eventPublisher;
    private final ObjectMapper objectMapper;
    private final Counter receivedCounter;
    private final Counter rejectedCounter;

    @Value("${pyrosense.mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${pyrosense.mqtt.enabled:true}")
    private boolean mqttEnabled;

    private Mqtt5AsyncClient mqttClient;

    public MqttTelemetryListener(IngestTelemetryUseCase ingestTelemetry,
                                  IngestHeartbeatUseCase ingestHeartbeat,
                                  TelemetryEventPublisherPort eventPublisher,
                                  ObjectMapper objectMapper,
                                  MeterRegistry meterRegistry) {
        this.ingestTelemetry = ingestTelemetry;
        this.ingestHeartbeat = ingestHeartbeat;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.receivedCounter = Counter.builder("pyrosense.telemetry.received")
                .tag("source", "mqtt")
                .register(meterRegistry);
        this.rejectedCounter = Counter.builder("pyrosense.telemetry.rejected")
                .tag("source", "mqtt")
                .register(meterRegistry);
    }

    @PostConstruct
    public void connect() {
        if (!mqttEnabled) {
            log.info("MQTT listener disabled");
            return;
        }

        try {
            URI uri = URI.create(brokerUrl);
            mqttClient = MqttClient.builder()
                    .useMqttVersion5()
                    .identifier("pyrosense-ingestion-" + UUID.randomUUID().toString().substring(0, 8))
                    .serverHost(uri.getHost())
                    .serverPort(uri.getPort() > 0 ? uri.getPort() : 1883)
                    .automaticReconnect()
                        .initialDelay(1, TimeUnit.SECONDS)
                        .maxDelay(30, TimeUnit.SECONDS)
                        .applyAutomaticReconnect()
                    .addDisconnectedListener(context -> log.warn("MQTT disconnected: {}", context.getCause().getMessage()))
                    .addConnectedListener(context -> log.info("MQTT reconnected"))
                    .buildAsync();

            mqttClient.connect().thenAccept(ack -> {
                log.info("Connected to MQTT broker: {}", brokerUrl);
                subscribeToTopics();
            }).exceptionally(ex -> {
                log.error("Failed to connect to MQTT broker: {}", brokerUrl, ex);
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to initialize MQTT client", e);
        }
    }

    private void subscribeToTopics() {
        mqttClient.subscribeWith()
                .topicFilter("pyrosense/+/+/telemetry")
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(this::handleMessage)
                .send();
        mqttClient.subscribeWith()
                .topicFilter("pyrosense/+/+/heartbeat")
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(this::handleMessage)
                .send();
        mqttClient.subscribeWith()
                .topicFilter("pyrosense/+/+/events")
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(this::handleMessage)
                .send();
        log.info("Subscribed to pyrosense/+/+/telemetry|heartbeat|events");
    }

    private void handleMessage(Mqtt5Publish publish) {
        String topic = publish.getTopic().toString();
        byte[] payloadBytes = publish.getPayloadAsBytes();

        if (payloadBytes.length > MAX_PAYLOAD_SIZE) {
            log.warn("Payload too large on topic={} size={}", topic, payloadBytes.length);
            rejectedCounter.increment();
            return;
        }

        receivedCounter.increment();
        Matcher matcher = TOPIC_PATTERN.matcher(topic);
        if (!matcher.matches()) {
            log.warn("Unrecognized topic format: {}", topic);
            return;
        }

        String tenantIdStr = matcher.group(1);
        String deviceIdStr = matcher.group(2);
        String messageType = matcher.group(3);
        String payload = new String(payloadBytes, StandardCharsets.UTF_8);

        processingPool.submit(() -> {
            try {
                switch (messageType) {
                    case "telemetry", "events" -> handleTelemetry(tenantIdStr, deviceIdStr, payload);
                    case "heartbeat" -> handleHeartbeat(tenantIdStr, deviceIdStr, payload);
                }
            } catch (Exception e) {
                log.error("Error processing MQTT message: topic={}", topic, e);
                rejectedCounter.increment();
                eventPublisher.publishToDlq("ingestion-dlq", deviceIdStr, payload, e.getMessage());
            }
        });
    }

    private void handleTelemetry(String tenantIdStr, String deviceIdStr, String payload) throws Exception {
        var telemetry = objectMapper.readValue(payload, TelemetryPayload.class);
        var command = new IngestTelemetryUseCase.TelemetryCommand(
                DeviceId.from(deviceIdStr),
                TenantId.from(tenantIdStr),
                telemetry.timestamp(),
                telemetry.samplingWindowMs(),
                telemetry.rmsCurrent(),
                telemetry.rmsVoltage(),
                telemetry.activePower(),
                telemetry.reactivePower(),
                telemetry.powerFactor(),
                telemetry.thd(),
                telemetry.temperatureCelsius(),
                telemetry.hfNoiseLevel(),
                telemetry.microArcCount(),
                telemetry.transientCount(),
                telemetry.firmwareVersion(),
                payload
        );
        ingestTelemetry.execute(command);
    }

    private void handleHeartbeat(String tenantIdStr, String deviceIdStr, String payload) throws Exception {
        var heartbeat = objectMapper.readValue(payload, HeartbeatPayload.class);
        var command = new IngestHeartbeatUseCase.HeartbeatCommand(
                DeviceId.from(deviceIdStr),
                TenantId.from(tenantIdStr),
                heartbeat.timestamp(),
                heartbeat.firmwareVersion(),
                heartbeat.uptimeHours(),
                heartbeat.memoryUsagePercent(),
                heartbeat.cpuTemperatureCelsius()
        );
        ingestHeartbeat.execute(command);
    }

    @PreDestroy
    public void disconnect() {
        processingPool.shutdown();
        try {
            if (!processingPool.awaitTermination(5, TimeUnit.SECONDS)) {
                processingPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            processingPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (mqttClient != null) {
            mqttClient.disconnect();
            log.info("Disconnected from MQTT broker");
        }
    }
}
