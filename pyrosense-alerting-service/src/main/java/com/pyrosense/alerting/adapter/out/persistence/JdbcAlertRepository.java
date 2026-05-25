package com.pyrosense.alerting.adapter.out.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.alerting.application.port.out.AlertRepositoryPort;
import com.pyrosense.alerting.domain.model.*;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.valueobject.AlertSeverity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class JdbcAlertRepository implements AlertRepositoryPort {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcAlertRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public Alert save(Alert alert) {
        String commentsJson = serializeComments(alert.comments());

        int updated = jdbc.update("""
            UPDATE alerts SET status = ?, assigned_to = ?, escalation_level = ?,
                acknowledged_at = ?, acknowledged_by = ?, resolved_at = ?, resolved_by = ?,
                resolution_note = ?, last_escalated_at = ?, occurrence_count = ?,
                last_occurrence_at = ?, comments = ?
            WHERE id = ?
            """,
                alert.status().name(),
                alert.assignedTo() != null ? alert.assignedTo().value().toString() : null,
                alert.escalationLevel().name(),
                toTimestamp(alert.acknowledgedAt()),
                alert.acknowledgedBy() != null ? alert.acknowledgedBy().value().toString() : null,
                toTimestamp(alert.resolvedAt()),
                alert.resolvedBy() != null ? alert.resolvedBy().value().toString() : null,
                alert.resolutionNote(),
                toTimestamp(alert.lastEscalatedAt()),
                alert.occurrenceCount(),
                toTimestamp(alert.lastOccurrenceAt()),
                commentsJson,
                alert.getId().value().toString()
        );

        if (updated == 0) {
            jdbc.update("""
                INSERT INTO alerts (id, tenant_id, device_id, type, severity, title, description,
                    deduplication_key, status, assigned_to, escalation_level, created_at, sla_deadline,
                    acknowledged_at, acknowledged_by, resolved_at, resolved_by, resolution_note,
                    last_escalated_at, occurrence_count, last_occurrence_at, comments)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                    alert.getId().value().toString(),
                    alert.tenantId().value().toString(),
                    alert.deviceId().value().toString(),
                    alert.type().name(),
                    alert.severity().name(),
                    alert.title(),
                    alert.description(),
                    alert.deduplicationKey().toStringKey(),
                    alert.status().name(),
                    alert.assignedTo() != null ? alert.assignedTo().value().toString() : null,
                    alert.escalationLevel().name(),
                    Timestamp.from(alert.createdAt()),
                    Timestamp.from(alert.slaDeadline()),
                    toTimestamp(alert.acknowledgedAt()),
                    alert.acknowledgedBy() != null ? alert.acknowledgedBy().value().toString() : null,
                    toTimestamp(alert.resolvedAt()),
                    alert.resolvedBy() != null ? alert.resolvedBy().value().toString() : null,
                    alert.resolutionNote(),
                    toTimestamp(alert.lastEscalatedAt()),
                    alert.occurrenceCount(),
                    toTimestamp(alert.lastOccurrenceAt()),
                    commentsJson
            );
        }
        return alert;
    }

    @Override
    public Optional<Alert> findById(AlertId id) {
        List<Alert> results = jdbc.query(
                "SELECT * FROM alerts WHERE id = ?",
                new AlertRowMapper(), id.value().toString());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<Alert> findByTenantId(TenantId tenantId) {
        return jdbc.query("SELECT * FROM alerts WHERE tenant_id = ? ORDER BY created_at DESC",
                new AlertRowMapper(), tenantId.value().toString());
    }

    @Override
    public List<Alert> findByDeviceId(DeviceId deviceId) {
        return jdbc.query("SELECT * FROM alerts WHERE device_id = ? ORDER BY created_at DESC",
                new AlertRowMapper(), deviceId.value().toString());
    }

    @Override
    public List<Alert> findByStatus(AlertStatus status) {
        return jdbc.query("SELECT * FROM alerts WHERE status = ? ORDER BY created_at DESC",
                new AlertRowMapper(), status.name());
    }

    @Override
    public List<Alert> findByTenantAndStatus(TenantId tenantId, AlertStatus status) {
        return jdbc.query("SELECT * FROM alerts WHERE tenant_id = ? AND status = ? ORDER BY created_at DESC",
                new AlertRowMapper(), tenantId.value().toString(), status.name());
    }

    @Override
    public List<Alert> findBySeverity(AlertSeverity severity) {
        return jdbc.query("SELECT * FROM alerts WHERE severity = ? ORDER BY created_at DESC",
                new AlertRowMapper(), severity.name());
    }

    @Override
    public Optional<Alert> findActiveByDeduplicationKey(DeduplicationKey key) {
        List<Alert> results = jdbc.query(
                "SELECT * FROM alerts WHERE deduplication_key = ? AND status NOT IN ('RESOLVED', 'FALSE_POSITIVE') ORDER BY created_at DESC LIMIT 1",
                new AlertRowMapper(), key.toStringKey());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<Alert> findEscalationCandidates() {
        return jdbc.query(
                "SELECT * FROM alerts WHERE severity = 'CRITICAL' AND status IN ('OPEN', 'ACKNOWLEDGED')",
                new AlertRowMapper());
    }

    @Override
    public long countByTenantAndStatus(TenantId tenantId, AlertStatus status) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM alerts WHERE tenant_id = ? AND status = ?",
                Long.class, tenantId.value().toString(), status.name());
        return count != null ? count : 0;
    }

    @Override
    public long countSlaBreached(TenantId tenantId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM alerts WHERE tenant_id = ? AND status NOT IN ('RESOLVED', 'FALSE_POSITIVE') AND sla_deadline < ?",
                Long.class, tenantId.value().toString(), Timestamp.from(Instant.now()));
        return count != null ? count : 0;
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }

    private String serializeComments(List<AlertComment> comments) {
        try {
            List<Map<String, Object>> list = comments.stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.id().toString());
                m.put("author", c.author().value().toString());
                m.put("content", c.content());
                m.put("createdAt", c.createdAt().toString());
                return m;
            }).toList();
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<AlertComment> deserializeComments(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            List<Map<String, String>> list = objectMapper.readValue(json, new TypeReference<>() {});
            return list.stream().map(m -> new AlertComment(
                    UUID.fromString(m.get("id")),
                    new UserId(UUID.fromString(m.get("author"))),
                    m.get("content"),
                    Instant.parse(m.get("createdAt"))
            )).toList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private class AlertRowMapper implements RowMapper<Alert> {
        @Override
        public Alert mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Alert.builder()
                    .id(AlertId.from(rs.getString("id")))
                    .tenantId(new TenantId(UUID.fromString(rs.getString("tenant_id"))))
                    .deviceId(DeviceId.from(rs.getString("device_id")))
                    .type(AlertType.valueOf(rs.getString("type")))
                    .severity(AlertSeverity.valueOf(rs.getString("severity")))
                    .title(rs.getString("title"))
                    .description(rs.getString("description"))
                    .status(AlertStatus.valueOf(rs.getString("status")))
                    .assignedTo(rs.getString("assigned_to") != null ? new UserId(UUID.fromString(rs.getString("assigned_to"))) : null)
                    .escalationLevel(EscalationLevel.valueOf(rs.getString("escalation_level")))
                    .createdAt(rs.getTimestamp("created_at").toInstant())
                    .slaDeadline(rs.getTimestamp("sla_deadline").toInstant())
                    .acknowledgedAt(rs.getTimestamp("acknowledged_at") != null ? rs.getTimestamp("acknowledged_at").toInstant() : null)
                    .acknowledgedBy(rs.getString("acknowledged_by") != null ? new UserId(UUID.fromString(rs.getString("acknowledged_by"))) : null)
                    .resolvedAt(rs.getTimestamp("resolved_at") != null ? rs.getTimestamp("resolved_at").toInstant() : null)
                    .resolvedBy(rs.getString("resolved_by") != null ? new UserId(UUID.fromString(rs.getString("resolved_by"))) : null)
                    .resolutionNote(rs.getString("resolution_note"))
                    .lastEscalatedAt(rs.getTimestamp("last_escalated_at") != null ? rs.getTimestamp("last_escalated_at").toInstant() : null)
                    .occurrenceCount(rs.getInt("occurrence_count"))
                    .lastOccurrenceAt(rs.getTimestamp("last_occurrence_at") != null ? rs.getTimestamp("last_occurrence_at").toInstant() : null)
                    .comments(deserializeComments(rs.getString("comments")))
                    .build();
        }
    }
}
