package com.pyrosense.reporting.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.reporting.application.port.out.ReportRepositoryPort;
import com.pyrosense.reporting.domain.model.*;
import com.pyrosense.shared.id.BuildingId;
import com.pyrosense.shared.id.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcReportRepository implements ReportRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(JdbcReportRepository.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final ReportRowMapper rowMapper = new ReportRowMapper();

    public JdbcReportRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public Report save(Report report) {
        String metadataJson = serializeMetadata(report.getMetadata());

        int updated = jdbc.update("""
                UPDATE reports SET status = ?, metadata = ?::jsonb, signature_hash = ?,
                    signature_algorithm = ?, content = ?, file_name = ?, generated_at = ?
                WHERE id = ?
                """,
                report.getStatus().name(),
                metadataJson,
                report.getSignature() != null ? report.getSignature().hash() : null,
                report.getSignature() != null ? report.getSignature().algorithm() : null,
                report.getContent(),
                report.getFileName(),
                report.getGeneratedAt() != null ? Timestamp.from(report.getGeneratedAt()) : null,
                report.getId());

        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO reports (id, report_number, tenant_id, building_id, type, period_start,
                        period_end, status, metadata, signature_hash, signature_algorithm, content,
                        file_name, created_at, generated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?)
                    """,
                    report.getId(),
                    report.getReportNumber(),
                    report.getTenantId().value(),
                    report.getBuildingId().value(),
                    report.getType().name(),
                    Timestamp.from(report.getPeriodStart()),
                    Timestamp.from(report.getPeriodEnd()),
                    report.getStatus().name(),
                    metadataJson,
                    report.getSignature() != null ? report.getSignature().hash() : null,
                    report.getSignature() != null ? report.getSignature().algorithm() : null,
                    report.getContent(),
                    report.getFileName(),
                    Timestamp.from(report.getCreatedAt()),
                    report.getGeneratedAt() != null ? Timestamp.from(report.getGeneratedAt()) : null);
        }
        return report;
    }

    @Override
    public Optional<Report> findById(UUID id) {
        List<Report> results = jdbc.query("SELECT * FROM reports WHERE id = ?", rowMapper, id);
        return results.stream().findFirst();
    }

    @Override
    public List<Report> findByTenantId(TenantId tenantId) {
        return jdbc.query("SELECT * FROM reports WHERE tenant_id = ? ORDER BY created_at DESC",
                rowMapper, tenantId.value());
    }

    @Override
    public List<Report> findByTenantIdAndType(TenantId tenantId, ReportType type) {
        return jdbc.query("SELECT * FROM reports WHERE tenant_id = ? AND type = ? ORDER BY created_at DESC",
                rowMapper, tenantId.value(), type.name());
    }

    @Override
    public List<Report> findByBuildingId(BuildingId buildingId) {
        return jdbc.query("SELECT * FROM reports WHERE building_id = ? ORDER BY created_at DESC",
                rowMapper, buildingId.value());
    }

    private String serializeMetadata(ReportMetadata metadata) {
        if (metadata == null) return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("Failed to serialize/deserialize report metadata: {}", e.getMessage());
            return null;
        }
    }

    private class ReportRowMapper implements RowMapper<Report> {
        @Override
        public Report mapRow(ResultSet rs, int rowNum) throws SQLException {
            UUID id = rs.getObject("id", UUID.class);
            TenantId tenantId = new TenantId(rs.getObject("tenant_id", UUID.class));
            BuildingId buildingId = new BuildingId(rs.getObject("building_id", UUID.class));
            ReportType type = ReportType.valueOf(rs.getString("type"));
            Instant periodStart = rs.getTimestamp("period_start").toInstant();
            Instant periodEnd = rs.getTimestamp("period_end").toInstant();

            Report report = new Report(id, tenantId, buildingId, type, periodStart, periodEnd);

            String status = rs.getString("status");
            if ("GENERATING".equals(status) || "GENERATED".equals(status) || "FAILED".equals(status) || "EXPIRED".equals(status)) {
                report.startGeneration();
            }

            if ("GENERATED".equals(status)) {
                byte[] content = rs.getBytes("content");
                String metadataJson = rs.getString("metadata");
                ReportMetadata metadata = deserializeMetadata(metadataJson);
                if (content != null && metadata != null) {
                    report.markGenerated(content, metadata);
                }
            }

            if ("FAILED".equals(status)) {
                report.markFailed();
            }

            if ("EXPIRED".equals(status)) {
                report.markExpired();
            }

            return report;
        }

        private ReportMetadata deserializeMetadata(String json) {
            if (json == null) return null;
            try {
                return objectMapper.readValue(json, ReportMetadata.class);
            } catch (Exception e) {
                log.warn("Failed to serialize/deserialize report metadata: {}", e.getMessage());
                return null;
            }
        }
    }
}
