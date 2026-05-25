# PyroSense AI Platform - API Documentation

## Base URL

All API requests are routed through the API Gateway:

```
http://localhost:8080/api/v1
```

In production, replace with your deployment domain (e.g., `https://api.pyrosense.io/api/v1`).

### Service Ports (Direct Access - Development Only)

| Service          | Port |
|------------------|------|
| API Gateway      | 8080 |
| Identity         | 8081 |
| Device           | 8082 |
| Ingestion        | 8083 |
| Signal Analysis  | 8084 |
| Risk Scoring     | 8085 |
| Alerting         | 8086 |
| Notification     | 8087 |
| Maintenance      | 8089 |
| Reporting        | 8091 |

---

## Authentication

All endpoints (except health checks) require a valid JWT Bearer token issued by Keycloak.

### Header

```
Authorization: Bearer <token>
```

### JWT Claims

| Claim                | Description                          |
|----------------------|--------------------------------------|
| `sub`                | User ID (UUID)                       |
| `tenant_id`         | Tenant the user belongs to (UUID)    |
| `realm_access.roles` | List of roles assigned to the user  |

### Roles

| Role              | Description                              |
|-------------------|------------------------------------------|
| ADMIN             | Full platform access                     |
| TENANT_ADMIN      | Manage tenant resources                  |
| DEVICE_MANAGER    | Register, provision, manage devices      |
| OPERATOR          | Monitor alerts, acknowledge, assign      |
| ELECTRICIAN       | View and complete interventions          |
| VIEWER            | Read-only access                         |

---

## Common Headers

| Header             | Direction | Description                                      |
|--------------------|-----------|--------------------------------------------------|
| `Authorization`    | Request   | Bearer token (required)                          |
| `Content-Type`     | Request   | `application/json` for all JSON payloads         |
| `X-Correlation-Id` | Both      | Auto-generated trace ID; returned in responses   |
| `X-Tenant-Id`      | Response  | Extracted from JWT; never client-provided        |
| `X-RateLimit-Limit`| Response  | Max requests per window                          |
| `X-RateLimit-Remaining` | Response | Remaining requests in current window       |

---

## Error Format (RFC 7807)

All errors follow the Problem Details specification:

```json
{
  "type": "https://api.pyrosense.io/errors/device-not-found",
  "title": "Device Not Found",
  "status": 404,
  "detail": "No device found with ID 550e8400-e29b-41d4-a716-446655440000",
  "correlationId": "abc12345-def6-7890-ghij-klmnopqrstuv"
}
```

---

## Endpoints

### Device Service

#### Register Device

```
POST /api/v1/devices
```

**Roles**: `ADMIN`, `DEVICE_MANAGER`

**Request Body:**

```json
{
  "serialNumber": "PYR-2025-001234",
  "firmwareVersion": "2.1.0",
  "model": "PyroSense-Pro-3P",
  "connectivityType": "WIFI",
  "metadata": {
    "manufacturer": "PyroTech",
    "hardwareRevision": "rev-C"
  }
}
```

**Response (201 Created):**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "serialNumber": "PYR-2025-001234",
  "firmwareVersion": "2.1.0",
  "model": "PyroSense-Pro-3P",
  "connectivityType": "WIFI",
  "status": "REGISTERED",
  "metadata": {
    "manufacturer": "PyroTech",
    "hardwareRevision": "rev-C"
  },
  "registeredAt": "2025-01-15T09:00:00Z",
  "registeredBy": "d290f1ee-6c54-4b01-90e6-d701748f0851"
}
```

---

#### Get Device

```
GET /api/v1/devices/{id}
```

**Roles**: Any authenticated user within the tenant

**Response (200 OK):**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "serialNumber": "PYR-2025-001234",
  "firmwareVersion": "2.1.0",
  "model": "PyroSense-Pro-3P",
  "connectivityType": "WIFI",
  "status": "ACTIVE",
  "tenantId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "buildingId": "b1c2d3e4-f5a6-7890-bcde-fa1234567890",
  "panelId": "c1d2e3f4-a5b6-7890-cdef-ab1234567890",
  "lastHeartbeat": "2025-01-15T10:29:00Z",
  "registeredAt": "2025-01-15T09:00:00Z",
  "provisionedAt": "2025-01-15T09:30:00Z",
  "activatedAt": "2025-01-15T10:00:00Z"
}
```

---

#### List Devices

```
GET /api/v1/devices?tenantId={tenantId}&status={status}
```

**Roles**: Any authenticated user within the tenant

**Query Parameters:**

| Parameter  | Type   | Required | Description                                      |
|------------|--------|----------|--------------------------------------------------|
| `tenantId` | UUID   | No       | Filter by tenant (auto-filtered for non-admins)  |
| `status`   | String | No       | REGISTERED, PROVISIONED, ACTIVE, REVOKED         |
| `page`     | int    | No       | Page number (default: 0)                         |
| `size`     | int    | No       | Page size (default: 20, max: 100)                |

**Response (200 OK):**

```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "serialNumber": "PYR-2025-001234",
      "model": "PyroSense-Pro-3P",
      "status": "ACTIVE",
      "lastHeartbeat": "2025-01-15T10:29:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 47,
  "totalPages": 3
}
```

---

#### Provision Device

```
POST /api/v1/devices/{id}/provision
```

**Roles**: `ADMIN`, `DEVICE_MANAGER`

