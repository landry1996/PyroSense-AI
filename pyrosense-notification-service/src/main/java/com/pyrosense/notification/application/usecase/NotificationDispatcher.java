package com.pyrosense.notification.application.usecase;

import com.pyrosense.notification.application.port.out.*;
import com.pyrosense.notification.domain.model.Notification;
import com.pyrosense.notification.domain.model.Recipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final EmailProviderPort emailProvider;
    private final SmsProviderPort smsProvider;
    private final PushProviderPort pushProvider;
    private final WebhookProviderPort webhookProvider;

    public NotificationDispatcher(EmailProviderPort emailProvider,
                                   SmsProviderPort smsProvider,
                                   PushProviderPort pushProvider,
                                   WebhookProviderPort webhookProvider) {
        this.emailProvider = emailProvider;
        this.smsProvider = smsProvider;
        this.pushProvider = pushProvider;
        this.webhookProvider = webhookProvider;
    }

    public void dispatch(Notification notification, Recipient recipient) {
        try {
            switch (notification.getChannel()) {
                case EMAIL -> emailProvider.send(recipient.email(),
                        notification.getSubject(), notification.getBody());
                case SMS -> smsProvider.send(recipient.phone(), notification.getBody());
                case PUSH -> pushProvider.send(recipient.pushToken(),
                        notification.getSubject(), notification.getBody());
                case WEBHOOK -> webhookProvider.send(null, Map.of(
                        "notificationId", notification.getId().toString(),
                        "severity", notification.getSeverity().name(),
                        "subject", notification.getSubject(),
                        "body", notification.getBody()
                ));
                case DASHBOARD -> { /* stored in repository, no external dispatch */ }
            }
            notification.markSent();
        } catch (Exception e) {
            log.warn("Notification dispatch failed: id={} channel={} reason={}",
                    notification.getId(), notification.getChannel(), e.getMessage());
            notification.markFailed(e.getMessage());
        }
    }
}
