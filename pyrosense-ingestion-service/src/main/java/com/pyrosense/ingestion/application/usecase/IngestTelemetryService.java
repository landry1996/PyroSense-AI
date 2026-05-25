package com.pyrosense.ingestion.application.usecase;

import com.pyrosense.ingestion.application.port.in.IngestTelemetryUseCase;
import com.pyrosense.ingestion.application.port.out.DeviceAuthorizationPort;
import com.pyrosense.ingestion.application.port.out.IdempotencyPort;
import com.pyrosense.ingestion.application.port.out.TelemetryEventPublisherPort;
import com.pyrosense.ingestion.application.port.out.TelemetryRepositoryPort;
import com.pyrosense.ingestion.domain.event.MicroArcDetectedEvent;
import com.pyrosense.ingestion.domain.event.TelemetryReceivedEvent;
import com.pyrosense.ingestion.domain.model.TelemetryReading;
import com.pyrosense.ingestion.domain.validation.TelemetryValidator;
import com.pyrosense.shared.util.ClockProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

public class IngestTelemetryService implements IngestTelemetryUseCase {

    private static final Logger log = LoggerFactory.getLogger(IngestTelemetryService.class);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final TelemetryRepositoryPort repository;
    private final TelemetryEventPublisherPort eventPublisher;
    private final DeviceAuthorizationPort deviceAuth;
    private final IdempotencyPort idempotency;

    public IngestTelemetryService(TelemetryRepositoryPort repository,
                                   TelemetryEventPublisherPort eventPublisher,
                                   DeviceAuthorizationPort deviceAuth,
                                   IdempotencyPort idempotency) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.deviceAuth = deviceAuth;
        this.idempotency = idempotency;
    }

    @Override
    public IngestionResult execute(TelemetryCommand command) {
        if (!deviceAuth.isDeviceActive(command.deviceId())) {
            log.warn("Telemetry rejected: device {} is not active", command.deviceId());
            return IngestionResult.REJECTED_UNAUTHORIZED;
        }

        if (!deviceAuth.isDeviceOwnedByTenant(command.deviceId(), command.tenantId())) {
            log.warn("Telemetry rejected: device {} does not belong to tenant {}", command.deviceId(), command.tenantId());
            return IngestionResult.REJECTED_UNAUTHORIZED;
        }

        var violations = TelemetryValidator.validate(
                command.rmsVoltage(), command.rmsCurrent(), command.activePower(),
                command.reactivePower(), command.powerFactor(), command.thd(),
                command.temperatureCelsius(), command.hfNoiseLevel(),
                command.microArcCount(), command.transientCount(),
                command.samplingWindowMs(), command.timestamp()
        );

        if (!violations.isEmpty()) {
            log.warn("Telemetry rejected: device={} violations={}", command.deviceId(), violations);
            return IngestionResult.REJECTED_INVALID;
        }

        String payloadHash = computeHash(command.rawPayload());
        String idempotencyKey = command.deviceId().toString() + ":" + command.timestamp() + ":" + payloadHash;

        if (idempotency.isDuplicate(idempotencyKey)) {
            log.debug("Duplicate telemetry: device={} timestamp={}", command.deviceId(), command.timestamp());
            return IngestionResult.DUPLICATE;
        }

        var reading = TelemetryReading.builder()
                .id(UUID.randomUUID())
                .deviceId(command.deviceId())
                .tenantId(command.tenantId())
                .timestamp(command.timestamp())
                .samplingWindowMs(command.samplingWindowMs())
                .rmsCurrent(command.rmsCurrent())
                .rmsVoltage(command.rmsVoltage())
                .activePower(command.activePower())
                .reactivePower(command.reactivePower())
                .powerFactor(command.powerFactor())
                .thd(command.thd())
                .temperatureCelsius(command.temperatureCelsius())
                .hfNoiseLevel(command.hfNoiseLevel())
                .microArcCount(command.microArcCount())
                .transientCount(command.transientCount())
                .firmwareVersion(command.firmwareVersion())
                .payloadHash(payloadHash)
                .ingestedAt(ClockProvider.now())
                .build();

        repository.save(reading);
        idempotency.markProcessed(idempotencyKey, IDEMPOTENCY_TTL);

        publishEvents(reading);

        log.debug("Telemetry ingested: device={} timestamp={}", command.deviceId(), command.timestamp());
        return IngestionResult.ACCEPTED;
    }

    private void publishEvents(TelemetryReading reading) {
        var telemetryEvent = new TelemetryReceivedEvent(
                UUID.randomUUID(), ClockProvider.now(),
                reading.getDeviceId(), reading.getTenantId(),
                reading.getTimestamp(),
                reading.getRmsCurrent(), reading.getRmsVoltage(),
                reading.getActivePower(), reading.getPowerFactor(),
                reading.getThd(), reading.getTemperatureCelsius(),
                reading.getHfNoiseLevel(), reading.getMicroArcCount(),
                reading.getTransientCount()
        );
        eventPublisher.publish(telemetryEvent);

        if (reading.hasMicroArcActivity()) {
            var microArcEvent = new MicroArcDetectedEvent(
                    UUID.randomUUID(), ClockProvider.now(),
                    reading.getDeviceId(), reading.getTenantId(),
                    reading.getMicroArcCount(), reading.getHfNoiseLevel(),
                    reading.getTimestamp()
            );
            eventPublisher.publish(microArcEvent);
        }
    }

    private String computeHash(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
