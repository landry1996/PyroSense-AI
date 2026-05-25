package com.pyrosense.scoring.adapter.in.rest;

import com.pyrosense.scoring.application.port.in.GetTenantRiskQuery;
import com.pyrosense.scoring.application.port.in.GetTenantRiskQuery.RiskHistory;
import com.pyrosense.scoring.application.port.in.GetTenantRiskQuery.TenantRiskSummary;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/risk-scoring/tenant")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
public class TenantRiskController {

    private final GetTenantRiskQuery tenantRiskQuery;

    public TenantRiskController(GetTenantRiskQuery tenantRiskQuery) {
        this.tenantRiskQuery = tenantRiskQuery;
    }

    @GetMapping("/summary")
    public ResponseEntity<TenantRiskSummaryResponse> getTenantSummary() {
        TenantId tenantId = TenantContext.require();
        TenantRiskSummary summary = tenantRiskQuery.getTenantSummary(tenantId);
        return ResponseEntity.ok(new TenantRiskSummaryResponse(
                summary.avgScore(), summary.trend(), summary.buildingsAtRisk()));
    }

    @GetMapping("/history")
    public ResponseEntity<RiskHistoryResponse> getTenantHistory(
            @RequestParam(defaultValue = "30") int days) {
        TenantId tenantId = TenantContext.require();
        RiskHistory history = tenantRiskQuery.getTenantHistory(tenantId, days);
        List<RiskHistoryPointResponse> points = history.points().stream()
                .map(p -> new RiskHistoryPointResponse(p.date(), p.score()))
                .toList();
        return ResponseEntity.ok(new RiskHistoryResponse(points));
    }

    record TenantRiskSummaryResponse(double avgScore, String trend, int buildingsAtRisk) {}

    record RiskHistoryResponse(List<RiskHistoryPointResponse> points) {}

    record RiskHistoryPointResponse(String date, double score) {}
}
