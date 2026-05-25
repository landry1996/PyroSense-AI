package com.pyrosense.ingestion.application.usecase;

import com.pyrosense.ingestion.application.port.in.IngestHeartbeatUseCase;
import com.pyrosense.ingestion.application.port.out.DeviceAuthorizationPort;
import com.pyrosense.ingestion.application.port.out.HeartbeatRepositoryPort;
import com.pyrosense.ingestion.application.port.out.TelemetryEventPublisherPort;
import com.pyrosense.ingestion.domain.event.HeartbeatReceivedEvent;
import com.pyrosense.ingestion.domain.model.DeviceHeartbeat;
import com.pyrosense.shared.util.ClockProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class IngestHeartbeatService implements IngestHeartbeatUseCase {

    private static final Logger log = LoggerFactory.getLogger(IngestHeartbeatService.class);

    private final DeviceAuthorizationPort deviceAuth;
    private final TelemetryEventPublisherPort eventPublisher;
    private final HeartbeatRepositoryPort heartbeatRepository;

    public IngestHeartbeatService(DeviceAuthorizationPort deviceAuth,
                                   TelemetryEventPublisherPort eventPublisher,
                                   HeartbeatRepositoryPort heartbeatRepository) {
        this.deviceAuth = deviceAuth;
        this.eventPublisher = eventPublisher;
        this.heartbeatRepository = heartbeatRepository;
    }

    @Override
    public void execute(HeartbeatCommand command) {
        if (!deviceAuth.isDeviceActive(command.deviceId())) {
            log.warn("Heartbeat ignored: device {} is not active", command.deviceId());
            return;
        }

        var heartbeat = new DeviceHeartbeat(
                command.deviceId(), command.tenantId(), command.timestamp(),
                command.firmwareVersion(), command.uptimeHours(),
                command.memoryUsagePercent(), command.cpuTemperatureCelsius()
        );
        heartbeatRepository.save(heartbeat);

        var event = new HeartbeatReceivedEvent(
                UUID.randomUUID(), ClockProvider.now(),
                command.deviceId(), command.tenantId(),
                command.firmwareVersion()
        );
        eventPublisher.publish(event);

        log.debug("Heartbeat received: device={}", command.deviceId());
    }
}
