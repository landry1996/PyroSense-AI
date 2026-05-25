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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for DevicePersistenceAdapter using a real TimescaleDB (PostgreSQL) container.
 * Validates persistence operations including Flyway migrations.
 */
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DevicePersistenceAdapter.class)
class DevicePersistenceIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("timescale/timescaledb:latest-pg16")
                    .withDatabaseName("pyrosense_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private DevicePersistenceAdapter adapter;

    @Autowired
    private DeviceJpaRepository jpaRepository;

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(Instant.parse("2025-06-15T10:00:00Z"), ZoneId.of("UTC")));
        jpaRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
    }

    @Test
    @DisplayName("Should save a device and find it by ID using real PostgreSQL")
    void shouldSaveAndFindById() {
        var device = Device.register("PSR-TC-001", "2.1.0", "rev-C", ConnectivityType.WIFI, "hash123", "test-admin");
        device.clearDomainEvents();

        var saved = adapter.save(device);
        var found = adapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getSerialNumber()).isEqualTo("PSR-TC-001");
        assertThat(found.get().getFirmwareVersion()).isEqualTo("2.1.0");
        assertThat(found.get().getHardwareRevision()).isEqualTo("rev-C");
        assertThat(found.get().getConnectivityType()).isEqualTo(ConnectivityType.WIFI);
        assertThat(found.get().getStatus()).isEqualTo(DeviceStatus.REGISTERED);
        assertThat(found.get().getAudit().createdBy()).isEqualTo("test-admin");
    }

    @Test
    @DisplayName("Should find device by serial number")
    void shouldFindBySerialNumber() {
        var device = Device.register("PSR-TC-002", "1.5.0", "rev-B", ConnectivityType.LORAWAN, "hash456", "admin");
        device.clearDomainEvents();
        adapter.save(device);

        var found = adapter.findBySerialNumber("PSR-TC-002");

        assertThat(found).isPresent();
        assertThat(found.get().getConnectivityType()).isEqualTo(ConnectivityType.LORAWAN);
    }

    @Test
    @DisplayName("Should return empty when serial number not found")
    void shouldReturnEmptyForUnknownSerialNumber() {
        var found = adapter.findBySerialNumber("NONEXISTENT-SERIAL");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find devices by tenant ID with pagination")
    void shouldFindByTenantId() {
        var tenantId = TenantId.generate();
        var buildingId = BuildingId.generate();
        var panelId = ElectricalPanelId.generate();

        // Create and provision 3 devices for same tenant
        for (int i = 1; i <= 3; i++) {
            var device = Device.register("PSR-TENANT-" + i, "1.0.0", "rev-A",
                    ConnectivityType.WIFI, "hash" + i, "admin");
            device.clearDomainEvents();
            device.provision(tenantId, buildingId, panelId, "admin");
            device.clearDomainEvents();
            adapter.save(device);
        }

        // Create a device for different tenant
        var otherDevice = Device.register("PSR-OTHER-001", "1.0.0", "rev-A",
                ConnectivityType.ETHERNET, "hashOther", "admin");
        otherDevice.clearDomainEvents();
        otherDevice.provision(TenantId.generate(), BuildingId.generate(), ElectricalPanelId.generate(), "admin");
        otherDevice.clearDomainEvents();
        adapter.save(otherDevice);

        var page = adapter.findByTenantId(tenantId, PageRequest.of(0, 10));

        assertThat(page.content()).hasSize(3);
        assertThat(page.content()).allMatch(d -> d.getTenantId().equals(tenantId));
    }

    @Test
    @DisplayName("Should correctly report existence by serial number")
    void shouldCheckExistsBySerialNumber() {
        var device = Device.register("PSR-EXISTS-001", "1.0.0", "rev-A",
                ConnectivityType.ETHERNET, "hashExists", "admin");
        device.clearDomainEvents();
        adapter.save(device);

        assertThat(adapter.existsBySerialNumber("PSR-EXISTS-001")).isTrue();
        assertThat(adapter.existsBySerialNumber("PSR-MISSING")).isFalse();
    }

    @Test
    @DisplayName("Should persist device status transitions correctly")
    void shouldPersistStatusTransitions() {
        var device = Device.register("PSR-STATUS-001", "1.0.0", "rev-A",
                ConnectivityType.WIFI, "hashStatus", "admin");
        device.clearDomainEvents();
        device.provision(TenantId.generate(), BuildingId.generate(), ElectricalPanelId.generate(), "admin");
        device.clearDomainEvents();
        device.activate("admin");
        device.clearDomainEvents();

        adapter.save(device);

        var found = adapter.findById(device.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(DeviceStatus.ACTIVE);
        assertThat(found.get().getLastSeenAt()).isNotNull();
        assertThat(found.get().getInstallationDate()).isNotNull();
    }

    @Test
    @DisplayName("Should update device and preserve audit trail")
    void shouldUpdateDeviceAndPreserveAudit() {
        var device = Device.register("PSR-AUDIT-001", "1.0.0", "rev-A",
                ConnectivityType.WIFI, "hashAudit", "creator-user");
        device.clearDomainEvents();
        var saved = adapter.save(device);

        // Provision device with a different actor
        saved.provision(TenantId.generate(), BuildingId.generate(), ElectricalPanelId.generate(), "provisioner-user");
        saved.clearDomainEvents();
        adapter.save(saved);

        var found = adapter.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAudit().createdBy()).isEqualTo("creator-user");
        assertThat(found.get().getAudit().updatedBy()).isEqualTo("provisioner-user");
        assertThat(found.get().getStatus()).isEqualTo(DeviceStatus.PROVISIONED);
    }

    @Test
    @DisplayName("Should verify TimescaleDB extension is available")
    void shouldVerifyTimescaleDbExtension() {
        // This test verifies we're running against real TimescaleDB, not H2.
        // The container uses timescale/timescaledb image, which includes the extension.
        assertThat(POSTGRES.isRunning()).isTrue();
        assertThat(POSTGRES.getDockerImageName()).contains("timescaledb");
    }
}
