package com.pyrosense.identity.adapter.out.persistence;

import com.pyrosense.identity.application.port.out.EmergencyContactRepository;
import com.pyrosense.identity.domain.model.EmergencyContact;
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
public class JdbcEmergencyContactRepository implements EmergencyContactRepository {

    private final JdbcTemplate jdbc;

    public JdbcEmergencyContactRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<EmergencyContact> findByTenantId(TenantId tenantId) {
        return jdbc.query(
                "SELECT * FROM emergency_contacts WHERE tenant_id = ? ORDER BY priority ASC",
                contactRowMapper(), tenantId.value().toString()
        );
    }

    @Override
    public Optional<EmergencyContact> findById(UUID id) {
        List<EmergencyContact> results = jdbc.query(
                "SELECT * FROM emergency_contacts WHERE id = ?",
                contactRowMapper(), id.toString()
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public EmergencyContact save(EmergencyContact contact) {
        int updated = jdbc.update("""
            UPDATE emergency_contacts SET name = ?, phone = ?, email = ?, role = ?, priority = ?, updated_at = NOW()
            WHERE id = ?
            """,
                contact.getName(), contact.getPhone(), contact.getEmail(),
                contact.getRole(), contact.getPriority(), contact.getId().toString()
        );
        if (updated == 0) {
            jdbc.update("""
                INSERT INTO emergency_contacts (id, tenant_id, name, phone, email, role, priority, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """,
                    contact.getId().toString(), contact.getTenantId().value().toString(),
                    contact.getName(), contact.getPhone(), contact.getEmail(),
                    contact.getRole(), contact.getPriority(),
                    Timestamp.from(contact.getCreatedAt())
            );
        }
        return contact;
    }

    @Override
    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM emergency_contacts WHERE id = ?", id.toString());
    }

    private RowMapper<EmergencyContact> contactRowMapper() {
        return (rs, rowNum) -> EmergencyContact.reconstitute(
                UUID.fromString(rs.getString("id")),
                new TenantId(UUID.fromString(rs.getString("tenant_id"))),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("role"),
                rs.getInt("priority"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at"))
        );
    }

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
