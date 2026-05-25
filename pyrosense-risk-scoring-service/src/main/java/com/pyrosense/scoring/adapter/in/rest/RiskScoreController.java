package com.pyrosense.scoring.adapter.in.rest;

import com.pyrosense.scoring.application.port.in.GetRiskScoreQuery;
import com.pyrosense.scoring.application.port.in.GetRiskScoreQuery.BuildingSummary;
import com.pyrosense.scoring.domain.model.RiskAssessment;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risk-scores")
public class RiskScoreController {

    private final GetRiskScoreQuery riskScoreQuery;

    public RiskScoreController(GetRiskScoreQuery riskScoreQuery) {
        this.riskScoreQuery = riskScoreQuery;
    }

    @GetMapping("/devices/{deviceId}/latest")
    public ResponseEntity<RiskScoreResponse> getLatestByDevice(@PathVariable String deviceId) {
        return riskScoreQuery.getLatestByDevice(DeviceId.from(deviceId))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/panels/{panelId}/history")
    public ResponseEntity<List<RiskScoreResponse>> getHistoryByPanel(
            @PathVariable String panelId,
            @RequestParam(defaultValue = "7") int daysBack) {
        ElectricalPanelId id = new ElectricalPanelId(UUID.fromString(panelId));
        Instant from = Instant.now().minus(Duration.ofDays(daysBack));
        List<RiskScoreResponse> history = riskScoreQuery.getHistoryByPanel(id, from, Instant.now())
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/buildings/{buildingId}/summary")
    public ResponseEntity<BuildingSummaryResponse> getBuildingSummary(@PathVariable String buildingId) {
        BuildingSummary summary = riskScoreQuery.getBuildingSummary(new BuildingId(UUID.fromString(buildingId)));
        return ResponseEntity.ok(new BuildingSummaryResponse(
                buildingId, summary.deviceCount(), summary.averageScore(),
                summary.maxScore(), summary.criticalCount(), summary.highCount()));
    }

    private RiskScoreResponse toResponse(RiskAssessment a) {
        List<FactorResponse> factors = a.factors().stream()
                .map(f -> new FactorResponse(f.name(), f.description(), f.weight(), f.normalizedValue(), f.contribution()))
                .toList();
        return new RiskScoreResponse(
                a.deviceId().value().toString(),
                a.score().value(),
                a.level().name(),
                a.trend().name(),
                a.predictedIncidentWindow() != null ? a.predictedIncidentWindow().toDays() : null,
                a.recommendation(),
                factors,
                a.computedAt()
        );
    }

    record RiskScoreResponse(String deviceId, int score, String level, String trend,
                              Long predictedIncidentDays, String recommendation,
                              List<FactorResponse> factors, Instant computedAt) {}
    record FactorResponse(String name, String description, double weight, double value, double contribution) {}
    record BuildingSummaryResponse(String buildingId, int deviceCount, int avgScore,
                                    int maxScore, long criticalCount, long highCount) {}
}
