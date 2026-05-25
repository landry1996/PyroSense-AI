package com.pyrosense.device.domain.model;

import com.pyrosense.device.domain.event.*;
import com.pyrosense.shared.exception.InvalidStateTransitionException;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.ElectricalPanelId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.*;

class DeviceTest {

    private static final Instant FIXED_TIME = Instant.parse("2025-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(FIXED_TIME, ZoneId.of("UTC")));
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    private Device createRegisteredDevice() {
        return Device.register("SN-001", "1.0.0", "rev-A", ConnectivityType.WIFI, "hash123", "admin");
    }

    private Device createProvisionedDevice() {
        var device = createRegisteredDevice();
        device.clearDomainEvents();
        device.provision(TenantId.generate(), BuildingId.generate(), ElectricalPanelId.generate(), "admin");
        return device;
    }

    @Nested
    class Registration {

        @Test
        void shouldRegisterWithCorrectState() {
            var device = createRegisteredDevice();

            assertThat(device.getId()).isNotNull();
            assertThat(device.getSerialNumber()).isEqualTo("SN-001");
            assertThat(device.getFirmwareVersion()).isEqualTo("1.0.0");
            assertThat(device.getHardwareRevision()).isEqualTo("rev-A");
            assertThat(device.getConnectivityType()).isEqualTo(ConnectivityType.WIFI);
            assertThat(device.getStatus()).isEqualTo(DeviceStatus.REGISTERED);
            assertThat(device.getEnrollmentKeyHash()).isEqualTo("hash123");
        }

        @Test
        void shouldEmitDeviceRegisteredEvent() {
            var device = createRegisteredDevice();

            assertThat(device.getDomainEvents()).hasSize(1);
            assertThat(device.getDomainEvents().get(0)).isInstanceOf(DeviceRegisteredEvent.class);
            var event = (DeviceRegisteredEvent) device.getDomainEvents().get(0);
            assertThat(event.deviceId()).isEqualTo(device.getId());
            assertThat(event.serialNumber()).isEqualTo("SN-001");
        }

        @Test
        void shouldRejectNullSerialNumber() {
            assertThatThrownBy(() -> Device.register(null, "1.0.0", "rev-A", ConnectivityType.WIFI, "hash", "admin"))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Provisioning {

        @Test
        void shouldProvisionRegisteredDevice() {
            var device = createRegisteredDevice();
            device.clearDomainEvents();
            var tenantId = TenantId.generate();
            var buildingId = BuildingId.generate();
            var panelId = ElectricalPanelId.generate();

            device.provision(tenantId, buildingId, panelId, "admin");

            assertThat(device.getStatus()).isEqualTo(DeviceStatus.PROVISIONED);
            assertThat(device.getTenantId()).isEqualTo(tenantId);
            assertThat(device.getBuildingId()).isEqualTo(buildingId);
            assertThat(device.getPanelId()).isEqualTo(panelId);
            assertThat(device.getInstallationDate()).isEqualTo(FIXED_TIME);
        }

        @Test
        void shouldEmitDeviceProvisionedEvent() {
            var device = createRegisteredDevice();
            device.clearDomainEvents();
            device.provision(TenantId.generate(), BuildingId.generate(), ElectricalPanelId.generate(), "admin");

            assertThat(device.getDomainEvents()).hasSize(1);
            assertThat(device.getDomainEvents().get(0)).isInstanceOf(DeviceProvisionedEvent.class);
        }

        @Test
        void shouldRejectProvisioningFromActiveState() {
            var device = createProvisionedDevice();
            device.clearDomainEvents();
            device.activate("admin");

            assertThatThrownBy(() ->
                    device.provision(TenantId.generate(), BuildingId.generate(), ElectricalPanelId.generate(), "admin"))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }
    }

    @Nested
    class Activation {

        @Test
        void shouldActivateProvisionedDevice() {
            var device = createProvisionedDevice();
            device.clearDomainEvents();

            device.activate("admin");

            assertThat(device.getStatus()).isEqualTo(DeviceStatus.ACTIVE);
            assertThat(device.getLastSeenAt()).isEqualTo(FIXED_TIME);
        }

        @Test
        void shouldEmitDeviceActivatedEvent() {
            var device = createProvisionedDevice();
            device.clearDomainEvents();
            device.activate("admin");

            assertThat(device.getDomainEvents()).hasSize(1);
            assertThat(device.getDomainEvents().get(0)).isInstanceOf(DeviceActivatedEvent.class);
        }

        @Test
        void shouldRejectActivationFromRegisteredState() {
            var device = createRegisteredDevice();

            assertThatThrownBy(() -> device.activate("admin"))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }
    }

    @Nested
    class OfflineDetection {

        @Test
        void shouldMarkActiveDeviceOffline() {
            var device = createProvisionedDevice();
            device.clearDomainEvents();
            device.activate("admin");
            device.clearDomainEvents();

            device.markOffline();

            assertThat(device.getStatus()).isEqualTo(DeviceStatus.OFFLINE);
            assertThat(device.getDomainEvents()).hasSize(1);
            assertThat(device.getDomainEvents().get(0)).isInstanceOf(DeviceOfflineDetectedEvent.class);
        }

        @Test
        void shouldNotMarkRevokedDeviceOffline() {
            var device = createProvisionedDevice();
            device.clearDomainEvents();
            device.activate("admin");
            device.revoke("test", "admin");
            device.clearDomainEvents();

            device.markOffline();

            assertThat(device.getStatus()).isEqualTo(DeviceStatus.REVOKED);
            assertThat(device.getDomainEvents()).isEmpty();
        }
    }

    @Nested
    class Revocation {

        @Test
        void shouldRevokeActiveDevice() {
            var device = createProvisionedDevice();
            device.clearDomainEvents();
            device.activate("admin");
            device.clearDomainEvents();

            device.revoke("compromised", "admin");

            assertThat(device.getStatus()).isEqualTo(DeviceStatus.REVOKED);
        }

        @Test
        void shouldEmitDeviceRevokedEvent() {
            var device = createProvisionedDevice();
            device.clearDomainEvents();
            device.activate("admin");
            device.clearDomainEvents();

            device.revoke("end of life", "admin");

            assertThat(device.getDomainEvents()).hasSize(1);
            var event = (DeviceRevokedEvent) device.getDomainEvents().get(0);
            assertThat(event.reason()).isEqualTo("end of life");
        }

        @Test
        void shouldBeIdempotentForRevokedDevice() {
            var device = createProvisionedDevice();
            device.clearDomainEvents();
            device.activate("admin");
            device.revoke("first", "admin");
            device.clearDomainEvents();

            device.revoke("second", "admin");

            assertThat(device.getDomainEvents()).isEmpty();
        }
    }

    @Nested
    class Heartbeat {

        @Test
        void shouldRecordHeartbeatForActiveDevice() {
            var device = createProvisionedDevice();
            device.clearDomainEvents();
            device.activate("admin");

            device.recordHeartbeat();

            assertThat(device.getLastSeenAt()).isEqualTo(FIXED_TIME);
        }

        @Test
        void shouldReactivateOfflineDeviceOnHeartbeat() {
            var device = createProvisionedDevice();
            device.clearDomainEvents();
            device.activate("admin");
            device.markOffline();

            device.recordHeartbeat();

            assertThat(device.getStatus()).isEqualTo(DeviceStatus.ACTIVE);
        }

        @Test
        void shouldIgnoreHeartbeatForRevokedDevice() {
            var device = createProvisionedDevice();
            device.clearDomainEvents();
            device.activate("admin");
            device.revoke("test", "admin");

            device.recordHeartbeat();

            assertThat(device.getStatus()).isEqualTo(DeviceStatus.REVOKED);
        }
    }

    @Nested
    class Queries {

        @Test
        void shouldBeOperationalWhenActiveOrProvisioned() {
            var device = createProvisionedDevice();
            assertThat(device.isOperational()).isTrue();

            device.clearDomainEvents();
            device.activate("admin");
            assertThat(device.isOperational()).isTrue();
        }

        @Test
        void shouldNotBeOperationalWhenOffline() {
            var device = createProvisionedDevice();
            device.clearDomainEvents();
            device.activate("admin");
            device.markOffline();

            assertThat(device.isOperational()).isFalse();
        }

        @Test
        void shouldReportRevokedStatus() {
            var device = createProvisionedDevice();
            device.clearDomainEvents();
            device.activate("admin");
            device.revoke("test", "admin");

            assertThat(device.isRevoked()).isTrue();
        }
    }
}
