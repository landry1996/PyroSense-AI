package com.pyrosense.identity.adapter.out.persistence;

import com.pyrosense.identity.application.port.out.AuditLogRepository;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import com.pyrosense.shared.security.AuditEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcAuditLogRepository implements AuditLogRepository {

    private final JdbcTemplate jdbc;

    public JdbcAuditLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(AuditEntry entry) {
        jdbc.update("""
            INSERT INTO audit_log (id, action, resource_type, resource_id, user_id, tenant_id,
                ip_address, user_agent, details, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                entry.id().toString(),
                entry.action(),
                entry.resourceType(),
                entry.resourceId(),
                entry.userId() != null ? entry.userId().value().toString() : null,
                entry.tenantId() != null ? entry.tenantId().value().toString() : null,
                entry.ipAddress(),
                entry.userAgent(),
                entry.details(),
                Timestamp.from(entry.timestamp())
        );
    }

    @Override
    public List<AuditEntry> findByTenant(TenantId tenantId, Instant from, Instant to) {
        return jdbc.query("""
            SELECT * FROM audit_log
            WHERE tenant_id = ? AND timestamp >= ? AND timestamp <= ?
            ORDER BY timestamp DESC
            """,
                auditEntryRowMapper(),
                tenantId.value().toString(),
                Timestamp.from(from),
                Timestamp.from(to)
        );
    }

    private RowMapper<AuditEntry> auditEntryRowMapper() {
        return (rs, rowNum) -> new AuditEntry(
                UUID.fromString(rs.getString("id")),
                rs.getString("action"),
                rs.getString("resource_type"),
                rs.getString("resource_id"),
                rs.getString("user_id") != null ? new UserId(UUID.fromString(rs.getString("user_id"))) : null,
                rs.getString("tenant_id") != null ? new TenantId(UUID.fromString(rs.getString("tenant_id"))) : null,
                rs.getString("ip_address"),
                rs.getString("user_agent"),
                rs.getString("details"),
                rs.getTimestamp("timestamp").toInstant()
        );
    }
}
