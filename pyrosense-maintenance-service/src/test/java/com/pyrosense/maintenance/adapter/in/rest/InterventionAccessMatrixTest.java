package com.pyrosense.maintenance.adapter.in.rest;

import com.pyrosense.maintenance.application.port.in.AddInterventionCommentUseCase;
import com.pyrosense.maintenance.application.port.in.CreateInterventionUseCase;
import com.pyrosense.maintenance.application.port.in.GetInterventionQuery;
import com.pyrosense.maintenance.application.port.in.ManageInterventionUseCase;
import com.pyrosense.maintenance.config.SecurityConfig;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InterventionController.class)
@Import(SecurityConfig.class)
class InterventionAccessMatrixTest {

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

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/interventions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void tenantAdmin_canListInterventions() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        when(queryUseCase.findByTenant(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/interventions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PROPERTY_MANAGER")
    void propertyManager_canListInterventions() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        when(queryUseCase.findByTenant(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/interventions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ELECTRICIAN")
    void electrician_canListInterventions() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        when(queryUseCase.findByTenant(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/interventions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ELECTRICIAN")
    void electrician_cannotCreateIntervention() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        String body = """
                {"alertId":"alert-1","deviceId":"%s","severity":"CRITICAL","alertType":"MICRO_ARC","description":"test"}
                """.formatted(UUID.randomUUID());
        mockMvc.perform(post("/api/v1/interventions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ELECTRICIAN")
    void electrician_cannotAssignIntervention() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        String body = """
                {"electricianId":"%s","scheduledAt":"2026-06-01T10:00:00Z"}
                """.formatted(UUID.randomUUID());
        mockMvc.perform(post("/api/v1/interventions/" + UUID.randomUUID() + "/assign")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ELECTRICIAN")
    void electrician_cannotCancelIntervention() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        mockMvc.perform(post("/api/v1/interventions/" + UUID.randomUUID() + "/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"no longer needed\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPPORT_READONLY")
    void supportReadonly_canListInterventions() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        when(queryUseCase.findByTenant(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/interventions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SUPPORT_READONLY")
    void supportReadonly_cannotCreateIntervention() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        String body = """
                {"alertId":"alert-1","deviceId":"%s","severity":"CRITICAL","alertType":"MICRO_ARC","description":"test"}
                """.formatted(UUID.randomUUID());
        mockMvc.perform(post("/api/v1/interventions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OCCUPANT")
    void occupant_cannotAccessInterventions() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        mockMvc.perform(get("/api/v1/interventions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "INSURANCE_PARTNER")
    void insurancePartner_cannotAccessInterventions() throws Exception {
        TenantContext.set(new TenantId(UUID.randomUUID()));
        mockMvc.perform(get("/api/v1/interventions"))
                .andExpect(status().isForbidden());
    }
}
