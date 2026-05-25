package com.pyrosense.scoring.application.port.in;

import com.pyrosense.shared.id.TenantId;

import java.util.List;

public interface GetTenantRiskQuery {

    TenantRiskSummary getTenantSummary(TenantId tenantId);

    RiskHistory getTenantHistory(TenantId tenantId, int days);

    record TenantRiskSummary(double avgScore, String trend, int buildingsAtRisk) {}

    record RiskHistory(List<RiskHistoryPoint> points) {}

    record RiskHistoryPoint(String date, double score) {}
}
