package com.pyrosense.analysis.adapter.out.persistence;

import com.pyrosense.analysis.application.port.out.AnomalyRepositoryPort;
import com.pyrosense.analysis.domain.model.*;
import com.pyrosense.shared.id.DeviceId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcAnomalyRepository implements AnomalyRepositoryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAnomalyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveResult(AnalysisResult result) {
        jdbcTemplate.update("""
                INSERT INTO analysis_results (id, device_id, analyzed_at, anomaly_count, aggregate_risk_score, baseline_available)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                result.id(),
                result.deviceId().value(),
                Timestamp.from(result.analyzedAt()),
                result.anomalyCount(),
                result.aggregateRiskScore(),
                result.baselineAvailable());

        for (SignalAnomaly anomaly : result.anomalies()) {
            jdbcTemplate.update("""
                    INSERT INTO signal_anomalies (id, result_id, device_id, feature, anomaly_type,
                        current_value, baseline_value, deviation_sigma, confidence, detected_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    anomaly.id(),
                    result.id(),
                    anomaly.deviceId().value(),
                    anomaly.feature().metricName(),
                    anomaly.type().name(),
                    anomaly.currentValue(),
                    anomaly.baselineValue(),
                    anomaly.deviationSigma(),
                    anomaly.confidence(),
                    Timestamp.from(anomaly.detectedAt()));
        }
    }

    @Override
    public List<SignalAnomaly> findByDeviceId(DeviceId deviceId, Instant from, Instant to) {
        return jdbcTemplate.query("""
                SELECT id, device_id, feature, anomaly_type, current_value, baseline_value,
                       deviation_sigma, confidence, detected_at
                FROM signal_anomalies
                WHERE device_id = ? AND detected_at BETWEEN ? AND ?
                ORDER BY detected_at DESC
                """,
                this::mapAnomaly,
                deviceId.value(), Timestamp.from(from), Timestamp.from(to));
    }

    @Override
    public long countByDeviceId(DeviceId deviceId, Instant from, Instant to) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM signal_anomalies
                WHERE device_id = ? AND detected_at BETWEEN ? AND ?
                """, Long.class, deviceId.value(), Timestamp.from(from), Timestamp.from(to));
        return count != null ? count : 0;
    }

    private SignalAnomaly mapAnomaly(ResultSet rs, int rowNum) throws SQLException {
        return new SignalAnomaly(
                UUID.fromString(rs.getString("id")),
                new DeviceId(UUID.fromString(rs.getString("device_id"))),
                SignalFeature.fromMetricName(rs.getString("feature")),
                AnomalyType.valueOf(rs.getString("anomaly_type")),
                rs.getDouble("current_value"),
                rs.getDouble("baseline_value"),
                rs.getDouble("deviation_sigma"),
                rs.getDouble("confidence"),
                rs.getTimestamp("detected_at").toInstant()
        );
    }
}
