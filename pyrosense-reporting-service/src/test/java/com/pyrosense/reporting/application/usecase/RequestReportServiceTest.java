package com.pyrosense.reporting.application.usecase;

import com.pyrosense.reporting.application.port.in.GenerateReportUseCase;
import com.pyrosense.reporting.application.port.in.RequestReportUseCase.*;
import com.pyrosense.reporting.application.port.out.*;
import com.pyrosense.reporting.application.port.out.ReportRendererPort.ReportRenderRequest;
import com.pyrosense.reporting.domain.model.*;
import com.pyrosense.shared.domain.DomainEvent;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class RequestReportServiceTest {

    private RequestReportService service;
    private InMemoryReportRepository repository;
    private FakeFileStorage fileStorage;
    private FakeAuditLog auditLog;

    @BeforeEach
    void setUp() {
        repository = new InMemoryReportRepository();
        var renderer = new FakeRenderer();
        var dataProvider = new FakeDataProvider();
        var eventPublisher = new FakeEventPublisher();
        fileStorage = new FakeFileStorage();
        auditLog = new FakeAuditLog();

        var generateService = new GenerateReportService(repository, renderer, dataProvider, eventPublisher);
        service = new RequestReportService(generateService, repository, fileStorage, auditLog);
    }

    @Test
    void shouldRequestMonthlyHealthReport() {
        var command = new MonthlyHealthCommand(
                new TenantId(UUID.randomUUID()),
                new BuildingId(UUID.randomUUID()),
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-31T23:59:59Z"));

        Report report = service.requestMonthlyHealth(command);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATED);
        assertThat(report.getType()).isEqualTo(ReportType.MONTHLY_HEALTH);
        assertThat(report.getFileReference()).isNotNull();
        assertThat(auditLog.entries).anyMatch(e -> e.contains("MONTHLY_HEALTH_REQUESTED"));
    }

    @Test
    void shouldRequestMonitoringCertificate() {
        var command = new MonitoringCertificateCommand(
                new TenantId(UUID.randomUUID()),
                new BuildingId(UUID.randomUUID()),
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-31T23:59:59Z"));

        Report report = service.requestMonitoringCertificate(command);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATED);
        assertThat(report.getType()).isEqualTo(ReportType.CONTINUOUS_MONITORING_CERTIFICATE);
        assertThat(report.getFileReference()).isNotNull();
        assertThat(auditLog.entries).anyMatch(e -> e.contains("MONITORING_CERTIFICATE_REQUESTED"));
    }

    @Test
    void shouldRequestCriticalAlertReport() {
        UUID alertId = UUID.randomUUID();
        var command = new CriticalAlertReportCommand(
                new TenantId(UUID.randomUUID()),
                new BuildingId(UUID.randomUUID()),
                alertId,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-31T23:59:59Z"));

        Report report = service.requestCriticalAlertReport(command);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATED);
        assertThat(report.getType()).isEqualTo(ReportType.CRITICAL_ALERT_REPORT);
        assertThat(report.getSourceAlertId()).isEqualTo(alertId);
        assertThat(auditLog.entries).anyMatch(e -> e.contains("CRITICAL_ALERT_REPORT_REQUESTED"));
    }

    @Test
    void shouldRequestInterventionReport() {
        UUID interventionId = UUID.randomUUID();
        var command = new InterventionReportCommand(
                new TenantId(UUID.randomUUID()),
                new BuildingId(UUID.randomUUID()),
                interventionId,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-31T23:59:59Z"));

        Report report = service.requestInterventionReport(command);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATED);
        assertThat(report.getType()).isEqualTo(ReportType.INTERVENTION_REPORT);
        assertThat(report.getSourceInterventionId()).isEqualTo(interventionId);
        assertThat(auditLog.entries).anyMatch(e -> e.contains("INTERVENTION_REPORT_REQUESTED"));
    }

    @Test
    void shouldStoreFileOnGeneration() {
        var command = new MonthlyHealthCommand(
                new TenantId(UUID.randomUUID()),
                new BuildingId(UUID.randomUUID()),
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-31T23:59:59Z"));

        Report report = service.requestMonthlyHealth(command);

        assertThat(fileStorage.storedFiles).hasSize(1);
        assertThat(report.getFileReference().storagePath()).contains(report.getId().toString());
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
        public java.util.Optional<Report> findById(UUID id) {
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

    static class FakeRenderer implements ReportRendererPort {
        @Override
        public byte[] render(ReportRenderRequest request) {
            return "PDF content for %s".formatted(request.reportNumber()).getBytes();
        }
    }

    static class FakeDataProvider implements ReportDataProviderPort {
        @Override
        public ReportMetadata gatherMetadata(TenantId tenantId, BuildingId buildingId,
                                              ReportType type, Instant periodStart, Instant periodEnd) {
            return new ReportMetadata("Building A", "123 Rue Test", 10, 45.0, 5, 1, 3, 2,
                    List.of("Recommendation 1"));
        }
    }

    static class FakeEventPublisher implements ReportEventPublisherPort {
        @Override
        public void publish(DomainEvent event) {}
    }

    static class FakeFileStorage implements FileStoragePort {
        final List<String> storedFiles = new ArrayList<>();

        @Override
        public ReportFileReference store(UUID reportId, String fileName, byte[] content) {
            String path = reportId.toString() + "/" + fileName;
            storedFiles.add(path);
            return new ReportFileReference(path, "test", content.length);
        }

        @Override
        public byte[] retrieve(ReportFileReference reference) {
            return new byte[0];
        }

        @Override
        public void delete(ReportFileReference reference) {
            storedFiles.remove(reference.storagePath());
        }
    }

    static class FakeAuditLog implements ReportAuditLogPort {
        final List<String> entries = new ArrayList<>();

        @Override
        public void log(TenantId tenantId, UUID userId, UUID reportId, String action, String details) {
            entries.add(action + ": " + details);
        }
    }
}
