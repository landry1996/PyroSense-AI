package com.pyrosense.notification.application.usecase;

import com.pyrosense.notification.application.port.in.SendNotificationUseCase;
import com.pyrosense.notification.application.port.out.NotificationRepository;
import com.pyrosense.notification.application.port.out.NotificationSender;
import com.pyrosense.notification.domain.model.Notification;
import com.pyrosense.notification.domain.model.NotificationChannel;
import java.util.UUID;

public class SendNotificationService implements SendNotificationUseCase {

    private final NotificationRepository repository;
    private final NotificationSender sender;

    public SendNotificationService(NotificationRepository repository, NotificationSender sender) {
        this.repository = repository;
        this.sender = sender;
    }

    @Override
    public Notification send(SendNotificationCommand command) {
        var notification = new Notification(
                UUID.randomUUID(),
                command.recipientId(),
                NotificationChannel.valueOf(command.channel()),
                command.subject(),
                command.body()
        );
        var saved = repository.save(notification);
        try {
            sender.send(saved);
            saved.markSent();
        } catch (Exception e) {
            saved.markFailed();
        }
        return repository.save(saved);
    }
}
