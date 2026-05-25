package com.pyrosense.notification.domain;

import com.pyrosense.notification.domain.model.*;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.valueobject.AlertSeverity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    private static final TenantId TENANT = new TenantId(UUID.randomUUID());
    private static final UserId RECIPIENT = new UserId(UUID.randomUUID());

    @Test
    void shouldCreateWithPendingStatus() {
        Notification n = createNotification();
        assertEquals(NotificationStatus.PENDING, n.getStatus());
        assertEquals(0, n.getRetryCount());
        assertNull(n.getSentAt());
        assertNull(n.getFailureReason());
    }

    @Test
    void shouldMarkAsSent() {
        Notification n = createNotification();
        n.markSent();

        assertEquals(NotificationStatus.SENT, n.getStatus());
        assertNotNull(n.getSentAt());
        assertNull(n.getFailureReason());
    }

    @Test
    void shouldMarkAsRetryingOnFirstFailure() {
        Notification n = createNotification();
        n.markFailed("Connection timeout");

        assertEquals(NotificationStatus.RETRYING, n.getStatus());
        assertEquals(1, n.getRetryCount());
        assertNotNull(n.getNextRetryAt());
        assertEquals("Connection timeout", n.getFailureReason());
    }

    @Test
    void shouldMarkAsFailedAfterMaxRetries() {
        Notification n = createNotification();
        n.markFailed("Error 1");
        n.markFailed("Error 2");
        n.markFailed("Error 3");

        assertEquals(NotificationStatus.FAILED, n.getStatus());
        assertEquals(3, n.getRetryCount());
        assertTrue(n.isExhausted());
    }

    @Test
    void shouldIncrementRetryCountOnEachFailure() {
        Notification n = createNotification();
        n.markFailed("Fail 1");
        assertEquals(1, n.getRetryCount());
        assertEquals(NotificationStatus.RETRYING, n.getStatus());

        n.markFailed("Fail 2");
        assertEquals(2, n.getRetryCount());
        assertEquals(NotificationStatus.RETRYING, n.getStatus());

        n.markFailed("Fail 3");
        assertEquals(3, n.getRetryCount());
        assertEquals(NotificationStatus.FAILED, n.getStatus());
    }

    @Test
    void shouldNotBeExhaustedBeforeMaxRetries() {
        Notification n = createNotification();
        n.markFailed("Error");
        assertFalse(n.isExhausted());
    }

    @Test
    void shouldGenerateDeduplicationKey() {
        Notification n = createNotification();
        DeduplicationKey key = n.deduplicationKey();

        assertEquals(RECIPIENT, key.recipientId());
        assertEquals(NotificationChannel.EMAIL, key.channel());
        assertEquals("alert-123", key.alertFingerprint());
        assertNotNull(key.toKeyString());
    }

    @Test
    void shouldHaveCorrectFields() {
        Notification n = createNotification();

        assertEquals(TENANT, n.getTenantId());
        assertEquals(RECIPIENT, n.getRecipientId());
        assertEquals(NotificationChannel.EMAIL, n.getChannel());
        assertEquals(AlertSeverity.WARNING, n.getSeverity());
        assertEquals("Test Subject", n.getSubject());
        assertEquals("Test Body", n.getBody());
        assertNotNull(n.getCreatedAt());
        assertNotNull(n.getId());
    }

    private Notification createNotification() {
        return new Notification(UUID.randomUUID(), TENANT, RECIPIENT,
                NotificationChannel.EMAIL, AlertSeverity.WARNING,
                "Test Subject", "Test Body", "alert-123");
    }
}
