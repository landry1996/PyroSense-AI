package com.pyrosense.notification.application.port.in;

import com.pyrosense.notification.domain.model.Notification;
import com.pyrosense.notification.domain.model.NotificationChannel;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.valueobject.AlertSeverity;

import java.util.List;

public interface SendNotificationUseCase {

    List<Notification> dispatchForAlert(DispatchAlertNotificationCommand command);

    record DispatchAlertNotificationCommand(
            TenantId tenantId,
            String alertId,
            String deviceId,
            AlertSeverity severity,
            String alertType,
            String occurredAt
    ) {
        public String fingerprint() {
            return "%s:%s:%s".formatted(alertId, deviceId, severity.name());
        }
    }
}
