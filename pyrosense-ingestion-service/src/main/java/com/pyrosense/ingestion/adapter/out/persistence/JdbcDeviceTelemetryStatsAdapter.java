package com.pyrosense.ingestion.adapter.out.persistence;

import com.pyrosense.ingestion.application.port.out.DeviceTelemetryStatsPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Repository
public class JdbcDeviceTelemetryStatsAdapter implements DeviceTelemetryStatsPort {

    private static final int DEFAULT_TELEMETRY_INTERVAL_SECONDS = 10;
    private static final int DEFAULT_HEARTBEAT_INTERVAL_SECONDS = 60;

    private final JdbcTemplate jdbc;

    public JdbcDeviceTelemetryStatsAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public TelemetryStats computeStats(String deviceId, Instant from, Instant to) {
        var result = jdbc.queryForMap("""
                SELECT
                    COUNT(*) as total,
                    COUNT(*) FILTER (WHERE rms_voltage > 1000 OR rms_current > 500 OR temperature_celsius > 200 OR temperature_celsius < -40) as out_of_range,
                    COALESCE(AVG(signal_quality), 0) as avg_signal_quality,
                    COALESCE(AVG(hf_noise_level), 0) as avg_hf_noise,
                    COALESCE(MAX(hf_noise_level), 0) as max_hf_noise,
                    COALESCE(STDDEV(rms_current), 0) as current_stddev,
                    COALESCE(STDDEV(rms_voltage), 0) as voltage_stddev,
                    COALESCE(MAX(ABS(EXTRACT(EPOCH FROM (ingested_at - reading_timestamp)))), 0) as max_clock_drift
                FROM electrical_telemetry
                WHERE device_id = ?::uuid AND reading_timestamp >= ? AND reading_timestamp <= ?
                """, deviceId, java.sql.Timestamp.from(from), java.sql.Timestamp.from(to));

        int total = ((Number) result.get("total")).intValue();
        int outOfRange = ((Number) result.get("out_of_range")).intValue();
        double avgSignalQuality = ((Number) result.get("avg_signal_quality")).doubleValue();
        double avgHfNoise = ((Number) result.get("avg_hf_noise")).doubleValue();
        double maxHfNoise = ((Number) result.get("max_hf_noise")).doubleValue();
        double currentStdDev = ((Number) result.get("current_stddev")).doubleValue();
        double voltageStdDev = ((Number) result.get("voltage_stddev")).doubleValue();
        double maxClockDrift = ((Number) result.get("max_clock_drift")).doubleValue();

        List<String> outOfRangeFields = new java.util.ArrayList<>();
        if (outOfRange > 0) {
            outOfRangeFields.add("voltage/current/temperature");
        }

        return new TelemetryStats(
                total, outOfRange, outOfRangeFields, avgSignalQuality,
                maxClockDrift, avgHfNoise, maxHfNoise,
                currentStdDev, voltageStdDev, 0, 0, 0, "unknown");
    }

    @Override
    public int countExpectedMessages(String deviceId, Instant from, Instant to) {
        long seconds = Duration.between(from, to).getSeconds();
        return (int) (seconds / DEFAULT_TELEMETRY_INTERVAL_SECONDS);
    }

    @Override
    public int countReceivedMessages(String deviceId, Instant from, Instant to) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM electrical_telemetry
                WHERE device_id = ?::uuid AND reading_timestamp >= ? AND reading_timestamp <= ?
                """, Integer.class, deviceId, java.sql.Timestamp.from(from), java.sql.Timestamp.from(to));
        return count != null ? count : 0;
    }

    @Override
    public int countExpectedHeartbeats(String deviceId, Instant from, Instant to) {
        long seconds = Duration.between(from, to).getSeconds();
        return (int) (seconds / DEFAULT_HEARTBEAT_INTERVAL_SECONDS);
    }

    @Override
    public int countReceivedHeartbeats(String deviceId, Instant from, Instant to) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM device_heartbeats
                WHERE device_id = ?::uuid AND received_at >= ? AND received_at <= ?
                """, Integer.class, deviceId, java.sql.Timestamp.from(from), java.sql.Timestamp.from(to));
        return count != null ? count : 0;
    }
}
