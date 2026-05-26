package com.pyrosense.notification.application;

import com.pyrosense.notification.application.port.in.ManageTenantNotificationPolicyUseCase.UpdatePolicyCommand;
import com.pyrosense.notification.application.port.out.AuditLogPort;
import com.pyrosense.notification.application.port.out.TenantNotificationPolicyRepository;
import com.pyrosense.notification.application.usecase.ManageTenantNotificationPolicyService;
import com.pyrosense.notification.domain.model.TenantNotificationPolicy;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManageTenantPolicyServiceTest {

    private TenantNotificationPolicyRepository policyRepo;
    private AuditLogPort auditLog;
    private ManageTenantNotificationPolicyService service;

    private static final TenantId TENANT = new TenantId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        policyRepo = mock(TenantNotificationPolicyRepository.class);
        auditLog = mock(AuditLogPort.class);
        service = new ManageTenantNotificationPolicyService(policyRepo, auditLog);
    }

    @Test
    void getPolicyReturnsDefaultIfNotFound() {
        when(policyRepo.findByTenantId(TENANT)).thenReturn(Optional.empty());

        TenantNotificationPolicy result = service.getPolicy(TENANT);

        assertEquals(TENANT, result.getTenantId());
        assertTrue(result.isCriticalOverrideMandatory());
        assertTrue(result.isRequirePhoneVerificationForSms());
        assertTrue(result.isRequirePushTokenForPush());
    }

    @Test
    void managerCanUpdatePolicy() {
        when(policyRepo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(policyRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdatePolicyCommand cmd = new UpdatePolicyCommand(
                true, false, true, false, true, true, true,
                LocalTime.of(23, 0), LocalTime.of(6, 0), "fr");

        TenantNotificationPolicy result = service.updatePolicy(TENANT, cmd);

        assertTrue(result.isEmailEnabledByDefault());
        assertFalse(result.isSmsEnabledByDefault());
        assertTrue(result.isCriticalOverrideMandatory());
        assertEquals(LocalTime.of(23, 0), result.getDefaultQuietHoursStart());
        assertEquals(LocalTime.of(6, 0), result.getDefaultQuietHoursEnd());
    }

    @Test
    void updatePolicyIsAudited() {
        when(policyRepo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(policyRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdatePolicyCommand cmd = new UpdatePolicyCommand(
                true, true, true, true, false, false, false, null, null, "en");

        service.updatePolicy(TENANT, cmd);

        verify(auditLog).log(eq("TENANT_NOTIFICATION_POLICY_UPDATED"), eq(TENANT), anyString());
    }

    @Test
    void managerCanDisableCriticalOverride() {
        when(policyRepo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(policyRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdatePolicyCommand cmd = new UpdatePolicyCommand(
                true, true, true, true, false, true, true, null, null, "fr");

        TenantNotificationPolicy result = service.updatePolicy(TENANT, cmd);

        assertFalse(result.isCriticalOverrideMandatory());
    }

    @Test
    void managerCanRelaxPhoneVerification() {
        when(policyRepo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(policyRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdatePolicyCommand cmd = new UpdatePolicyCommand(
                true, true, true, true, true, false, false, null, null, "fr");

        TenantNotificationPolicy result = service.updatePolicy(TENANT, cmd);

        assertFalse(result.isRequirePhoneVerificationForSms());
        assertFalse(result.isRequirePushTokenForPush());
    }

    @Test
    void existingPolicyIsUpdated() {
        TenantNotificationPolicy existing = new TenantNotificationPolicy(TENANT);
        when(policyRepo.findByTenantId(TENANT)).thenReturn(Optional.of(existing));
        when(policyRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdatePolicyCommand cmd = new UpdatePolicyCommand(
                false, false, false, false, false, false, false, null, null, "de");

        TenantNotificationPolicy result = service.updatePolicy(TENANT, cmd);

        assertFalse(result.isEmailEnabledByDefault());
        assertEquals("de", result.getDefaultLanguage());
    }
}
