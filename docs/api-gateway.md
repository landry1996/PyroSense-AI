# PyroSense API Gateway

## Role

Single entry point for all web, mobile, and partner clients. Routes requests to downstream microservices with authentication, rate limiting, header propagation, and security enforcement.

## Architecture

```
Client → API Gateway (8080) → Downstream Services (8081-8091)
                │
                ├── TenantHeaderSanitizationFilter (-20)  Strip spoofed headers
                ├── PayloadSizeLimitFilter (-15)           Reject oversized requests
                ├── RequestTracingFilter (-10)             Generate/propagate correlation ID
                ├── JwtHeaderPropagationFilter (-5)        Extract JWT → headers
                ├── SecurityHeadersFilter (-2)             Add security response headers
                └── RateLimitingFilter (0)                 Redis-based rate limiting
```

## Routes

| Route | Path Pattern | Target Service | Port |
|-------|--------------|----------------|------|
| identity-users | `/api/v1/users/**` | Identity Service | 8081 |
| identity-auth | `/api/v1/auth/**` | Identity Service | 8081 |
| identity-tenants | `/api/v1/tenants/**` | Identity Service | 8081 |
| devices | `/api/v1/devices/**` | Device Service | 8082 |
| ingestion | `/api/v1/ingestion/**` | Ingestion Service | 8083 |
| analysis | `/api/v1/analysis/**` | Signal Analysis | 8084 |
| scoring | `/api/v1/risk/**` | Risk Scoring | 8085 |
| alerting | `/api/v1/alerts/**` | Alerting Service | 8086 |
| notifications | `/api/v1/notifications/**` | Notification Service | 8087 |
| reporting | `/api/v1/reports/**` | Reporting Service | 8091 |
| maintenance | `/api/v1/interventions/**` | Maintenance Service | 8089 |

## Header Propagation

The gateway extracts information from the JWT and propagates as headers:

| Header | Source | Description |
|--------|--------|-------------|
| `X-Correlation-Id` | Generated or incoming | Unique request identifier for tracing |
| `X-Tenant-Id` | JWT `tenant_id` claim | Tenant context for multi-tenancy |
| `X-User-Id` | JWT `sub` claim | Authenticated user identifier |
| `X-Roles` | JWT `realm_access.roles` | Comma-separated role list |

## Security

### Authentication
- OAuth2 Resource Server with Keycloak JWT (RS256)
- Roles extracted from `realm_access.roles` or flat `roles` claim → `ROLE_*` authorities
- Permissions from `permissions` claim → `PERM_*` authorities

### Public Endpoints
- `GET /actuator/health` — Health check
- `GET /actuator/info` — Service info
- `POST /api/v1/auth/device/validate` — Device HMAC authentication
- `GET /api/v1/reports/*/download` — Token-gated report download

### Protected Endpoints
- `GET /actuator/**` (except health/info) — Requires `ROLE_ADMIN`
- All other `/**` — Requires authentication

### Header Sanitization
Incoming requests have `X-Tenant-Id`, `X-User-Id`, and `X-Roles` headers **stripped** before processing. The gateway only sets these from the authenticated JWT, preventing spoofing from untrusted clients.

### Error Masking
Internal server errors (5xx) are masked — clients receive generic messages like "Internal server error" with the correlation ID for support, but no stack traces, connection strings, or internal details.

## Rate Limiting

| Endpoint Type | Limit | Bucket |
|---------------|-------|--------|
| Auth (`/api/v1/auth/**`) | 10 req/min | auth |
| Device/Ingestion | 120 req/min | ingestion |
| All other | 60 req/min | default |

Response headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `Retry-After` (on 429).

Backend: Redis via `RateLimitStore` interface (pluggable).

## Payload Size Limit

Maximum: **1 MB** (configurable via `pyrosense.gateway.max-payload-bytes`).
Production: **512 KB**.

Returns HTTP 413 Payload Too Large with JSON error body.

## CORS

| Setting | Value |
|---------|-------|
| Origins | Configurable (`CORS_ORIGINS` env var) |
| Methods | GET, POST, PUT, PATCH, DELETE, OPTIONS |
| Headers | All (`*`) |
| Exposed | X-Correlation-Id, X-RateLimit-Limit, X-RateLimit-Remaining |
| Credentials | true |
| Max Age | 3600s (local), 86400s (prod) |

## Security Headers

| Header | Value |
|--------|-------|
| X-Frame-Options | DENY |
| Content-Security-Policy | default-src 'self'; frame-ancestors 'none' |
| X-Content-Type-Options | nosniff |
| X-XSS-Protection | 0 (modern approach) |
| Referrer-Policy | strict-origin-when-cross-origin |
| Permissions-Policy | camera=(), microphone=(), geolocation=() |
| Strict-Transport-Security | max-age=31536000; includeSubDomains; preload |
| Cache-Control | disabled |

## Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `KEYCLOAK_ISSUER` | JWT issuer URI | http://localhost:8180/realms/pyrosense |
| `REDIS_HOST` | Redis host | localhost |
| `REDIS_PORT` | Redis port | 6379 |
| `CORS_ORIGINS` | Allowed origins | http://localhost:3000 |
| `MAX_PAYLOAD_BYTES` | Max request body | 1048576 |
| `IDENTITY_SERVICE_URL` | Identity service URL | http://localhost:8081 |
| `DEVICE_SERVICE_URL` | Device service URL | http://localhost:8082 |
| `INGESTION_SERVICE_URL` | Ingestion service URL | http://localhost:8083 |
| `ANALYSIS_SERVICE_URL` | Analysis service URL | http://localhost:8084 |
| `SCORING_SERVICE_URL` | Scoring service URL | http://localhost:8085 |
| `ALERTING_SERVICE_URL` | Alerting service URL | http://localhost:8086 |
| `NOTIFICATION_SERVICE_URL` | Notification service URL | http://localhost:8087 |
| `REPORTING_SERVICE_URL` | Reporting service URL | http://localhost:8091 |
| `MAINTENANCE_SERVICE_URL` | Maintenance service URL | http://localhost:8089 |

## Profiles

| Profile | Use | Specifics |
|---------|-----|-----------|
| default | Development | Console logging, localhost URLs |
| local | Local with debug | Extended CORS origins, DEBUG logging |
| prod | Production | JSON structured logging, strict CORS, 512KB payload limit |
| test | Testing | Mocked JWT decoder, in-memory rate limit, no Redis |

## Structured Logging

- **local/test**: Human-readable console format
- **docker/prod**: JSON via logstash-logback-encoder with `correlationId` MDC field

Every request logs: method, path, status, duration, correlationId.

## Tests

- **Config**: SecurityConfigTest (5), GatewayConfigTest (2)
- **Filters**: RequestTracingFilterTest (3), TenantHeaderSanitizationFilterTest (5), PayloadSizeLimitFilterTest (4), RateLimitingFilterTest (6), SecurityHeadersFilterTest (2), ErrorMaskingFilterTest (4), JwtHeaderPropagationFilterTest (5), FilterOrderTest (3)
- **Integration**: GatewayApplicationTest (4) — context load, health, 401, info

Port: **8080**
