package com.pyrosense.notification.domain;

import com.pyrosense.notification.domain.model.NotificationChannel;
import com.pyrosense.notification.domain.model.NotificationPreferences;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.valueobject.AlertSeverity;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationPreferencesRulesTest {

    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final TenantId TENANT = new TenantId(UUID.randomUUID());

    @Test
    void userCanDisableEmailForInfoWarning() {
        NotificationPreferences prefs = createPrefsWithChannels(false, true, true, true);
        prefs.updateFull(false, true, true, true, null, null, "fr", true, true, false, true);

        assertFalse(prefs.isChannelAllowed(NotificationChannel.EMAIL, AlertSeverity.INFO));
        assertFalse(prefs.isChannelAllowed(NotificationChannel.EMAIL, AlertSeverity.WARNING));
    }

    @Test
    void criticalOverrideForcesSmsEvenIfDisabled() {
        NotificationPreferences prefs = NotificationPreferences.reconstituteFull(
                USER, TENANT, true, false, true, true, null, null,
                "fr", true, true, true, true,
                java.time.Instant.now(), java.time.Instant.now());

        assertTrue(prefs.isChannelAllowed(NotificationChannel.SMS, AlertSeverity.CRITICAL));
    }

    @Test
    void criticalOverrideForcesEmailEvenIfDisabled() {
        NotificationPreferences prefs = NotificationPreferences.reconstituteFull(
                USER, TENANT, false, false, false, false, null, null,
                "fr", true, true, true, true,
                java.time.Instant.now(), java.time.Instant.now());

        assertTrue(prefs.isChannelAllowed(NotificationChannel.EMAIL, AlertSeverity.CRITICAL));
    }

    @Test
    void criticalOverrideDisabledDoesNotForce() {
        NotificationPreferences prefs = NotificationPreferences.reconstituteFull(
                USER, TENANT, false, false, false, false, null, null,
                "fr", false, true, true, true,
                java.time.Instant.now(), java.time.Instant.now());

        assertFalse(prefs.isChannelAllowed(NotificationChannel.EMAIL, AlertSeverity.CRITICAL));
    }

    @Test
    void quietHoursBlockNonCritical() {
        LocalTime start = LocalTime.of(22, 0);
        LocalTime end = LocalTime.of(7, 0);
        NotificationPreferences prefs = NotificationPreferences.reconstituteFull(
                USER, TENANT, true, true, true, true, start, end,
                "fr", true, true, true, true,
                java.time.Instant.now(), java.time.Instant.now());

        assertTrue(prefs.isInQuietHours(LocalTime.of(23, 30)));
        assertTrue(prefs.isInQuietHours(LocalTime.of(2, 0)));
        assertFalse(prefs.isInQuietHours(LocalTime.of(12, 0)));
    }

    @Test
    void quietHoursDoNotApplyToCritical() {
        LocalTime start = LocalTime.of(22, 0);
        LocalTime end = LocalTime.of(7, 0);
        NotificationPreferences prefs = NotificationPreferences.reconstituteFull(
                USER, TENANT, true, true, true, true, start, end,
                "fr", true, true, true, true,
                java.time.Instant.now(), java.time.Instant.now());

        assertTrue(prefs.isChannelAllowed(NotificationChannel.SMS, AlertSeverity.CRITICAL));
        assertTrue(prefs.isChannelAllowed(NotificationChannel.EMAIL, AlertSeverity.CRITICAL));
        assertTrue(prefs.isChannelAllowed(NotificationChannel.PUSH, AlertSeverity.CRITICAL));
    }

    @Test
    void smsBlockedIfPhoneNotVerified() {
        NotificationPreferences prefs = NotificationPreferences.reconstituteFull(
                USER, TENANT, true, true, true, true, null, null,
                "fr", false, false, true, true,
                java.time.Instant.now(), java.time.Instant.now());

        assertFalse(prefs.isChannelAllowed(NotificationChannel.SMS, AlertSeverity.WARNING));
    }

    @Test
    void smsAllowedIfPhoneVerified() {
        NotificationPreferences prefs = NotificationPreferences.reconstituteFull(
                USER, TENANT, true, true, true, true, null, null,
                "fr", false, true, true, true,
                java.time.Instant.now(), java.time.Instant.now());

        assertTrue(prefs.isChannelAllowed(NotificationChannel.SMS, AlertSeverity.WARNING));
    }

    @Test
    void pushBlockedIfTokenNotRegistered() {
        NotificationPreferences prefs = NotificationPreferences.reconstituteFull(
                USER, TENANT, true, true, true, true, null, null,
                "fr", false, true, true, false,
                java.time.Instant.now(), java.time.Instant.now());

        assertFalse(prefs.isChannelAllowed(NotificationChannel.PUSH, AlertSeverity.WARNING));
    }

    @Test
    void pushAllowedIfTokenRegistered() {
        NotificationPreferences prefs = NotificationPreferences.reconstituteFull(
                USER, TENANT, true, true, true, true, null, null,
                "fr", false, true, true, true,
                java.time.Instant.now(), java.time.Instant.now());

        assertTrue(prefs.isChannelAllowed(NotificationChannel.PUSH, AlertSeverity.WARNING));
    }

    @Test
    void criticalOverrideStillRequiresPhoneVerificationForSms() {
        NotificationPreferences prefs = NotificationPreferences.reconstituteFull(
                USER, TENANT, true, true, true, true, null, null,
                "fr", true, false, true, true,
                java.time.Instant.now(), java.time.Instant.now());

        assertFalse(prefs.isChannelAllowed(NotificationChannel.SMS, AlertSeverity.CRITICAL));
    }

    @Test
    void dashboardAlwaysAllowed() {
        NotificationPreferences prefs = NotificationPreferences.reconstituteFull(
                USER, TENANT, false, false, false, false, null, null,
                "fr", false, false, false, false,
                java.time.Instant.now(), java.time.Instant.now());

        assertTrue(prefs.isChannelAllowed(NotificationChannel.DASHBOARD, AlertSeverity.INFO));
        assertTrue(prefs.isChannelAllowed(NotificationChannel.DASHBOARD, AlertSeverity.WARNING));
        assertTrue(prefs.isChannelAllowed(NotificationChannel.DASHBOARD, AlertSeverity.CRITICAL));
    }

    @Test
    void defaultPreferencesAllowAllVerifiedChannels() {
        NotificationPreferences prefs = new NotificationPreferences(USER, TENANT);
        assertTrue(prefs.isEmailEnabled());
        assertTrue(prefs.isSmsEnabled());
        assertTrue(prefs.isPushEnabled());
        assertTrue(prefs.isCriticalOverrideEnabled());
        assertEquals("fr", prefs.getLanguage());
        assertFalse(prefs.isPhoneVerified());
        assertFalse(prefs.isEmailVerified());
        assertFalse(prefs.isPushTokenRegistered());
    }

    @Test
    void quietHoursSameDayRange() {
        LocalTime start = LocalTime.of(12, 0);
        LocalTime end = LocalTime.of(14, 0);
        NotificationPreferences prefs = NotificationPreferences.reconstituteFull(
                USER, TENANT, true, true, true, true, start, end,
                "fr", true, true, true, true,
                java.time.Instant.now(), java.time.Instant.now());

        assertTrue(prefs.isInQuietHours(LocalTime.of(13, 0)));
        assertFalse(prefs.isInQuietHours(LocalTime.of(15, 0)));
        assertFalse(prefs.isInQuietHours(LocalTime.of(11, 0)));
    }

    @Test
    void noQuietHoursIfNotConfigured() {
        NotificationPreferences prefs = NotificationPreferences.reconstituteFull(
                USER, TENANT, true, true, true, true, null, null,
                "fr", true, true, true, true,
                java.time.Instant.now(), java.time.Instant.now());

        assertFalse(prefs.isInQuietHours(LocalTime.of(3, 0)));
    }

    private NotificationPreferences createPrefsWithChannels(boolean email, boolean sms, boolean push, boolean webhook) {
        return NotificationPreferences.reconstituteFull(
                USER, TENANT, email, sms, push, webhook, null, null,
                "fr", true, true, true, true,
                java.time.Instant.now(), java.time.Instant.now());
    }
}
