# PyroSense AI Platform - Security Architecture

## 1. Authentication & Authorization

### Identity Provider
- **Keycloak** (self-hosted, OIDC-compliant, RS256)
- Realm: `pyrosense`
- Client: `pyrosense-platform`
- Token format: JWT with custom claims (`tenant_id`, `roles`, `permissions`, `device_id`, `scope`)

### JWT Claims Structure
```json
{
  "sub": "user-uuid",
  "iss": "https://keycloak.pyrosense.io/realms/pyrosense",
  "tenant_id": "tenant-uuid",
  "roles": ["PROPERTY_MANAGER"],
  "permissions": ["ALERT_READ", "ALERT_ACKNOWLEDGE", "DEVICE_READ"],
  "device_id": null,
  "scope": "openid profile",
  "exp": 1710000000,
  "iat": 1709996400
}
```

### Multi-Tenant Context
- `tenant_id` claim extracted from JWT by `TenantContextFilter` in every service
- Propagated via ThreadLocal (`TenantContext.set()`/`TenantContext.get()`)
- Automatically cleared after every request (finally block)
- All repository queries MUST filter by tenant — enforced by architecture tests

---

## 2. Role & Permission Matrix

### Platform Roles (8)

| Role | Description | Admin Level |
|------|-------------|-------------|
| `PLATFORM_ADMIN` | Full platform control | Yes |
| `TENANT_ADMIN` | Tenant-level administration | Yes |
| `PROPERTY_MANAGER` | Manages buildings and devices | No |
| `OCCUPANT` | Read-only dashboards | No |
| `ELECTRICIAN` | Alerts resolution and maintenance | No |
| `INSURANCE_PARTNER` | Risk data access | No |
| `DEVICE` | IoT device service account | No |
| `SUPPORT_READONLY` | Cross-tenant read support | No |

### Permission Matrix (34 permissions)

| Permission | PLATFORM_ADMIN | TENANT_ADMIN | PROPERTY_MANAGER | OCCUPANT | ELECTRICIAN | INSURANCE_PARTNER | DEVICE | SUPPORT_READONLY |
|-----------|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| USER_CREATE | x | x | | | | | | |
| USER_READ | x | x | x | | | | | x |
| USER_UPDATE | x | x | | | | | | |
| USER_DELETE | x | x | | | | | | |
| TENANT_CREATE | x | | | | | | | |
| TENANT_READ | x | x | x | | | | | x |
| TENANT_UPDATE | x | x | | | | | | |
| TENANT_DELETE | x | | | | | | | |
| DEVICE_REGISTER | x | x | x | | | | | |
| DEVICE_READ | x | x | x | x | x | | | x |
| DEVICE_UPDATE | x | x | x | | | | | |
| DEVICE_DECOMMISSION | x | x | x | | | | | |
| TELEMETRY_INGEST | x | | | | | | x | |
| TELEMETRY_READ | x | x | x | x | x | | | x |
| ALERT_CREATE | x | x | x | | | | | |
| ALERT_READ | x | x | x | x | x | x | | x |
| ALERT_ACKNOWLEDGE | x | x | x | | x | | | |
| ALERT_RESOLVE | x | x | x | | x | | | |
| ALERT_ASSIGN | x | x | x | | | | | |
| ALERT_ESCALATE | x | x | x | | | | | |
| RISK_READ | x | x | x | x | | x | | x |
| RISK_EXPORT | x | x | | | | x | | |
| REPORT_READ | x | x | x | x | | x | | x |
| REPORT_GENERATE | x | x | x | | | | | |
| MAINTENANCE_CREATE | x | x | x | | x | | | |
| MAINTENANCE_READ | x | x | x | | x | | | x |
| MAINTENANCE_UPDATE | x | x | x | | x | | | |
| MAINTENANCE_CLOSE | x | x | x | | x | | | |
| SYSTEM_CONFIG | x | | | | | | | |
| SYSTEM_MONITOR | x | x | | | | | | x |
| AUDIT_READ | x | x | | | | | | x |
| AUDIT_EXPORT | x | | | | | | | |
| NOTIFICATION_SEND | x | x | x | | | | | |
| NOTIFICATION_READ | x | x | x | x | x | | | x |

---

## 3. STRIDE Threat Model

### S — Spoofing

