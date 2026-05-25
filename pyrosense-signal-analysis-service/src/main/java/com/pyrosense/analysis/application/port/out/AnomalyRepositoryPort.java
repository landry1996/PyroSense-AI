package com.pyrosense.analysis.application.port.out;

import com.pyrosense.analysis.domain.model.AnalysisResult;
import com.pyrosense.analysis.domain.model.SignalAnomaly;
import com.pyrosense.shared.id.DeviceId;

import java.time.Instant;
import java.util.List;

public interface AnomalyRepositoryPort {
    void saveResult(AnalysisResult result);
    List<SignalAnomaly> findByDeviceId(DeviceId deviceId, Instant from, Instant to);
    long countByDeviceId(DeviceId deviceId, Instant from, Instant to);
}
