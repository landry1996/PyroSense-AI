package com.pyrosense.dashboard.adapter.in.rest;

import com.pyrosense.dashboard.application.port.in.*;
import com.pyrosense.dashboard.domain.model.*;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetDashboardOverviewQuery overviewQuery;
    @MockitoBean
    private GetRiskyBuildingsQuery riskyBuildingsQuery;
    @MockitoBean
    private GetRiskTrendQuery riskTrendQuery;
    @MockitoBean
    private GetRecentAlertsQuery recentAlertsQuery;
    @MockitoBean
    private GetPriorityInterventionsQuery interventionsQuery;
    @MockitoBean
    private GetDeviceHealthQuery deviceHealthQuery;

    private final TenantId tenantId = new TenantId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        TenantContext.set(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void getOverview_shouldReturnOverview() throws Exception {
        DashboardOverview overview = new DashboardOverview(
                tenantId, 10, 50, 45, 5, 35.5, 2, 8, 3, 1, Instant.now());
        when(overviewQuery.getOverview(tenantId)).thenReturn(overview);

        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBuildings").value(10))
                .andExpect(jsonPath("$.totalDevices").value(50))
                .andExpect(jsonPath("$.activeDevices").value(45))
                .andExpect(jsonPath("$.criticalAlerts").value(2))
                .andExpect(jsonPath("$.averageRiskScore").value(35.5));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void getRiskyBuildings_shouldReturnList() throws Exception {
        List<RiskyBuilding> buildings = List.of(
                new RiskyBuilding("b1", "HQ", "1 Rue A", 78.5, "AT_RISK", 5, 2, "CRITICAL", Instant.now()));
        when(riskyBuildingsQuery.getRiskyBuildings(eq(tenantId), anyInt())).thenReturn(buildings);

        mockMvc.perform(get("/api/v1/dashboard/risky-buildings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("HQ"))
                .andExpect(jsonPath("$[0].riskScore").value(78.5));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void getRiskTrend_shouldParsePeriod() throws Exception {
        List<RiskTrendPoint> trend = List.of(
                new RiskTrendPoint(LocalDate.of(2026, 5, 1), 40.0, 65.0, 3));
        when(riskTrendQuery.getRiskTrend(eq(tenantId), eq(30))).thenReturn(trend);

        mockMvc.perform(get("/api/v1/dashboard/risk-trend?period=30d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].averageScore").value(40.0))
                .andExpect(jsonPath("$[0].alertCount").value(3));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void getRecentAlerts_shouldReturnAlerts() throws Exception {
        List<RecentAlert> alerts = List.of(
                new RecentAlert("a1", "Temperature elevee", "CRITICAL", "OPEN",
                        "OVERHEATING", "b1", "Building A", "d1", Instant.now(), false));
        when(recentAlertsQuery.getRecentAlerts(eq(tenantId), anyInt())).thenReturn(alerts);

        mockMvc.perform(get("/api/v1/dashboard/recent-alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Temperature elevee"))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void getDeviceHealth_shouldReturnSummary() throws Exception {
        DeviceHealthSummary health = new DeviceHealthSummary(50, 45, 3, 1, 1, 92.5, 4, 2);
        when(deviceHealthQuery.getDeviceHealth(tenantId)).thenReturn(health);

        mockMvc.perform(get("/api/v1/dashboard/device-health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDevices").value(50))
                .andExpect(jsonPath("$.activeDevices").value(45))
                .andExpect(jsonPath("$.devicesWithHighRisk").value(4));
    }

    @Test
    void shouldReject_unauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isUnauthorized());
    }
}
