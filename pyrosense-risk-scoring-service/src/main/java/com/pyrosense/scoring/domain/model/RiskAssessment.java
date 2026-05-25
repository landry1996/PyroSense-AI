package com.pyrosense.scoring.domain.model;

import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class RiskAssessment {

    private final UUID id;
    private final DeviceId deviceId;
    private final ElectricalPanelId panelId;
    private final UUID circuitId;
    private final RiskScore score;
    private final RiskLevel level;
    private final RiskTrend trend;
    private final List<RiskFactor> factors;
    private final Duration predictedIncidentWindow;
    private final String recommendation;
    private final Instant computedAt;

    public RiskAssessment(UUID id, DeviceId deviceId, ElectricalPanelId panelId, UUID circuitId,
                          RiskScore score, RiskTrend trend, List<RiskFactor> factors,
                          Duration predictedIncidentWindow, String recommendation, Instant computedAt) {
        this.id = Objects.requireNonNull(id);
        this.deviceId = Objects.requireNonNull(deviceId);
        this.panelId = panelId;
        this.circuitId = circuitId;
        this.score = Objects.requireNonNull(score);
        this.level = score.level();
        this.trend = Objects.requireNonNull(trend);
        this.factors = List.copyOf(factors);
        this.predictedIncidentWindow = predictedIncidentWindow;
        this.recommendation = Objects.requireNonNull(recommendation);
        this.computedAt = Objects.requireNonNull(computedAt);
    }

    public UUID id() { return id; }
    public DeviceId deviceId() { return deviceId; }
    public ElectricalPanelId panelId() { return panelId; }
    public UUID circuitId() { return circuitId; }
    public RiskScore score() { return score; }
    public RiskLevel level() { return level; }
    public RiskTrend trend() { return trend; }
    public List<RiskFactor> factors() { return factors; }
    public Duration predictedIncidentWindow() { return predictedIncidentWindow; }
    public String recommendation() { return recommendation; }
    public Instant computedAt() { return computedAt; }

    public boolean isCritical() { return score.isCritical(); }
    public boolean isLevelChange(RiskLevel previousLevel) { return this.level != previousLevel; }
}
