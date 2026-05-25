package com.pyrosense.notification.application.port.in;

import com.pyrosense.notification.domain.model.Notification;
import java.util.UUID;

public interface SendNotificationUseCase {
    Notification send(SendNotificationCommand command);

    record SendNotificationCommand(UUID recipientId, String channel, String subject, String body) {}
}
