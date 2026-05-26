package com.pyrosense.notification.application;

import com.pyrosense.notification.application.port.in.ManageNotificationPreferencesUseCase.UpdatePreferencesCommand;
import com.pyrosense.notification.application.port.out.AuditLogPort;
import com.pyrosense.notification.application.port.out.NotificationPreferencesRepository;
import com.pyrosense.notification.application.port.out.TenantNotificationPolicyRepository;
import com.pyrosense.notification.application.usecase.ManageNotificationPreferencesService;
import com.pyrosense.notification.domain.model.NotificationPreferences;
import com.pyrosense.notification.domain.model.TenantNotificationPolicy;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManagePreferencesServiceTest {

    private NotificationPreferencesRepository prefsRepo;
    private TenantNotificationPolicyRepository policyRepo;
    private AuditLogPort auditLog;
    private ManageNotificationPreferencesService service;

    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final TenantId TENANT = new TenantId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        prefsRepo = mock(NotificationPreferencesRepository.class);
        policyRepo = mock(TenantNotificationPolicyRepository.class);
        auditLog = mock(AuditLogPort.class);
        service = new ManageNotificationPreferencesService(prefsRepo, policyRepo, auditLog);
    }

    @Test
    void normalUserCanUpdatePreferences() {
        when(prefsRepo.findByUserId(USER)).thenReturn(Optional.empty());
        when(policyRepo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(prefsRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdatePreferencesCommand cmd = new UpdatePreferencesCommand(
                true, true, true, false, null, null, "fr", true, true, true, true);

        NotificationPreferences result = service.updatePreferences(USER, TENANT, cmd);

        assertTrue(result.isEmailEnabled());
        assertTrue(result.isSmsEnabled());
        assertTrue(result.isPushEnabled());
        assertFalse(result.isWebhookEnabled());
        verify(auditLog).log(eq("PREFERENCES_UPDATED"), eq(TENANT), anyString());
    }

    @Test
    void criticalOverrideForcedWhenPolicyMandatory() {
        TenantNotificationPolicy policy = new TenantNotificationPolicy(TENANT);
        when(prefsRepo.findByUserId(USER)).thenReturn(Optional.empty());
        when(policyRepo.findByTenantId(TENANT)).thenReturn(Optional.of(policy));
        when(prefsRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdatePreferencesCommand cmd = new UpdatePreferencesCommand(
                true, true, true, false, null, null, "fr", false, true, true, true);

        NotificationPreferences result = service.updatePreferences(USER, TENANT, cmd);

        assertTrue(result.isCriticalOverrideEnabled());
    }

    @Test
    void smsDisabledIfPhoneNotVerifiedAndPolicyRequires() {
        TenantNotificationPolicy policy = new TenantNotificationPolicy(TENANT);
        when(prefsRepo.findByUserId(USER)).thenReturn(Optional.empty());
        when(policyRepo.findByTenantId(TENANT)).thenReturn(Optional.of(policy));
        when(prefsRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdatePreferencesCommand cmd = new UpdatePreferencesCommand(
                true, true, true, false, null, null, "fr", true, false, true, true);

        NotificationPreferences result = service.updatePreferences(USER, TENANT, cmd);

        assertFalse(result.isSmsEnabled());
    }

    @Test
    void pushDisabledIfTokenNotRegisteredAndPolicyRequires() {
        TenantNotificationPolicy policy = new TenantNotificationPolicy(TENANT);
        when(prefsRepo.findByUserId(USER)).thenReturn(Optional.empty());
        when(policyRepo.findByTenantId(TENANT)).thenReturn(Optional.of(policy));
        when(prefsRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdatePreferencesCommand cmd = new UpdatePreferencesCommand(
                true, true, true, false, null, null, "fr", true, true, true, false);

        NotificationPreferences result = service.updatePreferences(USER, TENANT, cmd);

        assertFalse(result.isPushEnabled());
    }

    @Test
    void quietHoursStoredCorrectly() {
        when(prefsRepo.findByUserId(USER)).thenReturn(Optional.empty());
        when(policyRepo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(prefsRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        LocalTime start = LocalTime.of(22, 0);
        LocalTime end = LocalTime.of(7, 0);
        UpdatePreferencesCommand cmd = new UpdatePreferencesCommand(
                true, true, true, false, start, end, "fr", true, true, true, true);

        NotificationPreferences result = service.updatePreferences(USER, TENANT, cmd);

        assertEquals(start, result.getQuietHoursStart());
        assertEquals(end, result.getQuietHoursEnd());
    }

    @Test
    void getPreferencesReturnsDefaultIfNotFound() {
        when(prefsRepo.findByUserId(USER)).thenReturn(Optional.empty());

        NotificationPreferences result = service.getPreferences(USER, TENANT);

        assertEquals(USER, result.getUserId());
        assertEquals(TENANT, result.getTenantId());
        assertTrue(result.isEmailEnabled());
        assertTrue(result.isCriticalOverrideEnabled());
    }

    @Test
    void updateIsAudited() {
        when(prefsRepo.findByUserId(USER)).thenReturn(Optional.empty());
        when(policyRepo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(prefsRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdatePreferencesCommand cmd = new UpdatePreferencesCommand(
                false, false, false, false, null, null, "en", false, false, false, false);

        service.updatePreferences(USER, TENANT, cmd);

        verify(auditLog).log(eq("PREFERENCES_UPDATED"), eq(TENANT), contains(USER.value().toString()));
    }

    @Test
    void smsAllowedWhenPhoneVerifiedAndPolicyRequires() {
        TenantNotificationPolicy policy = new TenantNotificationPolicy(TENANT);
        when(prefsRepo.findByUserId(USER)).thenReturn(Optional.empty());
        when(policyRepo.findByTenantId(TENANT)).thenReturn(Optional.of(policy));
        when(prefsRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdatePreferencesCommand cmd = new UpdatePreferencesCommand(
                true, true, true, false, null, null, "fr", true, true, true, true);

        NotificationPreferences result = service.updatePreferences(USER, TENANT, cmd);

        assertTrue(result.isSmsEnabled());
    }

    @Test
    void languageIsStored() {
        when(prefsRepo.findByUserId(USER)).thenReturn(Optional.empty());
        when(policyRepo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(prefsRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdatePreferencesCommand cmd = new UpdatePreferencesCommand(
                true, true, true, false, null, null, "en", true, true, true, true);

        NotificationPreferences result = service.updatePreferences(USER, TENANT, cmd);

        assertEquals("en", result.getLanguage());
    }
}