**Request Body:**

```json
{
  "tenantId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "buildingId": "b1c2d3e4-f5a6-7890-bcde-fa1234567890",
  "panelId": "c1d2e3f4-a5b6-7890-cdef-ab1234567890",
  "installationLocation": "Building A, Floor 3, Panel 7B"
}
```

**Response (200 OK):**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PROVISIONED",
  "tenantId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "buildingId": "b1c2d3e4-f5a6-7890-bcde-fa1234567890",
  "panelId": "c1d2e3f4-a5b6-7890-cdef-ab1234567890",
  "installationLocation": "Building A, Floor 3, Panel 7B",
  "provisionedAt": "2025-01-15T09:30:00Z",
  "deviceToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

> **Note**: `deviceToken` is returned ONLY during provisioning. Store it securely on the device. The server stores only the SHA-256 hash.

---

#### Activate Device

```
POST /api/v1/devices/{id}/activate
```

**Roles**: `ADMIN`, `DEVICE_MANAGER`

**Response (200 OK):**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ACTIVE",
  "activatedAt": "2025-01-15T10:00:00Z"
}
```

---

#### Revoke Device

```
POST /api/v1/devices/{id}/revoke
```

**Roles**: `ADMIN`, `DEVICE_MANAGER`

**Request Body:**

```json
{
  "reason": "Device decommissioned - hardware failure"
}
```

**Response (200 OK):**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "REVOKED",
  "revokedAt": "2025-01-15T11:00:00Z",
  "revocationReason": "Device decommissioned - hardware failure"
}
```

---

#### Record Heartbeat

```
POST /api/v1/devices/{id}/heartbeat
```

**Roles**: Device token (M2M)

**Request Body:**

```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "uptimeSeconds": 86400,
  "firmwareVersion": "2.1.0",
  "signalStrength": -45,
  "freeMemoryBytes": 524288
}
```

**Response (204 No Content)**

---

### Ingestion Service

#### Submit Telemetry (REST Fallback)

```
POST /api/v1/telemetry
```

**Roles**: Device token (M2M)

> **Note**: The primary ingestion path is MQTT. This REST endpoint is a fallback for devices that cannot use MQTT.

**Request Body:**

```json
{
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-01-15T10:30:45.123Z",
  "firmwareVersion": "2.1.0",
  "sampleRate": 1000,
  "rmsCurrent": 12.45,
  "rmsVoltage": 230.1,
  "activePower": 2863.0,
  "reactivePower": 312.5,
  "powerFactor": 0.92,
  "thd": 4.7,
  "temperatureCelsius": 42.3,
  "hfNoiseLevel": 0.15,
  "microArcCount": 0,
  "transientCount": 2
}
```

**Response (202 Accepted):**

```json
{
  "accepted": true,
  "correlationId": "abc12345-def6-7890-ghij-klmnopqrstuv",
  "timestamp": "2025-01-15T10:30:45.200Z"
}
```

---

### Signal Analysis Service

#### Get Anomalies

```
GET /api/v1/analysis/anomalies/{deviceId}
```

**Roles**: `ADMIN`, `OPERATOR`, `TENANT_ADMIN`, `VIEWER`

**Query Parameters:**

| Parameter | Type     | Required | Description                        |
|-----------|----------|----------|------------------------------------|
| `from`    | ISO 8601 | No       | Start of time range                |
| `to`      | ISO 8601 | No       | End of time range                  |
| `type`    | String   | No       | ARC_FAULT, OVERLOAD, HARMONIC, THERMAL |
| `page`    | int      | No       | Page number (default: 0)           |
| `size`    | int      | No       | Page size (default: 20)            |

**Response (200 OK):**

```json
{
  "content": [
    {
      "id": "d1e2f3a4-b5c6-7890-def1-234567890abc",
      "deviceId": "550e8400-e29b-41d4-a716-446655440000",
      "type": "ARC_FAULT",
      "severity": "HIGH",
      "confidence": 0.94,
      "detectedAt": "2025-01-15T10:30:45Z",
      "metrics": {
        "hfNoiseLevel": 0.85,
        "microArcCount": 3,
        "baselineHfNoise": 0.15
      },
      "description": "High-frequency noise spike with micro-arc detection indicating potential arc fault"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 5,
  "totalPages": 1
}
```

---

#### Get Baseline Profile

```
GET /api/v1/analysis/baseline/{deviceId}
```

**Roles**: `ADMIN`, `OPERATOR`, `TENANT_ADMIN`, `VIEWER`

**Response (200 OK):**

```json
{
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "computedAt": "2025-01-15T00:00:00Z",
  "trainingPeriodDays": 30,
  "metrics": {
    "rmsCurrent": {
      "mean": 11.2,
      "stdDev": 1.8,
      "min": 8.5,
      "max": 14.1
    },
    "activePower": {
      "mean": 2580.0,
      "stdDev": 320.0,
      "min": 1950.0,
      "max": 3200.0
    },
    "thd": {
      "mean": 3.2,
      "stdDev": 0.9,
      "min": 1.8,
      "max": 5.5
    },
    "temperatureCelsius": {
      "mean": 38.5,
      "stdDev": 3.2,
      "min": 32.0,
      "max": 45.0
    },
    "hfNoiseLevel": {
      "mean": 0.12,
      "stdDev": 0.04,
      "min": 0.05,
      "max": 0.22
    }
  }
}
```

