# PyroSense AI Platform - Security Architecture

## 1. Threat Model (STRIDE)

### S - Spoofing

| Threat | Mitigation |
|--------|------------|
| User identity spoofing | JWT authentication via Keycloak (RS256 signature verification). Short-lived tokens (1 hour). Refresh token rotation. |
| Device impersonation | HMAC-SHA256 device tokens. Per-device secrets with hash-only storage. Automatic rotation. |
| Tenant spoofing via headers | Gateway strips `X-Tenant-Id`, `X-User-Id`, `X-Roles` from all incoming requests. Tenant derived exclusively from JWT claims. |
| Service impersonation | Internal network only (no auth between services currently). Future: mTLS between all services. |

### T - Tampering

| Threat | Mitigation |
|--------|------------|
| Modified sensor readings | HMAC-SHA256 payload integrity. `X-Signature` header = HMAC(device_secret, timestamp + "." + body). Replay window: 5 minutes. |
| API parameter manipulation | Jakarta Bean Validation on all DTOs. Mass-assignment protection via Java records (immutable). |
| Token modification | RS256 signature verification via Keycloak JWKS endpoint. |
| Data in transit | TLS 1.3 enforced in production on all channels. |

### R - Repudiation

| Threat | Mitigation |
|--------|------------|
| Denial of user actions | AOP-based audit logging (`@Audited` annotation): captures userId, tenantId, action, resourceType, resourceId, IP, user-agent, timestamp. |
| Denial of data submission | Immutable ingestion log with device_id + timestamp + HMAC stored in append-only table. |
| Denial of admin operations | All mutations logged with SecurityContext userId. Kafka event trail for all domain events. |

### I - Information Disclosure

| Threat | Mitigation |
|--------|------------|
| PII in logs | No PII logged. Structured logging filters redact `Authorization` headers. Error responses return code + message only (no stack traces). |
| Credential leakage | All secrets via environment variables. `application-secret.yml` in `.gitignore`. No secrets in source code. |
| Cross-tenant data leak | `TenantContext` ThreadLocal enforcement. All repository queries filtered by tenant_id (enforced by ArchUnit tests). |
| Credential masking | All sensitive values masked in log outputs and actuator endpoints. |

### D - Denial of Service

| Threat | Mitigation |
|--------|------------|
| API request flooding | Redis-based rate limiting: 60 req/min (default), 10 req/min (auth), 120 req/min (device ingestion). |
| Large payload attacks | Payload size limit: 1 MB at gateway level, 8 KB at ingestion service. |
| Cascade failures | Circuit breaker pattern (Resilience4j) on all inter-service calls. |
| Resource exhaustion | Connection pooling (HikariCP max 10), thread pool limits, request timeouts. |

### E - Elevation of Privilege

| Threat | Mitigation |
|--------|------------|
| Self-escalation | Role assignment restricted to SUPER_ADMIN/ADMIN only. |
| Cross-tenant access | JWT `tenant_id` claim is authoritative. Never derived from request parameters or headers. |
| Unauthorized endpoint access | RBAC with 8 roles and 34 permissions. Method-level `@PreAuthorize` on all service operations. |
| Device accessing user data | DEVICE_MANAGER role scoped to device-related permissions only. |

---

## 2. Authentication

### Users: OAuth2/OIDC via Keycloak (RS256 JWT)

- Identity Provider: Keycloak (self-hosted, OIDC-compliant)
- Realm: `pyrosense`
- Client: `pyrosense-platform`
- Token signing: RS256 (asymmetric)
- Access token lifetime: 1 hour
- Refresh token rotation enabled
- Account lockout: 5 failed attempts, 30-minute auto-unlock

### Devices: HMAC-SHA256 Tokens

- 32-byte cryptographically random token generated at provisioning
- Only SHA-256 hash stored in database (plaintext never persisted)
- Token validated by hashing incoming token and comparing against stored hash
- Constant-time comparison to prevent timing attacks
- Auto-expiry configurable (default 90 days)

### Service-to-Service

- Current: Internal network only, no authentication between services
- Services communicate via HTTP on private network and Kafka events
- Future: mTLS between all services using service mesh (Istio)

---

## 3. Authorization (RBAC)

### Role Definitions

| Role | Description | Scope |
|------|-------------|-------|
| `SUPER_ADMIN` | Full platform control, cross-tenant access | Platform-wide |
| `ADMIN` | Tenant-level administration | Single tenant |
| `PROPERTY_MANAGER` | Manages buildings, devices, and alerts | Single tenant |
| `ELECTRICIAN` | Alert resolution and maintenance tasks | Single tenant |
| `BUILDING_OWNER` | Read-only access to property data and risk reports | Single tenant |
| `INSURER` | Risk data access and export capabilities | Single tenant |
| `DEVICE_MANAGER` | Device provisioning and management | Single tenant |
| `VIEWER` | Read-only dashboards and reports | Single tenant |

