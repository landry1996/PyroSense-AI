# PyroSense Time-Series Storage Design

## Technology Choice

**PostgreSQL 16 + TimescaleDB 2.x**

Why not InfluxDB/Clickhouse/QuestDB:
- Single operational database (PostgreSQL) simplifies infrastructure
- TimescaleDB extends PostgreSQL with transparent hypertables
- Full SQL compatibility (joins with device metadata, RBAC via RLS)
- Continuous aggregates handle rollups without external ETL
- Built-in retention policies automate data lifecycle
- ACID transactions for ingestion correctness

## Schema Design

### Primary Table: `electrical_telemetry`

```
┌──────────────────────────────────────────────────────────────────┐
│ electrical_telemetry (hypertable, 7-day chunks)                  │
├──────────────────────────────────────────────────────────────────┤
│ tenant_id           UUID NOT NULL                                │
│ device_id           UUID NOT NULL                                │
│ building_id         UUID NOT NULL                                │
│ electrical_panel_id UUID NOT NULL                                │
│ circuit_id          UUID (nullable)                              │
│ measured_at         TIMESTAMPTZ NOT NULL  ← partition key        │
│ rms_current         DOUBLE PRECISION                             │
│ rms_voltage         DOUBLE PRECISION                             │
│ active_power        DOUBLE PRECISION                             │
│ reactive_power      DOUBLE PRECISION                             │
│ power_factor        DOUBLE PRECISION                             │
│ thd                 DOUBLE PRECISION                             │
│ temperature_celsius DOUBLE PRECISION                             │
│ hf_noise_level      DOUBLE PRECISION                             │
│ micro_arc_count     INTEGER                                      │
│ transient_count     INTEGER                                      │
│ created_at          TIMESTAMPTZ DEFAULT NOW()                    │
└──────────────────────────────────────────────────────────────────┘
```

**Design decisions:**
- No UUID surrogate PK (time-series anti-pattern: waste of space + index bloat)
- Composite natural key via `(tenant_id, device_id, measured_at)` index
- `measured_at` is the hypertable partition column (7-day chunks)
- `DOUBLE PRECISION` (float64) sufficient for electrical measurements
- No raw waveform storage (privacy-by-design: only derived features stored)

### Supporting Tables

| Table | Purpose | Chunk Interval |
|-------|---------|----------------|
| `device_heartbeats` | Device health tracking | 7 days |
| `ingestion_rejections` | Audit trail for rejected data | 30 days |

## Indexes

| Index | Purpose | Type |
|-------|---------|------|
| `idx_et_tenant_device_time` | Primary query path (device analysis) | B-tree |
| `idx_et_tenant_building_time` | Building-level dashboard | B-tree |
| `idx_et_risk_indicators` | Anomaly detection (covering index) | B-tree + INCLUDE |
| `idx_et_micro_arcs` | Sparse micro-arc queries | Partial (WHERE > 0) |
| `idx_et_circuit_time` | Circuit-level analysis | Partial (WHERE NOT NULL) |

## Continuous Aggregates

Pre-materialized rollups updated incrementally by TimescaleDB:

| View | Resolution | Refresh | Retention | Use Case |
|------|-----------|---------|-----------|----------|
| `telemetry_1min` | 1 minute | Every 1 min | 180 days | Real-time dashboard |
| `telemetry_15min` | 15 minutes | Every 5 min | 1 year | Risk scoring input |
| `telemetry_1hour` | 1 hour | Every 15 min | 2 years | Reporting, trends |
| `telemetry_daily` | 1 day | Every 1 hour | Indefinite | Long-term analytics |

Each aggregate includes: AVG, MAX, SUM(arcs), COUNT, STDDEV where useful.

## Retention Policy

| Data | Retention | Justification |
|------|-----------|---------------|
| Raw telemetry | 90 days | High volume; aggregates cover older data |
| 1-min aggregate | 180 days | Dashboard lookback |
| 15-min aggregate | 1 year | Risk model training window |
| 1-hour aggregate | 2 years | Regulatory / reporting |
| Daily aggregate | No auto-drop | Minimal size, long-term value |
| Heartbeats | 90 days | Operational only |
| Rejections | 30 days | Short-term debugging |

Retention is enforced by TimescaleDB `add_retention_policy` (background job drops old chunks).

## Write Path

```
MQTT/REST → Validation → Idempotency Check → JDBC Batch Insert → Kafka Publish
```

**Why JDBC batch over JPA:**
1. Append-only data — no dirty checking, no merge, no L1 cache needed
2. `PreparedStatement.addBatch()` maps to PostgreSQL multi-row INSERT
3. 10-50x throughput improvement over single-row JPA `persist()`
4. No identity generation overhead (no `@GeneratedValue`, no sequence round-trips)
5. TimescaleDB hypertables don't integrate cleanly with JPA's `@Table`/`@Id` model

Batch size: configurable, default 100 rows per batch (matches HikariCP connection budget).

## Read Path

```
Dashboard/API → Query Router → Appropriate Aggregate View → RowMapper → DTO
```

Query routing logic:
- Last 1 hour: query raw `electrical_telemetry`
- Last 24 hours: query `telemetry_1min`
- Last 7 days: query `telemetry_15min`
- Last 30 days: query `telemetry_1hour`
- Historical: query `telemetry_daily`

All queries include `tenant_id` in WHERE clause (mandatory for security + chunk exclusion).

## Multi-Tenancy

- Tenant isolation via `tenant_id` column in every table
- All indexes are tenant-prefixed for chunk exclusion
- Future: PostgreSQL Row-Level Security (RLS) policies per tenant
- Tenant-specific retention overrides possible via custom policies

## GDPR / Data Privacy

### Anonymization
```sql
-- Anonymize a tenant's data (right to erasure)
-- Option A: Delete (fast with hypertables, drops entire chunks if possible)
DELETE FROM electrical_telemetry WHERE tenant_id = '<tenant_uuid>';

-- Option B: Anonymize (keep aggregates, null identifiers)
UPDATE electrical_telemetry 
SET device_id = '00000000-0000-0000-0000-000000000000'
WHERE tenant_id = '<tenant_uuid>';
```

### Export
```sql
-- GDPR data export (right to portability)
COPY (
    SELECT * FROM electrical_telemetry
    WHERE tenant_id = '<tenant_uuid>'
    ORDER BY measured_at
) TO STDOUT WITH CSV HEADER;
```

### Design Constraints
- No raw waveform storage (privacy-by-design)
- No PII in telemetry tables (device IDs are pseudonymous)
- Retention policies ensure automatic data minimization

## Capacity Planning

Assumptions: 1000 devices, 1 reading/second each

| Metric | Value |
|--------|-------|
| Rows/second | 1,000 |
| Row size | ~200 bytes |
| Daily raw volume | ~16 GB |
| 90-day raw storage | ~1.5 TB |
| 1-min aggregates (90d) | ~25 GB |
| Compression ratio | 10-20x (TimescaleDB native compression) |
| Effective 90-day storage | ~100-150 GB compressed |

## Configuration

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20    # write-heavy workload
      minimum-idle: 5
  flyway:
    locations: classpath:db/migration
```

## Migration Files

| File | Purpose |
|------|---------|
| V001 | Enable TimescaleDB extension |
| V002 | Create `electrical_telemetry` hypertable + indexes |
| V003 | Create `device_heartbeats` hypertable |
| V004 | Create `ingestion_rejections` hypertable |
| V005 | Create continuous aggregates (1min, 15min, 1hour, daily) |
| V006 | Configure retention policies + refresh schedules |
