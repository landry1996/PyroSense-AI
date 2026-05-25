package com.pyrosense.notification.application.port.out;

import com.pyrosense.notification.domain.model.Notification;
import com.pyrosense.notification.domain.model.NotificationStatus;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepositoryPort {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    List<Notification> findByRecipientId(UserId recipientId);

    List<Notification> findByTenantId(TenantId tenantId);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findPendingRetries();

    long countByTenantIdAndStatus(TenantId tenantId, NotificationStatus status);
}
