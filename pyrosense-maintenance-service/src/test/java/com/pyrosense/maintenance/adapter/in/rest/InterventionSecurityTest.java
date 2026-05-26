package com.pyrosense.maintenance.adapter.in.rest;

import com.pyrosense.maintenance.application.port.in.*;
import com.pyrosense.maintenance.config.SecurityConfig;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InterventionController.class)
@Import(SecurityConfig.class)
class InterventionSecurityTest {

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

    @BeforeEach
    void setUp() {
        TenantContext.set(new TenantId(UUID.randomUUID()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void unauthenticated_shouldReturn401() throws Exception {
        TenantContext.clear();
        mockMvc.perform(get("/api/v1/interventions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void tenantAdmin_shouldAccessList() throws Exception {
        mockMvc.perform(get("/api/v1/interventions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PROPERTY_MANAGER")
    void propertyManager_shouldAccessList() throws Exception {
        mockMvc.perform(get("/api/v1/interventions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ELECTRICIAN")
    void electrician_shouldAccessList() throws Exception {
        mockMvc.perform(get("/api/v1/interventions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SUPPORT_READONLY")
    void supportReadonly_shouldAccessList() throws Exception {
        mockMvc.perform(get("/api/v1/interventions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "UNKNOWN_ROLE")
    void unknownRole_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/interventions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ELECTRICIAN")
    void electrician_shouldNotCreate() throws Exception {
        mockMvc.perform(post("/api/v1/interventions")
                        .contentType("application/json")
                        .content("{\"tenantId\":\"id\",\"alertId\":\"id\",\"deviceId\":\"id\",\"severity\":\"CRITICAL\",\"description\":\"d\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPPORT_READONLY")
    void supportReadonly_shouldNotCancel() throws Exception {
        mockMvc.perform(post("/api/v1/interventions/" + UUID.randomUUID() + "/cancel")
                        .contentType("application/json")
                        .content("{\"reason\":\"test\"}"))
                .andExpect(status().isForbidden());
    }
}
