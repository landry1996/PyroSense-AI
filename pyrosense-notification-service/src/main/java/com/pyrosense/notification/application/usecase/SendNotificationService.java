package com.pyrosense.notification.application.usecase;

import com.pyrosense.notification.application.port.in.SendNotificationUseCase;
import com.pyrosense.notification.application.port.out.*;
import com.pyrosense.notification.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SendNotificationService implements SendNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendNotificationService.class);

    private final NotificationRepositoryPort repository;
    private final RecipientResolverPort recipientResolver;
    private final DeduplicationPort deduplication;
    private final NotificationDispatcher dispatcher;

    public SendNotificationService(NotificationRepositoryPort repository,
                                    RecipientResolverPort recipientResolver,
                                    DeduplicationPort deduplication,
                                    NotificationDispatcher dispatcher) {
        this.repository = repository;
        this.recipientResolver = recipientResolver;
        this.deduplication = deduplication;
        this.dispatcher = dispatcher;
    }

    @Override
    public List<Notification> dispatchForAlert(DispatchAlertNotificationCommand command) {
        Set<NotificationChannel> channels = ChannelRoutingPolicy.channelsFor(command.severity());
        List<RecipientType> recipientTypes = ChannelRoutingPolicy.recipientTypesFor(command.severity());
        List<Recipient> recipients = recipientResolver.resolve(command.tenantId(), recipientTypes);

        NotificationTemplate template = NotificationTemplate.forSeverity(command.severity());
        Map<String, String> variables = Map.of(
                "alertType", command.alertType(),
                "deviceId", command.deviceId(),
                "occurredAt", command.occurredAt()
        );

        List<Notification> notifications = new ArrayList<>();

        for (Recipient recipient : recipients) {
            for (NotificationChannel channel : channels) {
                if (!recipient.canReceive(channel)) continue;

                Notification notification = new Notification(
                        UUID.randomUUID(),
                        command.tenantId(),
                        recipient.userId(),
                        channel,
                        command.severity(),
                        template.renderSubject(variables),
                        template.renderBody(variables),
                        command.fingerprint()
                );

                DeduplicationKey key = notification.deduplicationKey();
                if (deduplication.isDuplicate(key)) {
                    log.debug("Skipping duplicate notification for recipient={} channel={}",
                            recipient.userId().value(), channel);
                    continue;
                }

                repository.save(notification);
                dispatcher.dispatch(notification, recipient);
                repository.save(notification);
                deduplication.markSent(key);
                notifications.add(notification);

                log.info("Notification dispatched: id={} channel={} recipient={} status={}",
                        notification.getId(), channel, recipient.maskedEmail(), notification.getStatus());
            }
        }

        return notifications;
    }
}
