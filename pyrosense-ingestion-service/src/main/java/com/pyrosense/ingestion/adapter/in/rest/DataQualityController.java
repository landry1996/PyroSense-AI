package com.pyrosense.ingestion.adapter.in.rest;

import com.pyrosense.ingestion.application.port.in.AssessTelemetryQualityUseCase;
import com.pyrosense.ingestion.application.port.in.ListDataQualityIssuesQuery;
import com.pyrosense.ingestion.application.port.in.MarkDataQualityIssueReviewedUseCase;
import com.pyrosense.ingestion.application.port.out.DataQualityRepositoryPort;
import com.pyrosense.ingestion.domain.model.quality.DataQualityAssessment;
import com.pyrosense.ingestion.domain.model.quality.DataQualityIssue;
import com.pyrosense.shared.security.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'DEVICE_MANAGER', 'PROPERTY_MANAGER')")
public class DataQualityController {

    private final AssessTelemetryQualityUseCase assessUseCase;
    private final ListDataQualityIssuesQuery issuesQuery;
    private final MarkDataQualityIssueReviewedUseCase reviewUseCase;
    private final DataQualityRepositoryPort qualityRepository;

    public DataQualityController(AssessTelemetryQualityUseCase assessUseCase,
                                  ListDataQualityIssuesQuery issuesQuery,
                                  MarkDataQualityIssueReviewedUseCase reviewUseCase,
                                  DataQualityRepositoryPort qualityRepository) {
        this.assessUseCase = assessUseCase;
        this.issuesQuery = issuesQuery;
        this.reviewUseCase = reviewUseCase;
        this.qualityRepository = qualityRepository;
    }

    @GetMapping("/devices/{deviceId}/data-quality/latest")
    public ResponseEntity<DataQualityResponse> getLatest(@PathVariable UUID deviceId) {
        var tenantId = TenantContext.require();
        var assessment = qualityRepository.findLatestByDeviceAndTenant(
                deviceId.toString(), tenantId.value().toString());
        return assessment
                .map(a -> ResponseEntity.ok(toResponse(a)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/devices/{deviceId}/data-quality/history")
    public ResponseEntity<List<DataQualityResponse>> getHistory(
            @PathVariable UUID deviceId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        var tenantId = TenantContext.require();
        Instant toTime = to != null ? Instant.parse(to) : Instant.now();
        Instant fromTime = from != null ? Instant.parse(from) : toTime.minus(30, ChronoUnit.DAYS);

        var history = qualityRepository.findHistoryByDeviceAndTenant(
                deviceId.toString(), tenantId.value().toString(), fromTime, toTime);
        return ResponseEntity.ok(history.stream().map(this::toResponse).toList());
    }

    @GetMapping("/data-quality/issues")
    public ResponseEntity<IssueListResponse> listIssues(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var tenantId = TenantContext.require();
        var filter = new ListDataQualityIssuesQuery.IssueFilter(
                tenantId.value().toString(),
                deviceId,
                status != null ? DataQualityIssue.IssueStatus.valueOf(status) : null,
                type != null ? DataQualityIssue.IssueType.valueOf(type) : null,
                page, Math.min(size, 100));

        var result = issuesQuery.list(filter);
        return ResponseEntity.ok(new IssueListResponse(
                result.issues().stream().map(this::toIssueResponse).toList(),
                result.totalCount(), result.page(), result.totalPages()));
    }

    @PatchMapping("/data-quality/issues/{id}/review")
    public ResponseEntity<IssueResponse> reviewIssue(
            @PathVariable UUID id,
            @RequestBody ReviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = TenantContext.require();
        String reviewer = jwt.getClaimAsString("preferred_username");

        var command = new MarkDataQualityIssueReviewedUseCase.ReviewCommand(
                id, tenantId.value().toString(), reviewer,
                request.comment(), request.dismiss() != null && request.dismiss());

        var issue = reviewUseCase.execute(command);
        return ResponseEntity.ok(toIssueResponse(issue));
    }

    private DataQualityResponse toResponse(DataQualityAssessment a) {
        return new DataQualityResponse(
                a.getId().toString(), a.getDeviceId(), a.getAssessedAt(),
                a.getPeriodStart(), a.getPeriodEnd(),
                a.getOverallScore(), a.getGrade().name(), a.getGrade().getDescription(),
                a.isTrustworthy(), a.allowsCriticalAlerts(), a.allowsMlTraining(),
                a.getMissingDataRatio().completenessPercent(),
                a.getAverageSignalQuality(), a.getClockDriftSeconds(),
                a.getOfflineCount(), a.getFirmwareVersion(), a.isFirmwareObsolete(),
                a.getIssues().size());
    }

    private IssueResponse toIssueResponse(DataQualityIssue i) {
        return new IssueResponse(
                i.getId().toString(), i.getDeviceId(), i.getType().name(),
                i.getType().getDescription(), i.getSeverity().name(),
                i.getDetails(), i.getStatus().name(), i.getDetectedAt(),
                i.getReviewedBy(), i.getReviewedAt(), i.getReviewComment());
    }

    record DataQualityResponse(
            String id, String deviceId, Instant assessedAt,
            Instant periodStart, Instant periodEnd,
            int overallScore, String grade, String gradeDescription,
            boolean trustworthy, boolean allowsCriticalAlerts, boolean allowsMlTraining,
            double completenessPercent, double averageSignalQuality,
            double clockDriftSeconds, int offlineCount,
            String firmwareVersion, boolean firmwareObsolete, int issueCount) {}

    record IssueResponse(
            String id, String deviceId, String type, String typeDescription,
            String severity, String details, String status, Instant detectedAt,
            String reviewedBy, Instant reviewedAt, String reviewComment) {}

    record IssueListResponse(List<IssueResponse> issues, int totalCount, int page, int totalPages) {}

    record ReviewRequest(String comment, Boolean dismiss) {}
}
