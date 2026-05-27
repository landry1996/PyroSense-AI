package com.pyrosense.device.adapter.out.persistence;

import com.pyrosense.device.application.port.out.ClaimTokenRepositoryPort;
import com.pyrosense.device.domain.model.ClaimToken;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcClaimTokenRepository implements ClaimTokenRepositoryPort {

    private final JdbcTemplate jdbc;

    public JdbcClaimTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(ClaimToken token) {
        jdbc.update("""
                INSERT INTO claim_tokens (id, device_id, tenant_id, token_hash, created_at, expires_at, created_by, consumed, consumed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET consumed = EXCLUDED.consumed, consumed_at = EXCLUDED.consumed_at
                """,
                token.getId(),
                token.getDeviceId().value(),
                token.getTenantId().value(),
                token.getTokenHash(),
                Timestamp.from(token.getCreatedAt()),
                Timestamp.from(token.getExpiresAt()),
                token.getCreatedBy(),
                token.isConsumed(),
                token.getConsumedAt() != null ? Timestamp.from(token.getConsumedAt()) : null);
    }

    @Override
    public Optional<ClaimToken> findById(UUID id) {
        var results = jdbc.query(
                "SELECT * FROM claim_tokens WHERE id = ?",
                this::mapRow, id);
        return results.stream().findFirst();
    }

    @Override
    public Optional<ClaimToken> findActiveByDeviceId(DeviceId deviceId) {
        var results = jdbc.query(
                "SELECT * FROM claim_tokens WHERE device_id = ? AND consumed = FALSE AND expires_at > NOW() ORDER BY created_at DESC LIMIT 1",
                this::mapRow, deviceId.value());
        return results.stream().findFirst();
    }

    @Override
    public void invalidateAllForDevice(DeviceId deviceId) {
        jdbc.update(
                "UPDATE claim_tokens SET consumed = TRUE, consumed_at = NOW() WHERE device_id = ? AND consumed = FALSE",
                deviceId.value());
    }

    @Override
    public int countRecentByIp(String sourceIp, int windowMinutes) {
        var count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM provisioning_sessions WHERE source_ip = ? AND created_at > NOW() - INTERVAL '" + windowMinutes + " minutes'",
                Integer.class, sourceIp);
        return count != null ? count : 0;
    }

    private ClaimToken mapRow(ResultSet rs, int rowNum) throws SQLException {
        Instant consumedAt = rs.getTimestamp("consumed_at") != null
                ? rs.getTimestamp("consumed_at").toInstant() : null;
        return ClaimToken.reconstitute(
                UUID.fromString(rs.getString("id")),
                DeviceId.from(rs.getString("device_id")),
                new TenantId(UUID.fromString(rs.getString("tenant_id"))),
                rs.getString("token_hash"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getString("created_by"),
                rs.getBoolean("consumed"),
                consumedAt);
    }
}
