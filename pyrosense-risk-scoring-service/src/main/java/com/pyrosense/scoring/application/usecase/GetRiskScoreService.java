package com.pyrosense.scoring.application.usecase;

import com.pyrosense.scoring.application.port.in.GetRiskScoreQuery;
import com.pyrosense.scoring.application.port.out.RiskAssessmentRepositoryPort;
import com.pyrosense.scoring.domain.model.RiskAssessment;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class GetRiskScoreService implements GetRiskScoreQuery {

    private final RiskAssessmentRepositoryPort repository;

    public GetRiskScoreService(RiskAssessmentRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RiskAssessment> getLatestByDevice(DeviceId deviceId) {
        return repository.findLatestByDevice(deviceId);
    }

    @Override
    public List<RiskAssessment> getHistoryByPanel(ElectricalPanelId panelId, Instant from, Instant to) {
        return repository.findHistoryByPanel(panelId, from, to);
    }

    @Override
    public BuildingSummary getBuildingSummary(BuildingId buildingId) {
        List<RiskAssessment> latestAssessments = repository.findLatestByBuilding(buildingId);

        if (latestAssessments.isEmpty()) {
            return new BuildingSummary(buildingId, 0, 0, 0, 0, 0, List.of());
        }

        int avgScore = (int) latestAssessments.stream()
                .mapToInt(a -> a.score().value()).average().orElse(0);
        int maxScore = latestAssessments.stream()
                .mapToInt(a -> a.score().value()).max().orElse(0);
        long criticalCount = latestAssessments.stream()
                .filter(a -> a.score().isCritical()).count();
        long highCount = latestAssessments.stream()
                .filter(a -> a.score().isHigh() && !a.score().isCritical()).count();

        List<RiskAssessment> topRisks = latestAssessments.stream()
                .sorted((a, b) -> Integer.compare(b.score().value(), a.score().value()))
                .limit(5)
                .toList();

        return new BuildingSummary(buildingId, latestAssessments.size(),
                avgScore, maxScore, criticalCount, highCount, topRisks);
    }
}
