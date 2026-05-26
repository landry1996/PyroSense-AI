package com.pyrosense.maintenance.adapter.in.rest;

import com.pyrosense.maintenance.application.port.in.*;
import com.pyrosense.maintenance.application.port.in.GetInterventionQuery.InterventionStatistics;
import com.pyrosense.maintenance.config.SecurityConfig;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InterventionController.class)
@Import(SecurityConfig.class)
class InterventionTenantIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateInterventionUseCase createUseCase;
    @MockitoBean
    private ManageInterventionUseCase manageUseCase;
    @MockitoBean
    private GetInterventionQuery queryUseCase;
    @MockitoBean
    private AddInterventionCommentUseCase commentUseCase;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void list_shouldOnlyQueryCurrentTenant() throws Exception {
        TenantId tenantA = new TenantId(UUID.randomUUID());
        TenantContext.set(tenantA);

        when(queryUseCase.findByTenant(eq(tenantA))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/interventions"))
                .andExpect(status().isOk());

        verify(queryUseCase).findByTenant(eq(tenantA));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void kanban_shouldOnlyQueryCurrentTenant() throws Exception {
        TenantId tenantA = new TenantId(UUID.randomUUID());
        TenantContext.set(tenantA);

        when(queryUseCase.findByTenant(eq(tenantA))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/interventions/kanban"))
                .andExpect(status().isOk());

        verify(queryUseCase).findByTenant(eq(tenantA));
    }

    @Test
    @WithMockUser(roles = "PROPERTY_MANAGER")
    void statistics_shouldOnlyQueryCurrentTenant() throws Exception {
        TenantId tenantA = new TenantId(UUID.randomUUID());
        TenantContext.set(tenantA);

        when(queryUseCase.getStatistics(eq(tenantA))).thenReturn(
                new InterventionStatistics(0, 0, 0, 0, 0.0));

        mockMvc.perform(get("/api/v1/interventions/statistics"))
                .andExpect(status().isOk());

        verify(queryUseCase).getStatistics(eq(tenantA));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void noTenantContext_shouldFail() throws Exception {
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/interventions"))
                .andExpect(status().is5xxServerError());
    }
}
