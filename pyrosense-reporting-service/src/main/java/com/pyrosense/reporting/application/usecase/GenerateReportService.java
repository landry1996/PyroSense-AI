package com.pyrosense.reporting.application.usecase;

import com.pyrosense.reporting.application.port.in.GenerateReportUseCase;
import com.pyrosense.reporting.application.port.out.*;
import com.pyrosense.reporting.application.port.out.ReportRendererPort.ReportRenderRequest;
import com.pyrosense.reporting.domain.event.ComplianceCertificateGeneratedEvent;
import com.pyrosense.reporting.domain.event.ReportGeneratedEvent;
import com.pyrosense.reporting.domain.model.Report;
import com.pyrosense.reporting.domain.model.ReportMetadata;
import com.pyrosense.shared.util.ClockProvider;

import java.util.UUID;

public class GenerateReportService implements GenerateReportUseCase {

    private final ReportRepositoryPort repository;
    private final ReportRendererPort renderer;
    private final ReportDataProviderPort dataProvider;
    private final ReportEventPublisherPort eventPublisher;

    public GenerateReportService(ReportRepositoryPort repository,
                                  ReportRendererPort renderer,
                                  ReportDataProviderPort dataProvider,
                                  ReportEventPublisherPort eventPublisher) {
        this.repository = repository;
        this.renderer = renderer;
        this.dataProvider = dataProvider;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Report generate(GenerateReportCommand command) {
        var report = new Report(UUID.randomUUID(), command.tenantId(), command.buildingId(),
                command.type(), command.periodStart(), command.periodEnd());
        report.startGeneration();
        repository.save(report);

        try {
            ReportMetadata metadata = dataProvider.gatherMetadata(
                    command.tenantId(), command.buildingId(), command.type(),
                    command.periodStart(), command.periodEnd());

            byte[] content = renderer.render(new ReportRenderRequest(
                    report.getReportNumber(), command.type(), metadata.buildingName(),
                    command.periodStart(), command.periodEnd(), metadata));

            report.markGenerated(content, metadata);
            repository.save(report);

            publishEvents(report);

            return report;
        } catch (Exception e) {
            report.markFailed();
            repository.save(report);
            throw new RuntimeException("Report generation failed: " + e.getMessage(), e);
        }
    }

    private void publishEvents(Report report) {
        eventPublisher.publish(new ReportGeneratedEvent(
                UUID.randomUUID(), ClockProvider.now(), report.getId(),
                report.getReportNumber(), report.getTenantId(),
                report.getBuildingId(), report.getType()));

        if (report.getType().isComplianceCertificate()) {
            eventPublisher.publish(new ComplianceCertificateGeneratedEvent(
                    UUID.randomUUID(), ClockProvider.now(), report.getId(),
                    report.getReportNumber(), report.getTenantId(), report.getBuildingId(),
                    report.getPeriodStart(), report.getPeriodEnd(),
                    report.getSignature().hash()));
        }
    }
}
