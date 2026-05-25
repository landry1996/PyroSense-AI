# PyroSense API Gateway

## Responsibility
Single entry point for all client requests. Routes, authenticates, and rate-limits.

## Key Features
- Route-based service forwarding
- JWT validation (Keycloak)
- CORS configuration
- Request tracing (X-Request-Id)
- Rate limiting (Redis-based)

## Routes
| Path Pattern | Target Service |
|--------------|----------------|
| /api/v1/users/**, /api/v1/auth/** | identity-service:8081 |
| /api/v1/installations/**, /api/v1/sensors/** | device-service:8082 |
| /api/v1/signals/** | ingestion-service:8083 |
| /api/v1/analysis/**, /api/v1/baselines/** | signal-analysis-service:8084 |
| /api/v1/risk/** | risk-scoring-service:8085 |
| /api/v1/alerts/** | alerting-service:8086 |
| /api/v1/notifications/** | notification-service:8087 |
| /api/v1/reports/** | reporting-service:8088 |
| /api/v1/interventions/** | maintenance-service:8089 |

## Port
8080

## Dependencies
- Redis (rate limiting)
- Keycloak (JWT validation)
