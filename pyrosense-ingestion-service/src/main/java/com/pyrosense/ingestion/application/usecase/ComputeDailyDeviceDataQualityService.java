package com.pyrosense.ingestion.application.usecase;

import com.pyrosense.ingestion.application.port.in.AssessTelemetryQualityUseCase;
import com.pyrosense.ingestion.application.port.in.ComputeDailyDeviceDataQualityUseCase;
import com.pyrosense.ingestion.domain.model.quality.DataQualityAssessment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class ComputeDailyDeviceDataQualityService implements ComputeDailyDeviceDataQualityUseCase {

    private static final Logger log = LoggerFactory.getLogger(ComputeDailyDeviceDataQualityService.class);

    private final AssessTelemetryQualityUseCase assessUseCase;

    public ComputeDailyDeviceDataQualityService(AssessTelemetryQualityUseCase assessUseCase) {
        this.assessUseCase = assessUseCase;
    }

    @Override
    public DataQualityAssessment computeForDevice(String deviceId, String tenantId) {
        Instant now = Instant.now();
        Instant yesterday = now.minus(24, ChronoUnit.HOURS);
        var command = new AssessTelemetryQualityUseCase.AssessCommand(deviceId, tenantId, yesterday, now);
        return assessUseCase.execute(command);
    }

    @Override
    public int computeForAllDevices(String tenantId) {
        log.info("Daily data quality computation requested for tenant={}", tenantId);
        return 0;
    }
}
