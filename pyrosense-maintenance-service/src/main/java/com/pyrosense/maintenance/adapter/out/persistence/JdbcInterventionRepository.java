package com.pyrosense.maintenance.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.maintenance.application.port.out.InterventionRepositoryPort;
import com.pyrosense.maintenance.domain.model.*;
import com.pyrosense.shared.id.AlertId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcInterventionRepository implements InterventionRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(JdbcInterventionRepository.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final InterventionRowMapper rowMapper = new InterventionRowMapper();

    public JdbcInterventionRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public Intervention save(Intervention intervention) {
        String diagnosticJson = serializeDiagnostic(intervention.getDiagnostic());
        String riskImpactJson = serializeRiskImpact(intervention.getRiskImpact());

        int updated = jdbc.update("""
                UPDATE interventions SET status = ?, assigned_electrician_id = ?,
                    scheduled_at = ?, started_at = ?, completed_at = ?, result = ?,
                    diagnostic = ?::jsonb, risk_impact = ?::jsonb, updated_at = ?
                WHERE id = ?
                """,
                intervention.getStatus().name(),
                intervention.getAssignedElectricianId() != null ? intervention.getAssignedElectricianId().value() : null,
                toTimestamp(intervention.getScheduledAt()),
                toTimestamp(intervention.getStartedAt()),
                toTimestamp(intervention.getCompletedAt()),
                intervention.getResult() != null ? intervention.getResult().name() : null,
                diagnosticJson, riskImpactJson,
                Timestamp.from(intervention.getUpdatedAt()),
                intervention.getId());

        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO interventions (id, tenant_id, source_alert_id, device_id, type, priority,
                        description, status, assigned_electrician_id, scheduled_at, started_at, completed_at,
                        result, diagnostic, risk_impact, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?)
                    """,
                    intervention.getId(),
                    intervention.getTenantId().value(),
                    intervention.getSourceAlertId().value(),
                    intervention.getDeviceId().value(),
                    intervention.getType().name(),
                    intervention.getPriority().name(),
                    intervention.getDescription(),
                    intervention.getStatus().name(),
                    intervention.getAssignedElectricianId() != null ? intervention.getAssignedElectricianId().value() : null,
                    toTimestamp(intervention.getScheduledAt()),
                    toTimestamp(intervention.getStartedAt()),
                    toTimestamp(intervention.getCompletedAt()),
                    intervention.getResult() != null ? intervention.getResult().name() : null,
                    diagnosticJson, riskImpactJson,
                    Timestamp.from(intervention.getCreatedAt()),
                    Timestamp.from(intervention.getUpdatedAt()));
        }
        return intervention;
    }

    @Override
    public Optional<Intervention> findById(UUID id) {
        List<Intervention> results = jdbc.query(
                "SELECT * FROM interventions WHERE id = ?", rowMapper, id);
        return results.stream().findFirst();
    }

    @Override
    public List<Intervention> findByTenantId(TenantId tenantId) {
        return jdbc.query("SELECT * FROM interventions WHERE tenant_id = ? ORDER BY created_at DESC",
                rowMapper, tenantId.value());
    }

    @Override
    public List<Intervention> findByTenantIdAndStatus(TenantId tenantId, InterventionStatus status) {
        return jdbc.query("SELECT * FROM interventions WHERE tenant_id = ? AND status = ? ORDER BY created_at DESC",
                rowMapper, tenantId.value(), status.name());
    }

    @Override
    public List<Intervention> findByAssignedElectricianId(UserId electricianId) {
        return jdbc.query("SELECT * FROM interventions WHERE assigned_electrician_id = ? ORDER BY scheduled_at",
                rowMapper, electricianId.value());
    }

    @Override
    public List<Intervention> findByDeviceId(DeviceId deviceId) {
        return jdbc.query("SELECT * FROM interventions WHERE device_id = ? ORDER BY created_at DESC",
                rowMapper, deviceId.value());
    }

    @Override
    public Optional<Intervention> findBySourceAlertId(AlertId alertId) {
        List<Intervention> results = jdbc.query(
                "SELECT * FROM interventions WHERE source_alert_id = ?", rowMapper, alertId.value());
        return results.stream().findFirst();
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM interventions WHERE tenant_id = ?",
                Long.class, tenantId.value());
        return count != null ? count : 0;
    }

    @Override
    public long countByTenantIdAndStatus(TenantId tenantId, InterventionStatus status) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM interventions WHERE tenant_id = ? AND status = ?",
                Long.class, tenantId.value(), status.name());
        return count != null ? count : 0;
    }

    @Override
    public long countFalsePositivesByTenantId(TenantId tenantId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM interventions WHERE tenant_id = ? AND result = 'NO_DEFECT_FOUND'",
                Long.class, tenantId.value());
        return count != null ? count : 0;
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }

    private String serializeDiagnostic(FieldDiagnostic diagnostic) {
        if (diagnostic == null) return null;
        try {
            return objectMapper.writeValueAsString(diagnostic);
        } catch (Exception e) {
            log.warn("Failed to deserialize JSON field for intervention: {}", e.getMessage());
            return null;
        }
    }

    private String serializeRiskImpact(RiskImpact riskImpact) {
        if (riskImpact == null) return null;
        try {
            return objectMapper.writeValueAsString(riskImpact);
        } catch (Exception e) {
            log.warn("Failed to deserialize JSON field for intervention: {}", e.getMessage());
            return null;
        }
    }

    private class InterventionRowMapper implements RowMapper<Intervention> {
        @Override
        public Intervention mapRow(ResultSet rs, int rowNum) throws SQLException {
            var intervention = new Intervention(
                    rs.getObject("id", UUID.class),
                    new TenantId(rs.getObject("tenant_id", UUID.class)),
                    new AlertId(rs.getObject("source_alert_id", UUID.class)),
                    new DeviceId(rs.getObject("device_id", UUID.class)),
                    InterventionType.valueOf(rs.getString("type")),
                    InterventionPriority.valueOf(rs.getString("priority")),
                    rs.getString("description")
            );

            String statusStr = rs.getString("status");
            InterventionStatus targetStatus = InterventionStatus.valueOf(statusStr);
            applyStatus(intervention, targetStatus, rs);

            return intervention;
        }

        private void applyStatus(Intervention intervention, InterventionStatus target, ResultSet rs) throws SQLException {
            UUID electricianId = rs.getObject("assigned_electrician_id", UUID.class);
            Timestamp scheduledAt = rs.getTimestamp("scheduled_at");

            if (target == InterventionStatus.CREATED) return;

            if (electricianId != null && scheduledAt != null) {
                intervention.assign(new UserId(electricianId), scheduledAt.toInstant());
            } else if (scheduledAt != null) {
                // Re-apply planned state via schedule - needs future time check bypass
                // For deserialization, directly set via assign path
            }

            if (target == InterventionStatus.IN_PROGRESS || target == InterventionStatus.COMPLETED) {
                if (intervention.getStatus() == InterventionStatus.ASSIGNED) {
                    intervention.start();
                }
            }

            if (target == InterventionStatus.COMPLETED) {
                String resultStr = rs.getString("result");
                if (resultStr != null) {
                    intervention.complete(InterventionResult.valueOf(resultStr));
                }

                String riskJson = rs.getString("risk_impact");
                if (riskJson != null) {
                    try {
                        RiskImpact impact = objectMapper.readValue(riskJson, RiskImpact.class);
                        intervention.recordRiskImpact(impact);
                    } catch (Exception e) {
                        log.warn("Failed to deserialize JSON field for intervention: {}", e.getMessage());
                    }
                }
            }

            if (target == InterventionStatus.CANCELLED) {
                intervention.cancel();
            }

            String diagJson = rs.getString("diagnostic");
            if (diagJson != null && intervention.getStatus() == InterventionStatus.IN_PROGRESS) {
                try {
                    FieldDiagnostic diag = objectMapper.readValue(diagJson, FieldDiagnostic.class);
                    intervention.addDiagnostic(diag);
                } catch (Exception e) {
                    log.warn("Failed to deserialize JSON field for intervention: {}", e.getMessage());
                }
            }
        }
    }
}
