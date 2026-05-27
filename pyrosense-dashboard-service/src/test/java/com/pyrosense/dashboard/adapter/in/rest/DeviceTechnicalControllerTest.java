package com.pyrosense.dashboard.adapter.in.rest;

import com.pyrosense.dashboard.application.port.in.*;
import com.pyrosense.dashboard.domain.model.*;
import com.pyrosense.dashboard.domain.model.PilotDashboard.IncidentsSummary;
import com.pyrosense.dashboard.domain.model.PilotDashboard.PilotDeviceSummary;
import com.pyrosense.dashboard.domain.model.TelemetryQualityReport.*;
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

@WebMvcTest(DeviceTechnicalController.class)
class DeviceTechnicalControllerTest {

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
    void getTechnicalHealth_shouldReturnHealth() throws Exception {
        var health = new DeviceTechnicalHealth(
                "dev-001", "PYR-A1B2", "2.4.1", "rev-C", "WIFI",
                Instant.now(), 86400L, 87.5, 92.3, "A",
                null, 42.5, 120L, 2, 7, "ACTIVE");
        when(deviceTechnicalQuery.getDeviceTechnicalHealth(eq("dev-001"), anyString()))
                .thenReturn(health);

        mockMvc.perform(get("/api/v1/devices/dev-001/technical-health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("dev-001"))
                .andExpect(jsonPath("$.firmwareVersion").value("2.4.1"))
                .andExpect(jsonPath("$.signalQuality").value(87.5))
                .andExpect(jsonPath("$.dataQualityGrade").value("A"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void getTelemetryQuality_shouldReturnReport() throws Exception {
        Instant now = Instant.now();
        var report = new TelemetryQualityReport(
                "dev-001",
                List.of(new HourlyCount(now, 60)),
                List.of(new HourlyCount(now, 2)),
                List.of(new RejectionReason("CHECKSUM_MISMATCH", 2)),
                List.of(new QualityPoint(now, 88.0)),
                List.of(new OfflinePeriod(now.minusSeconds(3600), now.minusSeconds(1800), 30L)));
        when(deviceTechnicalQuery.getTelemetryQuality(eq("dev-001"), anyString(), any(), any()))
                .thenReturn(report);

        mockMvc.perform(get("/api/v1/devices/dev-001/telemetry-quality")
                        .param("from", "2026-05-26T00:00:00Z")
                        .param("to", "2026-05-27T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("dev-001"))
                .andExpect(jsonPath("$.rejectionReasons[0].reason").value("CHECKSUM_MISMATCH"))
                .andExpect(jsonPath("$.offlinePeriods[0].durationMinutes").value(30));
    }

    @Test
    @WithMockUser(roles = "PROPERTY_MANAGER")
    void getSecurityStatus_shouldReturnStatus() throws Exception {
        var status = new DeviceSecurityStatus(
                "dev-001", "ACTIVE", 3,
                Instant.now().minusSeconds(86400 * 14), 0, 0, false, null);
        when(deviceTechnicalQuery.getDeviceSecurityStatus(eq("dev-001"), anyString()))
                .thenReturn(status);

        mockMvc.perform(get("/api/v1/devices/dev-001/security-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentialStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.credentialVersion").value(3))
                .andExpect(jsonPath("$.failedAuthAttempts").value(0));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void getPilotDashboard_shouldReturnDashboard() throws Exception {
        UUID pilotId = UUID.randomUUID();
        var dashboard = new PilotDashboard(
                pilotId, "Field Pilot 10", "ACTIVE",
                List.of(new PilotDeviceSummary("dev-001", "PYR-A1", "ACTIVE", 90.0, "A", Instant.now(), "INSTALLED")),
                new IncidentsSummary(2, 1, 5, 8),
                PilotKpiSnapshot.compute(pilotId, LocalDate.now(), 10, 8, 2, 80.0, 99.0, 75.0, 5, 3, 1, 2, 5, 30.0));
        when(deviceTechnicalQuery.getPilotDashboard(eq(pilotId), anyString()))
                .thenReturn(dashboard);

        mockMvc.perform(get("/api/v1/pilots/{id}/dashboard", pilotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pilotName").value("Field Pilot 10"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.devices[0].deviceId").value("dev-001"))
                .andExpect(jsonPath("$.incidentsSummary.open").value(2));
    }

    @Test
    @WithMockUser(roles = "SUPPORT_READONLY")
    void supportReadonly_shouldAccessTechnicalHealth() throws Exception {
        var health = new DeviceTechnicalHealth(
                "dev-001", "PYR-A1", "2.4.1", "rev-C", "WIFI",
                Instant.now(), 86400L, 87.5, 92.3, "A",
                null, 42.5, 120L, 0, 0, "ACTIVE");
        when(deviceTechnicalQuery.getDeviceTechnicalHealth(eq("dev-001"), anyString()))
                .thenReturn(health);

        mockMvc.perform(get("/api/v1/devices/dev-001/technical-health"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/devices/dev-001/technical-health"))
                .andExpect(status().isUnauthorized());
    }
}
