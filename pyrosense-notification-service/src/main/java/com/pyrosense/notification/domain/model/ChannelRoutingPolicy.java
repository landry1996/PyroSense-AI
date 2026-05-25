package com.pyrosense.notification.domain.model;

import com.pyrosense.shared.valueobject.AlertSeverity;

import java.util.List;
import java.util.Set;

public final class ChannelRoutingPolicy {

    private ChannelRoutingPolicy() {}

    public static Set<NotificationChannel> channelsFor(AlertSeverity severity) {
        return switch (severity) {
            case INFO -> Set.of(NotificationChannel.DASHBOARD);
            case WARNING -> Set.of(NotificationChannel.EMAIL, NotificationChannel.DASHBOARD);
            case CRITICAL -> Set.of(NotificationChannel.SMS, NotificationChannel.PUSH,
                    NotificationChannel.EMAIL, NotificationChannel.DASHBOARD);
        };
    }

    public static List<RecipientType> recipientTypesFor(AlertSeverity severity) {
        return switch (severity) {
            case INFO -> List.of(RecipientType.PROPERTY_MANAGER, RecipientType.TENANT_ADMIN);
            case WARNING -> List.of(RecipientType.PROPERTY_MANAGER, RecipientType.TENANT_ADMIN, RecipientType.OCCUPANT);
            case CRITICAL -> List.of(RecipientType.PROPERTY_MANAGER, RecipientType.TENANT_ADMIN,
                    RecipientType.OCCUPANT, RecipientType.ELECTRICIAN);
        };
    }
}
