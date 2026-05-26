package com.pyrosense.notification.application;

import com.pyrosense.notification.application.port.in.GetAuditLogQuery.AuditLogFilter;
import com.pyrosense.notification.application.port.in.GetAuditLogQuery.AuditLogPage;
import com.pyrosense.notification.application.port.out.AuditLogRepository;
import com.pyrosense.notification.application.usecase.GetAuditLogService;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.security.AuditEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetAuditLogServiceTest {

    private AuditLogRepository repository;
    private GetAuditLogService service;
    private static final TenantId TENANT = new TenantId(UUID.randomUUID());
    private static final TenantId OTHER_TENANT = new TenantId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        repository = mock(AuditLogRepository.class);
        service = new GetAuditLogService(repository);
    }

    @Test
    void findByTenantReturnsPaginatedResults() {
        Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant to = Instant.now();
        List<AuditEntry> entries = List.of(
                createEntry("ACTION_1"),
                createEntry("ACTION_2")
        );
        when(repository.findByTenant(TENANT, from, to, 0, 50)).thenReturn(entries);
        when(repository.countByTenant(TENANT, from, to)).thenReturn(2L);

        AuditLogFilter filter = new AuditLogFilter(from, to, null, null, 0, 50);
        AuditLogPage result = service.findByTenant(TENANT, filter);

        assertEquals(2, result.entries().size());
        assertEquals(2L, result.totalCount());
        assertEquals(0, result.page());
        assertEquals(50, result.size());
        assertEquals(1, result.totalPages());
    }

    @Test
    void findByTenantFiltersOnAction() {
        Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant to = Instant.now();
        when(repository.findByTenantAndAction(TENANT, "ALERT_RESOLVED", from, to, 0, 50))
                .thenReturn(List.of(createEntry("ALERT_RESOLVED")));
        when(repository.countByTenant(TENANT, from, to)).thenReturn(1L);

        AuditLogFilter filter = new AuditLogFilter(from, to, "ALERT_RESOLVED", null, 0, 50);
        AuditLogPage result = service.findByTenant(TENANT, filter);

        assertEquals(1, result.entries().size());
        assertEquals("ALERT_RESOLVED", result.entries().get(0).action());
    }

    @Test
    void findByTenantFiltersOnResourceType() {
        Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant to = Instant.now();
        when(repository.findByTenantAndResource(TENANT, "INTERVENTION", from, to, 0, 50))
                .thenReturn(List.of(createEntry("INTERVENTION_CREATED")));
        when(repository.countByTenant(TENANT, from, to)).thenReturn(1L);

        AuditLogFilter filter = new AuditLogFilter(from, to, null, "INTERVENTION", 0, 50);
        AuditLogPage result = service.findByTenant(TENANT, filter);

        assertEquals(1, result.entries().size());
    }

    @Test
    void findByIdReturnsEntryForSameTenant() {
        UUID id = UUID.randomUUID();
        AuditEntry entry = AuditEntry.createFull("ACTION", "RESOURCE", "res-1",
                new UserId(UUID.randomUUID()), "ADMIN", TENANT,
                "192.168.1.1", "agent", "corr-1", "details", Map.of());
        when(repository.findById(id)).thenReturn(Optional.of(entry));

        Optional<AuditEntry> result = service.findById(id, TENANT);
        assertTrue(result.isPresent());
    }

    @Test
    void findByIdFiltersDifferentTenant() {
        UUID id = UUID.randomUUID();
        AuditEntry entry = AuditEntry.createFull("ACTION", "RESOURCE", "res-1",
                new UserId(UUID.randomUUID()), "ADMIN", TENANT,
                "192.168.1.1", "agent", "corr-1", "details", Map.of());
        when(repository.findById(id)).thenReturn(Optional.of(entry));

        Optional<AuditEntry> result = service.findById(id, OTHER_TENANT);
        assertTrue(result.isEmpty());
    }

    @Test
    void findByIdReturnsEmptyIfNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        Optional<AuditEntry> result = service.findById(id, TENANT);
        assertTrue(result.isEmpty());
    }

    @Test
    void pageSizeIsCappedAt200() {
        AuditLogFilter filter = new AuditLogFilter(Instant.now(), Instant.now(), null, null, 0, 500);
        assertEquals(200, filter.size());
    }

    @Test
    void negativePageDefaultsToZero() {
        AuditLogFilter filter = new AuditLogFilter(Instant.now(), Instant.now(), null, null, -1, 50);
        assertEquals(0, filter.page());
    }

    @Test
    void totalPagesCalculatedCorrectly() {
        Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant to = Instant.now();
        when(repository.findByTenant(TENANT, from, to, 0, 10)).thenReturn(List.of());
        when(repository.countByTenant(TENANT, from, to)).thenReturn(25L);

        AuditLogFilter filter = new AuditLogFilter(from, to, null, null, 0, 10);
        AuditLogPage result = service.findByTenant(TENANT, filter);

        assertEquals(3, result.totalPages());
    }

    private AuditEntry createEntry(String action) {
        return AuditEntry.createFull(action, "NOTIFICATION", "n-1",
                new UserId(UUID.randomUUID()), "TENANT_ADMIN", TENANT,
                "10.0.0.1", "TestAgent", UUID.randomUUID().toString(),
                "test details", Map.of("key", "value"));
    }
}
