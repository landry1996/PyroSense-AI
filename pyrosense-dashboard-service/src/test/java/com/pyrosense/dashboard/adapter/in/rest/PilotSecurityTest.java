package com.pyrosense.dashboard.adapter.in.rest;

import com.pyrosense.dashboard.application.port.in.*;
import com.pyrosense.dashboard.application.port.in.ManagePilotUseCase.*;
import com.pyrosense.dashboard.config.SecurityConfig;
import com.pyrosense.dashboard.domain.model.*;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PilotController.class)
@Import(SecurityConfig.class)
class PilotSecurityTest {

    @Autowired
    private MockMvc mockMvc;

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
        mockMvc.perform(get("/api/v1/pilots"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void tenantAdmin_shouldAccessPilots() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        when(pilotUseCase.listPilots(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pilots"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PROPERTY_MANAGER")
    void propertyManager_shouldAccessPilots() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        when(pilotUseCase.listPilots(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pilots"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void platformAdmin_shouldAccessPilots() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        when(pilotUseCase.listPilots(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pilots"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ELECTRICIAN")
    void electrician_shouldNotAccessPilots() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/pilots"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OCCUPANT")
    void occupant_shouldNotAccessPilots() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/pilots"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPPORT_READONLY")
    void supportReadonly_shouldNotAccessPilots() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/pilots"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void tenantAdmin_shouldCreatePilot() throws Exception {
        TenantId tid = new TenantId(UUID.randomUUID());
        TenantContext.set(tid);
        var pilot = new PilotProgram(UUID.randomUUID(), tid.value().toString(), "Test", "Desc");
        when(pilotUseCase.createPilot(any(CreatePilotCommand.class))).thenReturn(pilot);

        mockMvc.perform(post("/api/v1/pilots")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test","description":"Desc"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ELECTRICIAN")
    void electrician_shouldNotCreatePilot() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/pilots")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test","description":"Desc"}
                                """))
                .andExpect(status().isForbidden());
    }
}