---

### Risk Scoring Service

#### Latest Risk Assessment

```
GET /api/v1/risk/devices/{deviceId}/latest
```

**Roles**: `ADMIN`, `OPERATOR`, `TENANT_ADMIN`, `VIEWER`

**Response (200 OK):**

```json
{
  "id": "e1f2a3b4-c5d6-7890-ef12-345678901abc",
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "riskScore": 72.5,
  "riskLevel": "HIGH",
  "computedAt": "2025-01-15T10:31:00Z",
  "factors": [
    {
      "name": "ARC_FAULT_PROBABILITY",
      "weight": 0.35,
      "value": 0.85,
      "contribution": 29.75
    },
    {
      "name": "THERMAL_STRESS",
      "weight": 0.25,
      "value": 0.60,
      "contribution": 15.0
    },
    {
      "name": "HARMONIC_DISTORTION",
      "weight": 0.20,
      "value": 0.55,
      "contribution": 11.0
    },
    {
      "name": "LOAD_ANOMALY",
      "weight": 0.20,
      "value": 0.84,
      "contribution": 16.75
    }
  ],
  "recommendation": "SCHEDULE_INSPECTION",
  "nextAssessmentAt": "2025-01-15T10:36:00Z"
}
```

---

#### Panel Risk History

```
GET /api/v1/risk/panels/{panelId}/history
```

**Roles**: `ADMIN`, `OPERATOR`, `TENANT_ADMIN`, `VIEWER`

**Query Parameters:**

| Parameter | Type     | Required | Description            |
|-----------|----------|----------|------------------------|
| `from`    | ISO 8601 | No       | Start of time range    |
| `to`      | ISO 8601 | No       | End of time range      |
| `interval`| String   | No       | HOUR, DAY, WEEK (default: DAY) |

**Response (200 OK):**

```json
{
  "panelId": "c1d2e3f4-a5b6-7890-cdef-ab1234567890",
  "interval": "DAY",
  "history": [
    {
      "timestamp": "2025-01-14T00:00:00Z",
      "avgRiskScore": 45.2,
      "maxRiskScore": 68.0,
      "riskLevel": "MEDIUM"
    },
    {
      "timestamp": "2025-01-15T00:00:00Z",
      "avgRiskScore": 62.8,
      "maxRiskScore": 72.5,
      "riskLevel": "HIGH"
    }
  ]
}
```

---

#### Building Risk Summary

```
GET /api/v1/risk/buildings/{buildingId}/summary
```

**Roles**: `ADMIN`, `OPERATOR`, `TENANT_ADMIN`, `VIEWER`

**Response (200 OK):**

```json
{
  "buildingId": "b1c2d3e4-f5a6-7890-bcde-fa1234567890",
  "buildingName": "Warehouse Complex A",
  "computedAt": "2025-01-15T10:31:00Z",
  "overallRiskScore": 58.3,
  "overallRiskLevel": "MEDIUM",
  "totalDevices": 24,
  "devicesByRiskLevel": {
    "CRITICAL": 1,
    "HIGH": 3,
    "MEDIUM": 8,
    "LOW": 12
  },
  "panels": [
    {
      "panelId": "c1d2e3f4-a5b6-7890-cdef-ab1234567890",
      "panelName": "Main Distribution Board",
      "riskScore": 72.5,
      "riskLevel": "HIGH",
      "deviceCount": 6
    }
  ],
  "trendDirection": "INCREASING",
  "trendPercentage": 12.5
}
```

---

### Alerting Service

#### List Alerts

```
GET /api/v1/alerts
```

**Roles**: `ADMIN`, `OPERATOR`, `TENANT_ADMIN`, `VIEWER`

**Query Parameters:**

| Parameter  | Type   | Required | Description                                   |
|------------|--------|----------|-----------------------------------------------|
| `tenantId` | UUID   | No       | Filter by tenant (auto-filtered for non-admins) |
| `status`   | String | No       | OPEN, ACKNOWLEDGED, ASSIGNED, RESOLVED, FALSE_POSITIVE |
| `severity` | String | No       | CRITICAL, HIGH, MEDIUM, LOW                   |
| `deviceId` | UUID   | No       | Filter by device                              |
| `page`     | int    | No       | Page number (default: 0)                      |
| `size`     | int    | No       | Page size (default: 20)                       |

**Response (200 OK):**

```json
{
  "content": [
    {
      "id": "f1a2b3c4-d5e6-7890-fa12-bcdef1234567",
      "tenantId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "deviceId": "550e8400-e29b-41d4-a716-446655440000",
      "type": "ARC_FAULT_RISK",
      "severity": "CRITICAL",
      "status": "OPEN",
      "title": "Critical Arc Fault Risk Detected",
      "description": "Device PYR-2025-001234 shows high probability of arc fault. Risk score: 72.5/100.",
      "riskScore": 72.5,
      "createdAt": "2025-01-15T10:31:05Z",
      "updatedAt": "2025-01-15T10:31:05Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 12,
  "totalPages": 1
}
```

---

#### Get Alert

```
GET /api/v1/alerts/{id}
```

**Roles**: `ADMIN`, `OPERATOR`, `TENANT_ADMIN`, `VIEWER`

**Response (200 OK):**

