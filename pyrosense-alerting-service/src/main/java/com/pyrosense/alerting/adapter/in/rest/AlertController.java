package com.pyrosense.alerting.adapter.in.rest;

import com.pyrosense.alerting.application.port.in.GetAlertQuery;
import com.pyrosense.alerting.application.port.in.GetAlertQuery.AlertStatistics;
import com.pyrosense.alerting.application.port.in.ManageAlertUseCase;
import com.pyrosense.alerting.domain.model.Alert;
import com.pyrosense.alerting.domain.model.AlertComment;
import com.pyrosense.alerting.domain.model.AlertStatus;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.valueobject.AlertSeverity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final ManageAlertUseCase manageAlertUseCase;
    private final GetAlertQuery getAlertQuery;

    public AlertController(ManageAlertUseCase manageAlertUseCase, GetAlertQuery getAlertQuery) {
        this.manageAlertUseCase = manageAlertUseCase;
        this.getAlertQuery = getAlertQuery;
    }

    @GetMapping("/{alertId}")
    public ResponseEntity<AlertResponse> getById(@PathVariable String alertId) {
        return getAlertQuery.findById(AlertId.from(alertId))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<AlertResponse>> list(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String deviceId) {

        List<Alert> alerts;
        if (deviceId != null) {
            alerts = getAlertQuery.findByDevice(DeviceId.from(deviceId));
        } else if (tenantId != null && status != null) {
            alerts = getAlertQuery.findByTenantAndStatus(
                    new TenantId(UUID.fromString(tenantId)), AlertStatus.valueOf(status));
        } else if (tenantId != null) {
            alerts = getAlertQuery.findByTenant(new TenantId(UUID.fromString(tenantId)));
        } else if (status != null) {
            alerts = getAlertQuery.findByStatus(AlertStatus.valueOf(status));
        } else if (severity != null) {
            alerts = getAlertQuery.findBySeverity(AlertSeverity.valueOf(severity));
        } else {
            alerts = getAlertQuery.findByStatus(AlertStatus.OPEN);
        }

        return ResponseEntity.ok(alerts.stream().map(this::toResponse).toList());
    }

    @GetMapping("/critical")
    public ResponseEntity<List<AlertResponse>> getOpenCritical() {
        return ResponseEntity.ok(getAlertQuery.findOpenCritical().stream().map(this::toResponse).toList());
    }

    @GetMapping("/statistics")
    public ResponseEntity<AlertStatistics> getStatistics(@RequestParam String tenantId) {
        return ResponseEntity.ok(getAlertQuery.getStatistics(new TenantId(UUID.fromString(tenantId))));
    }

    @PostMapping("/{alertId}/acknowledge")
    public ResponseEntity<AlertResponse> acknowledge(@PathVariable String alertId,
                                                      @Valid @RequestBody AcknowledgeRequest request) {
        Alert alert = manageAlertUseCase.acknowledge(
                AlertId.from(alertId), new UserId(UUID.fromString(request.userId())));
        return ResponseEntity.ok(toResponse(alert));
    }

    @PostMapping("/{alertId}/assign")
    public ResponseEntity<AlertResponse> assign(@PathVariable String alertId,
                                                 @Valid @RequestBody AssignRequest request) {
        Alert alert = manageAlertUseCase.assign(
                AlertId.from(alertId),
                new UserId(UUID.fromString(request.assigneeId())),
                new UserId(UUID.fromString(request.assignedById())));
        return ResponseEntity.ok(toResponse(alert));
    }

    @PostMapping("/{alertId}/resolve")
    public ResponseEntity<AlertResponse> resolve(@PathVariable String alertId,
                                                  @Valid @RequestBody ResolveRequest request) {
        Alert alert = manageAlertUseCase.resolve(
                AlertId.from(alertId),
                new UserId(UUID.fromString(request.userId())),
                request.resolutionNote());
        return ResponseEntity.ok(toResponse(alert));
    }

    @PostMapping("/{alertId}/false-positive")
    public ResponseEntity<AlertResponse> falsePositive(@PathVariable String alertId,
                                                        @Valid @RequestBody FalsePositiveRequest request) {
        Alert alert = manageAlertUseCase.markFalsePositive(
                AlertId.from(alertId),
                new UserId(UUID.fromString(request.userId())),
                request.reason());
        return ResponseEntity.ok(toResponse(alert));
    }

    @PostMapping("/{alertId}/comments")
    public ResponseEntity<AlertResponse> addComment(@PathVariable String alertId,
                                                     @Valid @RequestBody CommentRequest request) {
        Alert alert = manageAlertUseCase.addComment(
                AlertId.from(alertId),
                new UserId(UUID.fromString(request.authorId())),
                request.content());
        return ResponseEntity.ok(toResponse(alert));
    }

    private AlertResponse toResponse(Alert alert) {
        List<CommentResponse> comments = alert.comments().stream()
                .map(c -> new CommentResponse(c.id().toString(), c.author().value().toString(), c.content(), c.createdAt()))
                .toList();
        return new AlertResponse(
                alert.getId().value().toString(),
                alert.tenantId().value().toString(),
                alert.deviceId().value().toString(),
                alert.type().name(),
                alert.severity().name(),
                alert.title(),
                alert.description(),
                alert.status().name(),
                alert.assignedTo() != null ? alert.assignedTo().value().toString() : null,
                alert.escalationLevel().name(),
                alert.createdAt(),
                alert.slaDeadline(),
                alert.acknowledgedAt(),
                alert.acknowledgedBy() != null ? alert.acknowledgedBy().value().toString() : null,
                alert.resolvedAt(),
                alert.resolvedBy() != null ? alert.resolvedBy().value().toString() : null,
                alert.resolutionNote(),
                alert.occurrenceCount(),
                alert.lastOccurrenceAt(),
                alert.isSlaBreached(),
                comments
        );
    }

    record AlertResponse(String id, String tenantId, String deviceId, String type, String severity,
                          String title, String description, String status, String assignedTo,
                          String escalationLevel, Instant createdAt, Instant slaDeadline,
                          Instant acknowledgedAt, String acknowledgedBy,
                          Instant resolvedAt, String resolvedBy, String resolutionNote,
                          int occurrenceCount, Instant lastOccurrenceAt,
                          boolean slaBreached, List<CommentResponse> comments) {}

    record CommentResponse(String id, String authorId, String content, Instant createdAt) {}

    record AcknowledgeRequest(@NotBlank String userId) {}
    record AssignRequest(@NotBlank String assigneeId, @NotBlank String assignedById) {}
    record ResolveRequest(@NotBlank String userId, @NotNull String resolutionNote) {}
    record FalsePositiveRequest(@NotBlank String userId, @NotBlank String reason) {}
    record CommentRequest(@NotBlank String authorId, @NotBlank String content) {}
}
