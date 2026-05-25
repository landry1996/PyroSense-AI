package com.pyrosense.notification.application.usecase;

import com.pyrosense.notification.application.port.in.RetryNotificationUseCase;
import com.pyrosense.notification.application.port.out.NotificationRepositoryPort;
import com.pyrosense.notification.application.port.out.RecipientResolverPort;
import com.pyrosense.notification.domain.model.Notification;
import com.pyrosense.notification.domain.model.Recipient;
import com.pyrosense.notification.domain.model.RecipientType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RetryNotificationService implements RetryNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(RetryNotificationService.class);

    private final NotificationRepositoryPort repository;
    private final RecipientResolverPort recipientResolver;
    private final NotificationDispatcher dispatcher;

    public RetryNotificationService(NotificationRepositoryPort repository,
                                     RecipientResolverPort recipientResolver,
                                     NotificationDispatcher dispatcher) {
        this.repository = repository;
        this.recipientResolver = recipientResolver;
        this.dispatcher = dispatcher;
    }

    @Override
    public int retryPendingNotifications() {
        List<Notification> pending = repository.findPendingRetries();
        int retried = 0;

        for (Notification notification : pending) {
            List<Recipient> recipients = recipientResolver.resolve(
                    notification.getTenantId(),
                    List.of(RecipientType.values()));

            Recipient recipient = recipients.stream()
                    .filter(r -> r.userId().equals(notification.getRecipientId()))
                    .findFirst()
                    .orElse(null);

            if (recipient == null) {
                notification.markFailed("Recipient not found for retry");
                repository.save(notification);
                continue;
            }

            dispatcher.dispatch(notification, recipient);
            repository.save(notification);
            retried++;

            log.info("Retried notification: id={} attempt={} status={}",
                    notification.getId(), notification.getRetryCount(), notification.getStatus());
        }

        return retried;
    }
}
