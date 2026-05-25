package com.pyrosense.identity.adapter.in.rest;

import com.pyrosense.identity.application.port.in.RegisterUserUseCase;
import com.pyrosense.identity.application.port.out.UserRepository;
import com.pyrosense.identity.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegisterUserUseCase registerUserUseCase;

    @MockBean
    private UserRepository userRepository;

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .param("tenantId", "00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUserWithoutAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .param("tenantId", "00000000-0000-0000-0000-000000000001")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OCCUPANT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowPlatformAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .param("tenantId", "00000000-0000-0000-0000-000000000001")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowTenantAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .param("tenantId", "00000000-0000-0000-0000-000000000001")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectOccupantFromUserCreation() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@test.com","fullName":"Test","tenantId":"00000000-0000-0000-0000-000000000001","roles":["OCCUPANT"]}
                                """)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OCCUPANT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectDeviceRoleFromUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .param("tenantId", "00000000-0000-0000-0000-000000000001")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DEVICE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowSupportReadonlyToViewUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .param("tenantId", "00000000-0000-0000-0000-000000000001")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPPORT_READONLY"))))
                .andExpect(status().isOk());
    }
}
