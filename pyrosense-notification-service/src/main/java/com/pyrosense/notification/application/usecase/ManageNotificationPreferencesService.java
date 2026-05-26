package com.pyrosense.notification.application.usecase;

import com.pyrosense.notification.application.port.in.ManageNotificationPreferencesUseCase;
import com.pyrosense.notification.application.port.out.NotificationPreferencesRepository;
import com.pyrosense.notification.domain.model.NotificationPreferences;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

public class ManageNotificationPreferencesService implements ManageNotificationPreferencesUseCase {

    private final NotificationPreferencesRepository repository;

    public ManageNotificationPreferencesService(NotificationPreferencesRepository repository) {
        this.repository = repository;
    }

    @Override
    public NotificationPreferences getPreferences(UserId userId, TenantId tenantId) {
        return repository.findByUserId(userId)
                .orElse(new NotificationPreferences(userId, tenantId));
    }

    @Override
    public NotificationPreferences updatePreferences(UserId userId, TenantId tenantId, UpdatePreferencesCommand command) {
        NotificationPreferences prefs = repository.findByUserId(userId)
                .orElse(new NotificationPreferences(userId, tenantId));
        prefs.update(command.emailEnabled(), command.smsEnabled(), command.pushEnabled(),
                command.webhookEnabled(), command.quietHoursStart(), command.quietHoursEnd());
        return repository.save(prefs);
    }
}
