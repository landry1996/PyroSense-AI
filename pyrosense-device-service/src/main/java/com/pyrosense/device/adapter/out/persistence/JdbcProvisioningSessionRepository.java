package com.pyrosense.device.adapter.out.persistence;

import com.pyrosense.device.application.port.out.ProvisioningSessionRepositoryPort;
import com.pyrosense.device.domain.model.DeviceProvisioningSession;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcProvisioningSessionRepository implements ProvisioningSessionRepositoryPort {

    private final JdbcTemplate jdbc;

    public JdbcProvisioningSessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(DeviceProvisioningSession session) {
        jdbc.update("""
                INSERT INTO provisioning_sessions (id, device_id, tenant_id, device_serial, device_model, firmware_version, source_ip, status, failure_reason, created_at, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, failure_reason = EXCLUDED.failure_reason, completed_at = EXCLUDED.completed_at
                """,
                session.getId(),
                session.getDeviceId().value(),
                session.getTenantId().value(),
                session.getDeviceSerial(),
                session.getDeviceModel(),
                session.getFirmwareVersion(),
                session.getSourceIp(),
                session.getStatus().name(),
                session.getFailureReason(),
                Timestamp.from(session.getCreatedAt()),
                session.getCompletedAt() != null ? Timestamp.from(session.getCompletedAt()) : null);
    }

    @Override
    public Optional<DeviceProvisioningSession> findById(UUID id) {
        var results = jdbc.query("SELECT * FROM provisioning_sessions WHERE id = ?", this::mapRow, id);
        return results.stream().findFirst();
    }

    @Override
    public List<DeviceProvisioningSession> findByDeviceId(DeviceId deviceId) {
        return jdbc.query("SELECT * FROM provisioning_sessions WHERE device_id = ? ORDER BY created_at DESC",
                this::mapRow, deviceId.value());
    }

    @Override
    public int countRecentFailedByIp(String sourceIp, int windowMinutes) {
        var count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM provisioning_sessions WHERE source_ip = ? AND status = 'FAILED' AND created_at > NOW() - INTERVAL '" + windowMinutes + " minutes'",
                Integer.class, sourceIp);
        return count != null ? count : 0;
    }

    private DeviceProvisioningSession mapRow(ResultSet rs, int rowNum) throws SQLException {
        Instant completedAt = rs.getTimestamp("completed_at") != null
                ? rs.getTimestamp("completed_at").toInstant() : null;
        return DeviceProvisioningSession.reconstitute(
                UUID.fromString(rs.getString("id")),
                DeviceId.from(rs.getString("device_id")),
                new TenantId(UUID.fromString(rs.getString("tenant_id"))),
                rs.getString("device_serial"),
                rs.getString("device_model"),
                rs.getString("firmware_version"),
                rs.getString("source_ip"),
                DeviceProvisioningSession.SessionStatus.valueOf(rs.getString("status")),
                rs.getString("failure_reason"),
                rs.getTimestamp("created_at").toInstant(),
                completedAt);
    }
}
