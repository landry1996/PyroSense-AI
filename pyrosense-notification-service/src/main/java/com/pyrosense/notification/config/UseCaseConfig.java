package com.pyrosense.notification.config;

import com.pyrosense.notification.application.port.out.NotificationRepository;
import com.pyrosense.notification.application.port.out.NotificationSender;
import com.pyrosense.notification.application.usecase.SendNotificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public SendNotificationService sendNotificationService(NotificationRepository repository,
                                                            NotificationSender sender) {
        return new SendNotificationService(repository, sender);
    }
}
