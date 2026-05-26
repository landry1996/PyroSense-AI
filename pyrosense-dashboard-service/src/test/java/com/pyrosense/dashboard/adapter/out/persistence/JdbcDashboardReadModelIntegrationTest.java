package com.pyrosense.dashboard.adapter.out.persistence;

import com.pyrosense.dashboard.domain.model.*;
import com.pyrosense.shared.id.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Testcontainers
@Import(JdbcDashboardReadModel.class)
class JdbcDashboardReadModelIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("pyrosense_dashboard_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JdbcDashboardReadModel readModel;

    private final TenantId tenantId = new TenantId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private final TenantId otherTenant = new TenantId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    @BeforeEach
    void setUp() {
        jdbc.execute("DELETE FROM dashboard_risk_trend");
        jdbc.execute("DELETE FROM interventions");
        jdbc.execute("DELETE FROM alerts");
        jdbc.execute("DELETE FROM devices");
        jdbc.execute("DELETE FROM buildings");
        seedTestData();
    }

    @Test
    void getOverview_shouldReturnAggregatedData() {
        DashboardOverview overview = readModel.getOverview(tenantId);

        assertThat(overview.totalBuildings()).isEqualTo(2);
        assertThat(overview.totalDevices()).isEqualTo(4);
        assertThat(overview.activeDevices()).isEqualTo(3);
        assertThat(overview.offlineDevices()).isEqualTo(1);
        assertThat(overview.criticalAlerts()).isEqualTo(1);
        assertThat(overview.warningAlerts()).isEqualTo(1);
    }

    @Test
    void getOverview_shouldIsolateTenants() {
        DashboardOverview overview = readModel.getOverview(otherTenant);

        assertThat(overview.totalBuildings()).isEqualTo(1);
        assertThat(overview.totalDevices()).isEqualTo(1);
    }

    @Test
    void getRiskyBuildings_shouldReturnOrderedByRisk() {
        List<RiskyBuilding> buildings = readModel.getRiskyBuildings(tenantId, 10);

        assertThat(buildings).hasSize(2);
        assertThat(buildings.get(0).riskScore()).isGreaterThanOrEqualTo(buildings.get(1).riskScore());
    }

    @Test
    void getRiskyBuildings_shouldRespectLimit() {
        List<RiskyBuilding> buildings = readModel.getRiskyBuildings(tenantId, 1);

        assertThat(buildings).hasSize(1);
    }

    @Test
    void getRecentAlerts_shouldReturnByCreatedAtDesc() {
        List<RecentAlert> alerts = readModel.getRecentAlerts(tenantId, 10);

        assertThat(alerts).hasSize(2);
        assertThat(alerts.get(0).createdAt()).isAfterOrEqualTo(alerts.get(1).createdAt());
    }

    @Test
    void getRecentAlerts_shouldIsolateTenants() {
        List<RecentAlert> alerts = readModel.getRecentAlerts(otherTenant, 10);

        assertThat(alerts).hasSize(1);
    }

    @Test
    void getRiskTrend_shouldReturnDataForPeriod() {
        List<RiskTrendPoint> trend = readModel.getRiskTrend(tenantId, 30);

        assertThat(trend).hasSize(2);
        assertThat(trend.get(0).date()).isBefore(trend.get(1).date());
    }

    @Test
    void getDeviceHealth_shouldReturnAccurateStats() {
        DeviceHealthSummary health = readModel.getDeviceHealth(tenantId);

        assertThat(health.totalDevices()).isEqualTo(4);
        assertThat(health.activeDevices()).isEqualTo(3);
        assertThat(health.offlineDevices()).isEqualTo(1);
    }

    @Test
    void getPriorityInterventions_shouldOrderByPriority() {
        List<PriorityIntervention> interventions = readModel.getPriorityInterventions(tenantId, 10);

        assertThat(interventions).isNotEmpty();
        assertThat(interventions.get(0).priority()).isEqualTo("CRITICAL");
    }

    @Test
    void getInterventionsByAssignee_shouldFilterByUser() {
        List<PriorityIntervention> interventions = readModel.getInterventionsByAssignee("user-elec-1", 10);

        assertThat(interventions).hasSize(1);
    }

    private void seedTestData() {
        String t1 = tenantId.value().toString();
        String t2 = otherTenant.value().toString();

        // Buildings
        jdbc.update("INSERT INTO buildings (id, tenant_id, name, address, risk_score, status) VALUES (?::uuid, ?::uuid, ?, ?, ?, ?)",
                "aaaa1111-0000-0000-0000-000000000001", t1, "Building A", "1 Rue A", 75.0, "AT_RISK");
        jdbc.update("INSERT INTO buildings (id, tenant_id, name, address, risk_score, status) VALUES (?::uuid, ?::uuid, ?, ?, ?, ?)",
                "aaaa1111-0000-0000-0000-000000000002", t1, "Building B", "2 Rue B", 40.0, "WATCH");
        jdbc.update("INSERT INTO buildings (id, tenant_id, name, address, risk_score, status) VALUES (?::uuid, ?::uuid, ?, ?, ?, ?)",
                "aaaa2222-0000-0000-0000-000000000001", t2, "Other Building", "3 Rue C", 20.0, "OK");

        // Devices
        jdbc.update("INSERT INTO devices (id, tenant_id, building_id, serial_number, status, last_seen_at) VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?, NOW())",
                UUID.randomUUID().toString(), t1, "aaaa1111-0000-0000-0000-000000000001", "SN-001", "ACTIVE");
        jdbc.update("INSERT INTO devices (id, tenant_id, building_id, serial_number, status, last_seen_at) VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?, NOW())",
                UUID.randomUUID().toString(), t1, "aaaa1111-0000-0000-0000-000000000001", "SN-002", "ACTIVE");
        jdbc.update("INSERT INTO devices (id, tenant_id, building_id, serial_number, status, last_seen_at) VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?, NOW())",
                UUID.randomUUID().toString(), t1, "aaaa1111-0000-0000-0000-000000000002", "SN-003", "ACTIVE");
        jdbc.update("INSERT INTO devices (id, tenant_id, building_id, serial_number, status, last_seen_at) VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?, NOW() - INTERVAL '48 hours')",
                UUID.randomUUID().toString(), t1, "aaaa1111-0000-0000-0000-000000000002", "SN-004", "OFFLINE");
        jdbc.update("INSERT INTO devices (id, tenant_id, building_id, serial_number, status, last_seen_at) VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?, NOW())",
                UUID.randomUUID().toString(), t2, "aaaa2222-0000-0000-0000-000000000001", "SN-100", "ACTIVE");

        // Alerts
        jdbc.update("INSERT INTO alerts (id, tenant_id, device_id, building_id, title, severity, status, type, created_at) VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?, ?, NOW() - INTERVAL '1 hour')",
                UUID.randomUUID().toString(), t1, "d1", "aaaa1111-0000-0000-0000-000000000001", "Micro-arc detecte", "CRITICAL", "OPEN", "MICRO_ARC_DETECTED");
        jdbc.update("INSERT INTO alerts (id, tenant_id, device_id, building_id, title, severity, status, type, created_at) VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?, ?, NOW() - INTERVAL '2 hours')",
                UUID.randomUUID().toString(), t1, "d2", "aaaa1111-0000-0000-0000-000000000001", "THD eleve", "WARNING", "ACKNOWLEDGED", "HARMONIC_DISTORTION");
        jdbc.update("INSERT INTO alerts (id, tenant_id, device_id, building_id, title, severity, status, type, created_at) VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?, ?, NOW())",
                UUID.randomUUID().toString(), t2, "d100", "aaaa2222-0000-0000-0000-000000000001", "Capteur offline", "INFO", "OPEN", "SENSOR_OFFLINE");

        // Interventions
        jdbc.update("INSERT INTO interventions (id, tenant_id, building_id, type, priority, status, assigned_to, scheduled_date, created_at) VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?, ?, NOW())",
                UUID.randomUUID().toString(), t1, "aaaa1111-0000-0000-0000-000000000001", "CORRECTIVE", "CRITICAL", "PLANNED", "user-elec-1", LocalDate.now().minusDays(1));
        jdbc.update("INSERT INTO interventions (id, tenant_id, building_id, type, priority, status, assigned_to, scheduled_date, created_at) VALUES (?::uuid, ?::uuid, ?, ?, ?, ?, ?, ?, NOW())",
                UUID.randomUUID().toString(), t1, "aaaa1111-0000-0000-0000-000000000002", "PREVENTIVE", "MEDIUM", "PLANNED", "user-elec-2", LocalDate.now().plusDays(5));

        // Risk trend
        jdbc.update("INSERT INTO dashboard_risk_trend (tenant_id, date, avg_score, max_score, alert_count) VALUES (?::uuid, ?, ?, ?, ?)",
                t1, LocalDate.now().minusDays(2), 38.5, 72.0, 3);
        jdbc.update("INSERT INTO dashboard_risk_trend (tenant_id, date, avg_score, max_score, alert_count) VALUES (?::uuid, ?, ?, ?, ?)",
                t1, LocalDate.now().minusDays(1), 42.0, 75.0, 5);
    }
}