### Key Permissions by Role

| Permission | SUPER_ADMIN | ADMIN | PROPERTY_MANAGER | ELECTRICIAN | BUILDING_OWNER | INSURER | DEVICE_MANAGER | VIEWER |
|-----------|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| USER_CREATE | x | x | | | | | | |
| USER_DELETE | x | x | | | | | | |
| TENANT_CREATE | x | | | | | | | |
| TENANT_DELETE | x | | | | | | | |
| DEVICE_REGISTER | x | x | x | | | | x | |
| DEVICE_DECOMMISSION | x | x | x | | | | x | |
| TELEMETRY_INGEST | x | | | | | | x | |
| TELEMETRY_READ | x | x | x | x | x | | x | x |
| ALERT_CREATE | x | x | x | | | | | |
| ALERT_ACKNOWLEDGE | x | x | x | x | | | | |
| ALERT_RESOLVE | x | x | x | x | | | | |
| RISK_READ | x | x | x | | x | x | | x |
| RISK_EXPORT | x | x | | | | x | | |
| REPORT_GENERATE | x | x | x | | | | | |
| SYSTEM_CONFIG | x | | | | | | | |
| AUDIT_READ | x | x | | | | | | |

Total: 34 fine-grained permissions across 8 roles.

---

## 4. JWT Claims Structure

```json
{
  "sub": "user-uuid",
  "tenant_id": "tenant-uuid",
  "realm_access": { "roles": ["PROPERTY_MANAGER"] },
  "iat": 1705312245,
  "exp": 1705315845,
  "iss": "http://keycloak:8080/realms/pyrosense"
}
```

Key points:
- `sub`: User UUID (unique identifier)
- `tenant_id`: Custom claim for multi-tenant isolation (authoritative source)
- `realm_access.roles`: Keycloak standard claim for role assignment
- `iss`: Issuer validated against configured Keycloak realm URL
- `exp`: Token expiration (1 hour from issuance)
- Signature: RS256 verified against Keycloak JWKS endpoint

---

## 5. Tenant Isolation

### Design Principles

1. **Every query is tenant-scoped**: `tenant_id` included in WHERE clause of all database queries (enforced by ArchUnit tests)
2. **Tenant derived from JWT only**: Never from client-supplied headers or request parameters
3. **Gateway sanitization**: `X-Tenant-Id`, `X-User-Id`, and `X-Roles` headers are stripped from all incoming requests at the gateway before forwarding
4. **TenantContext (ThreadLocal)**: Set by `TenantContextFilter` after JWT validation; automatically cleared in finally block after every request
5. **Database model**: Shared schema with `tenant_id` column on every business table (no schema-per-tenant)
6. **Kafka events**: `tenant_id` included in event payload (not topic-level partitioning); consumers filter by tenant context

### Enforcement Chain

```
Client Request
    │
    ▼
API Gateway (strips tenant headers from request)
    │
    ▼
JWT Validation (extracts tenant_id from verified token)
    │
    ▼
TenantContextFilter (sets ThreadLocal TenantContext)
    │
    ▼
Service Layer (@PreAuthorize checks)
    │
    ▼
Repository Layer (all queries include WHERE tenant_id = :tenantId)
    │
    ▼
Response (TenantContext cleared in finally block)
```

---

## 6. Device Authentication Protocol

### Provisioning

1. Admin registers device via API (requires DEVICE_REGISTER permission)
2. Platform generates 32-byte cryptographically random token (`SecureRandom`)
3. Token returned to admin in registration response (displayed once)
4. Only SHA-256 hash of token is persisted in database
5. Plaintext token is never stored or logged

### Storage

- Database stores: `device_id`, `token_hash` (SHA-256), `created_at`, `expires_at`, `status`
- No plaintext token exists after the initial response
- Hash comparison uses constant-time algorithm to prevent timing attacks

### Usage

1. Device includes token in request: `Authorization: Bearer <token>`
2. Platform computes SHA-256 hash of received token
3. Hash compared against stored hash (constant-time comparison)
4. If match: request proceeds with device context set
5. If mismatch: 401 Unauthorized returned

### Rotation

1. Admin initiates rotation (or auto-rotation near expiry)
2. New 32-byte token generated, returned to admin once
3. New hash stored; old token remains valid for configurable grace period
4. After grace period: old token invalidated
5. Device must be reconfigured with new token during grace period

### Revocation

