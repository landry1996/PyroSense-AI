package com.pyrosense.ingestion.adapter.out.persistence;

import com.pyrosense.ingestion.application.port.out.TelemetryQueryPort;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class JdbcTelemetryQueryAdapter implements TelemetryQueryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTelemetryQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TimeSeriesPoint> findRawByDevice(TenantId tenantId, DeviceId deviceId,
                                                  Instant from, Instant to, int limit) {
        return jdbcTemplate.query("""
                SELECT measured_at, rms_current, rms_voltage, active_power, power_factor,
                       thd, temperature_celsius, hf_noise_level, micro_arc_count, transient_count
                FROM electrical_telemetry
                WHERE tenant_id = ? AND device_id = ? AND measured_at BETWEEN ? AND ?
                ORDER BY measured_at DESC
                LIMIT ?
                """,
                new TimeSeriesPointRowMapper(),
                tenantId.value(), deviceId.value(),
                Timestamp.from(from), Timestamp.from(to), limit);
    }

    @Override
    public List<AggregatedPoint> findAggregated1Min(TenantId tenantId, DeviceId deviceId,
                                                     Instant from, Instant to) {
        return queryAggregate("telemetry_1min", tenantId, deviceId, from, to);
    }

    @Override
    public List<AggregatedPoint> findAggregated15Min(TenantId tenantId, DeviceId deviceId,
                                                      Instant from, Instant to) {
        return queryAggregate("telemetry_15min", tenantId, deviceId, from, to);
    }

    @Override
    public List<AggregatedPoint> findAggregated1Hour(TenantId tenantId, DeviceId deviceId,
                                                      Instant from, Instant to) {
        return queryAggregate("telemetry_1hour", tenantId, deviceId, from, to);
    }

    @Override
    public List<AggregatedPoint> findAggregatedDaily(TenantId tenantId, DeviceId deviceId,
                                                      Instant from, Instant to) {
        return queryAggregate("telemetry_daily", tenantId, deviceId, from, to);
    }

    @Override
    public List<AggregatedPoint> findByBuilding1Hour(TenantId tenantId, BuildingId buildingId,
                                                      Instant from, Instant to) {
        return jdbcTemplate.query("""
                SELECT bucket, avg_rms_current, avg_rms_voltage, avg_active_power,
                       avg_power_factor, avg_thd, avg_temperature, avg_hf_noise,
                       total_micro_arcs, total_transients, max_temperature, sample_count
                FROM telemetry_1hour
                WHERE tenant_id = ? AND building_id = ? AND bucket BETWEEN ? AND ?
                ORDER BY bucket DESC
                """,
                new AggregatedPointRowMapper(),
                tenantId.value(), buildingId.value(),
                Timestamp.from(from), Timestamp.from(to));
    }

    @Override
    public long countByDevice(TenantId tenantId, DeviceId deviceId, Instant from, Instant to) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM electrical_telemetry
                WHERE tenant_id = ? AND device_id = ? AND measured_at BETWEEN ? AND ?
                """,
                Long.class,
                tenantId.value(), deviceId.value(),
                Timestamp.from(from), Timestamp.from(to));
        return count != null ? count : 0L;
    }

    private List<AggregatedPoint> queryAggregate(String viewName, TenantId tenantId,
                                                  DeviceId deviceId, Instant from, Instant to) {
        String sql = """
                SELECT bucket, avg_rms_current, avg_rms_voltage, avg_active_power,
                       avg_power_factor, avg_thd, avg_temperature, avg_hf_noise,
                       total_micro_arcs, total_transients, max_temperature, sample_count
                FROM %s
                WHERE tenant_id = ? AND device_id = ? AND bucket BETWEEN ? AND ?
                ORDER BY bucket DESC
                """.formatted(viewName);

        return jdbcTemplate.query(sql,
                new AggregatedPointRowMapper(),
                tenantId.value(), deviceId.value(),
                Timestamp.from(from), Timestamp.from(to));
    }

    private static class TimeSeriesPointRowMapper implements RowMapper<TimeSeriesPoint> {
        @Override
        public TimeSeriesPoint mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new TimeSeriesPoint(
                    rs.getTimestamp("measured_at").toInstant(),
                    rs.getDouble("rms_current"),
                    rs.getDouble("rms_voltage"),
                    rs.getDouble("active_power"),
                    rs.getDouble("power_factor"),
                    rs.getDouble("thd"),
                    rs.getDouble("temperature_celsius"),
                    rs.getDouble("hf_noise_level"),
                    rs.getInt("micro_arc_count"),
                    rs.getInt("transient_count")
            );
        }
    }

    private static class AggregatedPointRowMapper implements RowMapper<AggregatedPoint> {
        @Override
        public AggregatedPoint mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AggregatedPoint(
                    rs.getTimestamp("bucket").toInstant(),
                    rs.getDouble("avg_rms_current"),
                    rs.getDouble("avg_rms_voltage"),
                    rs.getDouble("avg_active_power"),
                    rs.getDouble("avg_power_factor"),
                    rs.getDouble("avg_thd"),
                    rs.getDouble("avg_temperature"),
                    rs.getDouble("avg_hf_noise"),
                    rs.getInt("total_micro_arcs"),
                    rs.getInt("total_transients"),
                    rs.getDouble("max_temperature"),
                    rs.getLong("sample_count")
            );
        }
    }
}
