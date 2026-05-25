package com.pyrosense.reporting.application.usecase;

import com.pyrosense.reporting.application.port.in.GenerateReportUseCase.GenerateReportCommand;
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

import static org.junit.jupiter.api.Assertions.*;

class GenerateReportServiceTest {

    private InMemoryReportRepository repository;
    private FakeRenderer renderer;
    private FakeDataProvider dataProvider;
    private FakeEventPublisher eventPublisher;
    private GenerateReportService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryReportRepository();
        renderer = new FakeRenderer();
        dataProvider = new FakeDataProvider();
        eventPublisher = new FakeEventPublisher();
        service = new GenerateReportService(repository, renderer, dataProvider, eventPublisher);
    }

    @Test
    void shouldGenerateReportSuccessfully() {
        var command = new GenerateReportCommand(
                new TenantId(UUID.randomUUID()),
                new BuildingId(UUID.randomUUID()),
                ReportType.MONTHLY_HEALTH,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-31T23:59:59Z")
        );

        Report result = service.generate(command);

        assertEquals(ReportStatus.GENERATED, result.getStatus());
        assertNotNull(result.getContent());
        assertNotNull(result.getMetadata());
        assertNotNull(result.getSignature());
        assertNotNull(result.getFileName());
    }

    @Test
    void shouldPersistReport() {
        var command = new GenerateReportCommand(
                new TenantId(UUID.randomUUID()),
                new BuildingId(UUID.randomUUID()),
                ReportType.CRITICAL_ALERT_REPORT,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-31T23:59:59Z")
        );

        Report result = service.generate(command);

        assertTrue(repository.findById(result.getId()).isPresent());
    }

    @Test
    void shouldPublishReportGeneratedEvent() {
        var command = new GenerateReportCommand(
                new TenantId(UUID.randomUUID()),
                new BuildingId(UUID.randomUUID()),
                ReportType.MONTHLY_HEALTH,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-31T23:59:59Z")
        );

        service.generate(command);

        assertFalse(eventPublisher.events.isEmpty());
        assertTrue(eventPublisher.events.stream()
                .anyMatch(e -> e.eventType().equals("reporting.report.generated")));
    }

    @Test
    void shouldPublishComplianceCertificateEvent() {
        var command = new GenerateReportCommand(
                new TenantId(UUID.randomUUID()),
                new BuildingId(UUID.randomUUID()),
                ReportType.CONTINUOUS_MONITORING_CERTIFICATE,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-31T23:59:59Z")
        );

        service.generate(command);

        assertTrue(eventPublisher.events.stream()
                .anyMatch(e -> e.eventType().equals("reporting.compliance_certificate.generated")));
    }

    @Test
    void shouldMarkReportFailedOnError() {
        renderer.shouldFail = true;

        var command = new GenerateReportCommand(
                new TenantId(UUID.randomUUID()),
                new BuildingId(UUID.randomUUID()),
                ReportType.MONTHLY_HEALTH,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-31T23:59:59Z")
        );

        assertThrows(RuntimeException.class, () -> service.generate(command));

        var saved = repository.findAll();
        assertFalse(saved.isEmpty());
        assertEquals(ReportStatus.FAILED, saved.get(0).getStatus());
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

        List<Report> findAll() { return new ArrayList<>(reports); }
    }

    static class FakeRenderer implements ReportRendererPort {
        boolean shouldFail = false;

        @Override
        public byte[] render(ReportRenderRequest request) {
            if (shouldFail) throw new RuntimeException("Rendering failed");
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
        final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            events.add(event);
        }
    }
}
