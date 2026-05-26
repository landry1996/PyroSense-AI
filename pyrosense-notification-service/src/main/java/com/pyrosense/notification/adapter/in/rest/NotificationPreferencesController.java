package com.pyrosense.notification.adapter.in.rest;

import com.pyrosense.notification.application.port.in.ManageNotificationPreferencesUseCase;
import com.pyrosense.notification.application.port.in.ManageNotificationPreferencesUseCase.UpdatePreferencesCommand;
import com.pyrosense.notification.application.port.in.ManageTenantNotificationPolicyUseCase;
import com.pyrosense.notification.application.port.in.ManageTenantNotificationPolicyUseCase.UpdatePolicyCommand;
import com.pyrosense.notification.domain.model.NotificationPreferences;
import com.pyrosense.notification.domain.model.TenantNotificationPolicy;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class NotificationPreferencesController {

    private final ManageNotificationPreferencesUseCase useCase;
    private final ManageTenantNotificationPolicyUseCase policyUseCase;

    public NotificationPreferencesController(ManageNotificationPreferencesUseCase useCase,
                                              ManageTenantNotificationPolicyUseCase policyUseCase) {
        this.useCase = useCase;
        this.policyUseCase = policyUseCase;
    }

    @GetMapping("/notification-preferences/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PreferencesResponse> getMyPreferences(Authentication authentication) {
        UserId userId = extractUserId(authentication);
        TenantId tenantId = TenantContext.require();
        NotificationPreferences prefs = useCase.getPreferences(userId, tenantId);
        return ResponseEntity.ok(toResponse(prefs));
    }

    @PutMapping("/notification-preferences/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PreferencesResponse> updateMyPreferences(
            Authentication authentication,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        UserId userId = extractUserId(authentication);
        TenantId tenantId = TenantContext.require();
        NotificationPreferences prefs = useCase.updatePreferences(userId, tenantId,
                new UpdatePreferencesCommand(
                        request.emailEnabled(), request.smsEnabled(),
                        request.pushEnabled(), request.webhookEnabled(),
                        request.quietHoursStart(), request.quietHoursEnd(),
                        request.language() != null ? request.language() : "fr",
                        request.criticalOverrideEnabled(),
                        request.phoneVerified(), request.emailVerified(),
                        request.pushTokenRegistered()));
        return ResponseEntity.ok(toResponse(prefs));
    }

    @GetMapping("/notification-preferences/{userId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<PreferencesResponse> getPreferences(@PathVariable String userId) {
        TenantId tenantId = TenantContext.require();
        NotificationPreferences prefs = useCase.getPreferences(
                new UserId(UUID.fromString(userId)), tenantId);
        return ResponseEntity.ok(toResponse(prefs));
    }

    @PutMapping("/notification-preferences/{userId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
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
                        request.quietHoursStart(), request.quietHoursEnd(),
                        request.language() != null ? request.language() : "fr",
                        request.criticalOverrideEnabled(),
                        request.phoneVerified(), request.emailVerified(),
                        request.pushTokenRegistered()));
        return ResponseEntity.ok(toResponse(prefs));
    }

    @GetMapping("/tenants/{tenantId}/notification-policy")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<PolicyResponse> getTenantPolicy(@PathVariable String tenantId) {
        TenantId tid = new TenantId(UUID.fromString(tenantId));
        TenantNotificationPolicy policy = policyUseCase.getPolicy(tid);
        return ResponseEntity.ok(toPolicyResponse(policy));
    }

    @PutMapping("/tenants/{tenantId}/notification-policy")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<PolicyResponse> updateTenantPolicy(
            @PathVariable String tenantId,
            @Valid @RequestBody UpdatePolicyRequest request) {
        TenantId tid = new TenantId(UUID.fromString(tenantId));
        TenantNotificationPolicy policy = policyUseCase.updatePolicy(tid,
                new UpdatePolicyCommand(
                        request.emailEnabledByDefault(), request.smsEnabledByDefault(),
                        request.pushEnabledByDefault(), request.webhookEnabledByDefault(),
                        request.criticalOverrideMandatory(), request.requirePhoneVerificationForSms(),
                        request.requirePushTokenForPush(),
                        request.defaultQuietHoursStart(), request.defaultQuietHoursEnd(),
                        request.defaultLanguage() != null ? request.defaultLanguage() : "fr"));
        return ResponseEntity.ok(toPolicyResponse(policy));
    }

    private UserId extractUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getSubject();
            return new UserId(UUID.fromString(sub));
        }
        throw new IllegalStateException("Cannot extract user ID from authentication");
    }

    private PreferencesResponse toResponse(NotificationPreferences p) {
        return new PreferencesResponse(
                p.getUserId().value().toString(),
                p.getTenantId().value().toString(),
                p.isEmailEnabled(), p.isSmsEnabled(), p.isPushEnabled(), p.isWebhookEnabled(),
                p.getQuietHoursStart(), p.getQuietHoursEnd(),
                p.getLanguage(), p.isCriticalOverrideEnabled(),
                p.isPhoneVerified(), p.isEmailVerified(), p.isPushTokenRegistered(),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    private PolicyResponse toPolicyResponse(TenantNotificationPolicy p) {
        return new PolicyResponse(
                p.getTenantId().value().toString(),
                p.isEmailEnabledByDefault(), p.isSmsEnabledByDefault(),
                p.isPushEnabledByDefault(), p.isWebhookEnabledByDefault(),
                p.isCriticalOverrideMandatory(), p.isRequirePhoneVerificationForSms(),
                p.isRequirePushTokenForPush(),
                p.getDefaultQuietHoursStart(), p.getDefaultQuietHoursEnd(),
                p.getDefaultLanguage(), p.getCreatedAt(), p.getUpdatedAt());
    }

    record PreferencesResponse(String userId, String tenantId, boolean emailEnabled, boolean smsEnabled,
                                boolean pushEnabled, boolean webhookEnabled, LocalTime quietHoursStart,
                                LocalTime quietHoursEnd, String language, boolean criticalOverrideEnabled,
                                boolean phoneVerified, boolean emailVerified, boolean pushTokenRegistered,
                                Instant createdAt, Instant updatedAt) {}

    record UpdatePreferencesRequest(boolean emailEnabled, boolean smsEnabled, boolean pushEnabled,
                                     boolean webhookEnabled, LocalTime quietHoursStart, LocalTime quietHoursEnd,
                                     String language, boolean criticalOverrideEnabled,
                                     boolean phoneVerified, boolean emailVerified,
                                     boolean pushTokenRegistered) {}

    record PolicyResponse(String tenantId, boolean emailEnabledByDefault, boolean smsEnabledByDefault,
                           boolean pushEnabledByDefault, boolean webhookEnabledByDefault,
                           boolean criticalOverrideMandatory, boolean requirePhoneVerificationForSms,
                           boolean requirePushTokenForPush, LocalTime defaultQuietHoursStart,
                           LocalTime defaultQuietHoursEnd, String defaultLanguage,
                           Instant createdAt, Instant updatedAt) {}

    record UpdatePolicyRequest(boolean emailEnabledByDefault, boolean smsEnabledByDefault,
                                boolean pushEnabledByDefault, boolean webhookEnabledByDefault,
                                boolean criticalOverrideMandatory, boolean requirePhoneVerificationForSms,
                                boolean requirePushTokenForPush, LocalTime defaultQuietHoursStart,
                                LocalTime defaultQuietHoursEnd, String defaultLanguage) {}
}
