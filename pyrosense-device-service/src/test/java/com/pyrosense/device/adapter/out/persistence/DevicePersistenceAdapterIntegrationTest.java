package com.pyrosense.device.adapter.out.persistence;

import com.pyrosense.device.adapter.out.persistence.repository.DeviceJpaRepository;
import com.pyrosense.device.domain.model.ConnectivityType;
import com.pyrosense.device.domain.model.Device;
import com.pyrosense.device.domain.model.DeviceStatus;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.ElectricalPanelId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.pagination.PageRequest;
import com.pyrosense.shared.util.ClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(DevicePersistenceAdapter.class)
class DevicePersistenceAdapterIntegrationTest {

    @Autowired
    private DevicePersistenceAdapter adapter;

    @Autowired
    private DeviceJpaRepository jpaRepository;

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneId.of("UTC")));
        jpaRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    @Test
    void shouldSaveAndFindById() {
        var device = Device.register("SN-001", "1.0.0", "rev-A", ConnectivityType.WIFI, "hash", "admin");
        device.clearDomainEvents();

        var saved = adapter.save(device);
        var found = adapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getSerialNumber()).isEqualTo("SN-001");
        assertThat(found.get().getStatus()).isEqualTo(DeviceStatus.REGISTERED);
    }

    @Test
    void shouldFindBySerialNumber() {
        var device = Device.register("SN-002", "2.0.0", "rev-B", ConnectivityType.LORAWAN, "hash2", "admin");
        device.clearDomainEvents();
        adapter.save(device);

        var found = adapter.findBySerialNumber("SN-002");

        assertThat(found).isPresent();
        assertThat(found.get().getConnectivityType()).isEqualTo(ConnectivityType.LORAWAN);
    }

    @Test
    void shouldCheckExistsBySerialNumber() {
        var device = Device.register("SN-003", "1.0.0", "rev-A", ConnectivityType.ETHERNET, "hash3", "admin");
        device.clearDomainEvents();
        adapter.save(device);

        assertThat(adapter.existsBySerialNumber("SN-003")).isTrue();
        assertThat(adapter.existsBySerialNumber("SN-UNKNOWN")).isFalse();
    }

    @Test
    void shouldFindByTenantId() {
        var tenantId = TenantId.generate();
        var device = Device.register("SN-004", "1.0.0", "rev-A", ConnectivityType.WIFI, "hash4", "admin");
        device.clearDomainEvents();
        device.provision(tenantId, BuildingId.generate(), ElectricalPanelId.generate(), "admin");
        device.clearDomainEvents();
        adapter.save(device);

        var page = adapter.findByTenantId(tenantId, PageRequest.of(0, 10));

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void shouldPersistStatusTransitions() {
        var device = Device.register("SN-005", "1.0.0", "rev-A", ConnectivityType.WIFI, "hash5", "admin");
        device.clearDomainEvents();
        device.provision(TenantId.generate(), BuildingId.generate(), ElectricalPanelId.generate(), "admin");
        device.clearDomainEvents();
        device.activate("admin");
        device.clearDomainEvents();
        adapter.save(device);

        var found = adapter.findById(device.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(DeviceStatus.ACTIVE);
    }
}
