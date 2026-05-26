package com.pyrosense.dashboard.adapter.in.rest;

import com.pyrosense.dashboard.application.port.in.*;
import com.pyrosense.dashboard.config.SecurityConfig;
import com.pyrosense.dashboard.domain.model.DashboardOverview;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardTenantIsolationTest {

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

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void overview_shouldOnlyQueryCurrentTenant() throws Exception {
        TenantId tenantA = new TenantId(UUID.randomUUID());
        TenantId tenantB = new TenantId(UUID.randomUUID());
        TenantContext.set(tenantA);

        when(overviewQuery.getOverview(eq(tenantA))).thenReturn(
                new DashboardOverview(tenantA, 5, 20, 18, 2, 30.0, 1, 3, 2, 0, Instant.now()));

        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenantA.value().toString()));

        verify(overviewQuery).getOverview(eq(tenantA));
        verify(overviewQuery, never()).getOverview(eq(tenantB));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void riskyBuildings_shouldOnlyQueryCurrentTenant() throws Exception {
        TenantId tenantA = new TenantId(UUID.randomUUID());
        TenantContext.set(tenantA);

        when(riskyBuildingsQuery.getRiskyBuildings(eq(tenantA), any(int.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/dashboard/risky-buildings"))
                .andExpect(status().isOk());

        verify(riskyBuildingsQuery).getRiskyBuildings(eq(tenantA), any(int.class));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void riskTrend_shouldOnlyQueryCurrentTenant() throws Exception {
        TenantId tenantA = new TenantId(UUID.randomUUID());
        TenantContext.set(tenantA);

        when(riskTrendQuery.getRiskTrend(eq(tenantA), any(int.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/dashboard/risk-trend"))
                .andExpect(status().isOk());

        verify(riskTrendQuery).getRiskTrend(eq(tenantA), any(int.class));
    }

    @Test
    @WithMockUser(roles = "PROPERTY_MANAGER")
    void recentAlerts_shouldOnlyQueryCurrentTenant() throws Exception {
        TenantId tenantA = new TenantId(UUID.randomUUID());
        TenantContext.set(tenantA);

        when(recentAlertsQuery.getRecentAlerts(eq(tenantA), any(int.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/dashboard/recent-alerts"))
                .andExpect(status().isOk());

        verify(recentAlertsQuery).getRecentAlerts(eq(tenantA), any(int.class));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void noTenantContext_shouldFail() {
        TenantContext.clear();

        assertThatThrownBy(() ->
                mockMvc.perform(get("/api/v1/dashboard/overview")))
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
