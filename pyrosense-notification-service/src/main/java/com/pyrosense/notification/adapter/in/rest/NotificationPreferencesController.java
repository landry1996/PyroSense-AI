package com.pyrosense.notification.adapter.in.rest;

import com.pyrosense.notification.application.port.in.ManageNotificationPreferencesUseCase;
import com.pyrosense.notification.application.port.in.ManageNotificationPreferencesUseCase.UpdatePreferencesCommand;
import com.pyrosense.notification.domain.model.NotificationPreferences;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
public class NotificationPreferencesController {

    private final ManageNotificationPreferencesUseCase useCase;

    public NotificationPreferencesController(ManageNotificationPreferencesUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'ELECTRICIAN', 'OCCUPANT')")
    public ResponseEntity<PreferencesResponse> getPreferences(@PathVariable String userId) {
        TenantId tenantId = TenantContext.require();
        NotificationPreferences prefs = useCase.getPreferences(
                new UserId(UUID.fromString(userId)), tenantId);
        return ResponseEntity.ok(toResponse(prefs));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'ELECTRICIAN', 'OCCUPANT')")
    public ResponseEntity<PreferencesResponse> updatePreferences(
            @PathVariable String userId,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        TenantId tenantId = TenantContext.require();
        NotificationPreferences prefs = useCase.updatePreferences(
                new UserId(UUID.fromString(userId)),
                tenantId,
                new UpdatePreferencesCommand(
                        request.emailEnabled(), request.smsEnabled(),
                        request.pushEnabled(), request.webhookEnabled(),
                        request.quietHoursStart(), request.quietHoursEnd()));
        return ResponseEntity.ok(toResponse(prefs));
    }

    private PreferencesResponse toResponse(NotificationPreferences p) {
        return new PreferencesResponse(
                p.getUserId().value().toString(),
                p.getTenantId().value().toString(),
                p.isEmailEnabled(), p.isSmsEnabled(), p.isPushEnabled(), p.isWebhookEnabled(),
                p.getQuietHoursStart(), p.getQuietHoursEnd(),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    record PreferencesResponse(String userId, String tenantId, boolean emailEnabled, boolean smsEnabled,
                                boolean pushEnabled, boolean webhookEnabled, LocalTime quietHoursStart,
                                LocalTime quietHoursEnd, Instant createdAt, Instant updatedAt) {}

    record UpdatePreferencesRequest(boolean emailEnabled, boolean smsEnabled, boolean pushEnabled,
                                     boolean webhookEnabled, LocalTime quietHoursStart, LocalTime quietHoursEnd) {}
}
