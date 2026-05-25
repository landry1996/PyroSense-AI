package com.pyrosense.ingestion.adapter.out.persistence;

import com.pyrosense.ingestion.application.port.out.RejectionRepositoryPort;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.TenantId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcRejectionRepository implements RejectionRepositoryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcRejectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveRejection(TenantId tenantId, DeviceId deviceId, String reason,
                              String source, String payloadHash, List<String> violations) {
        jdbcTemplate.update("""
                INSERT INTO ingestion_rejections (tenant_id, device_id, reason, source, payload_hash, violations)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                tenantId != null ? tenantId.value() : null,
                deviceId != null ? deviceId.value() : null,
                reason, source, payloadHash,
                violations != null ? String.join("; ", violations) : null);
    }
}
