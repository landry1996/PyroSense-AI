# PyroSense Dashboard Query API

## Overview

The Dashboard Service provides aggregated read-model endpoints consumed by the property manager dashboard. All endpoints enforce multi-tenant isolation via JWT `tenant_id` claims.

**Base URL:** `http://localhost:8088/api/v1/dashboard`  
**Authentication:** Bearer token (Keycloak JWT)  
**Performance target:** < 500ms for `/overview`

## Architecture

```
┌─────────────────────────────────────────────────────┐
│ REST Controller (adapter/in/rest)                    │
│   └─ DashboardController                            │
├─────────────────────────────────────────────────────┤
│ Use Cases (application/usecase)                     │
│   ├─ GetDashboardOverviewService                    │
│   ├─ GetRiskyBuildingsService                       │
│   ├─ GetRiskTrendService                            │
│   ├─ GetRecentAlertsService                         │
│   ├─ GetPriorityInterventionsService                │
│   └─ GetDeviceHealthService                         │
├─────────────────────────────────────────────────────┤
│ Ports (application/port)                            │
│   ├─ in: Query interfaces                           │
│   └─ out: DashboardReadModelPort, DashboardCachePort│
├─────────────────────────────────────────────────────┤
│ Adapters (adapter/out)                              │
│   ├─ JdbcDashboardReadModel (persistence)           │
│   ├─ RedisDashboardCache (cache)                    │
│   └─ DashboardCacheInvalidationListener (Kafka)     │
└─────────────────────────────────────────────────────┘
```

## Endpoints

### 1. GET /api/v1/dashboard/overview

Returns aggregated tenant-level statistics.

**Response:**
```json
{
  "tenantId": "uuid",
  "totalBuildings": 120,
  "totalDevices": 500,
  "activeDevices": 480,
  "offlineDevices": 20,
  "averageRiskScore": 34.5,
  "criticalAlerts": 5,
  "warningAlerts": 18,
  "openInterventions": 12,
  "overdueInterventions": 3,
  "lastUpdatedAt": "2026-05-26T10:00:00Z"
}
```

**Cache:** 30s TTL, invalidated on events.

---

### 2. GET /api/v1/dashboard/risky-buildings

Returns buildings sorted by risk score (descending).

**Query params:**
- `limit` (int, default 10, max 50)

**Response:**
```json
[
  {
    "buildingId": "uuid",
    "name": "Building A",
    "address": "1 Rue Example",
    "riskScore": 78.5,
    "status": "AT_RISK",
    "openAlerts": 5,
    "criticalAlerts": 2,
    "highestSeverity": "CRITICAL",
    "lastAlertAt": "2026-05-26T09:00:00Z"
  }
]
```

**Cache:** 60s TTL.

---

### 3. GET /api/v1/dashboard/risk-trend

Returns daily risk score evolution.

**Query params:**
- `period` (string, default "30d", max "90d")

**Response:**
```json
[
  {
    "date": "2026-05-01",
    "averageScore": 38.5,
    "maxScore": 72.0,
    "alertCount": 3
  }
]
```

**Cache:** 5min TTL.

---

### 4. GET /api/v1/dashboard/recent-alerts

Returns most recent alerts.

**Query params:**
- `limit` (int, default 10, max 50)

**Response:**
```json
[
  {
    "alertId": "uuid",
    "title": "Micro-arc detecte",
    "severity": "CRITICAL",
    "status": "OPEN",
    "type": "MICRO_ARC_DETECTED",
    "buildingId": "uuid",
    "buildingName": "Building A",
    "deviceId": "uuid",
    "createdAt": "2026-05-26T09:30:00Z",
    "slaBreached": false
  }
]
```

**Not cached** (real-time data).

---

### 5. GET /api/v1/dashboard/priority-interventions

Returns open interventions ordered by priority.

**Query params:**
- `limit` (int, default 10, max 50)

**Business rules:**
- ELECTRICIAN role: only sees their assigned interventions
- Other roles: sees all tenant interventions

**Response:**
```json
[
  {
    "interventionId": "uuid",
    "type": "CORRECTIVE",
    "priority": "CRITICAL",
    "status": "PLANNED",
    "buildingId": "uuid",
    "buildingName": "Building A",
    "assignedTo": "user-id",
    "scheduledDate": "2026-05-27",
    "createdAt": "2026-05-25T14:00:00Z",
    "overdue": true
  }
]
```

---

### 6. GET /api/v1/dashboard/device-health

Returns device fleet health summary.

**Response:**
```json
{
  "totalDevices": 500,
  "activeDevices": 480,
  "offlineDevices": 15,
  "provisionedDevices": 3,
  "revokedDevices": 2,
  "avgUptimePercent": 96.0,
  "devicesWithHighRisk": 12,
  "devicesSilentOver24h": 5
}
```

**Cache:** 60s TTL.

---

## Role-Based Access

| Role | Access |
|------|--------|
| PLATFORM_ADMIN | All endpoints (anonymized aggregates) |
| TENANT_ADMIN | All endpoints (own tenant) |
| PROPERTY_MANAGER | All endpoints (own tenant) |
| ELECTRICIAN | All endpoints, interventions filtered to assigned |
| SUPPORT_READONLY | All endpoints (read-only, own tenant) |

## Cache Invalidation

The service listens to Kafka topics and invalidates Redis cache entries:

| Event | Kafka Topic | Cache Invalidated |
|-------|-------------|-------------------|
| RiskScoreUpdatedEvent | `risk-score-updated` | overview, risky-buildings, risk-trend, device-health |
| AlertCreatedEvent | `alert-created` | overview, risky-buildings |
| AlertResolvedEvent | `alert-resolved` | overview, risky-buildings |
| InterventionCompletedEvent | `intervention-completed` | overview |
| DeviceOfflineDetectedEvent | `device-offline-detected` | overview, device-health |

## Database

Read-model tables (separate schema `pyrosense_dashboard`):
- `buildings` - Building projections with risk scores
- `devices` - Device status projections
- `alerts` - Alert projections
- `interventions` - Intervention projections
- `dashboard_risk_trend` - Daily aggregated risk data

All tables indexed for tenant isolation and common query patterns.

## Configuration

```yaml
pyrosense:
  dashboard:
    cache:
      overview-ttl: 30s
      risky-buildings-ttl: 60s
      risk-trend-ttl: 300s
      device-health-ttl: 60s
```

## Running

```bash
# Build
mvn clean package -pl pyrosense-dashboard-service -am

# Run
java -jar pyrosense-dashboard-service/target/pyrosense-dashboard-service-0.1.0-SNAPSHOT.jar

# Integration tests
mvn verify -pl pyrosense-dashboard-service -P test
```
