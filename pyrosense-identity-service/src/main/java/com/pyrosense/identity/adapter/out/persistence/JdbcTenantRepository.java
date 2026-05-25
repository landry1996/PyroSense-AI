package com.pyrosense.identity.adapter.out.persistence;

import com.pyrosense.identity.application.port.out.TenantRepository;
import com.pyrosense.identity.domain.model.Tenant;
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
public class JdbcTenantRepository implements TenantRepository {

    private final JdbcTemplate jdbc;

    public JdbcTenantRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Tenant save(Tenant tenant) {
        int updated = jdbc.update("""
            UPDATE tenants SET name = ?, slug = ?, active = ?, updated_at = NOW()
            WHERE id = ?
            """,
                tenant.getName(), tenant.getSlug(), tenant.isActive(),
                tenant.getId().value().toString()
        );

        if (updated == 0) {
            jdbc.update("""
                INSERT INTO tenants (id, name, slug, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, NOW())
                """,
                    tenant.getId().value().toString(), tenant.getName(),
                    tenant.getSlug(), tenant.isActive(),
                    toTimestamp(tenant.getCreatedAt())
            );
        }
        return tenant;
    }

    @Override
    public Optional<Tenant> findById(TenantId id) {
        List<Tenant> tenants = jdbc.query(
                "SELECT * FROM tenants WHERE id = ?",
                tenantRowMapper(), id.value().toString()
        );
        return tenants.isEmpty() ? Optional.empty() : Optional.of(tenants.get(0));
    }

    @Override
    public Optional<Tenant> findBySlug(String slug) {
        List<Tenant> tenants = jdbc.query(
                "SELECT * FROM tenants WHERE slug = ?",
                tenantRowMapper(), slug
        );
        return tenants.isEmpty() ? Optional.empty() : Optional.of(tenants.get(0));
    }

    @Override
    public boolean existsBySlug(String slug) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tenants WHERE slug = ?", Integer.class, slug
        );
        return count != null && count > 0;
    }

    private RowMapper<Tenant> tenantRowMapper() {
        return (rs, rowNum) -> Tenant.reconstitute(
                new TenantId(UUID.fromString(rs.getString("id"))),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getBoolean("active"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at"))
        );
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
