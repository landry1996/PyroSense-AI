package com.pyrosense.ingestion.adapter.out.persistence;

import com.pyrosense.ingestion.application.port.out.TelemetryRepositoryPort;
import com.pyrosense.ingestion.domain.model.TelemetryReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

/**
 * JDBC batch insert adapter for time-series telemetry data.
 *
 * Why JDBC over JPA:
 * - Time-series data is append-only (no updates, no dirty checking needed)
 * - JPA entity lifecycle overhead (L1 cache, flush, merge) wastes CPU on hot path
 * - JDBC batch allows PostgreSQL multi-row INSERT (up to 50x faster than single-row JPA persist)
 * - No identity generation needed (no sequences, no UUID gen on DB side)
 * - TimescaleDB hypertables don't support JPA's @GeneratedValue cleanly
 */
@Repository
public class JdbcTelemetryRepository implements TelemetryRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(JdbcTelemetryRepository.class);

    private static final String INSERT_SQL = """
            INSERT INTO electrical_telemetry (
                tenant_id, device_id, building_id, electrical_panel_id, circuit_id,
                measured_at, rms_current, rms_voltage, active_power, reactive_power,
                power_factor, thd, temperature_celsius, hf_noise_level,
                micro_arc_count, transient_count, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_BATCH_SQL = INSERT_SQL;

    private final JdbcTemplate jdbcTemplate;

    public JdbcTelemetryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void save(TelemetryReading reading) {
        jdbcTemplate.update(INSERT_SQL,
                reading.getTenantId().value(),
                reading.getDeviceId().value(),
                reading.getBuildingId() != null ? reading.getBuildingId().value() : null,
                reading.getElectricalPanelId() != null ? reading.getElectricalPanelId().value() : null,
                reading.getCircuitId() != null ? reading.getCircuitId().value() : null,
                Timestamp.from(reading.getTimestamp()),
                reading.getRmsCurrent(),
                reading.getRmsVoltage(),
                reading.getActivePower(),
                reading.getReactivePower(),
                reading.getPowerFactor(),
                reading.getThd(),
                reading.getTemperatureCelsius(),
                reading.getHfNoiseLevel(),
                reading.getMicroArcCount(),
                reading.getTransientCount(),
                Timestamp.from(reading.getIngestedAt())
        );
    }

    @Transactional
    public void saveBatch(List<TelemetryReading> readings) {
        if (readings.isEmpty()) return;

        jdbcTemplate.batchUpdate(INSERT_BATCH_SQL, readings, readings.size(),
                (PreparedStatement ps, TelemetryReading r) -> {
                    ps.setObject(1, r.getTenantId().value());
                    ps.setObject(2, r.getDeviceId().value());
                    ps.setObject(3, r.getBuildingId() != null ? r.getBuildingId().value() : null);
                    ps.setObject(4, r.getElectricalPanelId() != null ? r.getElectricalPanelId().value() : null);
                    ps.setObject(5, r.getCircuitId() != null ? r.getCircuitId().value() : null);
                    ps.setTimestamp(6, Timestamp.from(r.getTimestamp()));
                    ps.setDouble(7, r.getRmsCurrent());
                    ps.setDouble(8, r.getRmsVoltage());
                    ps.setDouble(9, r.getActivePower());
                    ps.setDouble(10, r.getReactivePower());
                    ps.setDouble(11, r.getPowerFactor());
                    ps.setDouble(12, r.getThd());
                    ps.setDouble(13, r.getTemperatureCelsius());
                    ps.setDouble(14, r.getHfNoiseLevel());
                    ps.setInt(15, r.getMicroArcCount());
                    ps.setInt(16, r.getTransientCount());
                    ps.setTimestamp(17, Timestamp.from(r.getIngestedAt()));
                });

        log.debug("Batch inserted {} telemetry readings", readings.size());
    }
}
