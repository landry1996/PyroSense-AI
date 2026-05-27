-- Data Quality Assessments
CREATE TABLE data_quality_assessments (
    id UUID PRIMARY KEY,
    device_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    assessed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    overall_score INTEGER NOT NULL,
    grade TEXT NOT NULL,
    trustworthy BOOLEAN NOT NULL DEFAULT false,
    signal_quality_avg DOUBLE PRECISION,
    clock_drift_seconds DOUBLE PRECISION,
    offline_count INTEGER DEFAULT 0,
    firmware_version TEXT,
    firmware_obsolete BOOLEAN DEFAULT false,
    issue_count INTEGER DEFAULT 0,
    completeness_percent DOUBLE PRECISION
);

CREATE INDEX idx_dq_assessments_device ON data_quality_assessments (device_id, assessed_at DESC);
CREATE INDEX idx_dq_assessments_tenant ON data_quality_assessments (tenant_id, assessed_at DESC);
CREATE INDEX idx_dq_assessments_grade ON data_quality_assessments (grade) WHERE grade IN ('D', 'F');

-- Data Quality Issues
CREATE TABLE data_quality_issues (
    id UUID PRIMARY KEY,
    device_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    type TEXT NOT NULL,
    severity TEXT NOT NULL,
    details TEXT,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status TEXT NOT NULL DEFAULT 'OPEN',
    reviewed_by TEXT,
    reviewed_at TIMESTAMPTZ,
    review_comment TEXT
);

CREATE INDEX idx_dq_issues_tenant_status ON data_quality_issues (tenant_id, status, detected_at DESC);
CREATE INDEX idx_dq_issues_device ON data_quality_issues (device_id, detected_at DESC);
CREATE INDEX idx_dq_issues_open ON data_quality_issues (tenant_id, detected_at DESC) WHERE status = 'OPEN';
