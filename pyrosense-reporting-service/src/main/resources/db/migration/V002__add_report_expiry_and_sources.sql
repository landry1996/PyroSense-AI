ALTER TABLE reports ADD COLUMN expires_at TIMESTAMP;
ALTER TABLE reports ADD COLUMN source_alert_id UUID;
ALTER TABLE reports ADD COLUMN source_intervention_id UUID;
ALTER TABLE reports ADD COLUMN requested_by_user_id UUID;
ALTER TABLE reports ADD COLUMN requested_by_name VARCHAR(255);
ALTER TABLE reports ADD COLUMN requested_by_role VARCHAR(50);
ALTER TABLE reports ADD COLUMN file_storage_path TEXT;
ALTER TABLE reports ADD COLUMN file_bucket VARCHAR(100);
ALTER TABLE reports ADD COLUMN file_size_bytes BIGINT;

CREATE INDEX idx_reports_source_alert_id ON reports(source_alert_id);
CREATE INDEX idx_reports_source_intervention_id ON reports(source_intervention_id);
CREATE INDEX idx_reports_expires_at ON reports(expires_at);
