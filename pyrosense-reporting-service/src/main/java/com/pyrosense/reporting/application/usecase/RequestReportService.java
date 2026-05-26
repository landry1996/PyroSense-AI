package com.pyrosense.reporting.application.usecase;

import com.pyrosense.reporting.application.port.in.GenerateReportUseCase;
import com.pyrosense.reporting.application.port.in.GenerateReportUseCase.GenerateReportCommand;
import com.pyrosense.reporting.application.port.in.RequestReportUseCase;
import com.pyrosense.reporting.application.port.out.FileStoragePort;
import com.pyrosense.reporting.application.port.out.ReportAuditLogPort;
import com.pyrosense.reporting.application.port.out.ReportRepositoryPort;
import com.pyrosense.reporting.domain.model.Report;
import com.pyrosense.reporting.domain.model.ReportFileReference;
import com.pyrosense.reporting.domain.model.ReportType;

import java.util.UUID;

public class RequestReportService implements RequestReportUseCase {

    private final GenerateReportUseCase generateUseCase;
    private final ReportRepositoryPort repository;
    private final FileStoragePort fileStorage;
    private final ReportAuditLogPort auditLog;

    public RequestReportService(GenerateReportUseCase generateUseCase,
                                ReportRepositoryPort repository,
                                FileStoragePort fileStorage,
                                ReportAuditLogPort auditLog) {
        this.generateUseCase = generateUseCase;
        this.repository = repository;
        this.fileStorage = fileStorage;
        this.auditLog = auditLog;
    }

    @Override
    public Report requestMonthlyHealth(MonthlyHealthCommand command) {
        Report report = generateUseCase.generate(new GenerateReportCommand(
                command.tenantId(), command.buildingId(),
                ReportType.MONTHLY_HEALTH,
                command.periodStart(), command.periodEnd()));
        storeFileAndAudit(report, "MONTHLY_HEALTH_REQUESTED");
        return report;
    }

    @Override
    public Report requestMonitoringCertificate(MonitoringCertificateCommand command) {
        Report report = generateUseCase.generate(new GenerateReportCommand(
                command.tenantId(), command.buildingId(),
                ReportType.CONTINUOUS_MONITORING_CERTIFICATE,
                command.periodStart(), command.periodEnd()));
        storeFileAndAudit(report, "MONITORING_CERTIFICATE_REQUESTED");
        return report;
    }

    @Override
    public Report requestCriticalAlertReport(CriticalAlertReportCommand command) {
        Report report = generateUseCase.generate(new GenerateReportCommand(
                command.tenantId(), command.buildingId(),
                ReportType.CRITICAL_ALERT_REPORT,
                command.periodStart(), command.periodEnd()));
        report.setSourceAlertId(command.alertId());
        repository.save(report);
        storeFileAndAudit(report, "CRITICAL_ALERT_REPORT_REQUESTED");
        return report;
    }

    @Override
    public Report requestInterventionReport(InterventionReportCommand command) {
        Report report = generateUseCase.generate(new GenerateReportCommand(
                command.tenantId(), command.buildingId(),
                ReportType.INTERVENTION_REPORT,
                command.periodStart(), command.periodEnd()));
        report.setSourceInterventionId(command.interventionId());
        repository.save(report);
        storeFileAndAudit(report, "INTERVENTION_REPORT_REQUESTED");
        return report;
    }

    private void storeFileAndAudit(Report report, String action) {
        if (report.getContent() != null && report.getFileName() != null) {
            ReportFileReference ref = fileStorage.store(
                    report.getId(), report.getFileName(), report.getContent());
            report.setFileReference(ref);
            repository.save(report);
        }
        auditLog.log(report.getTenantId(), null, report.getId(), action,
                "Report %s generated: %s".formatted(report.getReportNumber(), report.getType()));
    }
}
