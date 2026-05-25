package com.pyrosense.analysis.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.analysis.application.port.out.BaselineProfileRepositoryPort;
import com.pyrosense.analysis.domain.model.BaselineProfile;
import com.pyrosense.analysis.domain.model.SignalFeature;
import com.pyrosense.analysis.domain.model.StatisticalRange;
import com.pyrosense.shared.id.DeviceId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JpaBaselineProfileRepository implements BaselineProfileRepositoryPort {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JpaBaselineProfileRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(BaselineProfile profile) {
        String statsJson = serializeStats(profile.featureStats());

        jdbcTemplate.update("""
                INSERT INTO baseline_profiles (id, device_id, feature_stats, sample_count, created_at, updated_at, minimum_samples)
                VALUES (?, ?, ?::jsonb, ?, ?, ?, ?)
                ON CONFLICT (device_id) DO UPDATE SET
                    feature_stats = EXCLUDED.feature_stats,
                    sample_count = EXCLUDED.sample_count,
                    updated_at = EXCLUDED.updated_at
                """,
                profile.id(),
                profile.deviceId().value(),
                statsJson,
                profile.sampleCount(),
                Timestamp.from(profile.createdAt()),
                Timestamp.from(profile.updatedAt()),
                100);
    }

    @Override
    public Optional<BaselineProfile> findByDeviceId(DeviceId deviceId) {
        var results = jdbcTemplate.query("""
                SELECT id, device_id, feature_stats, sample_count, created_at, updated_at, minimum_samples
                FROM baseline_profiles WHERE device_id = ?
                """, this::mapRow, deviceId.value());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public void deleteByDeviceId(DeviceId deviceId) {
        jdbcTemplate.update("DELETE FROM baseline_profiles WHERE device_id = ?", deviceId.value());
    }

    private BaselineProfile mapRow(ResultSet rs, int rowNum) throws SQLException {
        Map<SignalFeature, StatisticalRange> stats = deserializeStats(rs.getString("feature_stats"));
        return new BaselineProfile(
                UUID.fromString(rs.getString("id")),
                new DeviceId(UUID.fromString(rs.getString("device_id"))),
                stats,
                rs.getInt("sample_count"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getInt("minimum_samples")
        );
    }

    private String serializeStats(Map<SignalFeature, StatisticalRange> stats) {
        try {
            Map<String, StatisticalRange> named = stats.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().metricName(), Map.Entry::getValue));
            return objectMapper.writeValueAsString(named);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize feature stats", e);
        }
    }

    private Map<SignalFeature, StatisticalRange> deserializeStats(String json) {
        try {
            Map<String, StatisticalRange> named = objectMapper.readValue(json,
                    new TypeReference<Map<String, StatisticalRange>>() {});
            return named.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> SignalFeature.fromMetricName(e.getKey()),
                            Map.Entry::getValue));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize feature stats", e);
        }
    }
}
