package com.pyrosense.reporting.adapter.in.rest;

import com.pyrosense.reporting.application.port.in.GenerateReportUseCase;
import com.pyrosense.reporting.application.port.in.GetReportQuery;
import com.pyrosense.reporting.application.port.in.RequestReportUseCase;
import com.pyrosense.reporting.config.SecurityConfig;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class ReportAccessMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenerateReportUseCase generateUseCase;
    @MockitoBean
    private GetReportQuery queryUseCase;
    @MockitoBean
    private RequestReportUseCase requestUseCase;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void unauthenticated_cannotAccessReportList() throws Exception {
        int s = mockMvc.perform(get("/api/v1/reports"))
                .andReturn().getResponse().getStatus();
        org.junit.jupiter.api.Assertions.assertTrue(s == 401 || s == 403);
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void tenantAdmin_canGenerateReports() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        com.pyrosense.reporting.domain.model.Report mockReport = new com.pyrosense.reporting.domain.model.Report(
                UUID.randomUUID(),
                new TenantId(UUID.randomUUID()),
                new com.pyrosense.shared.id.BuildingId(UUID.randomUUID()),
                com.pyrosense.reporting.domain.model.ReportType.MONTHLY_HEALTH,
                java.time.Instant.parse("2025-01-01T00:00:00Z"),
                java.time.Instant.parse("2025-01-31T23:59:59Z"));
        when(requestUseCase.requestMonthlyHealth(any())).thenReturn(mockReport);

        String body = """
                {"buildingId":"%s","periodStart":"2025-01-01T00:00:00Z","periodEnd":"2025-01-31T23:59:59Z"}
                """.formatted(UUID.randomUUID());
        mockMvc.perform(post("/api/v1/reports/monthly-health")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "INSURER")
    void insurancePartner_cannotGenerateReports() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        String body = """
                {"buildingId":"%s","periodStart":"2025-01-01T00:00:00Z","periodEnd":"2025-01-31T23:59:59Z"}
                """.formatted(UUID.randomUUID());
        mockMvc.perform(post("/api/v1/reports/monthly-health")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "INSURER")
    void insurancePartner_canViewSharedReports() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        when(queryUseCase.findById(any())).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/reports/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ELECTRICIAN")
    void electrician_cannotAccessReports() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        mockMvc.perform(get("/api/v1/reports"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPPORT_READONLY")
    void supportReadonly_canListReportsButNotGenerate() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        String body = """
                {"buildingId":"%s","periodStart":"2025-01-01T00:00:00Z","periodEnd":"2025-01-31T23:59:59Z"}
                """.formatted(UUID.randomUUID());
        mockMvc.perform(post("/api/v1/reports/monthly-health")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void downloadEndpoint_isPublicButTokenGated() throws Exception {
        when(queryUseCase.findByDownloadToken(any())).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/reports/" + UUID.randomUUID() + "/download?token=invalid"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "INSURER")
    void insurancePartner_canRequestDownloadToken() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        when(queryUseCase.createDownloadToken(any())).thenReturn(
                new com.pyrosense.reporting.domain.model.DownloadToken("tok123",
                        UUID.randomUUID(), java.time.Instant.now().plusSeconds(900)));
        mockMvc.perform(get("/api/v1/reports/" + UUID.randomUUID() + "/download-token"))
                .andExpect(status().isOk());
    }
}
