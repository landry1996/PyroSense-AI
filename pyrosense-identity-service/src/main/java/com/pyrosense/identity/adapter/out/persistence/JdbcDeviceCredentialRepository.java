package com.pyrosense.identity.adapter.out.persistence;

import com.pyrosense.identity.application.port.out.DeviceCredentialRepository;
import com.pyrosense.identity.domain.model.DeviceCredential;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcDeviceCredentialRepository implements DeviceCredentialRepository {

    private final JdbcTemplate jdbc;

    public JdbcDeviceCredentialRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public DeviceCredential save(DeviceCredential credential) {
        int updated = jdbc.update("""
            UPDATE device_credentials SET token_hash = ?, expires_at = ?, active = ?,
                last_authenticated_at = ?, authentication_count = ?
            WHERE id = ?
            """,
                credential.getTokenHash(), toTimestamp(credential.getExpiresAt()),
                credential.isActive(), toTimestamp(credential.getLastAuthenticatedAt()),
                credential.getAuthenticationCount(),
                credential.getId().toString()
        );

        if (updated == 0) {
            jdbc.update("""
                INSERT INTO device_credentials (id, device_id, tenant_id, token_hash, expires_at,
                    active, created_at, last_authenticated_at, authentication_count)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                    credential.getId().toString(),
                    credential.getDeviceId().value().toString(),
                    credential.getTenantId().value().toString(),
                    credential.getTokenHash(), toTimestamp(credential.getExpiresAt()),
                    credential.isActive(), toTimestamp(credential.getCreatedAt()),
                    toTimestamp(credential.getLastAuthenticatedAt()),
                    credential.getAuthenticationCount()
            );
        }
        return credential;
    }

    @Override
    public Optional<DeviceCredential> findActiveByDeviceId(DeviceId deviceId) {
        List<DeviceCredential> credentials = jdbc.query(
                "SELECT * FROM device_credentials WHERE device_id = ? AND active = TRUE",
                credentialRowMapper(), deviceId.value().toString()
        );
        return credentials.isEmpty() ? Optional.empty() : Optional.of(credentials.get(0));
    }

    @Override
    public void deleteByDeviceId(DeviceId deviceId) {
        jdbc.update("DELETE FROM device_credentials WHERE device_id = ?",
                deviceId.value().toString());
    }

    private RowMapper<DeviceCredential> credentialRowMapper() {
        return (rs, rowNum) -> DeviceCredential.reconstitute(
                UUID.fromString(rs.getString("id")),
                new DeviceId(UUID.fromString(rs.getString("device_id"))),
                new TenantId(UUID.fromString(rs.getString("tenant_id"))),
                rs.getString("token_hash"),
                toInstant(rs.getTimestamp("expires_at")),
                rs.getBoolean("active"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("last_authenticated_at")),
                rs.getInt("authentication_count")
        );
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
