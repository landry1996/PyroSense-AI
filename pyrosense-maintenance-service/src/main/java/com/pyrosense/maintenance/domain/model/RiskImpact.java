package com.pyrosense.maintenance.domain.model;

import java.util.Objects;

public record RiskImpact(
        int riskScoreBefore,
        int riskScoreAfter,
        Integer avoidedIncidentEstimateDays
) {
    public RiskImpact {
        if (riskScoreBefore < 0 || riskScoreBefore > 100)
            throw new IllegalArgumentException("riskScoreBefore must be 0-100");
        if (riskScoreAfter < 0 || riskScoreAfter > 100)
            throw new IllegalArgumentException("riskScoreAfter must be 0-100");
    }

    public int riskReduction() {
        return Math.max(0, riskScoreBefore - riskScoreAfter);
    }

    public boolean hasImprovement() {
        return riskScoreAfter < riskScoreBefore;
    }
}
