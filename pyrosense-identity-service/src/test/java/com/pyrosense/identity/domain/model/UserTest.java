package com.pyrosense.identity.domain.model;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.util.ClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class UserTest {

    private final Instant now = Instant.parse("2025-01-15T10:00:00Z");

    @BeforeEach
    void setUp() { ClockProvider.setClock(Clock.fixed(now, ZoneId.of("UTC"))); }

    @AfterEach
    void tearDown() { ClockProvider.reset(); }

    @Test
    void shouldCreateUser() {
        User user = new User(UserId.generate(), "test@example.com", "Test User");

        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getMemberships()).isEmpty();
    }

    @Test
    void shouldAddMembership() {
        User user = new User(UserId.generate(), "test@example.com", "Test User");
        TenantId tenantId = TenantId.generate();

        user.addMembership(tenantId, Set.of(Role.PROPERTY_MANAGER));

        assertThat(user.getMemberships()).hasSize(1);
        assertThat(user.getRolesForTenant(tenantId)).containsExactly(Role.PROPERTY_MANAGER);
    }

    @Test
    void shouldReturnEmptyRolesForUnknownTenant() {
        User user = new User(UserId.generate(), "test@example.com", "Test User");

        assertThat(user.getRolesForTenant(TenantId.generate())).isEmpty();
    }

    @Test
    void shouldSuspendAndActivate() {
        User user = new User(UserId.generate(), "test@example.com", "Test User");

        user.suspend();
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);

        user.activate();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldLockAfterMaxFailedAttempts() {
        User user = new User(UserId.generate(), "test@example.com", "Test User");

        for (int i = 0; i < 5; i++) {
            user.recordLoginFailure(5);
        }

        assertThat(user.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(user.isLocked()).isTrue();
    }

    @Test
    void shouldAutoUnlockAfterTimeout() {
        User user = new User(UserId.generate(), "test@example.com", "Test User");
        for (int i = 0; i < 5; i++) user.recordLoginFailure(5);

        ClockProvider.setClock(Clock.fixed(now.plusSeconds(1801), ZoneId.of("UTC")));

        assertThat(user.isLocked()).isFalse();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldRecordLoginSuccess() {
        User user = new User(UserId.generate(), "test@example.com", "Test User");
        user.recordLoginFailure(5);
        user.recordLoginFailure(5);

        user.recordLoginSuccess();

        assertThat(user.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(user.getLastLoginAt()).isEqualTo(now);
    }
}
