# Device Provisioning Protocol

## Overview

The PyroSense device provisioning protocol enables secure enrollment of IoT sensors without exposing durable secrets. It uses a **claim token** pattern where a time-limited, single-use token bridges the gap between the management interface and the physical device.

## Workflow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Manager    │     │   Backend   │     │ Installer   │     │   Device    │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                    │                    │                    │
       │ 1. Register device │                    │                    │
       │───────────────────>│                    │                    │
       │                    │                    │                    │
       │ 2. Create claim    │                    │                    │
       │    token           │                    │                    │
       │───────────────────>│                    │                    │
       │    {token, expiry} │                    │                    │
       │<───────────────────│                    │                    │
       │                    │                    │                    │
       │ 3. Communicate token to installer       │                    │
       │─────────────────────────────────────────>                    │
       │                    │                    │                    │
       │                    │    4. Configure    │                    │
       │                    │       device       │                    │
       │                    │                    │───────────────────>│
       │                    │                    │                    │
       │                    │ 5. POST /provision │                    │
       │                    │<───────────────────────────────────────│
       │                    │                    │                    │
       │                    │──── Verify:        │                    │
       │                    │  • token valid     │                    │
       │                    │  • not expired     │                    │
       │                    │  • serial match    │                    │
       │                    │  • not revoked     │                    │
       │                    │                    │                    │
       │                    │ 6. {credentials}   │                    │
       │                    │───────────────────────────────────────>│
       │                    │                    │                    │
       │                    │                    │    7. Store creds  │
       │                    │                    │                    │
       │ 8. Activate device │                    │                    │
       │───────────────────>│                    │                    │
       │                    │                    │                    │
       │                    │                    │    9. Start MQTT   │
       │                    │                    │       heartbeat +  │
       │                    │                    │       telemetry    │
