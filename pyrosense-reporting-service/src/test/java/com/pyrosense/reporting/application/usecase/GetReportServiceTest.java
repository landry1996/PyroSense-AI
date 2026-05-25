package com.pyrosense.reporting.application.usecase;

import com.pyrosense.reporting.application.port.out.DownloadTokenStorePort;
import com.pyrosense.reporting.application.port.out.ReportRepositoryPort;
import com.pyrosense.reporting.domain.model.*;
import com.pyrosense.shared.exception.NotFoundException;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetReportServiceTest {

    private InMemoryReportRepository repository;
    private InMemoryTokenStore tokenStore;
    private GetReportService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryReportRepository();
        tokenStore = new InMemoryTokenStore();
        service = new GetReportService(repository, tokenStore);
    }

    @Test
    void shouldFindReportById() {
        Report report = createAndSaveReport();
        Optional<Report> found = service.findById(report.getId());

        assertTrue(found.isPresent());
        assertEquals(report.getId(), found.get().getId());
    }

    @Test
    void shouldReturnEmptyForUnknownId() {
        Optional<Report> found = service.findById(UUID.randomUUID());
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldFindByTenant() {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        Report report = createAndSaveReport(tenantId);

        List<Report> results = service.findByTenant(tenantId);
        assertEquals(1, results.size());
        assertEquals(report.getId(), results.get(0).getId());
    }

    @Test
    void shouldCreateDownloadToken() {
        Report report = createAndSaveReport();
        DownloadToken token = service.createDownloadToken(report.getId());

        assertNotNull(token);
        assertNotNull(token.token());
        assertEquals(report.getId(), token.reportId());
        assertTrue(token.isValid());
    }

    @Test
    void shouldThrowWhenCreatingTokenForNonExistentReport() {
        assertThrows(NotFoundException.class, () -> service.createDownloadToken(UUID.randomUUID()));
    }

    @Test
    void shouldFindReportByValidDownloadToken() {
        Report report = createGeneratedReport();
        repository.save(report);

        DownloadToken token = service.createDownloadToken(report.getId());
        Optional<Report> found = service.findByDownloadToken(token.token());

        assertTrue(found.isPresent());
        assertEquals(report.getId(), found.get().getId());
    }

    @Test
    void shouldInvalidateTokenAfterUse() {
        Report report = createGeneratedReport();
        repository.save(report);

        DownloadToken token = service.createDownloadToken(report.getId());
        service.findByDownloadToken(token.token());

        Optional<Report> secondAttempt = service.findByDownloadToken(token.token());
        assertTrue(secondAttempt.isEmpty());
    }

    @Test
    void shouldReturnEmptyForInvalidToken() {
        Optional<Report> found = service.findByDownloadToken("invalid-token");
        assertTrue(found.isEmpty());
    }

    private Report createAndSaveReport() {
        return createAndSaveReport(new TenantId(UUID.randomUUID()));
    }

    private Report createAndSaveReport(TenantId tenantId) {
        Report report = new Report(UUID.randomUUID(), tenantId, new BuildingId(UUID.randomUUID()),
                ReportType.MONTHLY_HEALTH, Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-31T23:59:59Z"));
        repository.save(report);
        return report;
    }

    private Report createGeneratedReport() {
        Report report = new Report(UUID.randomUUID(), new TenantId(UUID.randomUUID()),
                new BuildingId(UUID.randomUUID()), ReportType.MONTHLY_HEALTH,
                Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-01-31T23:59:59Z"));
        report.startGeneration();
        report.markGenerated("PDF content".getBytes(),
                new ReportMetadata("Building", "Address", 10, 45.0, 5, 1, 3, 2, List.of("Reco")));
        return report;
    }

    // Test doubles
    static class InMemoryReportRepository implements ReportRepositoryPort {
        private final List<Report> reports = new ArrayList<>();

        @Override
        public Report save(Report report) {
            reports.removeIf(r -> r.getId().equals(report.getId()));
            reports.add(report);
            return report;
        }

        @Override
        public Optional<Report> findById(UUID id) {
            return reports.stream().filter(r -> r.getId().equals(id)).findFirst();
        }

        @Override
        public List<Report> findByTenantId(TenantId tenantId) {
            return reports.stream().filter(r -> r.getTenantId().equals(tenantId)).toList();
        }

        @Override
        public List<Report> findByTenantIdAndType(TenantId tenantId, ReportType type) {
            return reports.stream()
                    .filter(r -> r.getTenantId().equals(tenantId) && r.getType() == type).toList();
        }

        @Override
        public List<Report> findByBuildingId(BuildingId buildingId) {
            return reports.stream().filter(r -> r.getBuildingId().equals(buildingId)).toList();
        }
    }

    static class InMemoryTokenStore implements DownloadTokenStorePort {
        private final Map<String, DownloadToken> store = new HashMap<>();

        @Override
        public void store(DownloadToken token) {
            store.put(token.token(), token);
        }

        @Override
        public Optional<DownloadToken> findByToken(String token) {
            return Optional.ofNullable(store.get(token));
        }

        @Override
        public void invalidate(String token) {
            store.remove(token);
        }
    }
}
