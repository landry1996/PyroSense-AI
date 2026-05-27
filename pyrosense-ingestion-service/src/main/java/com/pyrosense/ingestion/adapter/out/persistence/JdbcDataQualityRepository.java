package com.pyrosense.ingestion.adapter.out.persistence;

import com.pyrosense.ingestion.application.port.out.DataQualityRepositoryPort;
import com.pyrosense.ingestion.domain.model.quality.DataQualityAssessment;
import com.pyrosense.ingestion.domain.model.quality.DataQualityIssue;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcDataQualityRepository implements DataQualityRepositoryPort {

    private final JdbcTemplate jdbc;

    public JdbcDataQualityRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void saveAssessment(DataQualityAssessment assessment) {
        jdbc.update("""
                INSERT INTO data_quality_assessments
                (id, device_id, tenant_id, assessed_at, period_start, period_end,
                 overall_score, grade, trustworthy, signal_quality_avg, clock_drift_seconds,
                 offline_count, firmware_version, firmware_obsolete, issue_count, completeness_percent)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                assessment.getId(), assessment.getDeviceId(), assessment.getTenantId(),
                java.sql.Timestamp.from(assessment.getAssessedAt()),
                java.sql.Timestamp.from(assessment.getPeriodStart()),
                java.sql.Timestamp.from(assessment.getPeriodEnd()),
                assessment.getOverallScore(), assessment.getGrade().name(),
                assessment.isTrustworthy(), assessment.getAverageSignalQuality(),
                assessment.getClockDriftSeconds(), assessment.getOfflineCount(),
                assessment.getFirmwareVersion(), assessment.isFirmwareObsolete(),
                assessment.getIssues().size(),
                assessment.getMissingDataRatio().completenessPercent());
    }

    @Override
    public Optional<DataQualityAssessment> findLatestByDevice(String deviceId) {
        var results = jdbc.query("""
                SELECT * FROM data_quality_assessments WHERE device_id = ?
                ORDER BY assessed_at DESC LIMIT 1
                """, this::mapAssessmentRow, deviceId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<DataQualityAssessment> findLatestByDeviceAndTenant(String deviceId, String tenantId) {
        var results = jdbc.query("""
                SELECT * FROM data_quality_assessments WHERE device_id = ? AND tenant_id = ?
                ORDER BY assessed_at DESC LIMIT 1
                """, this::mapAssessmentRow, deviceId, tenantId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<DataQualityAssessment> findHistoryByDevice(String deviceId, Instant from, Instant to) {
        return jdbc.query("""
                SELECT * FROM data_quality_assessments
                WHERE device_id = ? AND assessed_at >= ? AND assessed_at <= ?
                ORDER BY assessed_at DESC
                """, this::mapAssessmentRow, deviceId,
                java.sql.Timestamp.from(from), java.sql.Timestamp.from(to));
    }

    @Override
    public List<DataQualityAssessment> findHistoryByDeviceAndTenant(String deviceId, String tenantId, Instant from, Instant to) {
        return jdbc.query("""
                SELECT * FROM data_quality_assessments
                WHERE device_id = ? AND tenant_id = ? AND assessed_at >= ? AND assessed_at <= ?
                ORDER BY assessed_at DESC
                """, this::mapAssessmentRow, deviceId, tenantId,
                java.sql.Timestamp.from(from), java.sql.Timestamp.from(to));
    }

    @Override
    public void saveIssue(DataQualityIssue issue) {
        jdbc.update("""
                INSERT INTO data_quality_issues
                (id, device_id, tenant_id, type, severity, details, detected_at, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                issue.getId(), issue.getDeviceId(), issue.getTenantId(),
                issue.getType().name(), issue.getSeverity().name(),
                issue.getDetails(), java.sql.Timestamp.from(issue.getDetectedAt()),
                issue.getStatus().name());
    }

    @Override
    public void saveIssues(List<DataQualityIssue> issues) {
        for (var issue : issues) {
            saveIssue(issue);
        }
    }

    @Override
    public Optional<DataQualityIssue> findIssueById(UUID id) {
        var results = jdbc.query(
                "SELECT * FROM data_quality_issues WHERE id = ?",
                this::mapIssueRow, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<DataQualityIssue> findIssues(String tenantId, String deviceId,
                                              DataQualityIssue.IssueStatus status,
                                              DataQualityIssue.IssueType type,
                                              int offset, int limit) {
        var sql = new StringBuilder("SELECT * FROM data_quality_issues WHERE tenant_id = ?");
        var params = new java.util.ArrayList<Object>();
        params.add(tenantId);

        if (deviceId != null) { sql.append(" AND device_id = ?"); params.add(deviceId); }
        if (status != null) { sql.append(" AND status = ?"); params.add(status.name()); }
        if (type != null) { sql.append(" AND type = ?"); params.add(type.name()); }

        sql.append(" ORDER BY detected_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbc.query(sql.toString(), this::mapIssueRow, params.toArray());
    }

    @Override
    public int countIssues(String tenantId, String deviceId,
                           DataQualityIssue.IssueStatus status,
                           DataQualityIssue.IssueType type) {
        var sql = new StringBuilder("SELECT COUNT(*) FROM data_quality_issues WHERE tenant_id = ?");
        var params = new java.util.ArrayList<Object>();
        params.add(tenantId);

        if (deviceId != null) { sql.append(" AND device_id = ?"); params.add(deviceId); }
        if (status != null) { sql.append(" AND status = ?"); params.add(status.name()); }
        if (type != null) { sql.append(" AND type = ?"); params.add(type.name()); }

        return jdbc.queryForObject(sql.toString(), Integer.class, params.toArray());
    }

    @Override
    public void updateIssue(DataQualityIssue issue) {
        jdbc.update("""
                UPDATE data_quality_issues
                SET status = ?, reviewed_by = ?, reviewed_at = ?, review_comment = ?
                WHERE id = ?
                """,
                issue.getStatus().name(), issue.getReviewedBy(),
                issue.getReviewedAt() != null ? java.sql.Timestamp.from(issue.getReviewedAt()) : null,
                issue.getReviewComment(), issue.getId());
    }

    private DataQualityAssessment mapAssessmentRow(ResultSet rs, int rowNum) throws SQLException {
        return DataQualityAssessment.builder()
                .deviceId(rs.getString("device_id"))
                .tenantId(rs.getString("tenant_id"))
                .periodStart(rs.getTimestamp("period_start").toInstant())
                .periodEnd(rs.getTimestamp("period_end").toInstant())
                .averageSignalQuality(rs.getDouble("signal_quality_avg"))
                .clockDriftSeconds(rs.getDouble("clock_drift_seconds"))
                .offlineCount(rs.getInt("offline_count"))
                .firmwareVersion(rs.getString("firmware_version"))
                .firmwareObsolete(rs.getBoolean("firmware_obsolete"))
                .build();
    }

    private DataQualityIssue mapIssueRow(ResultSet rs, int rowNum) throws SQLException {
        var issue = new DataQualityIssue(
                UUID.fromString(rs.getString("id")),
                rs.getString("device_id"),
                rs.getString("tenant_id"),
                DataQualityIssue.IssueType.valueOf(rs.getString("type")),
                DataQualityIssue.IssueSeverity.valueOf(rs.getString("severity")),
                rs.getString("details"),
                rs.getTimestamp("detected_at").toInstant());

        String status = rs.getString("status");
        String reviewedBy = rs.getString("reviewed_by");
        String reviewComment = rs.getString("review_comment");
        if ("REVIEWED".equals(status) && reviewedBy != null) {
            issue.markReviewed(reviewedBy, reviewComment);
        } else if ("DISMISSED".equals(status) && reviewedBy != null) {
            issue.dismiss(reviewedBy, reviewComment);
        }
        return issue;
    }
}