```json
{
  "id": "f1a2b3c4-d5e6-7890-fa12-bcdef1234567",
  "tenantId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "ARC_FAULT_RISK",
  "severity": "CRITICAL",
  "status": "ACKNOWLEDGED",
  "title": "Critical Arc Fault Risk Detected",
  "description": "Device PYR-2025-001234 shows high probability of arc fault. Risk score: 72.5/100.",
  "riskScore": 72.5,
  "buildingId": "b1c2d3e4-f5a6-7890-bcde-fa1234567890",
  "panelId": "c1d2e3f4-a5b6-7890-cdef-ab1234567890",
  "createdAt": "2025-01-15T10:31:05Z",
  "updatedAt": "2025-01-15T10:35:00Z",
  "acknowledgedAt": "2025-01-15T10:35:00Z",
  "acknowledgedBy": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "comments": [
    {
      "id": "a1b2c3d4-0000-1111-2222-333344445555",
      "author": "d290f1ee-6c54-4b01-90e6-d701748f0851",
      "authorName": "Jean Dupont",
      "content": "Acknowledged - scheduling inspection for tomorrow morning.",
      "createdAt": "2025-01-15T10:35:00Z"
    }
  ]
}
```

---

#### Active Critical Alerts

```
GET /api/v1/alerts/critical
```

**Roles**: `ADMIN`, `OPERATOR`, `TENANT_ADMIN`

**Response (200 OK):**

```json
{
  "alerts": [
    {
      "id": "f1a2b3c4-d5e6-7890-fa12-bcdef1234567",
      "deviceId": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Critical Arc Fault Risk Detected",
      "riskScore": 72.5,
      "status": "OPEN",
      "createdAt": "2025-01-15T10:31:05Z",
      "buildingName": "Warehouse Complex A",
      "panelName": "Main Distribution Board"
    }
  ],
  "totalCount": 3
}
```

---

#### Alert Statistics

```
GET /api/v1/alerts/statistics
```

**Roles**: `ADMIN`, `OPERATOR`, `TENANT_ADMIN`

**Query Parameters:**

| Parameter  | Type     | Required | Description         |
|------------|----------|----------|---------------------|
| `tenantId` | UUID     | No       | Filter by tenant    |
| `from`     | ISO 8601 | No       | Start of time range |
| `to`       | ISO 8601 | No       | End of time range   |

**Response (200 OK):**

```json
{
  "totalAlerts": 156,
  "byStatus": {
    "OPEN": 12,
    "ACKNOWLEDGED": 5,
    "ASSIGNED": 3,
    "RESOLVED": 128,
    "FALSE_POSITIVE": 8
  },
  "bySeverity": {
    "CRITICAL": 8,
    "HIGH": 24,
    "MEDIUM": 67,
    "LOW": 57
  },
  "meanTimeToAcknowledge": "PT12M30S",
  "meanTimeToResolve": "PT4H15M",
  "falsePositiveRate": 0.051
}
```

---

#### Acknowledge Alert

```
POST /api/v1/alerts/{id}/acknowledge
```

**Roles**: `ADMIN`, `OPERATOR`, `TENANT_ADMIN`

**Request Body:**

```json
{
  "comment": "Acknowledged - will investigate during next shift."
}
```

**Response (200 OK):**

```json
{
  "id": "f1a2b3c4-d5e6-7890-fa12-bcdef1234567",
  "status": "ACKNOWLEDGED",
  "acknowledgedAt": "2025-01-15T10:35:00Z",
  "acknowledgedBy": "d290f1ee-6c54-4b01-90e6-d701748f0851"
}
```

---

#### Assign Alert

```
POST /api/v1/alerts/{id}/assign
```

**Roles**: `ADMIN`, `OPERATOR`, `TENANT_ADMIN`

**Request Body:**

```json
{
  "assigneeId": "e390f2ff-7d65-5c12-a1f7-e812859f1962",
  "comment": "Assigning to electrician for on-site inspection."
}
```

**Response (200 OK):**

```json
{
  "id": "f1a2b3c4-d5e6-7890-fa12-bcdef1234567",
  "status": "ASSIGNED",
  "assignedTo": "e390f2ff-7d65-5c12-a1f7-e812859f1962",
  "assignedAt": "2025-01-15T10:40:00Z"
}
```

---

#### Resolve Alert

```
POST /api/v1/alerts/{id}/resolve
```

**Roles**: `ADMIN`, `OPERATOR`, `TENANT_ADMIN`, `ELECTRICIAN`

**Request Body:**

```json
{
  "resolution": "Loose connection identified and tightened on circuit breaker CB-7B. Arc fault risk eliminated.",
  "interventionId": "g1h2i3j4-k5l6-7890-mnop-qrstuvwxyz01"
}
```

**Response (200 OK):**

```json
{
  "id": "f1a2b3c4-d5e6-7890-fa12-bcdef1234567",
  "status": "RESOLVED",
  "resolvedAt": "2025-01-15T14:20:00Z",
  "resolvedBy": "e390f2ff-7d65-5c12-a1f7-e812859f1962",
  "resolution": "Loose connection identified and tightened on circuit breaker CB-7B. Arc fault risk eliminated."
}
```

---

#### Mark as False Positive

```
POST /api/v1/alerts/{id}/false-positive
```

**Roles**: `ADMIN`, `OPERATOR`, `TENANT_ADMIN`

**Request Body:**

```json
{
  "reason": "Triggered by planned maintenance activity - welding on adjacent circuit."
}
```