| Asset | Threat | Mitigation |
|-------|--------|------------|
| User identity | Credential theft, session hijacking | JWT with short expiry (15min), refresh tokens, Keycloak MFA |
| Device identity | Impersonation of IoT sensors | HMAC-SHA256 device tokens, per-device secrets, automatic rotation |
| Service identity | Service impersonation on internal network | mTLS between services (production), service accounts with client credentials |

### T — Tampering

| Asset | Threat | Mitigation |
|-------|--------|------------|
| Telemetry data | Modified sensor readings | HMAC signature on device payloads (`X-Signature` + `X-Timestamp` headers) |
| API requests | Parameter manipulation | Input validation (Jakarta Bean Validation), mass-assignment protection (`@AllowedFields`) |
| Audit logs | Evidence destruction | Append-only audit table, no DELETE/UPDATE permissions on audit schema |
| JWT tokens | Token modification | RS256 signature verification via Keycloak JWKS endpoint |

### R — Repudiation

| Asset | Threat | Mitigation |
|-------|--------|------------|
| User actions | Denial of actions taken | AOP-based audit logging (`@Audited`): who, what, when, tenant, IP, user-agent |
| Device telemetry | Denial of data submission | Immutable ingestion log with device_id + timestamp + HMAC |
| Admin operations | Unauthorized changes denied | All mutations logged with `SecurityContext.getUserId()` |

### I — Information Disclosure

| Asset | Threat | Mitigation |
|-------|--------|------------|
| Database credentials | Credential leakage | Environment variables only, `application-secret.yml` in .gitignore |
| Tenant data | Cross-tenant data leak | `TenantContext` ThreadLocal enforcement, repository-level filtering |
| JWT tokens | Token exposure in logs | Structured logging filters redact `Authorization` headers |
| Error details | Stack traces in API responses | `ApiErrorResponse` returns code + message only, no stack traces |

### D — Denial of Service

| Asset | Threat | Mitigation |
|-------|--------|------------|
| API Gateway | Request flooding | Redis-based rate limiting: 60 req/min default, 10 req/min auth, 120 req/min devices |
| Services | Resource exhaustion | Request size limits, connection pooling, circuit breakers |
| Database | Query flooding | Connection pool limits (HikariCP max 10), slow query alerting |

### E — Elevation of Privilege

| Asset | Threat | Mitigation |
|-------|--------|------------|
| User roles | Self-escalation | Role assignment restricted to PLATFORM_ADMIN/TENANT_ADMIN only |
| Tenant access | Cross-tenant access | JWT `tenant_id` claim is authoritative, not request parameters |
| Device scope | Device accessing user data | DEVICE role limited to TELEMETRY_INGEST permission only |
| Admin endpoints | Unauthorized admin access | `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` on all admin operations |

---

## 4. OWASP ASVS Compliance

### V1 — Architecture, Design, Threat Modeling
- [x] Hexagonal architecture with clear boundaries
- [x] Input validation at application boundary (controllers)
- [x] Security controls in dedicated config classes
- [x] STRIDE threat model documented

### V2 — Authentication
- [x] Keycloak handles authentication (delegated to IdP)
- [x] JWT RS256 signature verification
- [x] Account lockout after 5 failed attempts (30min auto-unlock)
- [x] Device credential rotation with configurable expiry

### V3 — Session Management
- [x] Stateless (no server-side sessions)
- [x] JWT short-lived tokens (15min)
- [x] Refresh token rotation via Keycloak

### V4 — Access Control
- [x] RBAC with 8 roles and 34 fine-grained permissions
- [x] Method-level security (`@PreAuthorize`)
- [x] Tenant isolation via JWT-derived `tenant_id`
- [x] No direct object reference exposure (UUIDs)

### V5 — Validation, Sanitization, Encoding
- [x] Jakarta Bean Validation on all DTOs
- [x] Mass-assignment protection (`@AllowedFields` annotation)
- [x] Content-Type enforcement
- [x] CSP headers: `default-src 'self'`

### V7 — Error Handling and Logging
- [x] Structured audit logging (AOP `@Audited`)
- [x] No sensitive data in error responses
- [x] RFC 7807 problem detail format
- [x] Correlation IDs (`X-Request-Id`)

### V8 — Data Protection
- [x] TLS 1.3 in production (all channels)
- [x] No secrets in source code
- [x] Database-level encryption at rest
- [x] HMAC for device payload integrity

### V9 — Communication Security
- [x] HTTPS enforced (HSTS header)
- [x] mTLS for inter-service communication (production)
- [x] MQTT over TLS for IoT devices
- [x] Kafka SASL/SCRAM + TLS

