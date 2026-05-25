package com.pyrosense.notification.application.port.out;

import com.pyrosense.notification.domain.model.Notification;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(UUID id);
}
