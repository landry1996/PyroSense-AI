-- Dataset Candidates (ML training data)
CREATE TABLE dataset_candidates (
    id UUID PRIMARY KEY,
    pseudonymized_device_id TEXT NOT NULL,
    pseudonymized_tenant_id TEXT NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING_LABEL',
    quality_tier TEXT NOT NULL,
    data_quality_score INTEGER NOT NULL,
    risk_score_at_time DOUBLE PRECISION,
    anomaly_type TEXT,
    source_alert_id UUID,
    source_intervention_id UUID,
    feature_summary JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    exported_at TIMESTAMPTZ
);

CREATE INDEX idx_dc_tenant_status ON dataset_candidates (pseudonymized_tenant_id, status, created_at DESC);
CREATE INDEX idx_dc_quality ON dataset_candidates (quality_tier, status) WHERE status IN ('LABELED', 'VALIDATED');
CREATE INDEX idx_dc_anomaly ON dataset_candidates (anomaly_type, created_at DESC) WHERE anomaly_type IS NOT NULL;

-- Technician Feedbacks
CREATE TABLE technician_feedbacks (
    id UUID PRIMARY KEY,
    intervention_id UUID NOT NULL,
    technician_id TEXT NOT NULL,
    pseudonymized_device_id TEXT NOT NULL,
    defect_observed TEXT NOT NULL,
    confidence_level DOUBLE PRECISION NOT NULL,
    visual_inspection TEXT,
    measurement_method TEXT,
    measurement_result TEXT,
    defect_confirmed BOOLEAN NOT NULL DEFAULT false,
    false_positive BOOLEAN NOT NULL DEFAULT false,
    additional_notes TEXT,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tf_intervention ON technician_feedbacks (intervention_id);
CREATE INDEX idx_tf_device ON technician_feedbacks (pseudonymized_device_id, submitted_at DESC);

-- Field Observations
CREATE TABLE field_observations (
    id UUID PRIMARY KEY,
    pseudonymized_device_id TEXT NOT NULL,
    pseudonymized_tenant_id TEXT NOT NULL,
    intervention_id UUID NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    environment_type TEXT,
    installation_type TEXT,
    circuit_type TEXT,
    notes TEXT,
    label_id UUID
);

CREATE INDEX idx_fo_tenant ON field_observations (pseudonymized_tenant_id, observed_at DESC);
CREATE INDEX idx_fo_intervention ON field_observations (intervention_id);

-- Defect Confirmations
CREATE TABLE defect_confirmations (
    id UUID PRIMARY KEY,
    intervention_id UUID NOT NULL,
    alert_id UUID,
    pseudonymized_device_id TEXT NOT NULL,
    confirmed_defect TEXT NOT NULL,
    defect_location TEXT,
    severity_observed TEXT,
    correction_applied TEXT,
    risk_score_at_detection DOUBLE PRECISION,
    risk_score_after_correction DOUBLE PRECISION,
    confirmed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confirmed_by TEXT NOT NULL
);

CREATE INDEX idx_dconf_intervention ON defect_confirmations (intervention_id);
CREATE INDEX idx_dconf_defect ON defect_confirmations (confirmed_defect, confirmed_at DESC);

-- False Positive Feedbacks
CREATE TABLE false_positive_feedbacks (
    id UUID PRIMARY KEY,
    alert_id UUID NOT NULL,
    intervention_id UUID,
    pseudonymized_device_id TEXT NOT NULL,
    original_alert_type TEXT NOT NULL,
    risk_score_at_alert DOUBLE PRECISION,
    reason TEXT NOT NULL,
    suggested_threshold_adjustment TEXT,
    reported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reported_by TEXT NOT NULL
);

CREATE INDEX idx_fp_alert ON false_positive_feedbacks (alert_id);
CREATE INDEX idx_fp_type ON false_positive_feedbacks (original_alert_type, reported_at DESC);

-- Dataset Export Jobs
CREATE TABLE dataset_export_jobs (
    id UUID PRIMARY KEY,
    requested_by TEXT NOT NULL,
    pseudonymized_tenant_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    format TEXT NOT NULL DEFAULT 'PARQUET',
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    label_filter TEXT,
    min_quality_tier TEXT NOT NULL DEFAULT 'MEDIUM',
    include_unlabeled BOOLEAN NOT NULL DEFAULT false,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    candidate_count INTEGER DEFAULT 0,
    file_size_bytes BIGINT DEFAULT 0,
    output_path TEXT,
    failure_reason TEXT
);

CREATE INDEX idx_dej_tenant ON dataset_export_jobs (pseudonymized_tenant_id, requested_at DESC);
CREATE INDEX idx_dej_status ON dataset_export_jobs (status) WHERE status = 'PENDING';

-- Data Labels (linked to candidates)
CREATE TABLE data_labels (
    id UUID PRIMARY KEY,
    candidate_id UUID NOT NULL REFERENCES dataset_candidates(id),
    label_value TEXT NOT NULL,
    label_source TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    labeled_by TEXT NOT NULL,
    labeled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    justification TEXT
);

CREATE INDEX idx_dl_candidate ON data_labels (candidate_id);
CREATE INDEX idx_dl_value ON data_labels (label_value, labeled_at DESC);
