package com.pyrosense.reporting.adapter.in.rest;

import com.pyrosense.reporting.application.port.in.GenerateReportUseCase;
import com.pyrosense.reporting.application.port.in.GenerateReportUseCase.GenerateReportCommand;
import com.pyrosense.reporting.application.port.in.GetReportQuery;
import com.pyrosense.reporting.application.port.in.RequestReportUseCase;
import com.pyrosense.reporting.application.port.in.RequestReportUseCase.*;
import com.pyrosense.reporting.domain.model.Report;
import com.pyrosense.reporting.domain.model.ReportType;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final GenerateReportUseCase generateUseCase;
    private final GetReportQuery queryUseCase;
    private final RequestReportUseCase requestUseCase;

    public ReportController(GenerateReportUseCase generateUseCase,
                            GetReportQuery queryUseCase,
                            RequestReportUseCase requestUseCase) {
        this.generateUseCase = generateUseCase;
        this.queryUseCase = queryUseCase;
        this.requestUseCase = requestUseCase;
    }

    @PostMapping("/monthly")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
    public ResponseEntity<ReportResponse> generateMonthly(@Valid @RequestBody GenerateReportRequest request) {
        var command = new GenerateReportCommand(
                new TenantId(UUID.fromString(request.tenantId())),
                new BuildingId(UUID.fromString(request.buildingId())),
                ReportType.valueOf(request.type()),
                request.periodStart(),
                request.periodEnd()
        );
        Report report = generateUseCase.generate(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(report));
    }

    @PostMapping("/monthly-health")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
    public ResponseEntity<ReportResponse> requestMonthlyHealth(@Valid @RequestBody PeriodReportRequest request) {
        var command = new MonthlyHealthCommand(
                new TenantId(UUID.fromString(request.tenantId())),
                new BuildingId(UUID.fromString(request.buildingId())),
                request.periodStart(), request.periodEnd());
        Report report = requestUseCase.requestMonthlyHealth(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(report));
    }

    @PostMapping("/monitoring-certificate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
    public ResponseEntity<ReportResponse> requestMonitoringCertificate(@Valid @RequestBody PeriodReportRequest request) {
        var command = new MonitoringCertificateCommand(
                new TenantId(UUID.fromString(request.tenantId())),
                new BuildingId(UUID.fromString(request.buildingId())),
                request.periodStart(), request.periodEnd());
        Report report = requestUseCase.requestMonitoringCertificate(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(report));
    }

    @PostMapping("/critical-alert/{alertId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
    public ResponseEntity<ReportResponse> requestCriticalAlertReport(
            @PathVariable UUID alertId,
            @Valid @RequestBody PeriodReportRequest request) {
        var command = new CriticalAlertReportCommand(
                new TenantId(UUID.fromString(request.tenantId())),
                new BuildingId(UUID.fromString(request.buildingId())),
                alertId, request.periodStart(), request.periodEnd());
        Report report = requestUseCase.requestCriticalAlertReport(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(report));
    }

    @PostMapping("/intervention/{interventionId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
    public ResponseEntity<ReportResponse> requestInterventionReport(
            @PathVariable UUID interventionId,
            @Valid @RequestBody PeriodReportRequest request) {
        var command = new InterventionReportCommand(
                new TenantId(UUID.fromString(request.tenantId())),
                new BuildingId(UUID.fromString(request.buildingId())),
                interventionId, request.periodStart(), request.periodEnd());
        Report report = requestUseCase.requestInterventionReport(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(report));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'INSURER', 'OCCUPANT', 'SUPPORT_READONLY')")
    public ResponseEntity<ReportResponse> getById(@PathVariable UUID id) {
        return queryUseCase.findById(id)
                .filter(this::canAccess)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download-token")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'INSURER', 'OCCUPANT')")
    public ResponseEntity<DownloadTokenResponse> createDownloadToken(@PathVariable UUID id) {
        var token = queryUseCase.createDownloadToken(id);
        return ResponseEntity.ok(new DownloadTokenResponse(token.token(), token.expiresAt()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id, @RequestParam String token) {
        return queryUseCase.findByDownloadToken(token)
                .filter(r -> r.getId().equals(id))
                .map(report -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"%s\"".formatted(report.getFileName()))
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(report.getContent()))
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'SUPPORT_READONLY')")
    public ResponseEntity<List<ReportResponse>> list(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.min(size, 200);
        int offset = page * safeSize;
        TenantId tid = TenantContext.require();
        List<Report> reports = (type != null)
                ? queryUseCase.findByTenantAndType(tid, ReportType.valueOf(type))
                : queryUseCase.findByTenant(tid);
        // TODO: Replace in-memory pagination with proper SQL LIMIT/OFFSET
        return ResponseEntity.ok(reports.stream()
                .skip(offset).limit(safeSize)
                .map(this::toResponse).toList());
    }

    @GetMapping("/building/{buildingId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'INSURER', 'OCCUPANT', 'SUPPORT_READONLY')")
    public ResponseEntity<List<ReportResponse>> listByBuilding(
            @PathVariable String buildingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.min(size, 200);
        int offset = page * safeSize;
        List<Report> reports = queryUseCase.findByBuilding(new BuildingId(UUID.fromString(buildingId)));
        // TODO: Replace in-memory pagination with proper SQL LIMIT/OFFSET
        return ResponseEntity.ok(reports.stream()
                .filter(this::canAccess)
                .skip(offset).limit(safeSize)
                .map(this::toResponse)
                .toList());
    }

    private boolean canAccess(Report report) {
        return TenantContext.get()
                .map(currentTenant -> report.getTenantId().equals(currentTenant))
                .orElse(false);
    }

    private ReportResponse toResponse(Report r) {
        return new ReportResponse(
                r.getId().toString(),
                r.getReportNumber(),
                r.getTenantId().value().toString(),
                r.getBuildingId().value().toString(),
                r.getType().name(),
                r.getStatus().name(),
                r.getPeriodStart(),
                r.getPeriodEnd(),
                r.getFileName(),
                r.getSignature() != null ? r.getSignature().hash() : null,
                r.getMetadata() != null ? new MetadataResponse(
                        r.getMetadata().buildingName(),
                        r.getMetadata().buildingAddress(),
                        r.getMetadata().sensorCount(),
                        r.getMetadata().averageRiskScore(),
                        r.getMetadata().alertCount(),
                        r.getMetadata().criticalAlertCount(),
                        r.getMetadata().interventionCount(),
                        r.getMetadata().resolvedInterventionCount(),
                        r.getMetadata().recommendations()
                ) : null,
                r.getCreatedAt(),
                r.getGeneratedAt()
        );
    }

    // Request DTOs
    record GenerateReportRequest(
            @NotBlank String tenantId,
            @NotBlank String buildingId,
            @NotBlank String type,
            @NotNull Instant periodStart,
            @NotNull Instant periodEnd
    ) {}

    record PeriodReportRequest(
            @NotBlank String tenantId,
            @NotBlank String buildingId,
            @NotNull Instant periodStart,
            @NotNull Instant periodEnd
    ) {}

    // Response DTOs
    record ReportResponse(
            String id, String reportNumber, String tenantId, String buildingId,
            String type, String status, Instant periodStart, Instant periodEnd,
            String fileName, String signatureHash, MetadataResponse metadata,
            Instant createdAt, Instant generatedAt
    ) {}

    record MetadataResponse(
            String buildingName, String buildingAddress, int sensorCount,
            double averageRiskScore, int alertCount, int criticalAlertCount,
            int interventionCount, int resolvedInterventionCount,
            java.util.List<String> recommendations
    ) {}

    record DownloadTokenResponse(String token, Instant expiresAt) {}
}
