# MVP 2 Security Hardening

## Overview

This document describes the security reinforcement applied across the MVP 2 services: Dashboard, Reporting, Maintenance (Interventions), Notifications, Alerting, and Audit.

## Role-Based Access Matrix

### Roles

| Role | Description |
|------|-------------|
| PLATFORM_ADMIN | Full platform access, cross-tenant |
| TENANT_ADMIN | Full tenant-level administration |
| PROPERTY_MANAGER | Building and device management |
| ELECTRICIAN | Field technician, limited to assigned resources |
| OCCUPANT | Building occupant, limited to own dwelling |
| INSURANCE_PARTNER | Insurance company, shared reports only |
| SUPPORT_READONLY | Support staff, read-only |

### Access Matrix

| Resource | PLATFORM_ADMIN | TENANT_ADMIN | PROPERTY_MANAGER | ELECTRICIAN | OCCUPANT | INSURANCE_PARTNER | SUPPORT_READONLY |
|----------|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **Dashboard overview** | R | R | R | - | - | - | - |
| **Risky buildings** | R | R | R | - | - | - | - |
| **Risk trend** | R | R | R | - | - | - | - |
| **Recent alerts** | R | R | R | R | R | - | - |
| **Priority interventions** | R | R | R | assigned | - | - | - |
| **Device health** | R | R | R | - | - | - | - |
| **Alerts - list** | R | R | R | R | own | - | R |
| **Alerts - acknowledge** | W | W | W | W | - | - | - |
| **Alerts - assign** | W | W | W | - | - | - | - |
| **Alerts - resolve** | W | W | W | W | - | - | - |
| **Alerts - false-positive** | W | W | W | - | - | - | - |
| **Alerts - comment** | W | W | W | W | - | - | - |
| **Interventions - list** | R | R | R | R | - | - | R |
| **Interventions - create** | W | W | W | - | - | - | - |
| **Interventions - assign** | W | W | W | - | - | - | - |
| **Interventions - start** | W | W | W | W | - | - | - |
| **Interventions - complete** | W | W | W | W | - | - | - |
| **Interventions - cancel** | W | W | W | - | - | - | - |
| **Interventions - statistics** | R | R | R | - | - | - | R |
| **Reports - list** | R | R | R | - | - | - | R |
| **Reports - generate** | W | W | W | - | - | - | - |
| **Reports - view** | R | R | R | - | R | R(shared) | R |
| **Reports - download token** | W | W | W | - | W | W | - |
| **Reports - download** | public (token) | public (token) | public (token) | public (token) | public (token) | public (token) | public (token) |
| **Notifications - tenant list** | R | R | R | - | - | - | R |
| **Notifications - own** | R | R | R | R | R | - | R |
| **Notifications - retry** | W | W | - | - | - | - | - |
| **Preferences - own** | RW | RW | RW | RW | RW | - | RW |
| **Preferences - admin** | RW | RW | - | - | - | - | - |
| **Tenant policy** | RW | RW | - | - | - | - | - |
| **Audit logs** | R(global) | R(tenant) | - | - | - | - | - |

*R = read, W = write, R* = read with no detail access*

## Method-Level Security

All controllers use Spring `@PreAuthorize` annotations at both class and method level:

```java
@RestController
@RequestMapping("/api/v1/alerts")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER', 'ELECTRICIAN', 'OCCUPANT', 'SUPPORT_READONLY')")
public class AlertController {

    @PostMapping("/{alertId}/assign")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN', 'PROPERTY_MANAGER')")
    public ResponseEntity<AlertResponse> assign(...) { ... }
}
```

Method-level annotations override class-level, enabling fine-grained write restrictions.

## Tenant Isolation

### Enforcement Points

1. **JWT tenant_id claim** — extracted by `TenantContextFilter` into `TenantContext` (ThreadLocal)
2. **TenantContext.require()** — used in every controller method before any data access
3. **Repository queries** — always include `WHERE tenant_id = ?` clause
4. **Response filtering** — `findById` methods verify `entry.tenantId().equals(currentTenant)` before returning

### Cross-Tenant Prevention

- `X-Tenant-Id` header is stripped at the API Gateway level
- Tenant ID is NEVER derived from request parameters or path variables
- `TenantContext.clear()` called in `finally` block after every request
- PLATFORM_ADMIN queries are scoped per-tenant (no cross-tenant in audit)

## Anti Mass-Assignment

The `@AllowedFields` annotation (shared-kernel) restricts which JSON fields a client can submit:

```java
@PutMapping("/preferences")
public ResponseEntity<?> update(
    @AllowedFields({"emailEnabled", "smsEnabled", "pushEnabled"}) 
    @RequestBody UpdateRequest request) { ... }
```

The `AllowedFieldsInterceptor` validates incoming JSON against the allowlist and returns 400 for any undeclared field.

### Protected Endpoints

| Service | Endpoint | Allowed Fields |
|---------|----------|----------------|
| Notification | PUT /notification-preferences/me | emailEnabled, smsEnabled, pushEnabled, webhookEnabled, quietHoursStart, quietHoursEnd, language, criticalOverrideEnabled, phoneVerified, emailVerified, pushTokenRegistered |
| Notification | PUT /tenants/{id}/notification-policy | emailEnabledByDefault, smsEnabledByDefault, pushEnabledByDefault, webhookEnabledByDefault, criticalOverrideMandatory, requirePhoneVerificationForSms, requirePushTokenForPush, defaultQuietHoursStart, defaultQuietHoursEnd, defaultLanguage |
| Reporting | POST /reports/monthly | tenantId, buildingId, type, periodStart, periodEnd |
| Reporting | POST /reports/monthly-health | tenantId, buildingId, periodStart, periodEnd |
| Reporting | POST /reports/monitoring-certificate | tenantId, buildingId, periodStart, periodEnd |

