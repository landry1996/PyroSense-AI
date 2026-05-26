# PyroSense Reporting Service

## Overview

The Reporting Service generates, stores, and distributes electrical safety reports for the PyroSense AI Platform. It produces compliance certificates, monthly health reports, critical alert reports, intervention reports, and ROI analyses.

## Architecture

Hexagonal architecture with:
- **Domain**: Report aggregate, ReportPeriod, ReportMetadata, ReportSignature, DownloadToken, ReportFileReference, ReportRecipient
- **Application**: Use cases (GenerateReportService, GetReportService, RequestReportService) + ports
- **Adapters**: REST controller, JDBC persistence, OpenPDF renderer, local file storage, Kafka event publisher

## Report Types

| Type | Prefix | Description |
|------|--------|-------------|
| MONTHLY_HEALTH | MH- | Monthly electrical health summary per building |
| CONTINUOUS_MONITORING_CERTIFICATE | CM- | Compliance certificate proving continuous monitoring |
| CRITICAL_ALERT_REPORT | CA- | Detailed report for a specific critical alert |
| INTERVENTION_REPORT | IR- | Report tied to a specific maintenance intervention |
| ROI_AVOIDED_INCIDENTS | ROI- | Analysis of incidents avoided by proactive monitoring |
| INSURER_EXPORT | IE- | Data export accessible by insurer roles |

## Report Lifecycle

```
REQUESTED → GENERATING → GENERATED → EXPIRED
                       ↘ FAILED
```

- **REQUESTED**: Report creation requested, awaiting generation
- **GENERATING**: PDF rendering in progress
- **GENERATED**: Report available for download (expires after 90 days)
- **FAILED**: Generation failed (renderer error, data unavailable)
- **EXPIRED**: Report content purged after TTL

## API Endpoints

### Report Generation

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| POST | /api/v1/reports/monthly-health | Request monthly health report | PLATFORM_ADMIN, TENANT_ADMIN, PROPERTY_MANAGER |
| POST | /api/v1/reports/monitoring-certificate | Request monitoring certificate | PLATFORM_ADMIN, TENANT_ADMIN, PROPERTY_MANAGER |
| POST | /api/v1/reports/critical-alert/{alertId} | Report for specific alert | PLATFORM_ADMIN, TENANT_ADMIN, PROPERTY_MANAGER |
| POST | /api/v1/reports/intervention/{interventionId} | Report for specific intervention | PLATFORM_ADMIN, TENANT_ADMIN, PROPERTY_MANAGER |
| POST | /api/v1/reports/monthly | Generic report generation (legacy) | PLATFORM_ADMIN, TENANT_ADMIN, PROPERTY_MANAGER |

### Report Queries

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| GET | /api/v1/reports | List reports (paginated) | PLATFORM_ADMIN, TENANT_ADMIN, PROPERTY_MANAGER, SUPPORT_READONLY |
| GET | /api/v1/reports/{id} | Get report by ID | All authenticated |
| GET | /api/v1/reports/building/{buildingId} | List reports by building | All authenticated |
| GET | /api/v1/reports/{id}/download-token | Create download token | PLATFORM_ADMIN, TENANT_ADMIN, PROPERTY_MANAGER, INSURER, OCCUPANT |
| GET | /api/v1/reports/{id}/download | Download PDF (token-gated, public) | Public (token required) |

### Request Body (Period Report)

```json
{
  "tenantId": "uuid",
  "buildingId": "uuid",
  "periodStart": "2025-01-01T00:00:00Z",
  "periodEnd": "2025-01-31T23:59:59Z"
}
```

## Secure Downloads

Downloads use expiring, one-time-use tokens:
1. Authenticated user requests a download token (GET /{id}/download-token)
2. Token has 15-minute TTL and is invalidated after first use
3. Download endpoint is public (no auth required) but requires valid token
4. Token verification ensures report ID matches

## Monthly Health Report Content

- Periode and tenant identification
- Buildings under surveillance
- Active/offline sensor counts
- Average risk score across all sensors
- Top risk buildings
- Alert breakdown by severity
- Interventions created/completed
- Confirmed defects and false positives
- Risk evolution percentage
- Recommendations
- ROI synthesis

## Continuous Monitoring Certificate Content

- Tenant and building identification
- Device/sensor information
- Monitoring period
- Availability rate (active / total sensors)
- Monitoring status (ACTIF / PARTIELLEMENT DEGRADE)
- Logical signature (report number)
- Unique certificate number

## Ports (Outbound)

| Port | Adapter | Description |
|------|---------|-------------|
| ReportRepositoryPort | JdbcReportRepository | PostgreSQL persistence with JSONB metadata |
| ReportRendererPort | OpenPdfReportRenderer | PDF generation using OpenPDF |
| ReportDataProviderPort | StubReportDataProvider | Data gathering (stub for dev) |
| DownloadTokenStorePort | InMemoryDownloadTokenStore | Token storage (in-memory for dev) |
| ReportEventPublisherPort | KafkaReportEventPublisher | Domain events to Kafka |
| FileStoragePort | LocalFileStorageAdapter | Local filesystem storage (S3-compatible port for prod) |
| ReportAuditLogPort | LoggingReportAuditLogAdapter | Audit trail via logging |

## Domain Events

| Event | Published When |
|-------|---------------|
| `reporting.report.generated` | Any report successfully generated |
| `reporting.compliance_certificate.generated` | Monitoring certificate generated (includes signature hash) |

## Insurer Access Control

Insurers can only access reports of types explicitly marked as insurer-accessible:
- INSURER_EXPORT
- ROI_AVOIDED_INCIDENTS
- CONTINUOUS_MONITORING_CERTIFICATE

Access requires: report is GENERATED + not expired + type is insurer-accessible.

## Database

PostgreSQL with Flyway migrations:
- V001: `reports` table with JSONB metadata, BYTEA content
- V002: Added expiry, source alert/intervention IDs, file storage reference, requester info

## Configuration

```yaml
server:
  port: 8091

pyrosense:
  reporting:
    kafka:
      output-topic: reporting-events
    storage:
      local-path: ./report-storage  # For local dev, replaced by S3 in prod
```

## Testing

- Unit tests: Report domain, DownloadToken, ReportSignature, ReportPeriod, ReportExpiry
- Service tests: GenerateReportService, GetReportService, RequestReportService
- Adapter tests: OpenPdfReportRenderer, LocalFileStorageAdapter
- Security tests: Role-based access control verification
- Architecture tests: ArchUnit hexagonal enforcement
- Integration test: Full Spring context with EmbeddedKafka
