package com.pyrosense.dashboard.adapter.in.rest;

import com.pyrosense.dashboard.application.port.in.*;
import com.pyrosense.dashboard.domain.model.*;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'PROPERTY_MANAGER', 'ELECTRICIAN', 'SUPPORT_READONLY', 'PLATFORM_ADMIN')")
public class DashboardController {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final int DEFAULT_PERIOD_DAYS = 30;
    private static final int MAX_PERIOD_DAYS = 90;

    private final GetDashboardOverviewQuery overviewQuery;
    private final GetRiskyBuildingsQuery riskyBuildingsQuery;
    private final GetRiskTrendQuery riskTrendQuery;
    private final GetRecentAlertsQuery recentAlertsQuery;
    private final GetPriorityInterventionsQuery interventionsQuery;
    private final GetDeviceHealthQuery deviceHealthQuery;

    public DashboardController(
            GetDashboardOverviewQuery overviewQuery,
            GetRiskyBuildingsQuery riskyBuildingsQuery,
            GetRiskTrendQuery riskTrendQuery,
            GetRecentAlertsQuery recentAlertsQuery,
            GetPriorityInterventionsQuery interventionsQuery,
            GetDeviceHealthQuery deviceHealthQuery) {
        this.overviewQuery = overviewQuery;
        this.riskyBuildingsQuery = riskyBuildingsQuery;
        this.riskTrendQuery = riskTrendQuery;
        this.recentAlertsQuery = recentAlertsQuery;
        this.interventionsQuery = interventionsQuery;
        this.deviceHealthQuery = deviceHealthQuery;
    }

    @GetMapping("/overview")
    public ResponseEntity<OverviewResponse> getOverview() {
        TenantId tenantId = TenantContext.require();
        DashboardOverview overview = overviewQuery.getOverview(tenantId);
        return ResponseEntity.ok(toOverviewResponse(overview));
    }

    @GetMapping("/risky-buildings")
    public ResponseEntity<List<RiskyBuildingResponse>> getRiskyBuildings(
            @RequestParam(defaultValue = "10") int limit) {
        TenantId tenantId = TenantContext.require();
        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<RiskyBuilding> buildings = riskyBuildingsQuery.getRiskyBuildings(tenantId, effectiveLimit);
        return ResponseEntity.ok(buildings.stream().map(this::toRiskyBuildingResponse).toList());
    }

    @GetMapping("/risk-trend")
    public ResponseEntity<List<RiskTrendResponse>> getRiskTrend(
            @RequestParam(defaultValue = "30d") String period) {
        TenantId tenantId = TenantContext.require();
        int days = parsePeriodDays(period);
        List<RiskTrendPoint> trend = riskTrendQuery.getRiskTrend(tenantId, days);
        return ResponseEntity.ok(trend.stream().map(this::toRiskTrendResponse).toList());
    }

    @GetMapping("/recent-alerts")
    public ResponseEntity<List<RecentAlertResponse>> getRecentAlerts(
            @RequestParam(defaultValue = "10") int limit) {
        TenantId tenantId = TenantContext.require();
        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<RecentAlert> alerts = recentAlertsQuery.getRecentAlerts(tenantId, effectiveLimit);
        return ResponseEntity.ok(alerts.stream().map(this::toRecentAlertResponse).toList());
    }

    @GetMapping("/priority-interventions")
    public ResponseEntity<List<PriorityInterventionResponse>> getPriorityInterventions(
            @RequestParam(defaultValue = "10") int limit) {
        TenantId tenantId = TenantContext.require();
        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);

