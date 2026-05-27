# IoT Security — MVP 3 Real Device Protection

## Overview

This document covers the security architecture protecting communication between PyroSense real sensors and the cloud platform. It addresses 13 identified threats through layered defenses: device authentication, message integrity, anti-replay, rate limiting, and continuous audit.

## STRIDE Threat Model — IoT Communication

| # | Threat | STRIDE Category | Attack Vector | Impact | Likelihood | Risk |
|---|--------|-----------------|---------------|--------|------------|------|
| 1 | Unauthorized device | Spoofing | Fabricate deviceId, publish to MQTT | Inject false data, corrupt analysis | Medium | **HIGH** |
| 2 | Credential theft | Spoofing | Extract HMAC key from compromised device | Impersonate device indefinitely | Medium | **HIGH** |
| 3 | Replay attack | Tampering / Repudiation | Capture and re-send valid messages | Duplicate readings, corrupt scores | High | **HIGH** |
| 4 | Payload falsification | Tampering | Modify payload in-transit | Inject false electrical data, trigger false alarms | Medium | **HIGH** |
| 5 | Revoked device continues sending | Spoofing | Device not aware of revocation | Continue data flow from compromised device | Medium | **MEDIUM** |
| 6 | Tenant/deviceId usurpation | Spoofing | Subscribe/publish to another tenant's topic | Cross-tenant data corruption | Low | **CRITICAL** |
| 7 | Brute force claim token | Spoofing | Enumerate claim tokens during provisioning | Unauthorized device provisioning | Medium | **HIGH** |
| 8 | Obsolete firmware | Elevation of Privilege | Exploit known vulnerabilities in old firmware | Device compromise, data manipulation | Medium | **MEDIUM** |
| 9 | MQTT topic abuse | Tampering | Publish to topics outside assigned scope | Inject data for other devices | Low | **HIGH** |
| 10 | Logs exposing secrets | Information Disclosure | Access to log files reveals keys/tokens | Credential compromise | Low | **HIGH** |
| 11 | Injection via payload JSON | Tampering | Malformed JSON with extreme values | Crash processing, corrupt analysis | Medium | **MEDIUM** |
| 12 | DoS by payload flood | Denial of Service | Send thousands of messages per second | Exhaust processing capacity | High | **MEDIUM** |
| 13 | Compromised pilot device | Spoofing + Tampering | Physical access to device, extract key | Full impersonation of device | Low | **HIGH** |

## Risk Matrix

```
                    Impact
                    Low    Medium    High     Critical
Likelihood  High   │  M   │   H    │  VH    │  VH    │
            Medium │  L   │   M    │   H    │   H    │
            Low    │  L   │   L    │   M    │   H    │
```

| Risk Level | Threats | Mitigation Priority |
|------------|---------|---------------------|
| CRITICAL | #6 | Immediate: topic-payload coherence + tenant isolation |
| HIGH | #1, #2, #3, #4, #7, #9, #10, #13 | Sprint: layered authentication + integrity |
| MEDIUM | #5, #8, #11, #12 | Planned: monitoring + enforcement |

## Security Controls Implementation

### 1. Device Authentication (Threat #1, #6)

**Mechanism**: Claim token provisioning + per-device HMAC-SHA256 key

```
┌─────────────┐          ┌──────────────┐         ┌─────────────┐
│  Device     │          │  Cloud       │         │  Identity   │
│  (ESP32)    │          │  Ingestion   │         │  Service    │
└──────┬──────┘          └──────┬───────┘         └──────┬──────┘
       │                        │                        │
       │ POST /provision        │                        │
       │ {serial, claimToken}   │                        │
       ├───────────────────────►│ validate token         │
       │                        ├───────────────────────►│
       │                        │ issue HMAC key         │
       │                        │◄───────────────────────┤
       │ {hmacKey, topicPrefix} │                        │
       │◄───────────────────────┤                        │
       │                        │                        │
       │ MQTT publish (signed)  │                        │
       ├───────────────────────►│ verify HMAC            │
       │                        ├───────────────────────►│
       │                        │ ✓ valid                │
       │                        │◄───────────────────────┤
```

**Properties**:
- Claim token: single-use, 24h TTL, hash-stored (never plaintext after creation)
- HMAC key: 256-bit, shown once at provisioning, SHA-256 hash stored
- Device cannot publish without completing provisioning first
- Topic prefix enforces tenant/device scope: `pyrosense/v1/{tenantId}/{deviceId}/`

### 2. Message Integrity (Threat #4)

**Mechanism**: HMAC-SHA256 signature on every message

```json
{
  "deviceId": "dev-001",
  "tenantId": "tenant-001",
  "features": { ... },
  "security": {
    "nonce": "6839ab0100001069a3f2bc01",
    "signature": "a3f2bc01...64hex..."
  }
}
```

