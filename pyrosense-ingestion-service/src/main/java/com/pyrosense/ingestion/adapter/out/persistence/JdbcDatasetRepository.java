package com.pyrosense.ingestion.adapter.out.persistence;

import com.pyrosense.ingestion.application.port.out.DatasetRepositoryPort;
import com.pyrosense.ingestion.domain.model.dataset.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcDatasetRepository implements DatasetRepositoryPort {

    private final JdbcTemplate jdbc;

    public JdbcDatasetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void saveCandidate(DatasetCandidate candidate) {
        jdbc.update("""
                INSERT INTO dataset_candidates
                (id, pseudonymized_device_id, pseudonymized_tenant_id, window_start, window_end,
                 status, quality_tier, data_quality_score, risk_score_at_time, anomaly_type,
                 source_alert_id, source_intervention_id, feature_summary, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
                """,
                candidate.getId(), candidate.getPseudonymizedDeviceId(),
                candidate.getPseudonymizedTenantId(),
                java.sql.Timestamp.from(candidate.getWindowStart()),
                java.sql.Timestamp.from(candidate.getWindowEnd()),
                candidate.getStatus().name(), candidate.getQualityTier().name(),
                candidate.getDataQualityScore(), candidate.getRiskScoreAtTime(),
                candidate.getAnomalyType(),
                candidate.getSourceAlertId(), candidate.getSourceInterventionId(),
                serializeFeatureSummary(candidate.getFeatureSummary()),
                java.sql.Timestamp.from(candidate.getCreatedAt()));
    }

    @Override
    public Optional<DatasetCandidate> findCandidateById(UUID id) {
        var results = jdbc.query(
                "SELECT * FROM dataset_candidates WHERE id = ?",
                this::mapCandidateRow, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<DatasetCandidate> findCandidates(String pseudonymizedTenantId,
                                                  DatasetCandidate.CandidateStatus status,
                                                  DatasetCandidate.DataQualityTier minQualityTier,
                                                  DataLabel.LabelValue labelFilter,
                                                  Instant from, Instant to,
                                                  int offset, int limit) {
        var sql = new StringBuilder("SELECT * FROM dataset_candidates WHERE pseudonymized_tenant_id = ?");
        var params = new java.util.ArrayList<Object>();
        params.add(pseudonymizedTenantId);

        appendFilters(sql, params, status, minQualityTier, from, to);

        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbc.query(sql.toString(), this::mapCandidateRow, params.toArray());
    }

    @Override
    public int countCandidates(String pseudonymizedTenantId,
                               DatasetCandidate.CandidateStatus status,
                               DatasetCandidate.DataQualityTier minQualityTier,
                               DataLabel.LabelValue labelFilter,
                               Instant from, Instant to) {
        var sql = new StringBuilder("SELECT COUNT(*) FROM dataset_candidates WHERE pseudonymized_tenant_id = ?");
        var params = new java.util.ArrayList<Object>();
        params.add(pseudonymizedTenantId);

        appendFilters(sql, params, status, minQualityTier, from, to);

        Integer count = jdbc.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    @Override
    public void updateCandidate(DatasetCandidate candidate) {
        jdbc.update("""
                UPDATE dataset_candidates SET status = ?, exported_at = ? WHERE id = ?
                """,
                candidate.getStatus().name(),
                candidate.getExportedAt() != null ? java.sql.Timestamp.from(candidate.getExportedAt()) : null,
                candidate.getId());
    }

    @Override
    public void saveFeedback(TechnicianFeedback feedback) {
        jdbc.update("""
                INSERT INTO technician_feedbacks
                (id, intervention_id, technician_id, pseudonymized_device_id,
                 defect_observed, confidence_level, visual_inspection,
                 measurement_method, measurement_result, defect_confirmed,
                 false_positive, additional_notes, submitted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                feedback.id(), feedback.interventionId(), feedback.technicianId(),
                feedback.pseudonymizedDeviceId(), feedback.defectObserved().name(),
                feedback.confidenceLevel(), feedback.visualInspection(),
                feedback.measurementMethod(), feedback.measurementResult(),
                feedback.defectConfirmed(), feedback.falsePositive(),
                feedback.additionalNotes(),
                java.sql.Timestamp.from(feedback.submittedAt()));
    }

    @Override
    public void saveFieldObservation(FieldObservation observation) {
        jdbc.update("""
                INSERT INTO field_observations
                (id, pseudonymized_device_id, pseudonymized_tenant_id, intervention_id,
                 observed_at, window_start, window_end, environment_type,
                 installation_type, circuit_type, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                observation.getId(), observation.getPseudonymizedDeviceId(),
                observation.getPseudonymizedTenantId(), observation.getInterventionId(),
                java.sql.Timestamp.from(observation.getObservedAt()),
                java.sql.Timestamp.from(observation.getWindowStart()),
                java.sql.Timestamp.from(observation.getWindowEnd()),
                observation.getEnvironmentType(), observation.getInstallationType(),
                observation.getCircuitType(), observation.getNotes());
    }

    @Override
    public void saveDefectConfirmation(DefectConfirmation confirmation) {
        jdbc.update("""
                INSERT INTO defect_confirmations
                (id, intervention_id, alert_id, pseudonymized_device_id,
                 confirmed_defect, defect_location, severity_observed,
                 correction_applied, risk_score_at_detection, risk_score_after_correction,
                 confirmed_at, confirmed_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                confirmation.id(), confirmation.interventionId(), confirmation.alertId(),
                confirmation.pseudonymizedDeviceId(), confirmation.confirmedDefect().name(),
                confirmation.defectLocation(), confirmation.severityObserved(),
                confirmation.correctionApplied(), confirmation.riskScoreAtDetection(),
                confirmation.riskScoreAfterCorrection(),
                java.sql.Timestamp.from(confirmation.confirmedAt()),
                confirmation.confirmedBy());
    }

    @Override
    public void saveFalsePositiveFeedback(FalsePositiveFeedback feedback) {
        jdbc.update("""
                INSERT INTO false_positive_feedbacks
                (id, alert_id, intervention_id, pseudonymized_device_id,
                 original_alert_type, risk_score_at_alert, reason,
                 suggested_threshold_adjustment, reported_at, reported_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                feedback.id(), feedback.alertId(), feedback.interventionId(),
                feedback.pseudonymizedDeviceId(), feedback.originalAlertType(),
                feedback.riskScoreAtAlert(), feedback.reason(),
                feedback.suggestedThresholdAdjustment(),
                java.sql.Timestamp.from(feedback.reportedAt()),
                feedback.reportedBy());
    }

    @Override
    public void saveExportJob(DatasetExportJob job) {
        jdbc.update("""
                INSERT INTO dataset_export_jobs
                (id, requested_by, pseudonymized_tenant_id, status, format,
                 window_start, window_end, label_filter, min_quality_tier,
                 include_unlabeled, requested_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                job.getId(), job.getRequestedBy(), job.getPseudonymizedTenantId(),
                job.getStatus().name(), job.getFormat().name(),
                java.sql.Timestamp.from(job.getWindowStart()),
                java.sql.Timestamp.from(job.getWindowEnd()),
                job.getLabelFilter() != null ? job.getLabelFilter().name() : null,
                job.getMinQualityTier().name(), job.isIncludeUnlabeled(),
                java.sql.Timestamp.from(job.getRequestedAt()));
    }

    @Override
    public Optional<DatasetExportJob> findExportJobById(UUID id) {
        var results = jdbc.query(
                "SELECT * FROM dataset_export_jobs WHERE id = ?",
                this::mapExportJobRow, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public void updateExportJob(DatasetExportJob job) {
        jdbc.update("""
                UPDATE dataset_export_jobs
                SET status = ?, completed_at = ?, candidate_count = ?,
                    file_size_bytes = ?, output_path = ?, failure_reason = ?
                WHERE id = ?
                """,
                job.getStatus().name(),
                job.getCompletedAt() != null ? java.sql.Timestamp.from(job.getCompletedAt()) : null,
                job.getCandidateCount(), job.getFileSizeBytes(),
                job.getOutputPath(), job.getFailureReason(), job.getId());
    }

    @Override
    public List<DatasetCandidate> findExportableCandidates(String pseudonymizedTenantId,
                                                            DatasetCandidate.DataQualityTier minQualityTier,
                                                            DataLabel.LabelValue labelFilter,
                                                            boolean includeUnlabeled,
                                                            Instant from, Instant to) {
        var sql = new StringBuilder("""
                SELECT * FROM dataset_candidates
                WHERE pseudonymized_tenant_id = ?
                AND created_at >= ? AND created_at <= ?
                """);
        var params = new java.util.ArrayList<Object>();
        params.add(pseudonymizedTenantId);
        params.add(java.sql.Timestamp.from(from));
        params.add(java.sql.Timestamp.from(to));

        if (!includeUnlabeled) {
            sql.append(" AND status IN ('LABELED', 'VALIDATED')");
        }
        if (minQualityTier != null) {
            sql.append(" AND quality_tier IN (?)");
            params.add(minQualityTier.name());
        }

        sql.append(" ORDER BY created_at");
        return jdbc.query(sql.toString(), this::mapCandidateRow, params.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> params,
                               DatasetCandidate.CandidateStatus status,
                               DatasetCandidate.DataQualityTier minQualityTier,
                               Instant from, Instant to) {
        if (status != null) { sql.append(" AND status = ?"); params.add(status.name()); }
        if (minQualityTier != null) { sql.append(" AND quality_tier = ?"); params.add(minQualityTier.name()); }
        if (from != null) { sql.append(" AND created_at >= ?"); params.add(java.sql.Timestamp.from(from)); }
        if (to != null) { sql.append(" AND created_at <= ?"); params.add(java.sql.Timestamp.from(to)); }
    }

    private DatasetCandidate mapCandidateRow(ResultSet rs, int rowNum) throws SQLException {
        var features = new FeatureSummary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        return new DatasetCandidate(
                UUID.fromString(rs.getString("id")),
                rs.getString("pseudonymized_device_id"),
                rs.getString("pseudonymized_tenant_id"),
                rs.getTimestamp("window_start").toInstant(),
                rs.getTimestamp("window_end").toInstant(),
                rs.getInt("data_quality_score"),
                rs.getDouble("risk_score_at_time"),
                rs.getString("anomaly_type"),
                rs.getString("source_alert_id") != null ? UUID.fromString(rs.getString("source_alert_id")) : null,
                rs.getString("source_intervention_id") != null ? UUID.fromString(rs.getString("source_intervention_id")) : null,
                features);
    }

    private DatasetExportJob mapExportJobRow(ResultSet rs, int rowNum) throws SQLException {
        String labelFilterStr = rs.getString("label_filter");
        var job = new DatasetExportJob(
                rs.getString("requested_by"),
                rs.getString("pseudonymized_tenant_id"),
                DatasetExportJob.ExportFormat.valueOf(rs.getString("format")),
                rs.getTimestamp("window_start").toInstant(),
                rs.getTimestamp("window_end").toInstant(),
                labelFilterStr != null ? DataLabel.LabelValue.valueOf(labelFilterStr) : null,
                DatasetCandidate.DataQualityTier.valueOf(rs.getString("min_quality_tier")),
                rs.getBoolean("include_unlabeled"));

        String status = rs.getString("status");
        if ("COMPLETED".equals(status)) {
            job.markCompleted(rs.getInt("candidate_count"), rs.getLong("file_size_bytes"), rs.getString("output_path"));
        } else if ("FAILED".equals(status)) {
            job.markFailed(rs.getString("failure_reason"));
        } else if ("RUNNING".equals(status)) {
            job.markRunning();
        }
        return job;
    }

    private String serializeFeatureSummary(FeatureSummary fs) {
        if (fs == null) return "{}";
        return """
                {"rmsVoltageAvg":%.2f,"rmsCurrentAvg":%.2f,"temperatureAvg":%.2f,\
                "thdAvg":%.2f,"thdMax":%.2f,"hfNoiseAvg":%.3f,"hfNoiseMax":%.3f,\
                "microArcCount":%d,"readingCount":%d,"signalQualityAvg":%.2f}""".formatted(
                fs.rmsVoltageAvg(), fs.rmsCurrentAvg(), fs.temperatureAvg(),
                fs.thdAvg(), fs.thdMax(), fs.hfNoiseAvg(), fs.hfNoiseMax(),
                fs.microArcCount(), fs.readingCount(), fs.signalQualityAvg());
    }
}
