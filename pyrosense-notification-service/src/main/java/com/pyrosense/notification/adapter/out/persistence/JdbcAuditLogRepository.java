package com.pyrosense.notification.adapter.out.persistence;

import com.pyrosense.notification.application.port.out.AuditLogRepository;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.security.AuditEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class JdbcAuditLogRepository implements AuditLogRepository {

    private final JdbcTemplate jdbc;

    public JdbcAuditLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(AuditEntry entry) {
        jdbc.update("""
            INSERT INTO audit_log (id, action, resource_type, resource_id, user_id, actor_role,
                tenant_id, ip_address, user_agent, correlation_id, details, metadata, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                entry.id().toString(),
                entry.action(),
                entry.resourceType(),
                entry.resourceId(),
                entry.userId() != null ? entry.userId().value().toString() : null,
                entry.actorRole(),
                entry.tenantId() != null ? entry.tenantId().value().toString() : null,
                sanitizeIp(entry.ipAddress()),
                truncate(entry.userAgent(), 500),
                entry.correlationId(),
                sanitizeDetails(entry.details()),
                metadataToString(entry.metadata()),
                Timestamp.from(entry.timestamp())
        );
    }

    @Override
    public Optional<AuditEntry> findById(UUID id) {
        List<AuditEntry> results = jdbc.query(
                "SELECT * FROM audit_log WHERE id = ?",
                rowMapper(), id.toString());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<AuditEntry> findByTenant(TenantId tenantId, Instant from, Instant to, int offset, int limit) {
        return jdbc.query("""
            SELECT * FROM audit_log
            WHERE tenant_id = ? AND timestamp >= ? AND timestamp <= ?
            ORDER BY timestamp DESC
            LIMIT ? OFFSET ?
            """,
                rowMapper(),
                tenantId.value().toString(),
                Timestamp.from(from), Timestamp.from(to),
                Math.min(limit, 200), offset);
    }

    @Override
    public List<AuditEntry> findByTenantAndAction(TenantId tenantId, String action, Instant from, Instant to, int offset, int limit) {
        return jdbc.query("""
            SELECT * FROM audit_log
            WHERE tenant_id = ? AND action = ? AND timestamp >= ? AND timestamp <= ?
            ORDER BY timestamp DESC
            LIMIT ? OFFSET ?
            """,
                rowMapper(),
                tenantId.value().toString(), action,
                Timestamp.from(from), Timestamp.from(to),
                Math.min(limit, 200), offset);
    }

    @Override
    public List<AuditEntry> findByTenantAndResource(TenantId tenantId, String resourceType, Instant from, Instant to, int offset, int limit) {
        return jdbc.query("""
            SELECT * FROM audit_log
            WHERE tenant_id = ? AND resource_type = ? AND timestamp >= ? AND timestamp <= ?
            ORDER BY timestamp DESC
            LIMIT ? OFFSET ?
            """,
                rowMapper(),
                tenantId.value().toString(), resourceType,
                Timestamp.from(from), Timestamp.from(to),
                Math.min(limit, 200), offset);
    }

    @Override
    public long countByTenant(TenantId tenantId, Instant from, Instant to) {
        Long count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM audit_log
            WHERE tenant_id = ? AND timestamp >= ? AND timestamp <= ?
            """,
                Long.class,
                tenantId.value().toString(),
                Timestamp.from(from), Timestamp.from(to));
        return count != null ? count : 0;
    }

    private RowMapper<AuditEntry> rowMapper() {
        return (rs, rowNum) -> new AuditEntry(
                UUID.fromString(rs.getString("id")),
                rs.getString("action"),
                rs.getString("resource_type"),
                rs.getString("resource_id"),
                rs.getString("user_id") != null ? new UserId(UUID.fromString(rs.getString("user_id"))) : null,
                rs.getString("actor_role"),
                rs.getString("tenant_id") != null ? new TenantId(UUID.fromString(rs.getString("tenant_id"))) : null,
                sanitizeIp(rs.getString("ip_address")),
                rs.getString("user_agent"),
                rs.getString("correlation_id"),
                rs.getString("details"),
                parseMetadata(rs.getString("metadata")),
                rs.getTimestamp("timestamp").toInstant()
        );
    }

    private String sanitizeIp(String ip) {
        if (ip == null) return null;
        return ip.length() <= 45 ? ip : ip.substring(0, 45);
    }

    private String sanitizeDetails(String details) {
        if (details == null) return null;
        return details
                .replaceAll("(?i)(phone|tel)[=:]\\s*\\+?[0-9]{6,}", "$1=***MASKED***")
                .replaceAll("(?i)(email)[=:]\\s*[^\\s,]+@[^\\s,]+", "$1=***MASKED***")
                .replaceAll("(?i)(password|secret|token)[=:]\\s*\\S+", "$1=***REDACTED***");
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String metadataToString(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : metadata.entrySet()) {
            if (!sb.isEmpty()) sb.append(";");
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }

    private Map<String, String> parseMetadata(String raw) {
        if (raw == null || raw.isBlank()) return Map.of();
        Map<String, String> map = new LinkedHashMap<>();
        for (String pair : raw.split(";")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                map.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        return map;
    }
}
