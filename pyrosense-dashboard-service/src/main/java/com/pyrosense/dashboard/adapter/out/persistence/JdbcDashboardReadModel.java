package com.pyrosense.dashboard.adapter.out.persistence;

import com.pyrosense.dashboard.application.port.out.DashboardReadModelPort;
import com.pyrosense.dashboard.domain.model.*;
import com.pyrosense.shared.id.TenantId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Repository
public class JdbcDashboardReadModel implements DashboardReadModelPort {

    private final JdbcTemplate jdbc;

    public JdbcDashboardReadModel(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public DashboardOverview getOverview(TenantId tenantId) {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM buildings WHERE tenant_id = ?) AS total_buildings,
                    (SELECT COUNT(*) FROM devices WHERE tenant_id = ?) AS total_devices,
                    (SELECT COUNT(*) FROM devices WHERE tenant_id = ? AND status = 'ACTIVE') AS active_devices,
                    (SELECT COUNT(*) FROM devices WHERE tenant_id = ? AND status = 'OFFLINE') AS offline_devices,
                    (SELECT COALESCE(AVG(risk_score), 0) FROM buildings WHERE tenant_id = ?) AS avg_risk_score,
                    (SELECT COUNT(*) FROM alerts WHERE tenant_id = ? AND status IN ('OPEN', 'ACKNOWLEDGED') AND severity = 'CRITICAL') AS critical_alerts,
                    (SELECT COUNT(*) FROM alerts WHERE tenant_id = ? AND status IN ('OPEN', 'ACKNOWLEDGED') AND severity = 'WARNING') AS warning_alerts,
                    (SELECT COUNT(*) FROM interventions WHERE tenant_id = ? AND status NOT IN ('COMPLETED', 'CANCELLED')) AS open_interventions,
                    (SELECT COUNT(*) FROM interventions WHERE tenant_id = ? AND status NOT IN ('COMPLETED', 'CANCELLED') AND scheduled_date < CURRENT_DATE) AS overdue_interventions
                """;
        String tenantValue = tenantId.value().toString();
        return jdbc.queryForObject(sql, (rs, rowNum) -> new DashboardOverview(
                tenantId,
                rs.getInt("total_buildings"),
                rs.getInt("total_devices"),
                rs.getInt("active_devices"),
                rs.getInt("offline_devices"),
                rs.getDouble("avg_risk_score"),
                rs.getInt("critical_alerts"),
                rs.getInt("warning_alerts"),
                rs.getInt("open_interventions"),
                rs.getInt("overdue_interventions"),
                Instant.now()
        ), tenantValue, tenantValue, tenantValue, tenantValue, tenantValue,
                tenantValue, tenantValue, tenantValue, tenantValue);
    }

    @Override
    public List<RiskyBuilding> getRiskyBuildings(TenantId tenantId, int limit) {
        String sql = """
                SELECT b.id, b.name, b.address, b.risk_score, b.status,
                       (SELECT COUNT(*) FROM alerts a WHERE a.building_id = b.id::text AND a.status IN ('OPEN', 'ACKNOWLEDGED')) AS open_alerts,
                       (SELECT COUNT(*) FROM alerts a WHERE a.building_id = b.id::text AND a.status IN ('OPEN', 'ACKNOWLEDGED') AND a.severity = 'CRITICAL') AS critical_alerts,
                       (SELECT a.severity FROM alerts a WHERE a.building_id = b.id::text AND a.status IN ('OPEN', 'ACKNOWLEDGED') ORDER BY CASE a.severity WHEN 'CRITICAL' THEN 1 WHEN 'WARNING' THEN 2 ELSE 3 END LIMIT 1) AS highest_severity,
                       (SELECT MAX(a.created_at) FROM alerts a WHERE a.building_id = b.id::text) AS last_alert_at
                FROM buildings b
                WHERE b.tenant_id = ? AND b.risk_score > 0
                ORDER BY b.risk_score DESC
                LIMIT ?
                """;
        return jdbc.query(sql, (rs, rowNum) -> new RiskyBuilding(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("address"),
                rs.getDouble("risk_score"),
                rs.getString("status"),
                rs.getInt("open_alerts"),
                rs.getInt("critical_alerts"),
                rs.getString("highest_severity"),
                toInstant(rs.getTimestamp("last_alert_at"))
        ), tenantId.value().toString(), limit);
    }

    @Override
    public List<RiskTrendPoint> getRiskTrend(TenantId tenantId, int days) {
        String sql = """
                SELECT date, avg_score, max_score, alert_count
                FROM dashboard_risk_trend
                WHERE tenant_id = ? AND date >= CURRENT_DATE - ? * INTERVAL '1 day'
                ORDER BY date ASC
                """;
        return jdbc.query(sql, (rs, rowNum) -> new RiskTrendPoint(
                rs.getDate("date").toLocalDate(),
                rs.getDouble("avg_score"),
                rs.getDouble("max_score"),
                rs.getInt("alert_count")
        ), tenantId.value().toString(), days);
    }

    @Override
    public List<RecentAlert> getRecentAlerts(TenantId tenantId, int limit) {
        String sql = """
                SELECT a.id, a.title, a.severity, a.status, a.type,
                       a.building_id, COALESCE(b.name, '') AS building_name,
                       a.device_id, a.created_at, a.sla_breached
                FROM alerts a
                LEFT JOIN buildings b ON b.id::text = a.building_id AND b.tenant_id = ?
                WHERE a.tenant_id = ?
                ORDER BY a.created_at DESC
                LIMIT ?
                """;
        return jdbc.query(sql, (rs, rowNum) -> new RecentAlert(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("severity"),
                rs.getString("status"),
                rs.getString("type"),
                rs.getString("building_id"),
                rs.getString("building_name"),
                rs.getString("device_id"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getBoolean("sla_breached")
        ), tenantId.value().toString(), tenantId.value().toString(), limit);
    }

    @Override
    public List<PriorityIntervention> getPriorityInterventions(TenantId tenantId, int limit) {
        String sql = """
                SELECT i.id, i.type, i.priority, i.status, i.building_id,
                       COALESCE(b.name, '') AS building_name, i.assigned_to,
                       i.scheduled_date, i.created_at,
                       (i.scheduled_date < CURRENT_DATE AND i.status NOT IN ('COMPLETED', 'CANCELLED')) AS overdue
                FROM interventions i
                LEFT JOIN buildings b ON b.id::text = i.building_id AND b.tenant_id = ?
                WHERE i.tenant_id = ? AND i.status NOT IN ('COMPLETED', 'CANCELLED')
                ORDER BY
                    CASE i.priority WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END,
                    i.scheduled_date ASC NULLS LAST
                LIMIT ?
                """;
        return jdbc.query(sql, this::mapIntervention, tenantId.value().toString(), tenantId.value().toString(), limit);
    }

    @Override
    public List<PriorityIntervention> getInterventionsByAssignee(String userId, int limit) {
        String sql = """
                SELECT i.id, i.type, i.priority, i.status, i.building_id,
                       COALESCE(b.name, '') AS building_name, i.assigned_to,
                       i.scheduled_date, i.created_at,
                       (i.scheduled_date < CURRENT_DATE AND i.status NOT IN ('COMPLETED', 'CANCELLED')) AS overdue
                FROM interventions i
                LEFT JOIN buildings b ON b.id::text = i.building_id
                WHERE i.assigned_to = ? AND i.status NOT IN ('COMPLETED', 'CANCELLED')
                ORDER BY
                    CASE i.priority WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END,
                    i.scheduled_date ASC NULLS LAST
                LIMIT ?
                """;
        return jdbc.query(sql, this::mapIntervention, userId, limit);
    }

    @Override
    public DeviceHealthSummary getDeviceHealth(TenantId tenantId) {
        String sql = """
                SELECT
                    COUNT(*) AS total,
                    COUNT(*) FILTER (WHERE status = 'ACTIVE') AS active,
                    COUNT(*) FILTER (WHERE status = 'OFFLINE') AS offline,
                    COUNT(*) FILTER (WHERE status = 'PROVISIONED') AS provisioned,
                    COUNT(*) FILTER (WHERE status = 'REVOKED') AS revoked,
                    COALESCE(AVG(CASE WHEN status = 'ACTIVE' THEN 100.0 WHEN status = 'OFFLINE' THEN 0.0 ELSE 50.0 END), 0) AS avg_uptime,
                    COUNT(*) FILTER (WHERE risk_score > 70) AS high_risk,
                    COUNT(*) FILTER (WHERE last_seen_at < NOW() - INTERVAL '24 hours') AS silent_24h
                FROM devices
                WHERE tenant_id = ?
                """;
        return jdbc.queryForObject(sql, (rs, rowNum) -> new DeviceHealthSummary(
                rs.getInt("total"),
                rs.getInt("active"),
                rs.getInt("offline"),
                rs.getInt("provisioned"),
                rs.getInt("revoked"),
                rs.getDouble("avg_uptime"),
                rs.getInt("high_risk"),
                rs.getInt("silent_24h")
        ), tenantId.value().toString());
    }

    private PriorityIntervention mapIntervention(ResultSet rs, int rowNum) throws SQLException {
        LocalDate scheduledDate = rs.getDate("scheduled_date") != null
                ? rs.getDate("scheduled_date").toLocalDate() : null;
        return new PriorityIntervention(
                rs.getString("id"),
                rs.getString("type"),
                rs.getString("priority"),
                rs.getString("status"),
                rs.getString("building_id"),
                rs.getString("building_name"),
                rs.getString("assigned_to"),
                scheduledDate,
                rs.getTimestamp("created_at").toInstant(),
                rs.getBoolean("overdue"));
    }

    private Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