## Rate Limiting

### API Gateway (Global)

| Endpoint Category | Limit |
|-------------------|-------|
| Authentication `/api/v1/auth` | 10 req/min |
| Device ingestion | 120 req/min |
| Default | 60 req/min |

### Service-Level

| Service | Operation | Limit |
|---------|-----------|-------|
| Reporting | Report generation (POST) | 5 req/min per tenant |
| Notification | Retry (POST) | 10 req/min per tenant |
| Notification | Preference updates (PUT) | 20 req/min per tenant |

Rate limiting uses per-tenant buckets to prevent one tenant from impacting others.
Response headers: `Retry-After: 60`. Response status: 429 Too Many Requests.

## PDF Download Security

### Token-Based Access

1. Authenticated user requests a download token: `GET /api/v1/reports/{id}/download-token`
2. Server returns a single-use, time-limited token (15-minute TTL)
3. Client downloads via public endpoint: `GET /api/v1/reports/{id}/download?token=xxx`
4. Token is invalidated after first use
5. Expired tokens return 403

### Security Properties

- No session/cookie required for download (shareable link)
- Token is cryptographically random (32 bytes, `SecureRandom`)
- Token is SHA-256 hashed in storage (plaintext only in response)
- One-time use prevents replay
- 15-minute expiry limits window of exposure
- Tenant ownership verified before token creation
- Response headers: `Content-Disposition: attachment`, `X-Content-Type-Options: nosniff`, `Cache-Control: no-store`
- Report ID in URL must match token's report ID (prevents token reuse across reports)

## Sensitive Data Masking

### Audit Logs

The `JdbcAuditLogRepository` applies PII masking before persistence:

| Pattern | Replacement |
|---------|-------------|
| Phone numbers (10+ digits) | `***-MASKED-***` |
| Email addresses | `***@***.***` |
| `password` values | `[REDACTED]` |
| `token`/`secret` values | `[REDACTED]` |

### API Responses

- `userAgent` stored in DB but NOT exposed in audit API responses
- Notification controller masks recipient details (no PII in logs)
- Notification `failureReason` masked when it contains exception details or exceeds 200 chars
- Report content never includes raw electrical measurements in API

### GDPR Compliance

- Logging providers mask phone (`***1234`) and email (`j***e@domain.com`)
- No PII in Kafka event payloads
- Audit entries record action/resource/actor but not affected user's personal data

## Download Token Expiration

```java
record DownloadToken(String token, Instant expiresAt) {
    boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
```

- Default TTL: 15 minutes
- Single-use: token removed from store after download
- Expired report (90-day TTL): content purged, download returns 404

## Security Tests

Each service has dedicated security test classes:

| Service | Test Class | Tests |
|---------|-----------|-------|
| Alerting | `AlertSecurityTest` | 13 (role matrix, write restrictions) |
| Dashboard | `DashboardSecurityTest` | 13 (role access matrix, forbidden by role) |
| Dashboard | `DashboardTenantIsolationTest` | 5 (tenant query isolation, no-context fail) |
| Maintenance | `InterventionAccessMatrixTest` | 11 (ELECTRICIAN/SUPPORT restrictions) |
| Maintenance | `InterventionTenantIsolationTest` | 4 (list/kanban/statistics isolation, no-context) |
| Reporting | `ReportAccessMatrixTest` | 8 (generate vs view, INSURER) |
| Reporting | `ReportTenantIsolationTest` | 5 (cross-tenant rejected, same-tenant allowed, role blocked) |
| Reporting | `RateLimitInterceptorTest` | 5 (rate limit enforcement) |
| Notification | `NotificationSecurityTest` | 10 (role matrix, retry restriction) |
| Notification | `NotificationTenantIsolationTest` | 5 (list isolation, no-context, annotation checks) |
| Notification | `AuditLogSecurityTest` | 8 (admin-only, PII masking) |

### Test Categories

1. **Unauthenticated access** — verifies 401/403 without JWT
2. **Role-based access** — verifies each role against each endpoint
3. **Write restriction** — verifies read-only roles cannot mutate state
4. **Tenant isolation** — verifies cross-tenant data is invisible
5. **Sensitive data** — verifies no secrets/PII in responses
6. **Rate limiting** — verifies 429 after threshold

## Configuration

### application.yml Properties

```yaml
pyrosense:
  security:
    rate-limit:
      report-generation-per-minute: 5
    download-token:
      ttl-minutes: 15
    audit:
      max-page-size: 200
```

### Security Headers (All Services)

```
Content-Security-Policy: default-src 'self'
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
Strict-Transport-Security: max-age=31536000; includeSubDomains
```

## Implementation Checklist

- [x] Method-level `@PreAuthorize` on all write endpoints
- [x] `@PreAuthorize` restricts ELECTRICIAN to assigned resources
- [x] OCCUPANT limited to own dwelling alerts
- [x] INSURANCE_PARTNER limited to shared reports
- [x] SUPPORT_READONLY blocked from all mutations
- [x] `AllowedFieldsInterceptor` for mass-assignment protection
- [x] `RateLimitInterceptor` on report generation (5/min/tenant)
- [x] Token-based PDF download (15min TTL, single-use)
- [x] PII masking in audit persistence
- [x] `userAgent` not exposed in API
- [x] Tenant isolation via TenantContext (never from request params)
- [x] Security tests per service (role matrix + tenant isolation)
- [x] Documentation (this file)
