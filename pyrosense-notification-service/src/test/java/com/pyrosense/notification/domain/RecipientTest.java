package com.pyrosense.notification.domain;

import com.pyrosense.notification.domain.model.NotificationChannel;
import com.pyrosense.notification.domain.model.Recipient;
import com.pyrosense.notification.domain.model.RecipientType;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecipientTest {

    @Test
    void shouldAcceptEmailWhenConsented() {
        Recipient r = createRecipient(true, false, false);
        assertTrue(r.canReceive(NotificationChannel.EMAIL));
        assertFalse(r.canReceive(NotificationChannel.SMS));
        assertFalse(r.canReceive(NotificationChannel.PUSH));
    }

    @Test
    void shouldRejectEmailWhenNotConsented() {
        Recipient r = createRecipient(false, true, true);
        assertFalse(r.canReceive(NotificationChannel.EMAIL));
    }

    @Test
    void shouldAlwaysAcceptDashboard() {
        Recipient r = createRecipient(false, false, false);
        assertTrue(r.canReceive(NotificationChannel.DASHBOARD));
    }

    @Test
    void shouldAlwaysAcceptWebhook() {
        Recipient r = createRecipient(false, false, false);
        assertTrue(r.canReceive(NotificationChannel.WEBHOOK));
    }

    @Test
    void shouldRejectSmsWithoutPhone() {
        Recipient r = new Recipient(new UserId(UUID.randomUUID()), new TenantId(UUID.randomUUID()),
                RecipientType.OCCUPANT, "test@test.com", null, null,
                true, true, true);
        assertFalse(r.canReceive(NotificationChannel.SMS));
    }

    @Test
    void shouldMaskEmail() {
        Recipient r = createRecipient(true, true, true);
        String masked = r.maskedEmail();
        assertFalse(masked.contains("john.doe"));
        assertTrue(masked.contains("@test.com"));
    }

    @Test
    void shouldMaskPhone() {
        Recipient r = createRecipient(true, true, true);
        String masked = r.maskedPhone();
        assertTrue(masked.startsWith("***"));
        assertTrue(masked.endsWith("0000"));
    }

    private Recipient createRecipient(boolean email, boolean sms, boolean push) {
        return new Recipient(
                new UserId(UUID.randomUUID()),
                new TenantId(UUID.randomUUID()),
                RecipientType.OCCUPANT,
                "john.doe@test.com",
                "+33600000000",
                "push-token-abc123",
                email, sms, push
        );
    }
}
