package com.pyrosense.simulator.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedTenantTest {

    @Test
    void shouldCreateTenantWithBuildingsAndDevices() {
        SimulatedTenant tenant = SimulatedTenant.create("acme", 3, 4);

        assertThat(tenant.name()).isEqualTo("acme");
        assertThat(tenant.tenantId()).isNotBlank();
        assertThat(tenant.buildings()).hasSize(3);
        assertThat(tenant.buildings().get(0).devices()).hasSize(4);
    }

    @Test
    void shouldReturnAllDevicesFlattened() {
        SimulatedTenant tenant = SimulatedTenant.create("corp", 2, 3);

        assertThat(tenant.allDevices()).hasSize(6);
    }

    @Test
    void shouldAssignTenantIdToAllBuildings() {
        SimulatedTenant tenant = SimulatedTenant.create("test", 2, 2);

        tenant.buildings().forEach(b ->
                assertThat(b.tenantId()).isEqualTo(tenant.tenantId()));
    }

    @Test
    void shouldAssignTenantIdToAllDevices() {
        SimulatedTenant tenant = SimulatedTenant.create("test", 2, 2);

        tenant.allDevices().forEach(d ->
                assertThat(d.tenantId()).isEqualTo(tenant.tenantId()));
    }
}
