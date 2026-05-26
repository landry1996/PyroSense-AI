package com.pyrosense.notification.adapter.in.rest;

import com.pyrosense.notification.application.port.in.GetNotificationQuery;
import com.pyrosense.notification.application.port.in.RetryNotificationUseCase;
import com.pyrosense.notification.domain.model.Notification;
import com.pyrosense.notification.domain.model.NotificationStatus;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.security.TenantContext;
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
    private final RetryNotificationUseCase retryUseCase;

    public NotificationController(GetNotificationQuery queryUseCase,
                                  RetryNotificationUseCase retryUseCase) {
        this.queryUseCase = queryUseCase;
        this.retryUseCase = retryUseCase;
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
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.min(size, 200);
        int offset = page * safeSize;
        TenantId tid = TenantContext.require();
        List<Notification> notifications;
        if (status != null) {
            notifications = queryUseCase.findByStatus(NotificationStatus.valueOf(status)).stream()
                    .filter(n -> n.getTenantId().equals(tid))
                    .toList();
        } else {
            notifications = queryUseCase.findByTenant(tid);
        }
        // TODO: Replace in-memory pagination with proper SQL LIMIT/OFFSET
        return ResponseEntity.ok(notifications.stream()
                .skip(offset).limit(safeSize)
                .map(this::toResponse).toList());
    }

    @GetMapping("/recipient/{recipientId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'ELECTRICIAN', 'OCCUPANT')")
    public ResponseEntity<List<NotificationResponse>> listByRecipient(
            @PathVariable String recipientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.min(size, 200);
        int offset = page * safeSize;
        TenantId currentTenant = TenantContext.require();
        List<Notification> notifications = queryUseCase.findByRecipient(
                new UserId(UUID.fromString(recipientId)));
        // TODO: Replace in-memory pagination with proper SQL LIMIT/OFFSET
        return ResponseEntity.ok(notifications.stream()
                .filter(n -> n.getTenantId().equals(currentTenant))
                .skip(offset).limit(safeSize)
                .map(this::toResponse).toList());
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'SUPPORT_READONLY')")
    public ResponseEntity<StatisticsResponse> statistics() {
        TenantId tid = TenantContext.require();
        long sent = queryUseCase.countByTenantAndStatus(tid, NotificationStatus.SENT);
        long failed = queryUseCase.countByTenantAndStatus(tid, NotificationStatus.FAILED);
        long pending = queryUseCase.countByTenantAndStatus(tid, NotificationStatus.PENDING);
        long retrying = queryUseCase.countByTenantAndStatus(tid, NotificationStatus.RETRYING);
        return ResponseEntity.ok(new StatisticsResponse(sent, failed, pending, retrying));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<NotificationResponse> retry(@PathVariable UUID id) {
        return queryUseCase.findById(id)
                .filter(n -> n.getStatus().canRetry())
                .map(n -> {
                    retryUseCase.retryPendingNotifications();
                    return queryUseCase.findById(id).orElse(n);
                })
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
                maskFailureReason(n.getFailureReason())
        );
    }

    private String maskFailureReason(String reason) {
        if (reason == null) return null;
        if (reason.contains("Exception") || reason.contains("stacktrace") || reason.length() > 200) {
            return "Delivery failed - contact support";
        }
        return reason;
    }

    record NotificationResponse(
            String id, String tenantId, String recipientId, String channel,
            String severity, String subject, String status, int retryCount,
            Instant createdAt, Instant sentAt, String failureReason
    ) {}

    record StatisticsResponse(long sent, long failed, long pending, long retrying) {}
}
