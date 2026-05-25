package com.pyrosense.notification.application.usecase;

import com.pyrosense.notification.application.port.out.*;
import com.pyrosense.notification.domain.model.*;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.valueobject.AlertSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RetryNotificationServiceTest {

    private RetryReadyRepo repository;
    private RetryNotificationService retryService;
    private TenantId tenantId;
    private UserId recipientId;

    @BeforeEach
    void setUp() {
        tenantId = new TenantId(UUID.randomUUID());
        recipientId = new UserId(UUID.randomUUID());
        repository = new RetryReadyRepo();
        var recipientResolver = new FakeResolver(tenantId, recipientId);
        var dispatcher = new NotificationDispatcher(
                new SendNotificationServiceTest.NoOpEmail(),
                new SendNotificationServiceTest.NoOpSms(),
                new SendNotificationServiceTest.NoOpPush(),
                new SendNotificationServiceTest.NoOpWebhook());
        retryService = new RetryNotificationService(repository, recipientResolver, dispatcher);
    }

    @Test
    void shouldRetryFailedNotification() {
        Notification n = createFailedNotification();
        repository.save(n);

        int retried = retryService.retryPendingNotifications();

        assertEquals(1, retried);
    }

    @Test
    void shouldNotRetryExhaustedNotification() {
        Notification n = createExhaustedNotification();
        repository.save(n);

        int retried = retryService.retryPendingNotifications();
        assertEquals(0, retried);
    }

    @Test
    void shouldReturnZeroWhenNothingToRetry() {
        int retried = retryService.retryPendingNotifications();
        assertEquals(0, retried);
    }

    private Notification createFailedNotification() {
        Notification n = new Notification(UUID.randomUUID(), tenantId, recipientId,
                NotificationChannel.EMAIL, AlertSeverity.WARNING,
                "Subject", "Body", "fingerprint-1");
        n.markFailed("Timeout");
        return n;
    }

    private Notification createExhaustedNotification() {
        Notification n = new Notification(UUID.randomUUID(), tenantId, recipientId,
                NotificationChannel.EMAIL, AlertSeverity.WARNING,
                "Subject", "Body", "fingerprint-2");
        n.markFailed("Error 1");
        n.markFailed("Error 2");
        n.markFailed("Error 3");
        return n;
    }

    static class RetryReadyRepo extends SendNotificationServiceTest.InMemoryNotificationRepo {
        @Override
        public List<Notification> findPendingRetries() {
            return findByStatus(NotificationStatus.RETRYING);
        }
    }

    static class FakeResolver implements RecipientResolverPort {
        private final TenantId tenantId;
        private final UserId userId;
        FakeResolver(TenantId tenantId, UserId userId) {
            this.tenantId = tenantId;
            this.userId = userId;
        }
        @Override
        public List<Recipient> resolve(TenantId tid, List<RecipientType> types) {
            return List.of(new Recipient(userId, tenantId, RecipientType.PROPERTY_MANAGER,
                    "test@test.com", "+33600000000", "push-token", true, true, true));
        }
    }
}