**Response (200 OK):**

```json
{
  "id": "f1a2b3c4-d5e6-7890-fa12-bcdef1234567",
  "status": "FALSE_POSITIVE",
  "markedAt": "2025-01-15T11:00:00Z",
  "markedBy": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "reason": "Triggered by planned maintenance activity - welding on adjacent circuit."
}
```

---

#### Add Comment

```
POST /api/v1/alerts/{id}/comments
```

**Roles**: `ADMIN`, `OPERATOR`, `TENANT_ADMIN`, `ELECTRICIAN`

**Request Body:**

```json
{
  "content": "On-site inspection scheduled for 2025-01-16 at 08:00. Electrician confirmed availability."
}
```

**Response (201 Created):**

```json
{
  "id": "a1b2c3d4-0000-1111-2222-333344445555",
  "alertId": "f1a2b3c4-d5e6-7890-fa12-bcdef1234567",
  "author": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "authorName": "Jean Dupont",
  "content": "On-site inspection scheduled for 2025-01-16 at 08:00. Electrician confirmed availability.",
  "createdAt": "2025-01-15T11:05:00Z"
}
```

---

### Notification Service

#### List Notifications

```
GET /api/v1/notifications?tenantId={tenantId}&status={status}
```

**Roles**: `ADMIN`, `TENANT_ADMIN`, `OPERATOR`

**Query Parameters:**

| Parameter     | Type   | Required | Description                      |
|---------------|--------|----------|----------------------------------|
| `tenantId`    | UUID   | No       | Filter by tenant                 |
| `status`      | String | No       | PENDING, SENT, DELIVERED, FAILED |
| `channel`     | String | No       | EMAIL, SMS, PUSH, WEBHOOK        |
| `page`        | int    | No       | Page number (default: 0)         |
| `size`        | int    | No       | Page size (default: 20)          |

**Response (200 OK):**

```json
{
  "content": [
    {
      "id": "n1o2p3q4-r5s6-7890-nopq-rstuvwxyz012",
      "tenantId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "alertId": "f1a2b3c4-d5e6-7890-fa12-bcdef1234567",
      "channel": "EMAIL",
      "recipientId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
      "recipientAddress": "jean.dupont@company.com",
      "subject": "[CRITICAL] Arc Fault Risk - Warehouse Complex A",
      "status": "DELIVERED",
      "sentAt": "2025-01-15T10:31:10Z",
      "deliveredAt": "2025-01-15T10:31:12Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3
}
```

---

#### Get Notification

```
GET /api/v1/notifications/{id}
```

**Roles**: `ADMIN`, `TENANT_ADMIN`, `OPERATOR`

**Response (200 OK):**

```json
{
  "id": "n1o2p3q4-r5s6-7890-nopq-rstuvwxyz012",
  "tenantId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "alertId": "f1a2b3c4-d5e6-7890-fa12-bcdef1234567",
  "channel": "EMAIL",
  "recipientId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "recipientAddress": "jean.dupont@company.com",
  "subject": "[CRITICAL] Arc Fault Risk - Warehouse Complex A",
  "body": "A critical arc fault risk has been detected on device PYR-2025-001234...",
  "status": "DELIVERED",
  "sentAt": "2025-01-15T10:31:10Z",
  "deliveredAt": "2025-01-15T10:31:12Z",
  "attempts": 1,
  "metadata": {
    "templateId": "critical-alert-email",
    "messageId": "msg-abc123@ses.amazonaws.com"
  }
}
```

---

#### Notifications by Recipient

```
GET /api/v1/notifications/recipient/{recipientId}
```

**Roles**: `ADMIN`, `TENANT_ADMIN`, `OPERATOR`

**Query Parameters:**

| Parameter | Type   | Required | Description                      |
|-----------|--------|----------|----------------------------------|
| `status`  | String | No       | PENDING, SENT, DELIVERED, FAILED |
| `page`    | int    | No       | Page number (default: 0)         |
| `size`    | int    | No       | Page size (default: 20)          |

**Response (200 OK):** Same paginated format as List Notifications.

---

#### Notification Statistics

```
GET /api/v1/notifications/statistics
```

**Roles**: `ADMIN`, `TENANT_ADMIN`

**Query Parameters:**

| Parameter  | Type     | Required | Description         |
|------------|----------|----------|---------------------|
| `tenantId` | UUID     | No       | Filter by tenant    |
| `from`     | ISO 8601 | No       | Start of time range |
| `to`       | ISO 8601 | No       | End of time range   |

**Response (200 OK):**

```json
{
  "totalNotifications": 1245,
  "byChannel": {
    "EMAIL": 890,
    "SMS": 210,
    "PUSH": 120,
    "WEBHOOK": 25
  },
  "byStatus": {
    "DELIVERED": 1180,
    "FAILED": 35,
    "PENDING": 30
  },
  "deliveryRate": 0.972,
  "avgDeliveryTimeMs": 2340
}
```

---

### Reporting Service

#### Generate Monthly Report

```
POST /api/v1/reports/monthly
```

**Roles**: `ADMIN`, `TENANT_ADMIN`

**Request Body:**

```json
{
  "tenantId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "buildingId": "b1c2d3e4-f5a6-7890-bcde-fa1234567890",
  "year": 2025,
  "month": 1,
  "includeDeviceDetails": true,
  "includeInterventionSummary": true
}
```

**Response (202 Accepted):**

