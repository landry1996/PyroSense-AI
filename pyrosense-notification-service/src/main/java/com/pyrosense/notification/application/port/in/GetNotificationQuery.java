package com.pyrosense.notification.application.port.in;

import com.pyrosense.notification.domain.model.Notification;
import com.pyrosense.notification.domain.model.NotificationStatus;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GetNotificationQuery {

    Optional<Notification> findById(UUID id);

    List<Notification> findByRecipient(UserId recipientId);

    List<Notification> findByTenant(TenantId tenantId);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findPendingRetries();

    long countByTenantAndStatus(TenantId tenantId, NotificationStatus status);
}
