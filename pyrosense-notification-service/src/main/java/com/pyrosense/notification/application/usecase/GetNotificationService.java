package com.pyrosense.notification.application.usecase;

import com.pyrosense.notification.application.port.in.GetNotificationQuery;
import com.pyrosense.notification.application.port.out.NotificationRepositoryPort;
import com.pyrosense.notification.domain.model.Notification;
import com.pyrosense.notification.domain.model.NotificationStatus;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GetNotificationService implements GetNotificationQuery {

    private final NotificationRepositoryPort repository;

    public GetNotificationService(NotificationRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<Notification> findByRecipient(UserId recipientId) {
        return repository.findByRecipientId(recipientId);
    }

    @Override
    public List<Notification> findByTenant(TenantId tenantId) {
        return repository.findByTenantId(tenantId);
    }

    @Override
    public List<Notification> findByStatus(NotificationStatus status) {
        return repository.findByStatus(status);
    }

    @Override
    public List<Notification> findPendingRetries() {
        return repository.findPendingRetries();
    }

    @Override
    public long countByTenantAndStatus(TenantId tenantId, NotificationStatus status) {
        return repository.countByTenantIdAndStatus(tenantId, status);
    }
}
