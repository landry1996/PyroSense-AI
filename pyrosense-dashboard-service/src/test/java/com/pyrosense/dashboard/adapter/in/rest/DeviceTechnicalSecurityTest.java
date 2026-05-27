package com.pyrosense.dashboard.adapter.in.rest;

import com.pyrosense.dashboard.application.port.in.*;
import com.pyrosense.dashboard.config.SecurityConfig;
import com.pyrosense.dashboard.domain.model.DeviceTechnicalHealth;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceTechnicalController.class)
@Import(SecurityConfig.class)
class DeviceTechnicalSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceTechnicalQuery deviceTechnicalQuery;

    @MockitoBean
    private ManagePilotUseCase pilotUseCase;
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
        mockMvc.perform(get("/api/v1/devices/dev-001/technical-health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void platformAdmin_shouldAccessTechnicalHealth() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        when(deviceTechnicalQuery.getDeviceTechnicalHealth(anyString(), anyString()))
                .thenReturn(stubHealth());

        mockMvc.perform(get("/api/v1/devices/dev-001/technical-health"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void tenantAdmin_shouldAccessTechnicalHealth() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        when(deviceTechnicalQuery.getDeviceTechnicalHealth(anyString(), anyString()))
                .thenReturn(stubHealth());

        mockMvc.perform(get("/api/v1/devices/dev-001/technical-health"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PROPERTY_MANAGER")
    void propertyManager_shouldAccessTechnicalHealth() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        when(deviceTechnicalQuery.getDeviceTechnicalHealth(anyString(), anyString()))
                .thenReturn(stubHealth());

        mockMvc.perform(get("/api/v1/devices/dev-001/technical-health"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SUPPORT_READONLY")
    void supportReadonly_shouldAccessTechnicalHealth() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        when(deviceTechnicalQuery.getDeviceTechnicalHealth(anyString(), anyString()))
                .thenReturn(stubHealth());

        mockMvc.perform(get("/api/v1/devices/dev-001/technical-health"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ELECTRICIAN")
    void electrician_shouldNotAccessTechnicalHealth() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/devices/dev-001/technical-health"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OCCUPANT")
    void occupant_shouldNotAccessTechnicalHealth() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/devices/dev-001/technical-health"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ELECTRICIAN")
    void electrician_shouldNotAccessSecurityStatus() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/devices/dev-001/security-status"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OCCUPANT")
    void occupant_shouldNotAccessPilotDashboard() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/pilots/" + UUID.randomUUID() + "/dashboard"))
                .andExpect(status().isForbidden());
    }

    private DeviceTechnicalHealth stubHealth() {
        return new DeviceTechnicalHealth(
                "dev-001", "PYR-A1", "2.4.1", "rev-C", "WIFI",
                Instant.now(), 86400L, 87.5, 92.3, "A",
                null, 42.5, 120L, 0, 0, "ACTIVE");
    }
}
