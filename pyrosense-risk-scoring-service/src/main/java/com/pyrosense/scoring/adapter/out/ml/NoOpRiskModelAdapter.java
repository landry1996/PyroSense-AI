package com.pyrosense.scoring.adapter.out.ml;

import com.pyrosense.scoring.application.port.out.RiskModelPort;
import com.pyrosense.scoring.domain.model.AnomalyInput;
import com.pyrosense.scoring.domain.model.RiskScore;
import com.pyrosense.shared.id.DeviceId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class NoOpRiskModelAdapter implements RiskModelPort {

    private static final String VERSION = "noop-v1.0.0";

    @Override
    public Optional<RiskScore> predict(DeviceId deviceId, List<AnomalyInput> anomalies) {
        return Optional.empty();
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String modelVersion() {
        return VERSION;
    }
}
