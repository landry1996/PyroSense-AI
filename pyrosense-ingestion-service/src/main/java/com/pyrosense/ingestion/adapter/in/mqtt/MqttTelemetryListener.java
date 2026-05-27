package com.pyrosense.ingestion.adapter.in.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.pyrosense.ingestion.adapter.in.mqtt.protocol.*;
import com.pyrosense.ingestion.adapter.in.rest.dto.HeartbeatPayload;
import com.pyrosense.ingestion.adapter.in.rest.dto.TelemetryPayload;
import com.pyrosense.ingestion.application.port.in.IngestHeartbeatUseCase;
import com.pyrosense.ingestion.application.port.in.IngestTelemetryUseCase;
import com.pyrosense.ingestion.application.port.out.*;
import com.pyrosense.ingestion.domain.event.*;
import com.pyrosense.ingestion.domain.model.DeviceClockDrift;
import com.pyrosense.ingestion.domain.model.IngestionRejectionReason;
import com.pyrosense.ingestion.domain.model.SignalQualityScore;
import com.pyrosense.ingestion.domain.validation.DeviceClockDriftDetector;
import com.pyrosense.ingestion.domain.validation.TelemetryQualityValidator;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
import com.pyrosense.ingestion.adapter.in.mqtt.protocol.ProtocolValidationPipeline.DeviceStatusChecker;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MqttTelemetryListener {

    private static final Logger log = LoggerFactory.getLogger(MqttTelemetryListener.class);

    private static final Pattern TOPIC_PATTERN_LEGACY = Pattern.compile(
            "pyrosense/([^/]+)/([^/]+)/(telemetry|events|heartbeat)");
    private static final Pattern TOPIC_PATTERN_V1 = Pattern.compile(
            MqttProtocolConstants.TOPIC_PATTERN_V1);
    private static final int MAX_PAYLOAD_SIZE = MqttProtocolConstants.MAX_PAYLOAD_SIZE_BYTES;

    private final ExecutorService processingPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> { Thread t = new Thread(r, "mqtt-processor"); t.setDaemon(true); return t; });

    private final IngestTelemetryUseCase ingestTelemetry;
    private final IngestHeartbeatUseCase ingestHeartbeat;
    private final TelemetryEventPublisherPort eventPublisher;
    private final RejectionRepositoryPort rejectionRepository;
    private final DeviceCapabilityLookupPort capabilityLookup;
    private final ProtocolValidationPipeline validationPipeline;
    private final DeviceRateLimiter rateLimiter;
    private final IoTSecurityAuditor auditor;
    private final DeviceStatusChecker deviceStatusChecker;
    private final ObjectMapper objectMapper;

    private final Counter receivedCounter;
    private final Counter rejectedCounter;
    private final Counter realDeviceReceivedCounter;
    private final Counter signatureInvalidCounter;
    private final Counter replayDetectedCounter;
    private final Counter lowSignalQualityCounter;
    private final Counter clockDriftCounter;

    @Value("${pyrosense.mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${pyrosense.mqtt.enabled:true}")
    private boolean mqttEnabled;

    @Value("${pyrosense.mqtt.legacy-handler.enabled:true}")
    private boolean legacyHandlerEnabled;

    private Mqtt5AsyncClient mqttClient;

    public MqttTelemetryListener(IngestTelemetryUseCase ingestTelemetry,
                                  IngestHeartbeatUseCase ingestHeartbeat,
                                  TelemetryEventPublisherPort eventPublisher,
                                  RejectionRepositoryPort rejectionRepository,
                                  DeviceCapabilityLookupPort capabilityLookup,
                                  ProtocolValidationPipeline validationPipeline,
                                  DeviceRateLimiter rateLimiter,
                                  IoTSecurityAuditor auditor,
                                  DeviceStatusChecker deviceStatusChecker,
                                  ObjectMapper objectMapper,
                                  MeterRegistry meterRegistry) {
        this.ingestTelemetry = ingestTelemetry;
        this.ingestHeartbeat = ingestHeartbeat;
        this.eventPublisher = eventPublisher;
        this.rejectionRepository = rejectionRepository;
        this.capabilityLookup = capabilityLookup;
        this.validationPipeline = validationPipeline;
        this.rateLimiter = rateLimiter;
        this.auditor = auditor;
        this.deviceStatusChecker = deviceStatusChecker;
        this.objectMapper = objectMapper;

        this.receivedCounter = Counter.builder("pyrosense.telemetry.received")
                .tag("source", "mqtt")
                .register(meterRegistry);
        this.rejectedCounter = Counter.builder("pyrosense.telemetry.rejected")
                .tag("source", "mqtt")
                .register(meterRegistry);
        this.realDeviceReceivedCounter = Counter.builder("real_device_telemetry_received_total")
                .description("Total telemetry messages from real devices")
                .register(meterRegistry);
        this.signatureInvalidCounter = Counter.builder("telemetry_signature_invalid_total")
                .description("Total messages with invalid HMAC signatures")
                .register(meterRegistry);
        this.replayDetectedCounter = Counter.builder("telemetry_replay_detected_total")
                .description("Total replay attacks detected")
                .register(meterRegistry);
        this.lowSignalQualityCounter = Counter.builder("low_signal_quality_total")
                .description("Total messages with low signal quality")
                .register(meterRegistry);
        this.clockDriftCounter = Counter.builder("device_clock_drift_total")
                .description("Total clock drift events detected")
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
        // V1 protocol topics (real devices)
        mqttClient.subscribeWith()
                .topicFilter("pyrosense/v1/+/+/telemetry")
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(this::handleMessage)
                .send();
        mqttClient.subscribeWith()
                .topicFilter("pyrosense/v1/+/+/heartbeat")
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(this::handleMessage)
                .send();
        mqttClient.subscribeWith()
                .topicFilter("pyrosense/v1/+/+/events")
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(this::handleMessage)
                .send();

        // Legacy topics (simulator backward compatibility)
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

        log.info("Subscribed to v1 topics: pyrosense/v1/+/+/[telemetry|heartbeat|events]");
        log.info("Subscribed to legacy topics: pyrosense/+/+/[telemetry|heartbeat|events]");
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
        String payload = new String(payloadBytes, StandardCharsets.UTF_8);

        // Try V1 format first
        Matcher v1Matcher = TOPIC_PATTERN_V1.matcher(topic);
        if (v1Matcher.matches()) {
            String tenantId = v1Matcher.group(1);
            String deviceId = v1Matcher.group(2);
            String messageType = v1Matcher.group(3);
            processingPool.submit(() -> handleV1Message(tenantId, deviceId, messageType, payload));
            return;
        }

        // Fall back to legacy format (simulator)
        Matcher legacyMatcher = TOPIC_PATTERN_LEGACY.matcher(topic);
        if (legacyMatcher.matches()) {
            String tenantId = legacyMatcher.group(1);
            String deviceId = legacyMatcher.group(2);
            String messageType = legacyMatcher.group(3);
            processingPool.submit(() -> handleLegacyMessage(tenantId, deviceId, messageType, payload));
            return;
        }

        log.warn("Unrecognized topic format: {}", topic);
    }

    private void handleV1Message(String tenantId, String deviceId, String messageType, String rawPayload) {
        try {
            switch (messageType) {
                case "telemetry" -> handleV1Telemetry(tenantId, deviceId, rawPayload);
                case "heartbeat" -> handleV1Heartbeat(tenantId, deviceId, rawPayload);
                case "events" -> handleV1Event(tenantId, deviceId, rawPayload);
                case "command-acks" -> log.debug("Command ack from device={}", deviceId);
            }
        } catch (Exception e) {
            log.error("Error processing v1 message: device={} type={}", deviceId, messageType, e);
            rejectedCounter.increment();
            eventPublisher.publishToDlq("ingestion-dlq", deviceId, rawPayload, e.getMessage());
        }
    }

    private void handleV1Telemetry(String tenantId, String deviceId, String rawPayload) throws Exception {
        var payload = objectMapper.readValue(rawPayload, TelemetryPayloadV1.class);

        // Run full protocol validation pipeline
        var pipelineResult = validationPipeline.validateTelemetry(payload, rawPayload, deviceId, tenantId);
        if (!pipelineResult.accepted()) {
            handleRejection(tenantId, deviceId, rawPayload, pipelineResult);
            return;
        }

        realDeviceReceivedCounter.increment();

        // Quality assessment
        var quality = TelemetryQualityValidator.assess(
                payload.features().signalQuality(),
                payload.features().rmsVoltage(),
                payload.features().rmsCurrent(),
                payload.features().hfNoiseLevel(),
                payload.samplingWindowMs());

        if (quality.signalScore().requiresAlert()) {
            lowSignalQualityCounter.increment();
            eventPublisher.publish(new LowSignalQualityDetectedEvent(
                    UUID.randomUUID(), ClockProvider.now(),
                    DeviceId.from(deviceId), TenantId.from(tenantId),
                    payload.features().signalQuality(),
                    quality.signalScore().level()));
        }

        // Clock drift detection
        var clockDrift = DeviceClockDriftDetector.detect(deviceId, payload.timestamp());
        if (clockDrift.requiresAlert()) {
            clockDriftCounter.increment();
            eventPublisher.publish(new DeviceClockDriftDetectedEvent(
                    UUID.randomUUID(), ClockProvider.now(),
                    DeviceId.from(deviceId), TenantId.from(tenantId),
                    clockDrift.drift(), clockDrift.severity()));
        }

        // Ingest via use case
        var command = new IngestTelemetryUseCase.TelemetryCommand(
                DeviceId.from(deviceId),
                TenantId.from(tenantId),
                payload.timestamp(),
                payload.samplingWindowMs(),
                payload.features().rmsCurrent(),
                payload.features().rmsVoltage(),
                payload.features().activePower(),
                payload.features().reactivePower(),
                payload.features().powerFactor(),
                payload.features().thd(),
                payload.features().temperatureCelsius(),
                payload.features().hfNoiseLevel(),
                payload.features().microArcCount(),
                payload.features().transientCount(),
                payload.firmwareVersion(),
                rawPayload
        );
        ingestTelemetry.execute(command);

        // Publish real device event
        eventPublisher.publish(new RealDeviceTelemetryReceivedEvent(
                UUID.randomUUID(), ClockProvider.now(),
                DeviceId.from(deviceId), TenantId.from(tenantId),
                payload.timestamp(), payload.firmwareVersion(),
                payload.features().signalQuality(), payload.sequenceNumber()));
    }

    private void handleV1Heartbeat(String tenantId, String deviceId, String rawPayload) throws Exception {
        // Step 0: Per-device rate limiting
        if (rateLimiter != null) {
            var rateResult = rateLimiter.check(deviceId);
            if (rateResult == DeviceRateLimiter.RateLimitResult.RATE_LIMITED) {
                if (auditor != null) {
                    auditor.logSecurityEvent(IoTSecurityAuditor.SecurityEvent.RATE_LIMITED, deviceId, tenantId, "Heartbeat rate limited");
                }
                log.warn("Heartbeat rate limited: device={}", deviceId);
                rejectedCounter.increment();
                return;
            }
        }

        // Step 2: Device authorization (reject revoked devices)
        if (deviceStatusChecker != null) {
            var deviceStatus = deviceStatusChecker.checkDevice(deviceId, tenantId);
            if (deviceStatus == DeviceStatusChecker.Status.REVOKED) {
                if (auditor != null) {
                    auditor.logSecurityEvent(IoTSecurityAuditor.SecurityEvent.DEVICE_REVOKED_ATTEMPT, deviceId, tenantId, "Revoked device heartbeat");
                }
                log.warn("Rejected heartbeat from revoked device={}", deviceId);
                rejectedCounter.increment();
                return;
            }
        }

        var payload = objectMapper.readValue(rawPayload, HeartbeatPayloadV1.class);

        var validationResult = new PayloadValidator().validateHeartbeat(payload, deviceId, tenantId);
        if (!validationResult.valid()) {
            log.warn("Heartbeat validation failed: device={} errors={}", deviceId, validationResult.errors());
            rejectedCounter.increment();
            return;
        }

        var command = new IngestHeartbeatUseCase.HeartbeatCommand(
                DeviceId.from(deviceId),
                TenantId.from(tenantId),
                payload.timestamp(),
                payload.firmwareVersion(),
                (int) (payload.uptimeSeconds() / 3600),
                payload.bufferUsagePercent(),
                payload.internalTempCelsius() != null ? payload.internalTempCelsius() : 0
        );
        ingestHeartbeat.execute(command);
    }

    private void handleV1Event(String tenantId, String deviceId, String rawPayload) throws Exception {
        var payload = objectMapper.readValue(rawPayload, DeviceEventPayloadV1.class);

        var pipelineResult = validationPipeline.validateEvent(payload, rawPayload, deviceId, tenantId);
        if (!pipelineResult.accepted()) {
            handleRejection(tenantId, deviceId, rawPayload,  pipelineResult);
            return;
        }

        // Device events are forwarded as telemetry with context data
        if (payload.context() != null && payload.context().rmsCurrent() != null) {
            var command = new IngestTelemetryUseCase.TelemetryCommand(
                    DeviceId.from(deviceId),
                    TenantId.from(tenantId),
                    payload.timestamp(),
                    1000,
                    payload.context().rmsCurrent(),
                    0, 0, 0, 0, 0,
                    payload.context().temperatureCelsius() != null ? payload.context().temperatureCelsius() : 0,
                    0, 0, 0,
                    payload.firmwareVersion(),
                    rawPayload
            );
            ingestTelemetry.execute(command);
        }

        log.info("Device event: device={} type={} severity={}", deviceId, payload.eventType(), payload.severity());
    }

    private void handleRejection(String tenantId, String deviceId, String rawPayload,
                                  ProtocolValidationPipeline.PipelineResult result) {
        rejectedCounter.increment();

        IngestionRejectionReason reason = IngestionRejectionReason.fromPipelineCode(result.rejectionCode());

        switch (reason) {
            case INVALID_SIGNATURE -> signatureInvalidCounter.increment();
            case REPLAY_DETECTED -> replayDetectedCounter.increment();
            default -> {}
        }

        rejectionRepository.saveRejection(
                TenantId.from(tenantId),
                DeviceId.from(deviceId),
                result.rejectionCode(),
                "mqtt-v1",
                null,
                List.of(result.rejectionReason()));

        eventPublisher.publish(new TelemetryRejectedEvent(
                UUID.randomUUID(), ClockProvider.now(),
                DeviceId.from(deviceId), TenantId.from(tenantId),
                reason, result.rejectionReason()));

        log.warn("Rejected v1 telemetry: device={} reason={} detail={}", deviceId, result.rejectionCode(), result.rejectionReason());
    }

    // --- Legacy (simulator) handlers — backward compatible, no signature/replay checks ---

    private void handleLegacyMessage(String tenantId, String deviceId, String messageType, String payload) {
        if (!legacyHandlerEnabled) {
            log.warn("SECURITY: Legacy MQTT handler is disabled. Rejecting message from device={} tenant={} type={}. "
                    + "Enable via pyrosense.mqtt.legacy-handler.enabled=true if backward compatibility is required.",
                    deviceId, tenantId, messageType);
            rejectedCounter.increment();
            return;
        }

        try {
            switch (messageType) {
                case "telemetry", "events" -> handleLegacyTelemetry(tenantId, deviceId, payload);
                case "heartbeat" -> handleLegacyHeartbeat(tenantId, deviceId, payload);
            }
        } catch (Exception e) {
            log.error("Error processing legacy MQTT message: device={}", deviceId, e);
            rejectedCounter.increment();
            eventPublisher.publishToDlq("ingestion-dlq", deviceId, payload, e.getMessage());
        }
    }

    private void handleLegacyTelemetry(String tenantIdStr, String deviceIdStr, String payload) throws Exception {
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

    private void handleLegacyHeartbeat(String tenantIdStr, String deviceIdStr, String payload) throws Exception {
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
