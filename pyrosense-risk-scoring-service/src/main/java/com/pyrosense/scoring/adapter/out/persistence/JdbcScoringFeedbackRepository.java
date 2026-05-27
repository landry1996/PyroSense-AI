package com.pyrosense.scoring.adapter.out.persistence;

import com.pyrosense.scoring.application.port.out.ScoringFeedbackRepositoryPort;
import com.pyrosense.scoring.domain.model.FeedbackOutcome;
import com.pyrosense.scoring.domain.model.ScoringAdjustment;
import com.pyrosense.scoring.domain.model.ScoringFeedback;
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
public class JdbcScoringFeedbackRepository implements ScoringFeedbackRepositoryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcScoringFeedbackRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveFeedback(ScoringFeedback feedback) {
        jdbcTemplate.update("""
                INSERT INTO scoring_feedbacks (id, device_id, tenant_id, alert_id, intervention_id,
                    outcome, anomaly_type, risk_score_at_alert, feedback_source, comment, received_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                feedback.getId(),
                feedback.getDeviceId().value(),
                feedback.getTenantId(),
                feedback.getAlertId(),
                feedback.getInterventionId(),
                feedback.getOutcome().name(),
                feedback.getAnomalyType(),
                feedback.getRiskScoreAtAlert(),
                feedback.getFeedbackSource(),
                feedback.getComment(),
                Timestamp.from(feedback.getReceivedAt()));
    }

    @Override
    public void saveAdjustment(ScoringAdjustment adjustment, String deviceId, String tenantId) {
        jdbcTemplate.update("""
                INSERT INTO scoring_adjustments (id, device_id, tenant_id, anomaly_type, adjustment_type,
                    previous_value, new_value, delta, mode, reason, applied_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                adjustment.id(),
                UUID.fromString(deviceId),
                tenantId,
                adjustment.anomalyType(),
                adjustment.type().name(),
                adjustment.previousValue(),
                adjustment.newValue(),
                adjustment.delta(),
                adjustment.mode().name(),
                adjustment.reason(),
                Timestamp.from(adjustment.appliedAt()));
    }

    @Override
    public List<ScoringFeedback> findByTenantAndDevice(String tenantId, String deviceId,
                                                        Instant from, Instant to,
                                                        int offset, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT * FROM scoring_feedbacks WHERE tenant_id = ?
                """);
        var params = new java.util.ArrayList<Object>();
        params.add(tenantId);

        if (deviceId != null) {
            sql.append(" AND device_id = ?");
            params.add(UUID.fromString(deviceId));
        }
        if (from != null) {
            sql.append(" AND received_at >= ?");
            params.add(Timestamp.from(from));
        }
        if (to != null) {
            sql.append(" AND received_at <= ?");
            params.add(Timestamp.from(to));
        }
        sql.append(" ORDER BY received_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), this::mapFeedback, params.toArray());
    }

    @Override
    public int countByTenantAndDevice(String tenantId, String deviceId, Instant from, Instant to) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM scoring_feedbacks WHERE tenant_id = ?");
        var params = new java.util.ArrayList<Object>();
        params.add(tenantId);

        if (deviceId != null) {
            sql.append(" AND device_id = ?");
            params.add(UUID.fromString(deviceId));
        }
        if (from != null) {
            sql.append(" AND received_at >= ?");
            params.add(Timestamp.from(from));
        }
        if (to != null) {
            sql.append(" AND received_at <= ?");
            params.add(Timestamp.from(to));
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    @Override
    public int countByOutcome(String deviceId, String anomalyType, FeedbackOutcome outcome) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM scoring_feedbacks
                WHERE device_id = ? AND anomaly_type = ? AND outcome = ?
                """, Integer.class, UUID.fromString(deviceId), anomalyType, outcome.name());
        return count != null ? count : 0;
    }

    @Override
    public List<ScoringAdjustment> findAdjustments(String deviceId, String anomalyType) {
        if (anomalyType != null) {
            return jdbcTemplate.query("""
                    SELECT * FROM scoring_adjustments
                    WHERE device_id = ? AND anomaly_type = ?
                    ORDER BY applied_at ASC
                    """, this::mapAdjustment, UUID.fromString(deviceId), anomalyType);
        }
        return jdbcTemplate.query("""
                SELECT * FROM scoring_adjustments WHERE device_id = ? ORDER BY applied_at ASC
                """, this::mapAdjustment, UUID.fromString(deviceId));
    }

    @Override
    public int countByOutcomeForTenant(String tenantId, String deviceId, FeedbackOutcome outcome) {
        if (deviceId != null) {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM scoring_feedbacks
                    WHERE tenant_id = ? AND device_id = ? AND outcome = ?
                    """, Integer.class, tenantId, UUID.fromString(deviceId), outcome.name());
            return count != null ? count : 0;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM scoring_feedbacks WHERE tenant_id = ? AND outcome = ?
                """, Integer.class, tenantId, outcome.name());
        return count != null ? count : 0;
    }

    private ScoringFeedback mapFeedback(ResultSet rs, int rowNum) throws SQLException {
        UUID alertId = rs.getObject("alert_id", UUID.class);
        UUID interventionId = rs.getObject("intervention_id", UUID.class);

        return new ScoringFeedback(
                UUID.fromString(rs.getString("id")),
                new DeviceId(UUID.fromString(rs.getString("device_id"))),
                rs.getString("tenant_id"),
                alertId,
                interventionId,
                FeedbackOutcome.valueOf(rs.getString("outcome")),
                rs.getString("anomaly_type"),
                rs.getDouble("risk_score_at_alert"),
                rs.getString("feedback_source"),
                rs.getString("comment"),
                rs.getTimestamp("received_at").toInstant());
    }

    private ScoringAdjustment mapAdjustment(ResultSet rs, int rowNum) throws SQLException {
        return new ScoringAdjustment(
                UUID.fromString(rs.getString("id")),
                rs.getString("anomaly_type"),
                ScoringAdjustment.AdjustmentType.valueOf(rs.getString("adjustment_type")),
                rs.getDouble("previous_value"),
                rs.getDouble("new_value"),
                rs.getDouble("delta"),
                ScoringAdjustment.AdjustmentMode.valueOf(rs.getString("mode")),
                rs.getString("reason"),
                rs.getTimestamp("applied_at").toInstant());
    }
}
