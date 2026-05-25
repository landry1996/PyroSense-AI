package com.pyrosense.scoring.application.port.in;

import com.pyrosense.scoring.domain.model.RiskAssessment;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GetRiskScoreQuery {

    Optional<RiskAssessment> getLatestByDevice(DeviceId deviceId);

    List<RiskAssessment> getHistoryByPanel(ElectricalPanelId panelId, Instant from, Instant to);

    BuildingSummary getBuildingSummary(BuildingId buildingId);

    record BuildingSummary(
            BuildingId buildingId,
            int deviceCount,
            int averageScore,
            int maxScore,
            long criticalCount,
            long highCount,
            List<RiskAssessment> topRisks
    ) {}
}
