package com.pyrosense.notification.config;

import com.pyrosense.notification.application.port.in.RetryNotificationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class RetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetryScheduler.class);

    private final RetryNotificationUseCase retryUseCase;

    public RetryScheduler(RetryNotificationUseCase retryUseCase) {
        this.retryUseCase = retryUseCase;
    }

    @Scheduled(fixedDelayString = "${pyrosense.notification.retry.interval-ms:30000}")
    public void retryFailedNotifications() {
        int retried = retryUseCase.retryPendingNotifications();
        if (retried > 0) {
            log.info("Retried {} failed notifications", retried);
        }
    }
}
