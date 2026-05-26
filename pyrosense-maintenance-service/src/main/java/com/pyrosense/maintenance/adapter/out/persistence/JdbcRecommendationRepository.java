package com.pyrosense.maintenance.adapter.out.persistence;

import com.pyrosense.maintenance.application.port.out.RecommendationRepositoryPort;
import com.pyrosense.maintenance.domain.model.*;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcRecommendationRepository implements RecommendationRepositoryPort {

    private final JdbcTemplate jdbc;
    private final RecommendationRowMapper rowMapper = new RecommendationRowMapper();

    public JdbcRecommendationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public InterventionRecommendation save(InterventionRecommendation rec) {
        int updated = jdbc.update("""
                UPDATE intervention_recommendations SET status = ?, rejection_reason = ?,
                    accepted_intervention_id = ?, decided_at = ?
                WHERE id = ?
                """,
                rec.getStatus().name(),
                rec.getRejectionReason(),
                rec.getAcceptedInterventionId(),
                rec.getDecidedAt() != null ? Timestamp.from(rec.getDecidedAt()) : null,
                rec.getId());

        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO intervention_recommendations (id, tenant_id, alert_id, device_id,
                        suggested_type, suggested_priority, reason, sla_deadline_seconds,
                        sla_expires_at, status, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    rec.getId(),
                    rec.getTenantId().value(),
                    rec.getAlertId().value(),
                    rec.getDeviceId().value(),
                    rec.getSuggestedType().name(),
                    rec.getSuggestedPriority().name(),
                    rec.getReason(),
                    rec.getSlaDeadline().getSeconds(),
                    Timestamp.from(rec.getSlaExpiresAt()),
                    rec.getStatus().name(),
                    Timestamp.from(rec.getCreatedAt()));
        }
        return rec;
    }

    @Override
    public Optional<InterventionRecommendation> findById(UUID id) {
        List<InterventionRecommendation> results = jdbc.query(
                "SELECT * FROM intervention_recommendations WHERE id = ?", rowMapper, id);
        return results.stream().findFirst();
    }

    @Override
    public Optional<InterventionRecommendation> findByAlertId(AlertId alertId) {
        List<InterventionRecommendation> results = jdbc.query(
                "SELECT * FROM intervention_recommendations WHERE alert_id = ?", rowMapper, alertId.value());
        return results.stream().findFirst();
    }

    @Override
    public List<InterventionRecommendation> findByTenantIdAndStatus(TenantId tenantId, RecommendationStatus status) {
        return jdbc.query(
                "SELECT * FROM intervention_recommendations WHERE tenant_id = ? AND status = ? ORDER BY created_at DESC",
                rowMapper, tenantId.value(), status.name());
    }

    @Override
    public List<InterventionRecommendation> findPendingExpired() {
        return jdbc.query(
                "SELECT * FROM intervention_recommendations WHERE status = 'PENDING' AND sla_expires_at < NOW()",
                rowMapper);
    }

    private static class RecommendationRowMapper implements RowMapper<InterventionRecommendation> {
        @Override
        public InterventionRecommendation mapRow(ResultSet rs, int rowNum) throws SQLException {
            var rec = new InterventionRecommendation(
                    rs.getObject("id", UUID.class),
                    new TenantId(rs.getObject("tenant_id", UUID.class)),
                    new AlertId(rs.getObject("alert_id", UUID.class)),
                    new DeviceId(rs.getObject("device_id", UUID.class)),
                    InterventionType.valueOf(rs.getString("suggested_type")),
                    InterventionPriority.valueOf(rs.getString("suggested_priority")),
                    rs.getString("reason"),
                    Duration.ofSeconds(rs.getLong("sla_deadline_seconds"))
            );

            String status = rs.getString("status");
            UUID acceptedId = rs.getObject("accepted_intervention_id", UUID.class);
            String rejectionReason = rs.getString("rejection_reason");

            if ("ACCEPTED".equals(status) && acceptedId != null) {
                rec.accept(acceptedId);
            } else if ("REJECTED".equals(status) && rejectionReason != null) {
                rec.reject(rejectionReason);
            } else if ("EXPIRED".equals(status)) {
                rec.expire();
            }

            return rec;
        }
    }
}
