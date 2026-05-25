package com.pyrosense.scoring.application.usecase;

import com.pyrosense.scoring.application.port.in.GetTenantRiskQuery;
import com.pyrosense.scoring.application.port.out.RiskAssessmentRepositoryPort;
import com.pyrosense.scoring.application.port.out.RiskAssessmentRepositoryPort.DailyRiskScore;
import com.pyrosense.shared.id.TenantId;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class GetTenantRiskService implements GetTenantRiskQuery {

    private static final int RISK_THRESHOLD = 50;
    private static final int TREND_WINDOW_DAYS = 7;

    private final RiskAssessmentRepositoryPort repository;

    public GetTenantRiskService(RiskAssessmentRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public TenantRiskSummary getTenantSummary(TenantId tenantId) {
        Instant now = Instant.now();
        Instant last7Days = now.minus(Duration.ofDays(TREND_WINDOW_DAYS));
        Instant previous7Days = last7Days.minus(Duration.ofDays(TREND_WINDOW_DAYS));

        double avgScore = repository.findAverageScoreByTenantBetween(tenantId, last7Days, now);
        double previousAvg = repository.findAverageScoreByTenantBetween(tenantId, previous7Days, last7Days);
        int buildingsAtRisk = repository.countBuildingsAtRisk(tenantId, RISK_THRESHOLD);

        String trend = computeTrend(avgScore, previousAvg);

        return new TenantRiskSummary(avgScore, trend, buildingsAtRisk);
    }

    @Override
    public RiskHistory getTenantHistory(TenantId tenantId, int days) {
        Instant from = Instant.now().minus(Duration.ofDays(days));
        List<DailyRiskScore> dailyScores = repository.findDailyAveragesByTenant(tenantId, from);

        List<RiskHistoryPoint> points = dailyScores.stream()
                .map(ds -> new RiskHistoryPoint(ds.date(), ds.score()))
                .toList();

        return new RiskHistory(points);
    }

    private String computeTrend(double currentAvg, double previousAvg) {
        if (previousAvg == 0 && currentAvg == 0) {
            return "STABLE";
        }
        double delta = currentAvg - previousAvg;
        if (delta > 5) return "DEGRADING";
        if (delta < -5) return "IMPROVING";
        return "STABLE";
    }
}
