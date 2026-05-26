# Audit System - PyroSense AI Platform

## Overview

The audit system provides a transversal traceability layer for security-sensitive actions across the platform. It records who did what, when, from where, and on which resource.

## Architecture

```
┌─────────────────┐     ┌──────────────┐     ┌─────────────────────┐
│  Domain Services│────▶│  AuditLogPort│────▶│LoggingAuditLogAdapter│
│  (use cases)    │     │  (port out)  │     │  + JdbcAuditLogRepo  │
└─────────────────┘     └──────────────┘     └─────────────────────┘
                                                        │
                                                        ▼
                                              ┌─────────────────┐
                                              │  PostgreSQL      │
                                              │  audit_log table │
                                              └─────────────────┘
```

### Query Path

```
┌──────────────────┐     ┌────────────────┐     ┌──────────────────┐
│AuditLogController│────▶│GetAuditLogQuery│────▶│GetAuditLogService │
│  (REST adapter)  │     │  (port in)     │     │  (use case)       │
└──────────────────┘     └────────────────┘     └──────────────────┘
                                                        │
                                                        ▼
                                              ┌─────────────────────┐
                                              │  AuditLogRepository │
                                              │  (port out)         │
                                              └─────────────────────┘
```

## Data Model

### AuditEntry

| Field           | Type               | Description                          |
|-----------------|--------------------|------------------------------------- |
| id              | UUID               | Unique identifier (auto-generated)   |
| action          | String             | Action performed (e.g. ALERT_RESOLVED) |
| resourceType    | String             | Target resource type (e.g. NOTIFICATION) |
| resourceId      | String             | Target resource identifier           |
| userId          | UserId             | Actor who performed the action       |
| actorRole       | String             | Role of the actor                    |
| tenantId        | TenantId           | Tenant context                       |
| ipAddress       | String             | Source IP address                    |
| userAgent       | String             | Browser/client user agent            |
| correlationId   | String             | Request correlation ID               |
| details         | String             | Human-readable description           |
| metadata        | Map<String,String> | Additional structured data           |
| timestamp       | Instant            | When the action occurred             |

### Database Schema (V005)

```sql
CREATE TABLE audit_log (
    id              UUID PRIMARY KEY,
    action          VARCHAR(100) NOT NULL,
    resource_type   VARCHAR(100) NOT NULL,
    resource_id     VARCHAR(255),
    user_id         UUID,
    actor_role      VARCHAR(50),
    tenant_id       UUID,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    correlation_id  VARCHAR(100),
    details         TEXT,
    metadata        TEXT,
    timestamp       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### Indexes

- `idx_audit_log_tenant_time` — Primary query path (tenant + time DESC)
- `idx_audit_log_action` — Filter by action type
- `idx_audit_log_resource` — Filter by resource type
- `idx_audit_log_user` — Lookup by user
- `idx_audit_log_correlation` — Trace request chains

## REST API

### Endpoints

All endpoints require `PLATFORM_ADMIN` or `TENANT_ADMIN` role.

#### List Audit Logs

```
GET /api/v1/audit-logs?from=&to=&action=&resourceType=&page=0&size=50
```

**Parameters:**

| Param        | Required | Default          | Description              |
|--------------|----------|------------------|--------------------------|
| from         | No       | 30 days ago      | Start date (ISO-8601)    |
| to           | No       | now              | End date (ISO-8601)      |
| action       | No       | —                | Filter by action type    |
| resourceType | No       | —                | Filter by resource type  |
| page         | No       | 0                | Page number (0-based)    |
| size         | No       | 50               | Page size (max 200)      |

**Response:**

```json
{
  "entries": [
    {
      "id": "uuid",
      "action": "PREFERENCES_UPDATED",
      "resourceType": "NOTIFICATION_PREFERENCES",
      "resourceId": "user-uuid",
      "actorId": "admin-uuid",
      "actorRole": "TENANT_ADMIN",
      "tenantId": "tenant-uuid",
      "ipAddress": "192.168.1.1",
      "correlationId": "corr-123",
      "details": "Updated notification preferences",
      "metadata": {},
      "timestamp": "2026-05-26T10:30:00Z"
    }
  ],
  "totalCount": 142,
  "page": 0,
  "size": 50,
  "totalPages": 3
}
```

#### Get Audit Log by ID

```
GET /api/v1/audit-logs/{id}
```

Returns 404 if not found or belongs to a different tenant (tenant isolation).

## Security

### Access Control

- Class-level `@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")`
- No access for ELECTRICIAN, PROPERTY_MANAGER, or OCCUPANT roles
- Tenant isolation enforced: queries always scoped to the caller's tenant

### PII Protection

The persistence layer applies regex-based masking before storing:

| Pattern                  | Replacement           |
|--------------------------|----------------------|
| Phone numbers (10+ digits) | `***-MASKED-***`   |
| Email addresses          | `***@***.***`        |
| Password fields          | `[REDACTED]`         |
| Token/secret values      | `[REDACTED]`         |

The `userAgent` field is stored but NOT exposed in API responses.

### Data Retention

Audit logs are immutable — no update or delete operations are exposed via the API.

## Usage

### Writing Audit Entries

Inject `AuditLogPort` in your use case:

```java
auditLog.log(AuditEntry.createFull(
    "ACTION_NAME",
    "RESOURCE_TYPE",
    resourceId,
    userId,
    actorRole,
    tenantId,
    ipAddress,
    userAgent,
    correlationId,
    "Human-readable details",
    Map.of("key", "value")
));
```

### Querying Audit Entries

Inject `GetAuditLogQuery`:

```java
AuditLogFilter filter = new AuditLogFilter(from, to, action, resourceType, page, size);
AuditLogPage result = auditLogQuery.findByTenant(tenantId, filter);
```

## Pagination Rules

- Page size capped at 200 (values > 200 are reduced to 200)
- Size <= 0 defaults to 50
- Negative page defaults to 0
- Response includes `totalCount` and `totalPages` for client navigation
