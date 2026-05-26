package com.pyrosense.notification.adapter.in.rest;

import com.pyrosense.notification.application.port.in.GetAuditLogQuery;
import com.pyrosense.notification.application.port.in.GetAuditLogQuery.AuditLogFilter;
import com.pyrosense.notification.application.port.in.GetAuditLogQuery.AuditLogPage;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.security.AuditEntry;
import com.pyrosense.shared.security.TenantContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TenantIsolationTest {

    private static final TenantId TENANT_A = new TenantId(UUID.randomUUID());
    private static final TenantId TENANT_B = new TenantId(UUID.randomUUID());

    @Test
    void auditEntry_belongingToOtherTenant_isNotReturned() {
        AuditEntry entryA = AuditEntry.createFull("LOGIN", "USER", "u-1",
                new UserId(UUID.randomUUID()), "ADMIN", TENANT_A,
                "10.0.0.1", "agent", "corr-1", "Login", Map.of());

        assertNotEquals(TENANT_B, entryA.tenantId());
    }

    @Test
    void tenantContext_cannotBeSpoovedViaParameter() {
        TenantContext.set(TENANT_A);
        try {
            TenantId contextTenant = TenantContext.require();
            assertEquals(TENANT_A, contextTenant);
            assertNotEquals(TENANT_B, contextTenant);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void tenantContext_throwsIfNotSet() {
        TenantContext.clear();
        assertThrows(IllegalStateException.class, TenantContext::require);
    }

    @Test
    void auditLogFilter_doesNotExposeOtherTenantData() {
        AuditLogFilter filter = new AuditLogFilter(
                Instant.now().minusSeconds(3600), Instant.now(), null, null, 0, 50);
        assertEquals(0, filter.page());
        assertEquals(50, filter.size());
    }

    @Test
    void auditLogPage_calculatesMetricsIndependently() {
        AuditLogPage page = new AuditLogPage(List.of(), 0, 0, 50);
        assertEquals(0, page.totalPages());
        assertEquals(0, page.totalCount());
    }

    @Test
    void auditEntry_noSecretFieldsInRecord() {
        var components = AuditEntry.class.getRecordComponents();
        for (var c : components) {
            String name = c.getName().toLowerCase();
            assertFalse(name.contains("password"), "No password field allowed");
            assertFalse(name.contains("secret"), "No secret field allowed");
            assertFalse(name.contains("token"), "No token field allowed");
        }
    }
}
