package com.pyrosense.ingestion.config;

import com.pyrosense.ingestion.application.port.out.DeviceAuthorizationPort;
import com.pyrosense.ingestion.application.port.out.HeartbeatRepositoryPort;
import com.pyrosense.ingestion.application.port.out.IdempotencyPort;
import com.pyrosense.ingestion.application.port.out.TelemetryEventPublisherPort;
import com.pyrosense.ingestion.application.port.out.TelemetryRepositoryPort;
import com.pyrosense.ingestion.application.usecase.IngestHeartbeatService;
import com.pyrosense.ingestion.application.usecase.IngestTelemetryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public IngestTelemetryService ingestTelemetryService(
            TelemetryRepositoryPort repository,
            TelemetryEventPublisherPort eventPublisher,
            DeviceAuthorizationPort deviceAuth,
            IdempotencyPort idempotency) {
        return new IngestTelemetryService(repository, eventPublisher, deviceAuth, idempotency);
    }

    @Bean
    public IngestHeartbeatService ingestHeartbeatService(
            DeviceAuthorizationPort deviceAuth,
            TelemetryEventPublisherPort eventPublisher,
            HeartbeatRepositoryPort heartbeatRepository) {
        return new IngestHeartbeatService(deviceAuth, eventPublisher, heartbeatRepository);
    }
}
