package com.pyrosense.scoring.application.port.out;

import com.pyrosense.scoring.domain.model.RiskAssessment;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RiskAssessmentRepositoryPort {

    void save(RiskAssessment assessment);

    Optional<RiskAssessment> findLatestByDevice(DeviceId deviceId);

    List<RiskAssessment> findHistoryByPanel(ElectricalPanelId panelId, Instant from, Instant to);

    List<RiskAssessment> findLatestByBuilding(BuildingId buildingId);

    List<Integer> findRecentScores(DeviceId deviceId, int limit);
}
