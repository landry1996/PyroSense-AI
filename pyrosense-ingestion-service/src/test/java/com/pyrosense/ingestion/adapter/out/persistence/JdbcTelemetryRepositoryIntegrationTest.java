package com.pyrosense.ingestion.adapter.out.persistence;

import com.pyrosense.ingestion.domain.model.TelemetryReading;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.util.ClockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcTelemetryRepository.class)
class JdbcTelemetryRepositoryIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcTelemetryRepository repository;

    @BeforeEach
    void setUp() {
        ClockProvider.setClock(Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneId.of("UTC")));
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS electrical_telemetry (
                    tenant_id UUID NOT NULL,
                    device_id UUID NOT NULL,
                    building_id UUID NOT NULL,
                    electrical_panel_id UUID NOT NULL,
                    circuit_id UUID,
                    measured_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    rms_current DOUBLE PRECISION NOT NULL,
                    rms_voltage DOUBLE PRECISION NOT NULL,
                    active_power DOUBLE PRECISION NOT NULL,
                    reactive_power DOUBLE PRECISION NOT NULL,
                    power_factor DOUBLE PRECISION NOT NULL,
                    thd DOUBLE PRECISION NOT NULL,
                    temperature_celsius DOUBLE PRECISION NOT NULL,
                    hf_noise_level DOUBLE PRECISION NOT NULL DEFAULT 0,
                    micro_arc_count INTEGER NOT NULL DEFAULT 0,
                    transient_count INTEGER NOT NULL DEFAULT 0,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
                )
                """);
    }

    @AfterEach
    void tearDown() {
        ClockProvider.reset();
        jdbcTemplate.execute("DROP TABLE IF EXISTS electrical_telemetry");
    }

    @Test
    void shouldSaveSingleReading() {
        var reading = createReading(DeviceId.generate(), TenantId.generate());
        repository.save(reading);

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM electrical_telemetry WHERE device_id = ?",
                Long.class, reading.getDeviceId().value());
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void shouldSaveBatchReadings() {
        var deviceId = DeviceId.generate();
        var tenantId = TenantId.generate();
        var readings = List.of(
                createReading(deviceId, tenantId),
                createReading(deviceId, tenantId),
                createReading(deviceId, tenantId)
        );

        repository.saveBatch(readings);

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM electrical_telemetry WHERE device_id = ?",
                Long.class, deviceId.value());
        assertThat(count).isEqualTo(3L);
    }

    @Test
    void shouldPersistAllFields() {
        var reading = TelemetryReading.builder()
                .id(UUID.randomUUID())
                .deviceId(DeviceId.generate())
                .tenantId(TenantId.generate())
                .buildingId(BuildingId.generate())
                .electricalPanelId(ElectricalPanelId.generate())
                .timestamp(Instant.parse("2025-01-15T09:55:00Z"))
                .samplingWindowMs(1000)
                .rmsCurrent(12.4)
                .rmsVoltage(230.1)
                .activePower(2800.5)
                .reactivePower(300.2)
                .powerFactor(0.94)
                .thd(4.7)
                .temperatureCelsius(39.5)
                .hfNoiseLevel(0.18)
                .microArcCount(2)
                .transientCount(1)
                .firmwareVersion("1.0.0")
                .payloadHash("abc123def456")
                .ingestedAt(ClockProvider.now())
                .build();

        repository.save(reading);

        var result = jdbcTemplate.queryForMap(
                "SELECT * FROM electrical_telemetry WHERE device_id = ?",
                reading.getDeviceId().value());
        assertThat(result.get("rms_current")).isEqualTo(12.4);
        assertThat(result.get("micro_arc_count")).isEqualTo(2);
        assertThat(result.get("power_factor")).isEqualTo(0.94);
    }

    private TelemetryReading createReading(DeviceId deviceId, TenantId tenantId) {
        return TelemetryReading.builder()
                .id(UUID.randomUUID())
                .deviceId(deviceId)
                .tenantId(tenantId)
                .buildingId(BuildingId.generate())
                .electricalPanelId(ElectricalPanelId.generate())
                .timestamp(Instant.now())
                .samplingWindowMs(1000)
                .rmsCurrent(12.0)
                .rmsVoltage(230.0)
                .activePower(2800.0)
                .reactivePower(300.0)
                .powerFactor(0.94)
                .thd(4.5)
                .temperatureCelsius(35.0)
                .hfNoiseLevel(0.1)
                .microArcCount(0)
                .transientCount(0)
                .firmwareVersion("1.0.0")
                .payloadHash("hash" + UUID.randomUUID().toString().substring(0, 8))
                .ingestedAt(ClockProvider.now())
                .build();
    }
}