```json
{
  "id": "r1s2t3u4-v5w6-7890-rstu-vwxyz0123456",
  "status": "GENERATING",
  "estimatedCompletionAt": "2025-01-15T10:35:00Z"
}
```

---

#### Get Report Metadata

```
GET /api/v1/reports/{id}
```

**Roles**: `ADMIN`, `TENANT_ADMIN`, `VIEWER`

**Response (200 OK):**

```json
{
  "id": "r1s2t3u4-v5w6-7890-rstu-vwxyz0123456",
  "tenantId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "buildingId": "b1c2d3e4-f5a6-7890-bcde-fa1234567890",
  "type": "MONTHLY",
  "title": "Monthly Risk Report - January 2025 - Warehouse Complex A",
  "status": "COMPLETED",
  "generatedAt": "2025-01-15T10:33:45Z",
  "period": {
    "year": 2025,
    "month": 1
  },
  "fileSizeBytes": 245780,
  "pageCount": 12
}
```

---

#### Get Download Token

```
GET /api/v1/reports/{id}/download-token
```

**Roles**: `ADMIN`, `TENANT_ADMIN`, `VIEWER`

**Response (200 OK):**

```json
{
  "token": "dGhpcyBpcyBhIHNlY3VyZSB0b2tlbg==",
  "expiresAt": "2025-01-15T10:45:00Z",
  "downloadUrl": "/api/v1/reports/r1s2t3u4-v5w6-7890-rstu-vwxyz0123456/download?token=dGhpcyBpcyBhIHNlY3VyZSB0b2tlbg=="
}
```

---

#### Download Report PDF

```
GET /api/v1/reports/{id}/download?token={token}
```

**Authentication**: Token-based (no Bearer required). Token from download-token endpoint.

