package com.pyrosense.identity.application.usecase;

import com.pyrosense.identity.adapter.out.persistence.InMemoryAuditLogRepository;
import com.pyrosense.identity.adapter.out.persistence.InMemoryTenantRepository;
import com.pyrosense.identity.adapter.out.persistence.InMemoryUserRepository;
import com.pyrosense.identity.application.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.pyrosense.identity.domain.model.Tenant;
import com.pyrosense.identity.domain.model.User;
import com.pyrosense.shared.exception.BusinessException;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class RegisterUserServiceTest {

    private InMemoryUserRepository userRepo;
    private InMemoryTenantRepository tenantRepo;
    private RegisterUserService service;
    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneId.of("UTC")));
        userRepo = new InMemoryUserRepository();
        tenantRepo = new InMemoryTenantRepository();
        service = new RegisterUserService(userRepo, tenantRepo, new InMemoryAuditLogRepository());

        tenantId = TenantId.generate();
        tenantRepo.save(new Tenant(tenantId, "Test Corp", "test-corp"));
    }

    @AfterEach
    void tearDown() { ClockProvider.reset(); }

    @Test
    void shouldRegisterUser() {
        var command = new RegisterUserCommand("user@test.com", "Test User", tenantId, Set.of("OCCUPANT"));

        User result = service.register(command);

        assertThat(result.getEmail()).isEqualTo("user@test.com");
        assertThat(result.getRolesForTenant(tenantId)).hasSize(1);
    }

    @Test
    void shouldRejectDuplicateEmail() {
        var command = new RegisterUserCommand("dup@test.com", "Dup User", tenantId, Set.of("OCCUPANT"));
        service.register(command);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void shouldRejectNonExistentTenant() {
        var command = new RegisterUserCommand("new@test.com", "New", TenantId.generate(), Set.of("OCCUPANT"));

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(NotFoundException.class);
    }
}
