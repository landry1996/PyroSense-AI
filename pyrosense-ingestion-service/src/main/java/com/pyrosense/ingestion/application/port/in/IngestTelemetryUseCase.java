package com.pyrosense.ingestion.application.port.in;

import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;

import java.time.Instant;

public interface IngestTelemetryUseCase {

    record TelemetryCommand(
            DeviceId deviceId,
            TenantId tenantId,
            Instant timestamp,
            int samplingWindowMs,
            double rmsCurrent,
            double rmsVoltage,
            double activePower,
            double reactivePower,
            double powerFactor,
            double thd,
            double temperatureCelsius,
            double hfNoiseLevel,
            int microArcCount,
            int transientCount,
            String firmwareVersion,
            String rawPayload
    ) {}

    enum IngestionResult {
        ACCEPTED,
        DUPLICATE,
        REJECTED_INVALID,
        REJECTED_UNAUTHORIZED
    }

    IngestionResult execute(TelemetryCommand command);
}
