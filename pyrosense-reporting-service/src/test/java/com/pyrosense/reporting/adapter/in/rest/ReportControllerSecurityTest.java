package com.pyrosense.reporting.adapter.in.rest;

import com.pyrosense.reporting.application.port.in.GenerateReportUseCase;
import com.pyrosense.reporting.application.port.in.GetReportQuery;
import com.pyrosense.reporting.application.port.in.RequestReportUseCase;
import com.pyrosense.reporting.domain.model.*;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.pyrosense.reporting.config.SecurityConfig.class)
class ReportControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GenerateReportUseCase generateUseCase;

    @MockBean
    private GetReportQuery queryUseCase;

    @MockBean
    private RequestReportUseCase requestUseCase;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantId(UUID.randomUUID()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void unauthenticatedShouldBeRejectedOnProtectedEndpoints() throws Exception {
        int status = mockMvc.perform(get("/api/v1/reports"))
                .andReturn().getResponse().getStatus();
        // Spring Security returns 401 or 403 for unauthenticated depending on configuration
        assertTrue(status == 401 || status == 403,
                "Expected 401 or 403 but got " + status);
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void adminShouldNotBeForbiddenOnReportList() throws Exception {
        mockMvc.perform(get("/api/v1/reports"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OCCUPANT")
    void occupantShouldNotAccessReportList() throws Exception {
        mockMvc.perform(get("/api/v1/reports"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void tenantAdminShouldAccessMonthlyHealthEndpoint() throws Exception {
        Report mockReport = new Report(UUID.randomUUID(),
                new TenantId(UUID.randomUUID()), new BuildingId(UUID.randomUUID()),
                ReportType.MONTHLY_HEALTH,
                Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-01-31T23:59:59Z"));
        when(requestUseCase.requestMonthlyHealth(any())).thenReturn(mockReport);

        String body = """
                {"buildingId":"00000000-0000-0000-0000-000000000002",
                 "periodStart":"2025-01-01T00:00:00Z",
                 "periodEnd":"2025-01-31T23:59:59Z"}
                """;
        mockMvc.perform(post("/api/v1/reports/monthly-health")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "INSURER")
    void insurerShouldNotAccessGenerationEndpoints() throws Exception {
        String body = """
                {"buildingId":"00000000-0000-0000-0000-000000000002",
                 "periodStart":"2025-01-01T00:00:00Z",
                 "periodEnd":"2025-01-31T23:59:59Z"}
                """;
        mockMvc.perform(post("/api/v1/reports/monthly-health")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/reports/monitoring-certificate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void adminShouldAccessCriticalAlertEndpoint() throws Exception {
        Report mockReport = new Report(UUID.randomUUID(),
                new TenantId(UUID.randomUUID()), new BuildingId(UUID.randomUUID()),
                ReportType.CRITICAL_ALERT_REPORT,
                Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-01-31T23:59:59Z"));
        when(requestUseCase.requestCriticalAlertReport(any())).thenReturn(mockReport);

        String body = """
                {"buildingId":"00000000-0000-0000-0000-000000000002",
                 "periodStart":"2025-01-01T00:00:00Z",
                 "periodEnd":"2025-01-31T23:59:59Z"}
                """;
        mockMvc.perform(post("/api/v1/reports/critical-alert/00000000-0000-0000-0000-000000000003")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void adminShouldAccessInterventionEndpoint() throws Exception {
        Report mockReport = new Report(UUID.randomUUID(),
                new TenantId(UUID.randomUUID()), new BuildingId(UUID.randomUUID()),
                ReportType.INTERVENTION_REPORT,
                Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-01-31T23:59:59Z"));
        when(requestUseCase.requestInterventionReport(any())).thenReturn(mockReport);

        String body = """
                {"buildingId":"00000000-0000-0000-0000-000000000002",
                 "periodStart":"2025-01-01T00:00:00Z",
                 "periodEnd":"2025-01-31T23:59:59Z"}
                """;
        mockMvc.perform(post("/api/v1/reports/intervention/00000000-0000-0000-0000-000000000003")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }
}
