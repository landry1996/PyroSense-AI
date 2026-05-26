package com.pyrosense.notification.adapter.in.rest;

import com.pyrosense.notification.application.port.in.GetNotificationQuery;
import com.pyrosense.notification.application.port.in.RetryNotificationUseCase;
import com.pyrosense.notification.config.SecurityConfig;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationTenantIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetNotificationQuery queryUseCase;
    @MockitoBean
    private RetryNotificationUseCase retryUseCase;
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
    @WithMockUser(roles = "TENANT_ADMIN")
    void list_shouldOnlyQueryCurrentTenant() throws Exception {
        TenantId tenantA = new TenantId(UUID.randomUUID());
        TenantContext.set(tenantA);

        when(queryUseCase.findByTenant(eq(tenantA))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void noTenantContext_shouldFail() throws Exception {
        TenantContext.clear();

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void electrician_shouldNotBeInListAnnotation() throws NoSuchMethodException {
        var method = NotificationController.class.getDeclaredMethod("list", String.class, int.class, int.class);
        var annotation = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.junit.jupiter.api.Assertions.assertNotNull(annotation);
        org.junit.jupiter.api.Assertions.assertFalse(annotation.value().contains("ELECTRICIAN"));
    }

    @Test
    void occupant_shouldNotBeInListAnnotation() throws NoSuchMethodException {
        var method = NotificationController.class.getDeclaredMethod("list", String.class, int.class, int.class);
        var annotation = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.junit.jupiter.api.Assertions.assertNotNull(annotation);
        org.junit.jupiter.api.Assertions.assertFalse(annotation.value().contains("OCCUPANT"));
    }

    @Test
    void electrician_shouldNotBeInRetryAnnotation() throws NoSuchMethodException {
        var method = NotificationController.class.getDeclaredMethod("retry", UUID.class);
        var annotation = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.junit.jupiter.api.Assertions.assertNotNull(annotation);
        org.junit.jupiter.api.Assertions.assertFalse(annotation.value().contains("ELECTRICIAN"));
    }
}