```

## Domain Model

### ClaimToken

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Token identifier |
| deviceId | UUID | Target device |
| tenantId | UUID | Owning tenant |
| tokenHash | String | SHA-256 hash of plain token |
| createdAt | Instant | Creation timestamp |
| expiresAt | Instant | Expiration (configurable, default 24h) |
| createdBy | String | Actor who created the token |
| consumed | boolean | Whether token has been used |
| consumedAt | Instant | When it was consumed |

### DeviceCredential

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Credential identifier |
| deviceId | UUID | Owning device |
| hmacKeyHash | String | SHA-256 hash of HMAC key |
| status | ACTIVE / REVOKED | Current status |
| issuedAt | Instant | Issuance timestamp |
| revokedAt | Instant | Revocation timestamp |
| version | int | Monotonically increasing version |

### DeviceProvisioningSession

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Session identifier |
| deviceId | UUID | Device being provisioned |
| tenantId | UUID | Target tenant |
| deviceSerial | String | Device serial number |
| deviceModel | String | Device model identifier |
| firmwareVersion | String | Firmware version reported |
| sourceIp | String | Request source IP |
| status | PENDING / COMPLETED / FAILED | Outcome |
| failureReason | String | Reason for failure |
| createdAt | Instant | Start of attempt |
| completedAt | Instant | End of attempt |

## REST API

### POST /api/v1/devices/{deviceId}/claim-token

Creates a single-use claim token for device provisioning.

**Access:** ADMIN, DEVICE_MANAGER, PLATFORM_ADMIN, TENANT_ADMIN

**Response:**
```json
{
  "token": "dGhpcyBpcyBhIHNlY3VyZSByYW5kb20gdG9rZW4",
  "expiresAt": "2025-01-16T10:00:00Z"
}
```

**Security:**
- Invalidates any existing active tokens for the device
- Token is shown only once (not stored in plain text)
- Default validity: 24 hours (configurable via `pyrosense.provisioning.claim-token-validity`)

### POST /api/v1/devices/provision

Called by the device itself to complete provisioning.

**Access:** Public (no JWT required — device has no credentials yet)

**Request:**
```json
{
  "claimToken": "dGhpcyBpcyBhIHNlY3VyZSByYW5kb20gdG9rZW4",
  "deviceSerial": "PYRO-2025-001234",
  "deviceModel": "PyroSense-v1",
  "firmwareVersion": "0.1.0"
}
```

**Response (success):**
```json
{
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "660e8400-e29b-41d4-a716-446655440001",
  "hmacKey": "YWJjZGVmZzEyMzQ1Njc4OTBhYmNkZWZn",
  "mqttBrokerUri": "tcp://broker.pyrosense.io",
  "mqttPort": 8883,
  "topicPrefix": "pyrosense/v1/660e8400.../550e8400..."
}
```

**Security:**
- Rate limited: max 10 failed attempts per IP per 15 minutes
- Token is single-use (consumed on success)
- HMAC key is shown only once
- All attempts logged (success + failure)
- Device serial must match registered device

### POST /api/v1/devices/{deviceId}/credentials/rotate

Rotates the device HMAC key. Revokes old credential immediately.

**Access:** ADMIN, DEVICE_MANAGER, PLATFORM_ADMIN

**Response:**
```json
{
  "hmacKey": "bmV3LWtleS1oZXJl",
  "version": 2
}
```

**Security:**
- New key shown only once
- Old key revoked immediately
- Version increments monotonically
- Audit trail recorded

### POST /api/v1/devices/{deviceId}/revoke

Revokes a device and all its credentials/tokens. Immediate effect.

**Access:** ADMIN, DEVICE_MANAGER, PLATFORM_ADMIN

**Request:**
```json
{
  "reason": "Device compromised"
}
```

**Effects:**
- All active credentials revoked
- All pending claim tokens invalidated
- Device status set to REVOKED
- Device can no longer authenticate to MQTT broker

## Domain Events

| Event | Published when | Kafka topic |
|-------|---------------|-------------|
| DeviceClaimTokenCreatedEvent | Claim token generated | device-events |
| DeviceProvisionedEvent | Device successfully provisioned | device-events |
| DeviceCredentialRotatedEvent | HMAC key rotated | device-events |
| DeviceRevokedEvent | Device revoked | device-events |

## Security Properties

### Token Security
- **Single-use:** Token is consumed atomically on first successful provisioning
- **Time-limited:** Default 24h validity, configurable
- **Hash-stored:** Only SHA-256 hash is persisted; plain token is never stored
- **Invalidation:** Creating a new token invalidates all previous ones for the device

### Credential Security
- **Never stored in plain:** Only SHA-256 hash of HMAC key is persisted
- **Shown once:** Plain key is returned only during issuance/rotation
- **Versioned:** Each credential has a monotonically increasing version
- **Immediate revocation:** Revoke takes effect immediately

### Brute Force Protection
- **IP-based rate limiting:** Max 10 failed attempts per IP per 15 minutes
- **Audit trail:** Every provisioning attempt (success or failure) is logged
- **Session tracking:** Full audit of device serial, model, firmware, IP per attempt
- **No timing oracle:** Token matching uses hash comparison (constant-time in SHA-256 check)

### Replay Protection
- **Single-use token:** Token cannot be reused after consumption
- **Session deduplication:** Each provisioning attempt creates a session record
- **Credential versioning:** Old credentials are immediately revoked on rotation

### Audit
- All claim token creations logged with actor
- All provisioning attempts logged with IP and outcome
- All credential rotations logged with version and actor
- All revocations logged with reason and actor
- No secrets appear in logs (hashes only)

## Configuration

```yaml
pyrosense:
  provisioning:
    claim-token-validity: PT24H  # ISO 8601 duration
  mqtt:
    broker-uri: tcp://broker.pyrosense.io
    port: 8883
```

## Database Schema

### claim_tokens
```sql
CREATE TABLE claim_tokens (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id),
    tenant_id UUID NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    consumed_at TIMESTAMPTZ
);
```

### device_credentials
```sql
CREATE TABLE device_credentials (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id),
    hmac_key_hash VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    issued_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    version INTEGER NOT NULL
);
```

### provisioning_sessions
```sql
CREATE TABLE provisioning_sessions (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    device_serial VARCHAR(50) NOT NULL,
    device_model VARCHAR(50),
    firmware_version VARCHAR(20) NOT NULL,
    source_ip VARCHAR(45),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);
```

## Future Enhancements

- **mTLS provisioning:** CSR-based certificate enrollment via ATECC608B
- **QR code tokens:** Encode claim token as QR for installer convenience
- **Batch provisioning:** Multi-device token generation for fleet deployments
- **Grace period on rotation:** Allow old key for N minutes after rotation
- **Geolocation verification:** Check provisioning IP against expected installation region
