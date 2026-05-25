package com.pyrosense.scoring.application.port.in;

import com.pyrosense.scoring.domain.model.AnomalyInput;
import com.pyrosense.scoring.domain.model.RiskAssessment;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;

import java.util.List;
import java.util.UUID;

public interface CalculateRiskUseCase {

    RiskAssessment calculate(CalculateRiskCommand command);

    record CalculateRiskCommand(
            DeviceId deviceId,
            ElectricalPanelId panelId,
            UUID circuitId,
            List<AnomalyInput> anomalies,
            boolean deviceOnline,
            boolean baselineAvailable
    ) {
        public CalculateRiskCommand {
            if (deviceId == null) throw new IllegalArgumentException("deviceId required");
            if (anomalies == null) anomalies = List.of();
        }
    }
}
