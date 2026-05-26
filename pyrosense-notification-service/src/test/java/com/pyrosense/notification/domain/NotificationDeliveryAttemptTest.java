package com.pyrosense.notification.domain;

import com.pyrosense.notification.domain.model.*;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.valueobject.AlertSeverity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationDeliveryAttemptTest {

    @Test
    void shouldTrackSuccessfulAttempt() {
        Notification n = createNotification();
        n.markSent();

        assertEquals(1, n.getDeliveryAttempts().size());
        assertTrue(n.getDeliveryAttempts().get(0).success());
        assertNull(n.getDeliveryAttempts().get(0).errorMessage());
    }

    @Test
    void shouldTrackFailedAttempt() {
        Notification n = createNotification();
        n.markFailed("Connection timeout");

        assertEquals(1, n.getDeliveryAttempts().size());
        assertFalse(n.getDeliveryAttempts().get(0).success());
        assertEquals("Connection timeout", n.getDeliveryAttempts().get(0).errorMessage());
    }

    @Test
    void shouldTrackMultipleAttempts() {
        Notification n = createNotification();
        n.markFailed("Error 1");
        n.markFailed("Error 2");
        n.markSent();

        assertEquals(3, n.getDeliveryAttempts().size());
        assertFalse(n.getDeliveryAttempts().get(0).success());
        assertFalse(n.getDeliveryAttempts().get(1).success());
        assertTrue(n.getDeliveryAttempts().get(2).success());
    }

    @Test
    void shouldMarkSuppressed() {
        Notification n = createNotification();
        n.markSuppressed();
        assertEquals(NotificationStatus.SUPPRESSED, n.getStatus());
        assertTrue(n.getStatus().isTerminal());
    }

    @Test
    void shouldMarkCancelled() {
        Notification n = createNotification();
        n.markCancelled();
        assertEquals(NotificationStatus.CANCELLED, n.getStatus());
        assertTrue(n.getStatus().isTerminal());
    }

    private Notification createNotification() {
        return new Notification(UUID.randomUUID(), new TenantId(UUID.randomUUID()),
                new UserId(UUID.randomUUID()), NotificationChannel.EMAIL,
                AlertSeverity.WARNING, "Subject", "Body", "fp-123");
    }
}
