package com.pyrosense.shared.security;

import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldSetAndGetTenant() {
        TenantId id = TenantId.generate();
        TenantContext.set(id);

        assertThat(TenantContext.get()).contains(id);
    }

    @Test
    void shouldReturnEmptyWhenNotSet() {
        assertThat(TenantContext.get()).isEmpty();
    }

    @Test
    void shouldClearContext() {
        TenantContext.set(TenantId.generate());
        TenantContext.clear();

        assertThat(TenantContext.get()).isEmpty();
    }

    @Test
    void shouldRequireTenantOrThrow() {
        assertThatThrownBy(TenantContext::require)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRequireReturnTenantWhenSet() {
        TenantId id = TenantId.generate();
        TenantContext.set(id);

        assertThat(TenantContext.require()).isEqualTo(id);
    }

    @Test
    void shouldIsolateAcrossThreads() throws Exception {
        TenantId mainTenant = TenantId.generate();
        TenantId otherTenant = TenantId.generate();
        TenantContext.set(mainTenant);

        Thread other = new Thread(() -> {
            TenantContext.set(otherTenant);
            assertThat(TenantContext.require()).isEqualTo(otherTenant);
            TenantContext.clear();
        });
        other.start();
        other.join();

        assertThat(TenantContext.require()).isEqualTo(mainTenant);
    }
}
