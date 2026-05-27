CREATE TABLE pilot_programs (
    id              UUID PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'PREPARING',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ
);

CREATE INDEX idx_pilot_programs_tenant ON pilot_programs(tenant_id);
CREATE INDEX idx_pilot_programs_status ON pilot_programs(tenant_id, status);

CREATE TABLE pilot_sites (
    id              UUID PRIMARY KEY,
    pilot_id        UUID NOT NULL REFERENCES pilot_programs(id),
    name            VARCHAR(255) NOT NULL,
    address         TEXT,
    contact_name    VARCHAR(255),
    contact_phone   VARCHAR(50),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pilot_sites_pilot ON pilot_sites(pilot_id);

CREATE TABLE pilot_devices (
    id                   UUID PRIMARY KEY,
    pilot_id             UUID NOT NULL REFERENCES pilot_programs(id),
    device_id            VARCHAR(255) NOT NULL,
    serial_number        VARCHAR(100) NOT NULL,
    site_name            VARCHAR(255),
    circuit_description  TEXT,
    status               VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    installed_at         TIMESTAMPTZ,
    removed_at           TIMESTAMPTZ,
    installation_notes   TEXT
);

CREATE INDEX idx_pilot_devices_pilot ON pilot_devices(pilot_id);
CREATE INDEX idx_pilot_devices_device ON pilot_devices(device_id);

CREATE TABLE pilot_observations (
    id              UUID PRIMARY KEY,
    pilot_id        UUID NOT NULL REFERENCES pilot_programs(id),
    author_id       VARCHAR(255) NOT NULL,
    author_name     VARCHAR(255) NOT NULL,
    type            VARCHAR(30) NOT NULL,
    content         TEXT NOT NULL,
    device_id       VARCHAR(255),
    site_name       VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pilot_observations_pilot ON pilot_observations(pilot_id, created_at DESC);

CREATE TABLE pilot_incidents (
    id              UUID PRIMARY KEY,
    pilot_id        UUID NOT NULL REFERENCES pilot_programs(id),
    reported_by     VARCHAR(255) NOT NULL,
    severity        VARCHAR(20) NOT NULL,
    category        VARCHAR(50) NOT NULL,
    title           VARCHAR(500) NOT NULL,
    description     TEXT NOT NULL,
    device_id       VARCHAR(255),
    site_name       VARCHAR(255),
    resolution      TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    reported_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMPTZ
);

CREATE INDEX idx_pilot_incidents_pilot ON pilot_incidents(pilot_id, reported_at DESC);
CREATE INDEX idx_pilot_incidents_status ON pilot_incidents(pilot_id, status);

CREATE TABLE pilot_kpi_snapshots (
    id                      UUID PRIMARY KEY,
    pilot_id                UUID NOT NULL REFERENCES pilot_programs(id),
    date                    DATE NOT NULL,
    total_devices           INT NOT NULL DEFAULT 0,
    active_devices          INT NOT NULL DEFAULT 0,
    offline_devices         INT NOT NULL DEFAULT 0,
    uptime_percent          DOUBLE PRECISION NOT NULL DEFAULT 0,
    telemetry_valid_percent DOUBLE PRECISION NOT NULL DEFAULT 0,
    avg_signal_quality      DOUBLE PRECISION NOT NULL DEFAULT 0,
    alerts_generated        INT NOT NULL DEFAULT 0,
    alerts_confirmed        INT NOT NULL DEFAULT 0,
    false_positives         INT NOT NULL DEFAULT 0,
    false_positive_rate     DOUBLE PRECISION NOT NULL DEFAULT 0,
    incidents_open          INT NOT NULL DEFAULT 0,
    incidents_resolved      INT NOT NULL DEFAULT 0,
    avg_latency_ms          DOUBLE PRECISION NOT NULL DEFAULT 0,
    computed_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pilot_kpis_pilot ON pilot_kpi_snapshots(pilot_id, date DESC);
