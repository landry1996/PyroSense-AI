package com.pyrosense.scoring.application.port.out;

import com.pyrosense.scoring.domain.model.AnomalyInput;
import com.pyrosense.scoring.domain.model.RiskScore;
import com.pyrosense.shared.id.DeviceId;

import java.util.List;
import java.util.Optional;

public interface RiskModelPort {

    Optional<RiskScore> predict(DeviceId deviceId, List<AnomalyInput> anomalies);

    boolean isAvailable();

    String modelVersion();
}
