package com.pyrosense.reporting.adapter.in.rest;

import com.pyrosense.reporting.application.port.in.GenerateReportUseCase;
import com.pyrosense.reporting.application.port.in.GetReportQuery;
import com.pyrosense.reporting.application.port.in.RequestReportUseCase;
import com.pyrosense.reporting.config.SecurityConfig;
import com.pyrosense.reporting.domain.model.Report;
import com.pyrosense.reporting.domain.model.ReportType;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class ReportTenantIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenerateReportUseCase generateUseCase;
    @MockitoBean
    private GetReportQuery queryUseCase;
    @MockitoBean
    private RequestReportUseCase requestUseCase;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void list_shouldOnlyReturnCurrentTenantReports() throws Exception {
        TenantId tenantA = new TenantId(UUID.randomUUID());
        TenantContext.set(tenantA);

        when(queryUseCase.findByTenant(eq(tenantA))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reports"))
                .andExpect(status().isOk());

        verify(queryUseCase).findByTenant(eq(tenantA));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void getById_shouldRejectCrossTenantAccess() throws Exception {
        TenantId tenantA = new TenantId(UUID.randomUUID());
        TenantId tenantB = new TenantId(UUID.randomUUID());
        TenantContext.set(tenantA);

        Report crossTenantReport = new Report(UUID.randomUUID(), tenantB,
                new BuildingId(UUID.randomUUID()), ReportType.MONTHLY_HEALTH,
                java.time.Instant.parse("2025-01-01T00:00:00Z"),
                java.time.Instant.parse("2025-01-31T23:59:59Z"));
        when(queryUseCase.findById(any(UUID.class))).thenReturn(Optional.of(crossTenantReport));

        mockMvc.perform(get("/api/v1/reports/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void getById_shouldAllowSameTenantAccess() throws Exception {
        TenantId tenantA = new TenantId(UUID.randomUUID());
        TenantContext.set(tenantA);

        Report sameTenantReport = new Report(UUID.randomUUID(), tenantA,
                new BuildingId(UUID.randomUUID()), ReportType.MONTHLY_HEALTH,
                java.time.Instant.parse("2025-01-01T00:00:00Z"),
                java.time.Instant.parse("2025-01-31T23:59:59Z"));
        when(queryUseCase.findById(any(UUID.class))).thenReturn(Optional.of(sameTenantReport));

        mockMvc.perform(get("/api/v1/reports/" + UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "INSURANCE_PARTNER")
    void insurancePartner_shouldNotGenerateReports() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/reports"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ELECTRICIAN")
    void electrician_shouldNotAccessReports() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/reports"))
                .andExpect(status().isForbidden());
    }
}