**Implementation** (`SignatureVerifier.java`):
- Signature computed over payload with signature field emptied
- Constant-time comparison prevents timing attacks
- Null/empty key disables verification (for test devices)
- Never logs the HMAC key or signature content

### 3. Anti-Replay Protection (Threat #3)

**Three-layer defense** (`AntiReplayGuard.java`):

| Layer | Mechanism | Store | TTL |
|-------|-----------|-------|-----|
| 1 | messageId uniqueness | Redis SET | 24h |
| 2 | Nonce uniqueness (24 hex chars) | Redis SET | 24h |
| 3 | Sequence number monotonicity | Redis HASH per device | Permanent |

**Guarantees**:
- Captured messages cannot be re-sent (nonce + messageId both checked)
- Out-of-order delivery detected (sequence regression)
- Clock drift tolerated: ±5 minutes future, 72h past in drain mode

### 4. Timestamp Tolerance (Threats #3, #8)

| Mode | Max Future | Max Past | Purpose |
|------|-----------|----------|---------|
| Normal | 300s | 300s | Real-time telemetry |
| Drain (isDrain=true) | 300s | 259,200s (72h) | Offline buffer flush |

### 5. Provisioning Security (Threat #7)

**Rate limiting**: 10 failed attempts per IP per 15 minutes

**Flow**:
1. Admin creates claim token (hash stored, plaintext shown once)
2. Device presents `{serial, claimToken, model, firmware}`
3. Cloud validates: token match → not expired → device not revoked
4. Token consumed atomically (single-use)
5. HMAC key issued, stored as hash
6. Session recorded for audit

### 6. Credential Rotation (Threat #2, #13)

**Properties**:
- `DeviceCredential.rotate(newTokenHash, newExpiresAt)` — atomic update
- Old credential immediately invalid after rotation
- Version counter incremented (audit trail)
- Rotation event logged (without key material)

### 7. Device Revocation (Threat #5)

**Pipeline position**: Step 2 (before signature check — no resources wasted)

**Enforcement**:
- `DeviceStatusChecker.checkDevice()` → `REVOKED` → immediate reject
- Revoked device messages logged as `DEVICE_REVOKED_ATTEMPT` security event
- Revocation is immediate and permanent until re-provisioned

### 8. Topic Authorization (Threat #9)

**Enforcement layers**:

| Layer | Mechanism | Location |
|-------|-----------|----------|
| 1 | Topic pattern extraction | MqttTelemetryListener regex |
| 2 | Topic ↔ payload coherence | PayloadValidator (topicDeviceId == payload.deviceId) |
| 3 | Device ↔ tenant binding | DeviceStatusChecker (registered pair) |
| 4 | MQTT broker ACL (future) | EMQX ACL rules per device |

**Current**: A device claiming `dev-001` in topic but `dev-002` in payload is immediately rejected with `TOPIC_PAYLOAD_MISMATCH`.

### 9. Payload Size Limit (Threat #12)

- **MQTT layer**: 8KB max (`MqttProtocolConstants.MAX_PAYLOAD_SIZE_BYTES`)
- **HTTP layer**: `PayloadSizeLimitFilter` (configurable, default 8KB for ingestion)
- Oversized payloads rejected before deserialization

### 10. Per-Device Rate Limiting (Threat #12)

**Implementation** (`DeviceRateLimiter.java`):
- Default: 120 messages per 60-second window per device
- Configurable via `pyrosense.security.device-rate-limit.*`
- Independent windows per device (no cross-device interference)
- Rejection code: `RATE_LIMITED`

### 11. Schema Validation (Threat #11)

**PayloadValidator** enforces:
- Schema version whitelist (`SUPPORTED_SCHEMA_VERSIONS`)
- All feature values within physical ranges (e.g., rmsCurrent 0-500A)
- Required fields (messageId, timestamp, firmwareVersion, features, security)
- Nonce exact length (24 hex chars)
- Sampling window bounds (1-60000ms)

### 12. Security Audit Trail (Threat #10)

**IoTSecurityAuditor** logs all security events with:
- Event type classification (12 categories)
- Masked device/tenant IDs (first 4 + last 4 chars only)
- Sanitized details (secrets regex-scrubbed)
- MDC context (securityEvent, deviceId, tenantId)

**Never logged**: HMAC keys, full payload content, claim tokens, signatures.

### 13. DLQ (Dead Letter Queue)

Failed messages routed to `ingestion-dlq` Kafka topic:
- Processing errors after validation passes
- Deserialization failures
- Downstream service unavailability

DLQ messages include: deviceId, error message (no raw payload in prod).

