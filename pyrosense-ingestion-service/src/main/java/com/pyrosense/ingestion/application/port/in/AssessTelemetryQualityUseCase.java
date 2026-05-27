package com.pyrosense.ingestion.application.port.in;

import com.pyrosense.ingestion.domain.model.quality.DataQualityAssessment;

import java.time.Instant;

public interface AssessTelemetryQualityUseCase {

    record AssessCommand(
            String deviceId,
            String tenantId,
            Instant periodStart,
            Instant periodEnd
    ) {}

    DataQualityAssessment execute(AssessCommand command);
}
