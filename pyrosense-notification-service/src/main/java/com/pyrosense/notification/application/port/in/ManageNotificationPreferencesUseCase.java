package com.pyrosense.notification.application.port.in;

import com.pyrosense.notification.domain.model.NotificationPreferences;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

import java.time.LocalTime;

public interface ManageNotificationPreferencesUseCase {
    NotificationPreferences getPreferences(UserId userId, TenantId tenantId);
    NotificationPreferences updatePreferences(UserId userId, TenantId tenantId, UpdatePreferencesCommand command);

    record UpdatePreferencesCommand(boolean emailEnabled, boolean smsEnabled, boolean pushEnabled,
                                     boolean webhookEnabled, LocalTime quietHoursStart, LocalTime quietHoursEnd) {}
}
