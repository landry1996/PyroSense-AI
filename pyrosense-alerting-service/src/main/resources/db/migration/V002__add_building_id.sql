ALTER TABLE alerts ADD COLUMN building_id VARCHAR(36);

CREATE INDEX idx_alerts_building_id ON alerts(building_id);
CREATE INDEX idx_alerts_tenant_building ON alerts(tenant_id, building_id);
