package com.pyrosense.notification.adapter.in.rest;

import com.pyrosense.notification.application.port.in.GetNotificationQuery;
import com.pyrosense.notification.domain.model.Notification;
import com.pyrosense.notification.domain.model.NotificationStatus;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final GetNotificationQuery queryUseCase;

    public NotificationController(GetNotificationQuery queryUseCase) {
        this.queryUseCase = queryUseCase;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'SUPPORT_READONLY')")
    public ResponseEntity<NotificationResponse> getById(@PathVariable UUID id) {
        return queryUseCase.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'SUPPORT_READONLY')")
    public ResponseEntity<List<NotificationResponse>> list(
            @RequestParam String tenantId,
            @RequestParam(required = false) String status) {
        TenantId tid = new TenantId(UUID.fromString(tenantId));
        List<Notification> notifications;
        if (status != null) {
            notifications = queryUseCase.findByStatus(NotificationStatus.valueOf(status)).stream()
                    .filter(n -> n.getTenantId().equals(tid))
                    .toList();
        } else {
            notifications = queryUseCase.findByTenant(tid);
        }
        return ResponseEntity.ok(notifications.stream().map(this::toResponse).toList());
    }

    @GetMapping("/recipient/{recipientId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'ELECTRICIAN', 'OCCUPANT')")
    public ResponseEntity<List<NotificationResponse>> listByRecipient(@PathVariable String recipientId) {
        List<Notification> notifications = queryUseCase.findByRecipient(
                new UserId(UUID.fromString(recipientId)));
        return ResponseEntity.ok(notifications.stream().map(this::toResponse).toList());
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'SUPPORT_READONLY')")
    public ResponseEntity<StatisticsResponse> statistics(@RequestParam String tenantId) {
        TenantId tid = new TenantId(UUID.fromString(tenantId));
        long sent = queryUseCase.countByTenantAndStatus(tid, NotificationStatus.SENT);
        long failed = queryUseCase.countByTenantAndStatus(tid, NotificationStatus.FAILED);
        long pending = queryUseCase.countByTenantAndStatus(tid, NotificationStatus.PENDING);
        long retrying = queryUseCase.countByTenantAndStatus(tid, NotificationStatus.RETRYING);
        return ResponseEntity.ok(new StatisticsResponse(sent, failed, pending, retrying));
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId().toString(),
                n.getTenantId().value().toString(),
                n.getRecipientId().value().toString(),
                n.getChannel().name(),
                n.getSeverity().name(),
                n.getSubject(),
                n.getStatus().name(),
                n.getRetryCount(),
                n.getCreatedAt(),
                n.getSentAt(),
                n.getFailureReason()
        );
    }

    record NotificationResponse(
            String id, String tenantId, String recipientId, String channel,
            String severity, String subject, String status, int retryCount,
            Instant createdAt, Instant sentAt, String failureReason
    ) {}

    record StatisticsResponse(long sent, long failed, long pending, long retrying) {}
}
