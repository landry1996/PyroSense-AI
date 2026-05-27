package com.pyrosense.ingestion.adapter.in.rest;

import com.pyrosense.ingestion.application.port.in.QueryDatasetCandidatesUseCase;
import com.pyrosense.ingestion.application.port.in.RequestDatasetExportUseCase;
import com.pyrosense.ingestion.application.port.in.SubmitFieldFeedbackUseCase;
import com.pyrosense.ingestion.domain.model.dataset.*;
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
public class FieldDataController {

    private final SubmitFieldFeedbackUseCase feedbackUseCase;
    private final QueryDatasetCandidatesUseCase candidatesUseCase;
    private final RequestDatasetExportUseCase exportUseCase;

    public FieldDataController(SubmitFieldFeedbackUseCase feedbackUseCase,
                                QueryDatasetCandidatesUseCase candidatesUseCase,
                                RequestDatasetExportUseCase exportUseCase) {
        this.feedbackUseCase = feedbackUseCase;
        this.candidatesUseCase = candidatesUseCase;
        this.exportUseCase = exportUseCase;
    }

    @PostMapping("/field-feedback")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'ELECTRICIAN', 'DEVICE_MANAGER')")
    public ResponseEntity<FeedbackResponse> submitFeedback(
            @RequestBody FeedbackRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = TenantContext.require();
        String technicianId = jwt.getClaimAsString("preferred_username");

        var command = new SubmitFieldFeedbackUseCase.FieldFeedbackCommand(
                request.interventionId(),
                technicianId,
                request.deviceId(),
                tenantId.value().toString(),
                request.defectObserved(),
                request.confidenceLevel(),
                request.visualInspection(),
                request.measurementMethod(),
                request.measurementResult(),
                request.defectConfirmed(),
                request.falsePositive(),
                request.additionalNotes());

        var feedback = feedbackUseCase.execute(command);
        return ResponseEntity.ok(new FeedbackResponse(
                feedback.id(), feedback.interventionId(),
                feedback.defectObserved().name(),
                feedback.defectConfirmed(), feedback.falsePositive(),
                feedback.submittedAt()));
    }

    @GetMapping("/dataset/candidates")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<CandidateListResponse> listCandidates(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String minQuality,
            @RequestParam(required = false) String label,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var tenantId = TenantContext.require();
        Instant toTime = to != null ? Instant.parse(to) : Instant.now();
        Instant fromTime = from != null ? Instant.parse(from) : toTime.minus(90, ChronoUnit.DAYS);

        var filter = new QueryDatasetCandidatesUseCase.CandidateFilter(
                tenantId.value().toString(),
                status != null ? DatasetCandidate.CandidateStatus.valueOf(status) : null,
                minQuality != null ? DatasetCandidate.DataQualityTier.valueOf(minQuality) : null,
                label != null ? DataLabel.LabelValue.valueOf(label) : null,
                fromTime, toTime, page, Math.min(size, 100));

        var result = candidatesUseCase.query(filter);
        return ResponseEntity.ok(new CandidateListResponse(
                result.candidates().stream().map(this::toCandidateResponse).toList(),
                result.totalCount(), result.page(), result.totalPages()));
    }

    @PostMapping("/dataset/export-jobs")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ExportJobResponse> requestExport(
            @RequestBody ExportRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = TenantContext.require();
        String requestedBy = jwt.getClaimAsString("preferred_username");

        var command = new RequestDatasetExportUseCase.ExportCommand(
                requestedBy,
                tenantId.value().toString(),
                request.format() != null ? DatasetExportJob.ExportFormat.valueOf(request.format()) : DatasetExportJob.ExportFormat.PARQUET,
                request.windowStart() != null ? Instant.parse(request.windowStart()) : Instant.now().minus(90, ChronoUnit.DAYS),
                request.windowEnd() != null ? Instant.parse(request.windowEnd()) : Instant.now(),
                request.labelFilter() != null ? DataLabel.LabelValue.valueOf(request.labelFilter()) : null,
                request.minQualityTier() != null ? DatasetCandidate.DataQualityTier.valueOf(request.minQualityTier()) : DatasetCandidate.DataQualityTier.MEDIUM,
                request.includeUnlabeled() != null && request.includeUnlabeled());

        var job = exportUseCase.requestExport(command);
        return ResponseEntity.accepted().body(toExportResponse(job));
    }

    @GetMapping("/dataset/export-jobs/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ExportJobResponse> getExportJob(@PathVariable UUID id) {
        var tenantId = TenantContext.require();
        return exportUseCase.getExportJob(id, tenantId.value().toString())
                .map(job -> ResponseEntity.ok(toExportResponse(job)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private CandidateResponse toCandidateResponse(DatasetCandidate c) {
        var primaryLabel = c.getPrimaryLabel();
        return new CandidateResponse(
                c.getId().toString(),
                c.getPseudonymizedDeviceId(),
                c.getWindowStart(), c.getWindowEnd(),
                c.getStatus().name(), c.getQualityTier().name(),
                c.getDataQualityScore(), c.getRiskScoreAtTime(),
                c.getAnomalyType(),
                primaryLabel != null ? primaryLabel.value().name() : null,
                primaryLabel != null ? primaryLabel.source().name() : null,
                c.getLabels().size(),
                c.getCreatedAt());
    }

    private ExportJobResponse toExportResponse(DatasetExportJob j) {
        return new ExportJobResponse(
                j.getId().toString(), j.getStatus().name(),
                j.getFormat().name(), j.getWindowStart(), j.getWindowEnd(),
                j.getCandidateCount(), j.getFileSizeBytes(),
                j.getRequestedAt(), j.getCompletedAt(), j.getFailureReason());
    }

    record FeedbackRequest(
            UUID interventionId,
            String deviceId,
            DataLabel.LabelValue defectObserved,
            double confidenceLevel,
            String visualInspection,
            String measurementMethod,
            String measurementResult,
            boolean defectConfirmed,
            boolean falsePositive,
            String additionalNotes) {}

    record FeedbackResponse(
            UUID id, UUID interventionId, String defectObserved,
            boolean defectConfirmed, boolean falsePositive, Instant submittedAt) {}

    record ExportRequest(
            String format, String windowStart, String windowEnd,
            String labelFilter, String minQualityTier, Boolean includeUnlabeled) {}

    record ExportJobResponse(
            String id, String status, String format,
            Instant windowStart, Instant windowEnd,
            int candidateCount, long fileSizeBytes,
            Instant requestedAt, Instant completedAt, String failureReason) {}

    record CandidateResponse(
            String id, String pseudonymizedDeviceId,
            Instant windowStart, Instant windowEnd,
            String status, String qualityTier,
            int dataQualityScore, double riskScoreAtTime,
            String anomalyType, String primaryLabel, String labelSource,
            int labelCount, Instant createdAt) {}

    record CandidateListResponse(
            List<CandidateResponse> candidates,
            int totalCount, int page, int totalPages) {}
}
