package com.pyrosense.identity.domain.model;

import com.pyrosense.shared.security.Permission;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RolePermissionTest {

    @Test
    void platformAdminHasAllPermissions() {
        assertThat(Role.PLATFORM_ADMIN.permissions()).containsAll(java.util.EnumSet.allOf(Permission.class));
    }

    @Test
    void tenantAdminCanManageUsers() {
        assertThat(Role.TENANT_ADMIN.hasPermission(Permission.USER_CREATE)).isTrue();
        assertThat(Role.TENANT_ADMIN.hasPermission(Permission.USER_DELETE)).isTrue();
    }

    @Test
    void occupantCanOnlyRead() {
        assertThat(Role.OCCUPANT.hasPermission(Permission.ALERT_READ)).isTrue();
        assertThat(Role.OCCUPANT.hasPermission(Permission.ALERT_RESOLVE)).isFalse();
        assertThat(Role.OCCUPANT.hasPermission(Permission.DEVICE_REGISTER)).isFalse();
    }

    @Test
    void electricianCanResolveAlerts() {
        assertThat(Role.ELECTRICIAN.hasPermission(Permission.ALERT_ACKNOWLEDGE)).isTrue();
        assertThat(Role.ELECTRICIAN.hasPermission(Permission.ALERT_RESOLVE)).isTrue();
        assertThat(Role.ELECTRICIAN.hasPermission(Permission.ALERT_ASSIGN)).isFalse();
    }

    @Test
    void deviceCanOnlyIngest() {
        assertThat(Role.DEVICE.hasPermission(Permission.TELEMETRY_INGEST)).isTrue();
        assertThat(Role.DEVICE.hasPermission(Permission.ALERT_READ)).isFalse();
    }

    @Test
    void insurancePartnerCanReadRiskAndReports() {
        assertThat(Role.INSURANCE_PARTNER.hasPermission(Permission.RISK_READ)).isTrue();
        assertThat(Role.INSURANCE_PARTNER.hasPermission(Permission.RISK_EXPORT)).isTrue();
        assertThat(Role.INSURANCE_PARTNER.hasPermission(Permission.REPORT_READ)).isTrue();
        assertThat(Role.INSURANCE_PARTNER.hasPermission(Permission.USER_CREATE)).isFalse();
    }

    @Test
    void supportReadonlyCanReadEverything() {
        assertThat(Role.SUPPORT_READONLY.hasPermission(Permission.USER_READ)).isTrue();
        assertThat(Role.SUPPORT_READONLY.hasPermission(Permission.AUDIT_READ)).isTrue();
        assertThat(Role.SUPPORT_READONLY.hasPermission(Permission.USER_CREATE)).isFalse();
    }
}
