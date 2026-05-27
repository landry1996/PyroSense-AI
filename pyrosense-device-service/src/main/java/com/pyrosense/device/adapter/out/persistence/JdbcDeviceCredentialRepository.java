package com.pyrosense.device.adapter.out.persistence;

import com.pyrosense.device.application.port.out.DeviceCredentialRepositoryPort;
import com.pyrosense.device.domain.model.CredentialStatus;
import com.pyrosense.device.domain.model.DeviceCredential;
import com.pyrosense.shared.id.DeviceId;
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
public class JdbcDeviceCredentialRepository implements DeviceCredentialRepositoryPort {

    private final JdbcTemplate jdbc;

    public JdbcDeviceCredentialRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(DeviceCredential credential) {
        jdbc.update("""
                INSERT INTO device_credentials (id, device_id, hmac_key_hash, status, issued_at, revoked_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, revoked_at = EXCLUDED.revoked_at
                """,
                credential.getId(),
                credential.getDeviceId().value(),
                credential.getHmacKeyHash(),
                credential.getStatus().name(),
                Timestamp.from(credential.getIssuedAt()),
                credential.getRevokedAt() != null ? Timestamp.from(credential.getRevokedAt()) : null,
                credential.getVersion());
    }

    @Override
    public Optional<DeviceCredential> findById(UUID id) {
        var results = jdbc.query("SELECT * FROM device_credentials WHERE id = ?", this::mapRow, id);
        return results.stream().findFirst();
    }

    @Override
    public Optional<DeviceCredential> findActiveByDeviceId(DeviceId deviceId) {
        var results = jdbc.query(
                "SELECT * FROM device_credentials WHERE device_id = ? AND status = 'ACTIVE' ORDER BY version DESC LIMIT 1",
                this::mapRow, deviceId.value());
        return results.stream().findFirst();
    }

    @Override
    public List<DeviceCredential> findAllByDeviceId(DeviceId deviceId) {
        return jdbc.query("SELECT * FROM device_credentials WHERE device_id = ? ORDER BY version DESC",
                this::mapRow, deviceId.value());
    }

    @Override
    public int countActiveByDeviceId(DeviceId deviceId) {
        var count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM device_credentials WHERE device_id = ? AND status = 'ACTIVE'",
                Integer.class, deviceId.value());
        return count != null ? count : 0;
    }

    @Override
    public int nextVersionForDevice(DeviceId deviceId) {
        var maxVersion = jdbc.queryForObject(
                "SELECT COALESCE(MAX(version), 0) FROM device_credentials WHERE device_id = ?",
                Integer.class, deviceId.value());
        return (maxVersion != null ? maxVersion : 0) + 1;
    }

    private DeviceCredential mapRow(ResultSet rs, int rowNum) throws SQLException {
        Instant revokedAt = rs.getTimestamp("revoked_at") != null
                ? rs.getTimestamp("revoked_at").toInstant() : null;
        return DeviceCredential.reconstitute(
                UUID.fromString(rs.getString("id")),
                DeviceId.from(rs.getString("device_id")),
                rs.getString("hmac_key_hash"),
                CredentialStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("issued_at").toInstant(),
                revokedAt,
                rs.getInt("version"));
    }
}
