CREATE TABLE devices (
    id              UUID PRIMARY KEY,
    serial_number   VARCHAR(50) NOT NULL UNIQUE,
    tenant_id       UUID,
    building_id     UUID,
    panel_id        UUID,
    firmware_version    VARCHAR(20) NOT NULL,
    hardware_revision   VARCHAR(20) NOT NULL,
    connectivity_type   VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    last_seen_at        TIMESTAMPTZ,
    installation_date   TIMESTAMPTZ,
    enrollment_key_hash VARCHAR(128),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100) NOT NULL,
    updated_by          VARCHAR(100) NOT NULL
);

CREATE INDEX idx_devices_serial_number ON devices (serial_number);
CREATE INDEX idx_devices_tenant_id ON devices (tenant_id);
CREATE INDEX idx_devices_status ON devices (status);
CREATE INDEX idx_devices_tenant_status ON devices (tenant_id, status);
