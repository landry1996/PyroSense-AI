package com.pyrosense.notification.application.port.out;

import com.pyrosense.notification.domain.model.NotificationPreferences;
import com.pyrosense.shared.id.UserId;

import java.util.Optional;

public interface NotificationPreferencesRepository {
    Optional<NotificationPreferences> findByUserId(UserId userId);
    NotificationPreferences save(NotificationPreferences preferences);
}
