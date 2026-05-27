package com.pyrosense.ingestion.application.port.in;

import com.pyrosense.ingestion.domain.model.quality.DataQualityAssessment;

public interface ComputeDailyDeviceDataQualityUseCase {

    DataQualityAssessment computeForDevice(String deviceId, String tenantId);

    int computeForAllDevices(String tenantId);
}
