CREATE INDEX idx_devices_building_id ON devices(building_id) WHERE building_id IS NOT NULL;
CREATE INDEX idx_devices_tenant_building ON devices(tenant_id, building_id) WHERE building_id IS NOT NULL;
