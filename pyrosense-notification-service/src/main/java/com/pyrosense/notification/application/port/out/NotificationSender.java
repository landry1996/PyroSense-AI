package com.pyrosense.notification.application.port.out;

import com.pyrosense.notification.domain.model.Notification;

public interface NotificationSender {
    void send(Notification notification);
}
