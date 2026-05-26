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
import com.pyrosense.shared.security.TenantContext;
import com.pyrosense.shared.valueobject.AlertSeverity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'ELECTRICIAN', 'OCCUPANT', 'SUPPORT_READONLY')")
public class AlertController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 200;

    private final ManageAlertUseCase manageAlertUseCase;
    private final GetAlertQuery getAlertQuery;

    public AlertController(ManageAlertUseCase manageAlertUseCase, GetAlertQuery getAlertQuery) {
        this.manageAlertUseCase = manageAlertUseCase;
        this.getAlertQuery = getAlertQuery;
    }

    @GetMapping("/{alertId}")
    public ResponseEntity<AlertResponse> getById(@PathVariable String alertId) {
        TenantId currentTenant = TenantContext.require();
        return getAlertQuery.findById(AlertId.from(alertId))
                .filter(alert -> alert.tenantId().equals(currentTenant))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<AlertResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String buildingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        TenantId tenantId = TenantContext.require();
        int effectiveSize = Math.min(Math.max(size, 1), MAX_SIZE);
        int offset = Math.max(page, 0) * effectiveSize;

        List<Alert> alerts;
        if (deviceId != null) {
            alerts = getAlertQuery.findByDevice(DeviceId.from(deviceId)).stream()
                    .filter(a -> a.tenantId().equals(tenantId))
                    .toList();
        } else if (buildingId != null) {
            alerts = getAlertQuery.findByTenantAndBuildingId(tenantId, buildingId, offset, effectiveSize);
        } else if (status != null) {
            alerts = getAlertQuery.findByTenantAndStatus(
                    tenantId, AlertStatus.valueOf(status), offset, effectiveSize);
        } else if (severity != null) {
            alerts = getAlertQuery.findByTenantAndSeverity(
                    tenantId, AlertSeverity.valueOf(severity), offset, effectiveSize);
        } else {
            alerts = getAlertQuery.findByTenant(tenantId, offset, effectiveSize);
        }

        return ResponseEntity.ok(alerts.stream().map(this::toResponse).toList());
    }

    @GetMapping("/critical")
    public ResponseEntity<List<AlertResponse>> getOpenCritical() {
        TenantId tenantId = TenantContext.require();
        return ResponseEntity.ok(getAlertQuery.findOpenCritical(tenantId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/statistics")
    public ResponseEntity<AlertStatistics> getStatistics() {
        TenantId tenantId = TenantContext.require();
        return ResponseEntity.ok(getAlertQuery.getStatistics(tenantId));
    }

    @PostMapping("/{alertId}/acknowledge")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'ELECTRICIAN')")
    public ResponseEntity<AlertResponse> acknowledge(@PathVariable String alertId,
                                                      @Valid @RequestBody AcknowledgeRequest request) {
        TenantId currentTenant = TenantContext.require();
        Alert alert = manageAlertUseCase.acknowledge(
                AlertId.from(alertId), new UserId(UUID.fromString(request.userId())));
        if (!alert.tenantId().equals(currentTenant)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(alert));
    }

    @PostMapping("/{alertId}/assign")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
    public ResponseEntity<AlertResponse> assign(@PathVariable String alertId,
                                                 @Valid @RequestBody AssignRequest request) {
        TenantId currentTenant = TenantContext.require();
        Alert alert = manageAlertUseCase.assign(
                AlertId.from(alertId),
                new UserId(UUID.fromString(request.assigneeId())),
                new UserId(UUID.fromString(request.assignedById())));
        if (!alert.tenantId().equals(currentTenant)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(alert));
    }

    @PostMapping("/{alertId}/resolve")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'ELECTRICIAN')")
    public ResponseEntity<AlertResponse> resolve(@PathVariable String alertId,
                                                  @Valid @RequestBody ResolveRequest request) {
        TenantId currentTenant = TenantContext.require();
        Alert alert = manageAlertUseCase.resolve(
                AlertId.from(alertId),
                new UserId(UUID.fromString(request.userId())),
                request.resolutionNote());
        if (!alert.tenantId().equals(currentTenant)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(alert));
    }

    @PostMapping("/{alertId}/false-positive")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
    public ResponseEntity<AlertResponse> falsePositive(@PathVariable String alertId,
                                                        @Valid @RequestBody FalsePositiveRequest request) {
        TenantId currentTenant = TenantContext.require();
        Alert alert = manageAlertUseCase.markFalsePositive(
                AlertId.from(alertId),
                new UserId(UUID.fromString(request.userId())),
                request.reason());
        if (!alert.tenantId().equals(currentTenant)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(alert));
    }

    @PostMapping("/{alertId}/comments")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'ELECTRICIAN')")
    public ResponseEntity<AlertResponse> addComment(@PathVariable String alertId,
                                                     @Valid @RequestBody CommentRequest request) {
        TenantId currentTenant = TenantContext.require();
        Alert alert = manageAlertUseCase.addComment(
                AlertId.from(alertId),
                new UserId(UUID.fromString(request.authorId())),
                request.content());
        if (!alert.tenantId().equals(currentTenant)) {
            return ResponseEntity.notFound().build();
        }
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
                alert.buildingId(),
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

    record AlertResponse(String id, String tenantId, String deviceId, String buildingId,
                          String type, String severity,
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