        List<PriorityIntervention> interventions;
        if (isElectrician()) {
            String userId = getCurrentUserId();
            interventions = interventionsQuery.getAssignedInterventions(userId, effectiveLimit);
        } else {
            interventions = interventionsQuery.getPriorityInterventions(tenantId, effectiveLimit);
        }
        return ResponseEntity.ok(interventions.stream().map(this::toInterventionResponse).toList());
    }

    @GetMapping("/device-health")
    public ResponseEntity<DeviceHealthResponse> getDeviceHealth() {
        TenantId tenantId = TenantContext.require();
        DeviceHealthSummary health = deviceHealthQuery.getDeviceHealth(tenantId);
        return ResponseEntity.ok(toDeviceHealthResponse(health));
    }

    private int parsePeriodDays(String period) {
        try {
            String cleaned = period.replaceAll("[^0-9]", "");
            int days = Integer.parseInt(cleaned);
            return Math.min(Math.max(days, 1), MAX_PERIOD_DAYS);
        } catch (NumberFormatException e) {
            return DEFAULT_PERIOD_DAYS;
        }
    }

    private boolean isElectrician() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ELECTRICIAN"))
                && auth.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_TENANT_ADMIN")
                        || a.getAuthority().equals("ROLE_PROPERTY_MANAGER"));
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getSubject();
        }
        return "";
    }

    private OverviewResponse toOverviewResponse(DashboardOverview o) {
        return new OverviewResponse(
                o.tenantId().value().toString(), o.totalBuildings(), o.totalDevices(),
                o.activeDevices(), o.offlineDevices(), o.averageRiskScore(),
                o.criticalAlerts(), o.warningAlerts(), o.openInterventions(),
                o.overdueInterventions(), o.lastUpdatedAt());
    }

    private RiskyBuildingResponse toRiskyBuildingResponse(RiskyBuilding b) {
        return new RiskyBuildingResponse(
                b.buildingId(), b.name(), b.address(), b.riskScore(),
                b.status(), b.openAlerts(), b.criticalAlerts(),
                b.highestSeverity(), b.lastAlertAt());
    }

    private RiskTrendResponse toRiskTrendResponse(RiskTrendPoint p) {
        return new RiskTrendResponse(p.date(), p.averageScore(), p.maxScore(), p.alertCount());
    }

    private RecentAlertResponse toRecentAlertResponse(RecentAlert a) {
        return new RecentAlertResponse(
                a.alertId(), a.title(), a.severity(), a.status(), a.type(),
                a.buildingId(), a.buildingName(), a.deviceId(),
                a.createdAt(), a.slaBreached());
    }

    private PriorityInterventionResponse toInterventionResponse(PriorityIntervention i) {
        return new PriorityInterventionResponse(
                i.interventionId(), i.type(), i.priority(), i.status(),
                i.buildingId(), i.buildingName(), i.assignedTo(),
                i.scheduledDate(), i.createdAt(), i.overdue());
    }

    private DeviceHealthResponse toDeviceHealthResponse(DeviceHealthSummary h) {
        return new DeviceHealthResponse(
                h.totalDevices(), h.activeDevices(), h.offlineDevices(),
                h.provisionedDevices(), h.revokedDevices(), h.avgUptimePercent(),
                h.devicesWithHighRisk(), h.devicesSilentOver24h());
    }

    // --- Response DTOs ---

    record OverviewResponse(
            String tenantId, int totalBuildings, int totalDevices,
            int activeDevices, int offlineDevices, double averageRiskScore,
            int criticalAlerts, int warningAlerts, int openInterventions,
            int overdueInterventions, Instant lastUpdatedAt) {}

    record RiskyBuildingResponse(
            String buildingId, String name, String address, double riskScore,
            String status, int openAlerts, int criticalAlerts,
            String highestSeverity, Instant lastAlertAt) {}

    record RiskTrendResponse(LocalDate date, double averageScore, double maxScore, int alertCount) {}

    record RecentAlertResponse(
            String alertId, String title, String severity, String status, String type,
            String buildingId, String buildingName, String deviceId,
            Instant createdAt, boolean slaBreached) {}

    record PriorityInterventionResponse(
            String interventionId, String type, String priority, String status,
            String buildingId, String buildingName, String assignedTo,
            LocalDate scheduledDate, Instant createdAt, boolean overdue) {}

    record DeviceHealthResponse(
            int totalDevices, int activeDevices, int offlineDevices,
            int provisionedDevices, int revokedDevices, double avgUptimePercent,
            int devicesWithHighRisk, int devicesSilentOver24h) {}
}
