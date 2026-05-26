package com.pyrosense.notification.config;

import com.pyrosense.notification.application.port.out.*;
import com.pyrosense.notification.application.port.out.NotificationPreferencesRepository;
import com.pyrosense.notification.application.usecase.*;
import com.pyrosense.notification.application.usecase.ManageNotificationPreferencesService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public NotificationDispatcher notificationDispatcher(EmailProviderPort emailProvider,
                                                          SmsProviderPort smsProvider,
                                                          PushProviderPort pushProvider,
                                                          WebhookProviderPort webhookProvider) {
        return new NotificationDispatcher(emailProvider, smsProvider, pushProvider, webhookProvider);
    }

    @Bean
    public SendNotificationService sendNotificationService(NotificationRepositoryPort repository,
                                                            RecipientResolverPort recipientResolver,
                                                            DeduplicationPort deduplication,
                                                            NotificationDispatcher dispatcher) {
        return new SendNotificationService(repository, recipientResolver, deduplication, dispatcher);
    }

    @Bean
    public GetNotificationService getNotificationService(NotificationRepositoryPort repository) {
        return new GetNotificationService(repository);
    }

    @Bean
    public RetryNotificationService retryNotificationService(NotificationRepositoryPort repository,
                                                              RecipientResolverPort recipientResolver,
                                                              NotificationDispatcher dispatcher) {
        return new RetryNotificationService(repository, recipientResolver, dispatcher);
    }

    @Bean
    public ManageNotificationPreferencesService manageNotificationPreferencesService(NotificationPreferencesRepository repository) {
        return new ManageNotificationPreferencesService(repository);
    }
}