## Prometheus Alerting

Security-specific alerts (from `pyrosense-mvp3` group):

| Alert | Condition | Action |
|-------|-----------|--------|
| Mvp3SignatureInvalidSpike | >0.5/s for 2m | Investigate device compromise |
| Mvp3ReplayAttackDetected | >3 in 5m | Block device, check network |
| Mvp3NoRealDeviceData | 0 rate for 15m | Check MQTT broker + devices |

## Pre-Pilot Security Checklist

### Device Commissioning
- [ ] Each device has unique serial number
- [ ] Claim token generated per device (24h validity, single-use)
- [ ] HMAC key issued at provisioning — never transmitted again
- [ ] Device topic prefix verified matches tenant/device binding
- [ ] Firmware version recorded and tracked

### Cloud Infrastructure
- [ ] MQTT broker TLS enabled (port 8883)
- [ ] PayloadSizeLimitFilter active (8KB)
- [ ] Per-device rate limiting configured (120 msg/60s)
- [ ] Anti-replay stores operational (Redis nonce/sequence/messageId)
- [ ] DLQ consumer monitoring active
- [ ] Prometheus alerts enabled (pyrosense-mvp3 group)

### Credential Management
- [ ] No plaintext secrets in config files (env vars or Vault)
- [ ] HMAC keys stored as SHA-256 hashes only
- [ ] Claim tokens hash-stored, single-use enforced
- [ ] Credential rotation procedure documented and tested
- [ ] Revocation procedure tested (immediate effect)

### Monitoring & Audit
- [ ] Security audit logger active (`com.pyrosense.security.audit`)
- [ ] Grafana security dashboard accessible (mvp3-ingestion-security)
- [ ] Log output verified: no secrets, no raw payloads
- [ ] MDC fields present in structured logs (traceId, deviceId, tenantId)

### Network Security
- [ ] MQTT broker not exposed to public internet
- [ ] Device ↔ broker communication over WiFi WPA3 (or LoRaWAN)
- [ ] Cloud services in private network (VPC)
- [ ] Actuator endpoints require ADMIN role

### Incident Response
- [ ] Revocation procedure: admin can revoke device in <5 minutes
- [ ] Compromised device: revoke + rotate all keys in fleet
- [ ] Rate limit spike: investigate before increasing limits
- [ ] Replay attack: check physical device integrity

## Future Evolutions (mTLS / Secure Element)

### Current Architecture (MVP 3)
```
Device ─── HMAC-SHA256 signature ──→ Cloud (verify with stored hash)
```

### Target Architecture (Production)
```
Device (ATECC608B) ─── mTLS X.509 ──→ EMQX (cert validation) ──→ Cloud
                                         │
                                    ACL per device cert CN
                                         │
                                    OCSP/CRL revocation
```

### Migration Path
1. **Phase 1 (current)**: HMAC-SHA256 per-device keys, hash-stored
2. **Phase 2**: Add X.509 certificate alongside HMAC (dual validation)
3. **Phase 3**: ATECC608B secure element stores private key (never extractable)
4. **Phase 4**: Remove HMAC, pure mTLS with broker-level ACL

### Compatibility
- `PayloadValidator` already validates `schemaVersion` — v2 payloads can add cert fields
- `DeviceStatusChecker` interface abstracts auth — swap HMAC for cert lookup
- `ProtocolValidationPipeline` pipeline structure supports inserting new steps
- Topic pattern (`pyrosense/v1/{tenantId}/{deviceId}/`) compatible with broker ACL rules

## Test Coverage

### IoTSecurityTest.java (24 tests)

| Threat | Tests | Validation |
|--------|-------|------------|
| #1 Unauthorized device | 2 | Unknown device, wrong tenant |
| #3 Replay attack | 3 | Nonce reuse, duplicate messageId, sequence regression |
| #4 Payload falsification | 2 | Invalid HMAC, missing signature |
| #5 Revoked device | 1 | Immediate rejection |
| #6 Tenant spoofing | 2 | Topic mismatch (tenant + device) |
| #8 Obsolete firmware | 1 | Version tracking |
| #9 Timestamp violations | 3 | Future, too old, drain mode OK |
| #11 Schema/injection | 3 | Bad version, out-of-range, bad nonce |
| #12 Rate limiting | 2 | Enforced + independent per device |
| #10 Log security | 1 | Sanitizes secrets |
| Signature | 4 | Valid, tampered, null key, constant-time |

### Existing Tests (ProtocolValidationPipelineTest.java — 8 tests)

Validates end-to-end pipeline with fake implementations.

### Provisioning Tests (DeviceProvisioningServiceTest.java — 7 tests)

Validates claim token flow, rate limiting, brute force protection.
