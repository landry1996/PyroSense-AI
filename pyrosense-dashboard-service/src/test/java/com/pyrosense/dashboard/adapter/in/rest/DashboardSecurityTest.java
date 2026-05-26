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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardSecurityTest {

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
    void unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void tenantAdmin_shouldHaveAccess() throws Exception {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        TenantContext.set(tenantId);
        when(overviewQuery.getOverview(any())).thenReturn(
                new DashboardOverview(tenantId, 0, 0, 0, 0, 0, 0, 0, 0, 0, Instant.now()));

        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PROPERTY_MANAGER")
    void propertyManager_shouldHaveAccess() throws Exception {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        TenantContext.set(tenantId);
        when(overviewQuery.getOverview(any())).thenReturn(
                new DashboardOverview(tenantId, 0, 0, 0, 0, 0, 0, 0, 0, 0, Instant.now()));

        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ELECTRICIAN")
    void electrician_shouldNotAccessOverview() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ELECTRICIAN")
    void electrician_shouldAccessRecentAlerts() throws Exception {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        TenantContext.set(tenantId);
        when(recentAlertsQuery.getRecentAlerts(any(), any(int.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/dashboard/recent-alerts"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ELECTRICIAN")
    void electrician_shouldAccessPriorityInterventions() throws Exception {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        TenantContext.set(tenantId);
        when(interventionsQuery.getAssignedInterventions(any(), any(int.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/dashboard/priority-interventions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ELECTRICIAN")
    void electrician_shouldNotAccessRiskTrend() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        mockMvc.perform(get("/api/v1/dashboard/risk-trend"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ELECTRICIAN")
    void electrician_shouldNotAccessDeviceHealth() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        mockMvc.perform(get("/api/v1/dashboard/device-health"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPPORT_READONLY")
    void supportReadonly_shouldNotAccessOverview() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPPORT_READONLY")
    void supportReadonly_shouldNotAccessRiskTrend() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        mockMvc.perform(get("/api/v1/dashboard/risk-trend"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OCCUPANT")
    void occupant_shouldAccessRecentAlerts() throws Exception {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        TenantContext.set(tenantId);
        when(recentAlertsQuery.getRecentAlerts(any(), any(int.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/dashboard/recent-alerts"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OCCUPANT")
    void occupant_shouldNotAccessOverview() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "UNKNOWN_ROLE")
    void unknownRole_shouldBeForbidden() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isForbidden());
    }
}
