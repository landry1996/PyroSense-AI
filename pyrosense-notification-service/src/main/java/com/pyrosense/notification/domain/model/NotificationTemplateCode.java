package com.pyrosense.notification.domain.model;

import com.pyrosense.shared.valueobject.AlertSeverity;

public enum NotificationTemplateCode {

    ALERT_WARNING(NotificationPriority.MEDIUM),
    ALERT_CRITICAL(NotificationPriority.URGENT),
    CRITICAL_RISK_DETECTED(NotificationPriority.URGENT),
    INTERVENTION_CREATED(NotificationPriority.MEDIUM),
    INTERVENTION_ASSIGNED(NotificationPriority.HIGH),
    INTERVENTION_COMPLETED(NotificationPriority.LOW),
    REPORT_GENERATED(NotificationPriority.LOW),
    DEVICE_OFFLINE(NotificationPriority.HIGH),
    DEVICE_BACK_ONLINE(NotificationPriority.LOW);

    private final NotificationPriority priority;

    NotificationTemplateCode(NotificationPriority priority) {
        this.priority = priority;
    }

    public NotificationPriority priority() {
        return priority;
    }

    public static NotificationTemplateCode fromAlertSeverity(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> ALERT_CRITICAL;
            case WARNING -> ALERT_WARNING;
            case INFO -> ALERT_WARNING;
        };
    }
}