- Immediate invalidation: device token marked as REVOKED in database
- All subsequent requests with revoked token return 401
- Device is blocked from all platform access instantly
- Revocation logged in audit trail

---

## 7. Secrets Management

### Development

- `.env.docker` file with local-only defaults (non-sensitive values)
- Docker Compose reads environment from `.env.docker`
- Values are development defaults only (e.g., `postgres`/`postgres`)

### Production

- All secrets provided via environment variables
- Container orchestrator manages secret injection (Docker Secrets, K8s Secrets)
- No secret files mounted except through orchestrator mechanisms
- Secrets rotated via orchestrator without service restart where possible

### Rules

1. **Never in Git**: `application-secret.yml` listed in `.gitignore`
2. **No secrets in code**: No hardcoded credentials anywhere
3. **No secrets logged**: All sensitive values masked in outputs
4. **Template provided**: `application-secret.example.yml` documents required variables

### Future: HashiCorp Vault Integration

```yaml
spring:
  cloud:
    vault:
      uri: https://vault.pyrosense.internal:8200
      authentication: KUBERNETES
      kubernetes:
        role: pyrosense-service
      kv:
        backend: secret
        default-context: pyrosense
```

Migration path:
1. Deploy Vault with Kubernetes auth backend
2. Store all secrets in `secret/pyrosense/<service-name>`
3. Add `spring-cloud-starter-vault-config` dependency
4. Replace environment variable references with Vault paths
5. Enable secret rotation policies (30-day max for database credentials)

---

## 8. OWASP Top 10 Mitigations

| # | Vulnerability | Mitigation |
|---|---------------|------------|
| A01 | Broken Access Control | RBAC (8 roles, 34 permissions), method-level `@PreAuthorize`, tenant isolation via JWT, no direct object references (UUIDs only) |
| A02 | Cryptographic Failures | TLS 1.3 in production, RS256 JWT signatures, HMAC-SHA256 for device payloads, no secrets in code/logs |
| A03 | Injection | Spring Data JPA parameterized queries, no native queries with user input, Jakarta Bean Validation on all DTOs |
| A04 | Insecure Design | Hexagonal architecture, threat modeling (STRIDE), security controls in dedicated config classes, ArchUnit enforcement |
| A05 | Security Misconfiguration | Security headers enforced, actuator endpoints protected, CORS strict configuration, debug disabled in production |
| A06 | Vulnerable Components | OWASP Dependency Check in CI (CVSS < 7 gate), Dependabot alerts, regular dependency updates |
| A07 | Identification & Auth Failures | Keycloak with account lockout, short-lived JWTs, device token expiry, rate limiting on auth endpoints (10/min) |
| A08 | Software & Data Integrity Failures | HMAC-SHA256 on device payloads, signed JWTs, Flyway-managed schema migrations, CI/CD pipeline integrity |
| A09 | Security Logging & Monitoring Failures | AOP audit logging, structured JSON logs, Prometheus alerting (14 rules), Kafka event trail, correlation IDs |
| A10 | Server-Side Request Forgery | No user-controlled outbound URLs, internal services use hardcoded base URLs, webhook URLs restricted to allowlist |

---

## 9. Security Headers

Set by the API Gateway on all responses:

| Header | Value | Purpose |
|--------|-------|---------|
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains; preload` | Force HTTPS for 1 year |
| `X-Content-Type-Options` | `nosniff` | Prevent MIME sniffing |
| `X-XSS-Protection` | `0` | Disabled (CSP is sufficient, avoids legacy browser issues) |
| `Content-Security-Policy` | `default-src 'self'` | Restrict resource loading to same origin |
| `X-Frame-Options` | `DENY` | Prevent clickjacking (no framing allowed) |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Limit referrer information leakage |
| `Permissions-Policy` | `camera=(), microphone=(), geolocation=()` | Disable unnecessary browser features |

---

## 10. Actuator Protection

Spring Boot Actuator endpoints are secured by role:

| Endpoint | Access Level | Notes |
|----------|-------------|-------|
| `/actuator/health` | Public (unauthenticated) | Used by load balancers and orchestrators |
| `/actuator/info` | Public (unauthenticated) | Build info and version only |
| `/actuator/prometheus` | Network-restricted | Prometheus scrape only (internal network) |
| `/actuator/metrics` | ADMIN role required | Detailed metrics data |
| `/actuator/env` | Not exposed | Disabled in production |
| `/actuator/beans` | Not exposed | Disabled in production |
| `/actuator/heapdump` | Not exposed | Disabled in production |
| `/actuator/threaddump` | Not exposed | Disabled in production |
| All others | ADMIN role required or not exposed | Default deny policy |

Configuration:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: when-authorized
```
