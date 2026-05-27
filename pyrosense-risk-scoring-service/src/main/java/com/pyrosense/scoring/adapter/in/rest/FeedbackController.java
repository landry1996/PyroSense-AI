package com.pyrosense.scoring.adapter.in.rest;

import com.pyrosense.scoring.application.port.in.GetFeedbackHistoryQuery;
import com.pyrosense.scoring.application.port.in.GetFeedbackHistoryQuery.FeedbackFilter;
import com.pyrosense.scoring.application.port.in.GetFeedbackHistoryQuery.FeedbackPageResult;
import com.pyrosense.scoring.application.port.in.GetFeedbackHistoryQuery.FeedbackStats;
import com.pyrosense.scoring.domain.model.ScoringAdjustment;
import com.pyrosense.scoring.domain.model.ScoringFeedback;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/scoring/feedback")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','PROPERTY_MANAGER','ELECTRICIAN','OPERATOR')")
public class FeedbackController {

    private final GetFeedbackHistoryQuery feedbackHistoryQuery;

    public FeedbackController(GetFeedbackHistoryQuery feedbackHistoryQuery) {
        this.feedbackHistoryQuery = feedbackHistoryQuery;
    }

    @GetMapping("/history")
    public ResponseEntity<FeedbackHistoryResponse> getHistory(
            @RequestParam String tenantId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String anomalyType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var filter = new FeedbackFilter(tenantId, deviceId, anomalyType, from, to, page, size);
        FeedbackPageResult result = feedbackHistoryQuery.getHistory(filter);

        List<FeedbackResponse> feedbacks = result.feedbacks().stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(new FeedbackHistoryResponse(
                feedbacks, result.totalCount(), result.page(), result.totalPages()));
    }

    @GetMapping("/stats")
    public ResponseEntity<FeedbackStats> getStats(
            @RequestParam String tenantId,
            @RequestParam(required = false) String deviceId) {
        return ResponseEntity.ok(feedbackHistoryQuery.getStats(tenantId, deviceId));
    }

    @GetMapping("/adjustments")
    public ResponseEntity<List<AdjustmentResponse>> getAdjustments(
            @RequestParam String deviceId,
            @RequestParam(required = false) String anomalyType) {
        List<AdjustmentResponse> adjustments = feedbackHistoryQuery.getAdjustmentHistory(deviceId, anomalyType)
                .stream().map(this::toAdjustmentResponse).toList();
        return ResponseEntity.ok(adjustments);
    }

    private FeedbackResponse toResponse(ScoringFeedback f) {
        return new FeedbackResponse(
                f.getId().toString(),
                f.getDeviceId().value().toString(),
                f.getTenantId(),
                f.getAlertId() != null ? f.getAlertId().toString() : null,
                f.getInterventionId() != null ? f.getInterventionId().toString() : null,
                f.getOutcome().name(),
                f.getAnomalyType(),
                f.getRiskScoreAtAlert(),
                f.getFeedbackSource(),
                f.getComment(),
                f.getReceivedAt(),
                f.getAdjustment() != null ? toAdjustmentResponse(f.getAdjustment()) : null);
    }

    private AdjustmentResponse toAdjustmentResponse(ScoringAdjustment a) {
        return new AdjustmentResponse(
                a.id().toString(), a.anomalyType(), a.type().name(),
                a.previousValue(), a.newValue(), a.delta(),
                a.mode().name(), a.reason(), a.appliedAt());
    }

    record FeedbackHistoryResponse(List<FeedbackResponse> feedbacks, int totalCount, int page, int totalPages) {}

    record FeedbackResponse(String id, String deviceId, String tenantId,
                            String alertId, String interventionId,
                            String outcome, String anomalyType,
                            double riskScoreAtAlert, String feedbackSource,
                            String comment, Instant receivedAt,
                            AdjustmentResponse adjustment) {}

    record AdjustmentResponse(String id, String anomalyType, String type,
                              double previousValue, double newValue, double delta,
                              String mode, String reason, Instant appliedAt) {}
}