### V13 — API Security
- [x] Rate limiting (Redis-backed, per-IP, per-endpoint-category)
- [x] Request size limits
- [x] CORS strict configuration (explicit origins, methods, headers)
- [x] Security headers (X-Frame-Options, CSP, HSTS, X-Content-Type-Options)

---

## 5. API Security Controls

### SecurityFilterChain (every service)
```
STATELESS session → CORS → CSP/Frame headers → authorize → OAuth2 JWT → TenantContext filter
```

### Security Headers
| Header | Value |
|--------|-------|
| `Content-Security-Policy` | `default-src 'self'` |
| `X-Frame-Options` | `DENY` |
| `X-Content-Type-Options` | `nosniff` |
| `X-XSS-Protection` | `0` (CSP is sufficient) |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains; preload` |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `Permissions-Policy` | `camera=(), microphone=(), geolocation=()` |

### CORS Configuration
- `allowedOrigins`: from `CORS_ORIGINS` environment variable
- `allowedMethods`: GET, POST, PUT, PATCH, DELETE
- `allowedHeaders`: Authorization, Content-Type, X-Tenant-Id, X-Request-Id
- `allowCredentials`: true
- `maxAge`: 3600s

### Rate Limiting (API Gateway)
| Endpoint Category | Limit | Window |
|-------------------|-------|--------|
| Authentication (`/api/v1/auth/**`) | 10 requests | 1 minute |
| Device ingestion (`/api/v1/signals/**`) | 120 requests | 1 minute |
| Default (all other) | 60 requests | 1 minute |

Response headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `Retry-After`

---

## 6. IoT Device Security

### Device Authentication Flow
```
1. Device registered → identity-service issues HMAC token (32-byte SecureRandom)
2. Token stored as SHA-256 hash (never plaintext)
3. Device sends telemetry with: Authorization: Bearer <token>
4. Gateway validates token via identity-service /api/v1/auth/device/validate
5. Device credential auto-expires (configurable, default 90 days)
```

### Payload Integrity (Anti-Tampering)
```
Headers:
  X-Timestamp: <unix-epoch-seconds>
  X-Signature: HMAC-SHA256(device_secret, timestamp + "." + request_body)

Server verification:
  1. Check |now - timestamp| < 5 minutes (replay window)
  2. Recompute HMAC and compare (constant-time)
  3. Reject if signature mismatch
```

### Device Credential Rotation
- Automatic rotation triggered when credential is within 7 days of expiry
- Rotation: generate new token → hash → store → revoke old immediately
- Grace period: none (old token invalid immediately after rotation)
- Max authentication count tracked for anomaly detection

### IoT Protection Rules
1. DEVICE role can ONLY call `TELEMETRY_INGEST` — no other endpoint accessible
2. Device tokens are tenant-scoped — cannot submit data for another tenant
3. Per-device rate limit: 120 req/min (covers 1-second polling)
4. Payload size limit: 10KB per telemetry request
5. Device credentials are non-transferable (bound to device_id)
6. Revoked devices immediately blocked (check on every request)
7. Suspicious patterns (burst beyond 3x normal rate) trigger auto-revocation alert

---

## 7. Attack Protections

### SQL Injection
- Spring Data JPA parameterized queries (no string concatenation)
- No native queries with user input
- Flyway-managed schema (no dynamic DDL)

### XSS (Cross-Site Scripting)
- API-only (no HTML rendering) — primary mitigation
- `Content-Security-Policy: default-src 'self'` prevents inline scripts
- `X-Content-Type-Options: nosniff` prevents MIME sniffing
- All string inputs validated with `@Size` constraints

### CSRF (Cross-Site Request Forgery)
- Disabled: stateless JWT API (no cookies for auth)
- CORS restricts origins that can make requests

### SSRF (Server-Side Request Forgery)
- No user-controlled outbound URLs in any service
- Webhook URLs (if added) restricted to allowed domain whitelist
- Internal service communication uses hardcoded base URLs (not from request)

### Replay Attacks
- Device telemetry: HMAC + timestamp (5-minute replay window)
- JWT tokens: `exp` claim + server-side validation
- Idempotency keys on ingestion (Redis-based dedup)

### Mass Assignment
- `@AllowedFields` annotation on controller parameters
- DTOs are Java records (immutable, no setters)
- Only explicitly mapped fields from request to domain

---

## 8. Audit Logging

### Implementation
- AOP aspect intercepts methods annotated with `@Audited`
- Captures: userId, tenantId, action, resourceType, resourceId, IP, user-agent, timestamp
- Stored in append-only `audit_log` table (no UPDATE/DELETE privileges)

### What is Audited
| Action | Resource | Actors |
|--------|----------|--------|
| User registration | User | PLATFORM_ADMIN, TENANT_ADMIN |
| Role assignment | Membership | PLATFORM_ADMIN, TENANT_ADMIN |
| Device credential issuance | DeviceCredential | PLATFORM_ADMIN, TENANT_ADMIN, PROPERTY_MANAGER |
| Alert acknowledgment | Alert | PROPERTY_MANAGER, ELECTRICIAN |
| Alert resolution | Alert | PROPERTY_MANAGER, ELECTRICIAN |
| Risk export | RiskReport | INSURANCE_PARTNER |
| Tenant creation/deactivation | Tenant | PLATFORM_ADMIN |

### Retention
- Audit logs retained for 5 years (compliance requirement)
- No PII in audit details (references by ID only)

---

## 9. Secrets Management

### Rules
1. **No secrets in Git** — enforced by `.gitignore`
2. `application-secret.yml` MUST be in `.gitignore`
3. All secrets via environment variables
4. `application-secret.example.yml` provides template

### Secret Categories
| Category | Variable | Example |
|----------|----------|---------|
| Database | `DB_PASSWORD` | (generated) |
| Keycloak | `KEYCLOAK_CLIENT_SECRET` | (from Keycloak admin) |
| Kafka SASL | `KAFKA_SASL_PASSWORD` | (generated) |
| Redis | `REDIS_PASSWORD` | (generated) |
| MQTT | `MQTT_TLS_KEYSTORE_PASSWORD` | (generated) |
| SMTP | `SMTP_PASSWORD` | (service-specific) |
| Twilio | `TWILIO_AUTH_TOKEN` | (from Twilio) |
| Device signing | `DEVICE_HMAC_SECRET` | (32-byte random) |

### HashiCorp Vault Migration Path
```yaml
# application-secret.yml (future with Vault)
spring:
  cloud:
    vault:
      uri: https://vault.pyrosense.internal:8200
      authentication: KUBERNETES
      kubernetes:
        role: pyrosense-service
        service-account-token-file: /var/run/secrets/kubernetes.io/serviceaccount/token
      kv:
        backend: secret
        default-context: pyrosense
```

When Vault is deployed:
1. Store all secrets in `secret/pyrosense/<service-name>`
2. Add `spring-cloud-starter-vault-config` dependency
3. Remove environment variable references
4. Configure Vault AppRole or Kubernetes auth method
5. Enable secret rotation policies (30-day max for DB credentials)

---

## 10. Communication Security

| Channel | Development | Production |
|---------|-------------|------------|
| REST API | HTTP | HTTPS (TLS 1.3) |
| MQTT | TCP (plain) | TLS + client certificate |
| Kafka | PLAINTEXT | SASL/SCRAM-SHA-512 + TLS |
| PostgreSQL | Plain | SSL mode=verify-full |
| Redis | No auth | AUTH + TLS |
| Inter-service | HTTP | mTLS |

---

## 11. Network Architecture (Production)

```
                        ┌─────────────────────────┐
                        │   WAF / Load Balancer   │
                        │   (TLS termination)     │
                        └───────────┬─────────────┘
                                    │ HTTPS
                        ┌───────────▼─────────────┐
                        │   API Gateway :8080     │
                        │   Rate limiting, CORS   │
                        │   JWT validation        │
                        └───────────┬─────────────┘
                                    │ Internal (mTLS)
        ┌───────────────────────────┼───────────────────────────┐
        │               │               │               │       │
  ┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐  │
  │ Identity  │  │ Alerting  │  │   Risk    │  │  Device   │  ...
  │   :8081   │  │   :8086   │  │   :8085   │  │   :8082   │
  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘
        │               │               │               │
        └───────────────┴───────┬───────┴───────────────┘
                                │
              ┌─────────────────┼─────────────────┐
              │                 │                  │
        ┌─────▼─────┐   ┌─────▼─────┐   ┌───────▼───────┐
        │PostgreSQL │   │   Redis   │   │    Kafka      │
        │  (SSL)    │   │  (AUTH)   │   │ (SASL+TLS)   │
        └───────────┘   └───────────┘   └───────────────┘
```

All internal services are NOT exposed to the internet. Only the API Gateway is publicly accessible behind the WAF/Load Balancer.