**Response (200 OK):**
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="report-january-2025.pdf"`
- Binary PDF content

**Error (401 Unauthorized):**

```json
{
  "type": "https://api.pyrosense.io/errors/token-expired",
  "title": "Download Token Expired",
  "status": 401,
  "detail": "The download token has expired. Please request a new one.",
  "correlationId": "abc12345-def6-7890-ghij-klmnopqrstuv"
}
```

---

#### List Reports

```
GET /api/v1/reports?tenantId={tenantId}&type={type}
```

**Roles**: `ADMIN`, `TENANT_ADMIN`, `VIEWER`

**Query Parameters:**

| Parameter  | Type   | Required | Description                          |
|------------|--------|----------|--------------------------------------|
| `tenantId` | UUID   | No       | Filter by tenant                     |
| `type`     | String | No       | MONTHLY, QUARTERLY, ANNUAL, AD_HOC   |
| `page`     | int    | No       | Page number (default: 0)             |
| `size`     | int    | No       | Page size (default: 20)              |

**Response (200 OK):**

```json
{
  "content": [
    {
      "id": "r1s2t3u4-v5w6-7890-rstu-vwxyz0123456",
      "type": "MONTHLY",
      "title": "Monthly Risk Report - January 2025 - Warehouse Complex A",
      "status": "COMPLETED",
      "generatedAt": "2025-01-15T10:33:45Z",
      "fileSizeBytes": 245780
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 6,
  "totalPages": 1
}
```

---

#### Reports by Building

```
GET /api/v1/reports/building/{buildingId}
```

**Roles**: `ADMIN`, `TENANT_ADMIN`, `VIEWER`

**Response (200 OK):** Same paginated format as List Reports, filtered by building.

---

### Maintenance Service

#### List Interventions

```
GET /api/v1/interventions?tenantId={tenantId}&status={status}
```

**Roles**: `ADMIN`, `TENANT_ADMIN`, `OPERATOR`, `ELECTRICIAN`

**Query Parameters:**

| Parameter  | Type   | Required | Description                                                        |
|------------|--------|----------|--------------------------------------------------------------------|
| `tenantId` | UUID   | No       | Filter by tenant                                                   |
| `status`   | String | No       | CREATED, SCHEDULED, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED    |
| `assignee` | UUID   | No       | Filter by assigned electrician                                     |
| `page`     | int    | No       | Page number (default: 0)                                           |
| `size`     | int    | No       | Page size (default: 20)                                            |

**Response (200 OK):**

```json
{
  "content": [
    {
      "id": "g1h2i3j4-k5l6-7890-mnop-qrstuvwxyz01",
      "tenantId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "alertId": "f1a2b3c4-d5e6-7890-fa12-bcdef1234567",
      "deviceId": "550e8400-e29b-41d4-a716-446655440000",
      "type": "INSPECTION",
      "priority": "HIGH",
      "status": "ASSIGNED",
      "title": "Inspect potential arc fault - Panel 7B",
      "scheduledAt": "2025-01-16T08:00:00Z",
      "assignedTo": "e390f2ff-7d65-5c12-a1f7-e812859f1962",
      "createdAt": "2025-01-15T10:45:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 8,
  "totalPages": 1
}
```

---

#### Get Intervention

```
GET /api/v1/interventions/{id}
```

**Roles**: `ADMIN`, `TENANT_ADMIN`, `OPERATOR`, `ELECTRICIAN`

**Response (200 OK):**

```json
{
  "id": "g1h2i3j4-k5l6-7890-mnop-qrstuvwxyz01",
  "tenantId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "alertId": "f1a2b3c4-d5e6-7890-fa12-bcdef1234567",
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "INSPECTION",
  "priority": "HIGH",
  "status": "COMPLETED",
  "title": "Inspect potential arc fault - Panel 7B",
  "description": "High-frequency noise detected indicating possible arc fault on circuit breaker CB-7B.",
  "buildingId": "b1c2d3e4-f5a6-7890-bcde-fa1234567890",
  "panelId": "c1d2e3f4-a5b6-7890-cdef-ab1234567890",
  "scheduledAt": "2025-01-16T08:00:00Z",
  "assignedTo": "e390f2ff-7d65-5c12-a1f7-e812859f1962",
  "assigneeName": "Marc Electricien",
  "startedAt": "2025-01-16T08:15:00Z",
  "completedAt": "2025-01-16T09:30:00Z",
  "diagnostic": {
    "findings": "Loose terminal connection on phase L2 of CB-7B causing intermittent arcing.",
    "photos": ["photo-001.jpg", "photo-002.jpg"],
    "measurements": {
      "contactResistanceOhms": 0.45,
      "thermalHotspotCelsius": 68.2
    }
  },
  "result": {
    "action": "REPAIRED",
    "details": "Terminal retightened to 2.5 Nm. Thermal paste applied. Contact resistance now 0.02 ohms.",
    "partsUsed": ["Terminal lug M8", "Thermal compound 5g"],
    "recommendFollowUp": false
  },
  "riskImpact": {
    "riskScoreBefore": 72.5,
    "riskScoreAfter": 15.2,
    "reduction": 57.3
  },
  "createdAt": "2025-01-15T10:45:00Z",
  "updatedAt": "2025-01-16T09:30:00Z"
}
```

---

#### Schedule Intervention

```
POST /api/v1/interventions/{id}/schedule
```

**Roles**: `ADMIN`, `TENANT_ADMIN`, `OPERATOR`

**Request Body:**

```json
{
  "scheduledAt": "2025-01-16T08:00:00Z",
  "estimatedDurationMinutes": 120,
  "notes": "Access via loading dock entrance. Security badge required."
}
```

**Response (200 OK):**

```json
{
  "id": "g1h2i3j4-k5l6-7890-mnop-qrstuvwxyz01",
  "status": "SCHEDULED",
  "scheduledAt": "2025-01-16T08:00:00Z",
  "estimatedDurationMinutes": 120
}
```

---

#### Assign Electrician

```
POST /api/v1/interventions/{id}/assign
```

**Roles**: `ADMIN`, `TENANT_ADMIN`, `OPERATOR`

**Request Body:**

```json
{
  "electricianId": "e390f2ff-7d65-5c12-a1f7-e812859f1962",
  "notes": "Specialist in arc fault detection. Available from 08:00."
}
```

**Response (200 OK):**

```json
{
  "id": "g1h2i3j4-k5l6-7890-mnop-qrstuvwxyz01",
  "status": "ASSIGNED",
  "assignedTo": "e390f2ff-7d65-5c12-a1f7-e812859f1962",
  "assignedAt": "2025-01-15T11:00:00Z"
}
```

---

#### Start Work

```
POST /api/v1/interventions/{id}/start
```

**Roles**: `ELECTRICIAN`, `ADMIN`

**Request Body:**

```json
{
  "notes": "Arrived on site. Beginning inspection of Panel 7B."
}
```

**Response (200 OK):**

```json
{
  "id": "g1h2i3j4-k5l6-7890-mnop-qrstuvwxyz01",
  "status": "IN_PROGRESS",
  "startedAt": "2025-01-16T08:15:00Z"
}
```

---

#### Submit Diagnostic

```
POST /api/v1/interventions/{id}/diagnostic
```

**Roles**: `ELECTRICIAN`, `ADMIN`

**Request Body:**

```json
{
  "findings": "Loose terminal connection on phase L2 of CB-7B causing intermittent arcing.",
  "photos": ["photo-001.jpg", "photo-002.jpg"],
  "measurements": {
    "contactResistanceOhms": 0.45,
    "thermalHotspotCelsius": 68.2
  }
}
```

**Response (200 OK):**

```json
{
  "id": "g1h2i3j4-k5l6-7890-mnop-qrstuvwxyz01",
  "status": "IN_PROGRESS",
  "diagnostic": {
    "findings": "Loose terminal connection on phase L2 of CB-7B causing intermittent arcing.",
    "submittedAt": "2025-01-16T08:45:00Z"
  }
}
```

---

#### Complete Intervention

```
POST /api/v1/interventions/{id}/complete
```

**Roles**: `ELECTRICIAN`, `ADMIN`

**Request Body:**

```json
{
  "action": "REPAIRED",
  "details": "Terminal retightened to 2.5 Nm. Thermal paste applied. Contact resistance now 0.02 ohms.",
  "partsUsed": ["Terminal lug M8", "Thermal compound 5g"],
  "recommendFollowUp": false
}
```

**Response (200 OK):**

```json
{
  "id": "g1h2i3j4-k5l6-7890-mnop-qrstuvwxyz01",
  "status": "COMPLETED",
  "completedAt": "2025-01-16T09:30:00Z",
  "result": {
    "action": "REPAIRED",
    "details": "Terminal retightened to 2.5 Nm. Thermal paste applied. Contact resistance now 0.02 ohms."
  }
}
```

---

#### Record Risk Impact

```
POST /api/v1/interventions/{id}/risk-impact
```

**Roles**: `ADMIN`, `OPERATOR`

**Request Body:**

```json
{
  "riskScoreBefore": 72.5,
  "riskScoreAfter": 15.2,
  "assessedAt": "2025-01-16T10:00:00Z"
}
```

**Response (200 OK):**

```json
{
  "id": "g1h2i3j4-k5l6-7890-mnop-qrstuvwxyz01",
  "riskImpact": {
    "riskScoreBefore": 72.5,
    "riskScoreAfter": 15.2,
    "reduction": 57.3,
    "assessedAt": "2025-01-16T10:00:00Z"
  }
}
```

---

#### Cancel Intervention

```
POST /api/v1/interventions/{id}/cancel
```

**Roles**: `ADMIN`, `TENANT_ADMIN`, `OPERATOR`

**Request Body:**

```json
{
  "reason": "Alert resolved as false positive - maintenance activity on adjacent circuit."
}
```

**Response (200 OK):**

```json
{
  "id": "g1h2i3j4-k5l6-7890-mnop-qrstuvwxyz01",
  "status": "CANCELLED",
  "cancelledAt": "2025-01-15T12:00:00Z",
  "cancellationReason": "Alert resolved as false positive - maintenance activity on adjacent circuit."
}
```

---

#### Intervention Statistics

```
GET /api/v1/interventions/statistics
```

**Roles**: `ADMIN`, `TENANT_ADMIN`, `OPERATOR`

**Query Parameters:**

| Parameter  | Type     | Required | Description         |
|------------|----------|----------|---------------------|
| `tenantId` | UUID     | No       | Filter by tenant    |
| `from`     | ISO 8601 | No       | Start of time range |
| `to`       | ISO 8601 | No       | End of time range   |

**Response (200 OK):**

```json
{
  "totalInterventions": 89,
  "byStatus": {
    "CREATED": 2,
    "SCHEDULED": 5,
    "ASSIGNED": 3,
    "IN_PROGRESS": 1,
    "COMPLETED": 72,
    "CANCELLED": 6
  },
  "byResult": {
    "REPAIRED": 58,
    "REPLACED": 8,
    "NO_FAULT_FOUND": 4,
    "DEFERRED": 2
  },
  "avgCompletionTimeHours": 3.7,
  "avgRiskReduction": 42.8,
  "interventionsThisMonth": 12,
  "upcomingScheduled": 5
}
```

---

## Error Codes

| HTTP Status | Error Code                  | Description                                         |
|-------------|-----------------------------|-----------------------------------------------------|
| 400         | `VALIDATION_ERROR`          | Request body failed validation                      |
| 400         | `INVALID_STATE_TRANSITION`  | Resource cannot transition to requested state       |
| 400         | `PAYLOAD_TOO_LARGE`         | Request body exceeds size limit                     |
| 400         | `INVALID_TIME_RANGE`        | Time range parameters are invalid                   |
| 401         | `UNAUTHORIZED`              | Missing or invalid authentication token             |
| 401         | `TOKEN_EXPIRED`             | JWT or download token has expired                   |
| 403         | `FORBIDDEN`                 | Authenticated but insufficient permissions          |
| 403         | `TENANT_MISMATCH`           | Attempting to access resource from another tenant   |
| 404         | `RESOURCE_NOT_FOUND`        | Requested resource does not exist                   |
| 404         | `DEVICE_NOT_FOUND`          | Device with given ID not found                      |
| 404         | `ALERT_NOT_FOUND`           | Alert with given ID not found                       |
| 404         | `INTERVENTION_NOT_FOUND`    | Intervention with given ID not found                |
| 409         | `DUPLICATE_DEVICE`          | Device with serial number already exists            |
| 409         | `IDEMPOTENCY_CONFLICT`      | Duplicate telemetry submission detected             |
| 422         | `DEVICE_NOT_ACTIVE`         | Operation requires device in ACTIVE state           |
| 429         | `RATE_LIMIT_EXCEEDED`       | Too many requests                                   |
| 500         | `INTERNAL_ERROR`            | Unexpected server error                             |
| 502         | `SERVICE_UNAVAILABLE`       | Downstream service unreachable                      |
| 503         | `CIRCUIT_BREAKER_OPEN`      | Service temporarily unavailable (circuit breaker)   |

---

## Rate Limiting

Rate limits are enforced per client (identified by JWT subject claim).

| Category    | Limit         | Description                    |
|-------------|---------------|--------------------------------|
| General     | 60 req/min    | All standard API endpoints     |
| Auth        | 10 req/min    | Authentication endpoints       |
| Telemetry   | 120 req/min   | Telemetry ingestion endpoints  |

### Response Headers

| Header                   | Description                        |
|--------------------------|------------------------------------|
| `X-RateLimit-Limit`     | Maximum requests allowed per window |
| `X-RateLimit-Remaining` | Requests remaining in current window |
| `X-RateLimit-Reset`     | Unix timestamp when window resets   |

### Rate Limit Exceeded Response (429)

```json
{
  "type": "https://api.pyrosense.io/errors/rate-limit-exceeded",
  "title": "Rate Limit Exceeded",
  "status": 429,
  "detail": "You have exceeded the rate limit of 60 requests per minute. Please retry after 23 seconds.",
  "correlationId": "abc12345-def6-7890-ghij-klmnopqrstuv"
}
```
