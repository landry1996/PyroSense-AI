package com.pyrosense.identity.adapter.out.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.identity.application.port.out.TenantSettingsRepository;
import com.pyrosense.identity.domain.model.TenantSettings;
import com.pyrosense.shared.id.TenantId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcTenantSettingsRepository implements TenantSettingsRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcTenantSettingsRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<TenantSettings> findByTenantId(TenantId tenantId) {
        List<TenantSettings> results = jdbc.query(
                "SELECT tenant_id, settings, updated_at FROM tenant_settings WHERE tenant_id = ?",
                (rs, rowNum) -> new TenantSettings(
                        new TenantId(UUID.fromString(rs.getString("tenant_id"))),
                        parseJson(rs.getString("settings")),
                        toInstant(rs.getTimestamp("updated_at"))
                ),
                tenantId.value().toString()
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public TenantSettings save(TenantSettings settings) {
        int updated = jdbc.update("""
            UPDATE tenant_settings SET settings = ?::jsonb, updated_at = NOW()
            WHERE tenant_id = ?
            """,
                toJson(settings.getSettings()),
                settings.getTenantId().value().toString()
        );
        if (updated == 0) {
            jdbc.update("""
                INSERT INTO tenant_settings (tenant_id, settings, updated_at)
                VALUES (?, ?::jsonb, NOW())
                """,
                    settings.getTenantId().value().toString(),
                    toJson(settings.getSettings())
            );
        }
        return settings;
    }

    private Map<String, Object> parseJson(String json) {
        try {
            if (json == null || json.isBlank()) return Map.of();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
