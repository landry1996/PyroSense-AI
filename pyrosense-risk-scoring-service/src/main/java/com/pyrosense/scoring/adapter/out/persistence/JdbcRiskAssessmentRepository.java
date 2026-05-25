package com.pyrosense.scoring.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.scoring.application.port.out.RiskAssessmentRepositoryPort;
import com.pyrosense.scoring.domain.model.*;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.DeviceId;
import com.pyrosense.shared.id.ElectricalPanelId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcRiskAssessmentRepository implements RiskAssessmentRepositoryPort {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcRiskAssessmentRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(RiskAssessment assessment) {
        String factorsJson = serializeFactors(assessment.factors());
        jdbcTemplate.update("""
                INSERT INTO risk_assessments (id, device_id, panel_id, circuit_id, score, level, trend,
                    factors, predicted_incident_days, recommendation, computed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                """,
                assessment.id(),
                assessment.deviceId().value(),
                assessment.panelId() != null ? assessment.panelId().value() : null,
                assessment.circuitId(),
                assessment.score().value(),
                assessment.level().name(),
                assessment.trend().name(),
                factorsJson,
                assessment.predictedIncidentWindow() != null ? assessment.predictedIncidentWindow().toDays() : null,
                assessment.recommendation(),
                Timestamp.from(assessment.computedAt()));
    }

    @Override
    public Optional<RiskAssessment> findLatestByDevice(DeviceId deviceId) {
        var results = jdbcTemplate.query("""
                SELECT * FROM risk_assessments WHERE device_id = ? ORDER BY computed_at DESC LIMIT 1
                """, this::mapRow, deviceId.value());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public List<RiskAssessment> findHistoryByPanel(ElectricalPanelId panelId, Instant from, Instant to) {
        return jdbcTemplate.query("""
                SELECT * FROM risk_assessments WHERE panel_id = ? AND computed_at BETWEEN ? AND ?
                ORDER BY computed_at DESC
                """, this::mapRow, panelId.value(), Timestamp.from(from), Timestamp.from(to));
    }

    @Override
    public List<RiskAssessment> findLatestByBuilding(BuildingId buildingId) {
        return jdbcTemplate.query("""
                SELECT DISTINCT ON (device_id) * FROM risk_assessments
                WHERE panel_id IN (SELECT panel_id FROM risk_assessments WHERE panel_id IS NOT NULL)
                ORDER BY device_id, computed_at DESC
                LIMIT 100
                """, this::mapRow);
    }

    @Override
    public List<Integer> findRecentScores(DeviceId deviceId, int limit) {
        return jdbcTemplate.queryForList("""
                SELECT score FROM risk_assessments WHERE device_id = ?
                ORDER BY computed_at DESC LIMIT ?
                """, Integer.class, deviceId.value(), limit);
    }

    private RiskAssessment mapRow(ResultSet rs, int rowNum) throws SQLException {
        UUID panelUuid = rs.getObject("panel_id", UUID.class);
        UUID circuitUuid = rs.getObject("circuit_id", UUID.class);
        Long predictedDays = rs.getObject("predicted_incident_days", Long.class);

        return new RiskAssessment(
                UUID.fromString(rs.getString("id")),
                new DeviceId(UUID.fromString(rs.getString("device_id"))),
                panelUuid != null ? new ElectricalPanelId(panelUuid) : null,
                circuitUuid,
                new RiskScore(rs.getInt("score")),
                RiskTrend.valueOf(rs.getString("trend")),
                deserializeFactors(rs.getString("factors")),
                predictedDays != null ? Duration.ofDays(predictedDays) : null,
                rs.getString("recommendation"),
                rs.getTimestamp("computed_at").toInstant()
        );
    }

    private String serializeFactors(List<RiskFactor> factors) {
        try {
            return objectMapper.writeValueAsString(factors);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize factors", e);
        }
    }

    private List<RiskFactor> deserializeFactors(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
