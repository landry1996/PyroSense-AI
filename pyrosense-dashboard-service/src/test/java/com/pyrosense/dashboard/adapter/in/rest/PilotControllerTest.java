package com.pyrosense.dashboard.adapter.in.rest;

import com.pyrosense.dashboard.application.port.in.*;
import com.pyrosense.dashboard.application.port.in.ManagePilotUseCase.*;
import com.pyrosense.dashboard.domain.model.*;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PilotController.class)
class PilotControllerTest {

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
    void createPilot_shouldReturn201() throws Exception {
        var pilot = new PilotProgram(UUID.randomUUID(), tenantId.value().toString(), "Pilot 10", "Field pilot");

        when(pilotUseCase.createPilot(any(CreatePilotCommand.class))).thenReturn(pilot);

        mockMvc.perform(post("/api/v1/pilots")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Pilot 10", "description": "Field pilot"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pilot 10"))
                .andExpect(jsonPath("$.status").value("PREPARING"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void listPilots_shouldReturnList() throws Exception {
        var pilot = new PilotProgram(UUID.randomUUID(), tenantId.value().toString(), "Pilot A", "Desc",
                PilotStatus.ACTIVE, Instant.now(), Instant.now(), null);
        when(pilotUseCase.listPilots(anyString())).thenReturn(List.of(pilot));

        mockMvc.perform(get("/api/v1/pilots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pilot A"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void getPilot_shouldReturnDetail() throws Exception {
        UUID pilotId = UUID.randomUUID();
        var pilot = new PilotProgram(pilotId, tenantId.value().toString(), "Pilot X", "Desc",
                PilotStatus.ACTIVE, Instant.now(), Instant.now(), null);
        var device = PilotDevice.plan(pilotId, "dev-1", "SN-001", "Site A", "Circuit 1");
        pilot.addDevice(device);
        when(pilotUseCase.getPilot(eq(pilotId), anyString())).thenReturn(pilot);

        mockMvc.perform(get("/api/v1/pilots/{id}", pilotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pilot X"))
                .andExpect(jsonPath("$.devices[0].serialNumber").value("SN-001"));
    }

    @Test
    @WithMockUser(roles = "PROPERTY_MANAGER")
    void updateStatus_shouldReturnUpdatedPilot() throws Exception {
        UUID pilotId = UUID.randomUUID();
        var pilot = new PilotProgram(pilotId, tenantId.value().toString(), "Pilot", "Desc",
                PilotStatus.ACTIVE, Instant.now(), Instant.now(), null);
        when(pilotUseCase.updateStatus(eq(pilotId), anyString(), eq(PilotStatus.ACTIVE))).thenReturn(pilot);

        mockMvc.perform(patch("/api/v1/pilots/{id}/status", pilotId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "ACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void addDevice_shouldReturn201() throws Exception {
        UUID pilotId = UUID.randomUUID();
        var device = PilotDevice.plan(pilotId, "device-99", "SN-X99", "Site B", "Breaker #3");
        when(pilotUseCase.addDevice(eq(pilotId), anyString(), any(AddDeviceCommand.class))).thenReturn(device);

        mockMvc.perform(post("/api/v1/pilots/{id}/devices", pilotId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceId":"device-99","serialNumber":"SN-X99","siteName":"Site B","circuitDescription":"Breaker #3"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deviceId").value("device-99"))
                .andExpect(jsonPath("$.status").value("PLANNED"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void getKpis_shouldReturnList() throws Exception {
        UUID pilotId = UUID.randomUUID();
        var kpi = PilotKpiSnapshot.compute(pilotId, LocalDate.of(2026, 5, 27),
                10, 8, 2, 80.0, 99.0, 75.0, 5, 3, 1, 2, 1, 45.0);
        when(pilotUseCase.getKpis(eq(pilotId), anyString())).thenReturn(List.of(kpi));

        mockMvc.perform(get("/api/v1/pilots/{id}/kpis", pilotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalDevices").value(10))
                .andExpect(jsonPath("$[0].activeDevices").value(8))
                .andExpect(jsonPath("$[0].uptimePercent").value(80.0));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void generateReport_shouldReturn201() throws Exception {
        UUID pilotId = UUID.randomUUID();
        var kpi = PilotKpiSnapshot.compute(pilotId, LocalDate.of(2026, 5, 27),
                10, 9, 1, 90.0, 99.5, 80.0, 3, 2, 0, 1, 0, 30.0);
        when(pilotUseCase.computeCurrentKpis(eq(pilotId), anyString())).thenReturn(kpi);

        mockMvc.perform(post("/api/v1/pilots/{id}/reports", pilotId)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalDevices").value(10))
                .andExpect(jsonPath("$.activeDevices").value(9));
    }

    @Test
    void unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/pilots"))
                .andExpect(status().isUnauthorized());
    }
}
