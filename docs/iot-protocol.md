# PyroSense AI Platform - IoT Protocol Documentation

## 1. Protocol Overview

PyroSense devices communicate with the platform via MQTT, chosen for its low overhead, reliable delivery guarantees, and suitability for constrained IoT devices deployed in electrical panels.

| Property           | Value                                          | Status   |
|--------------------|------------------------------------------------|----------|
| Protocol version   | MQTT 3.1.1 over TCP                           | MVP      |
| Future protocol    | MQTT 5.0 (shared subscriptions, flow control) | Planned  |
| Broker             | Eclipse Mosquitto (Docker: `eclipse-mosquitto:2`) | MVP   |
| Internal port      | 1883 (plain TCP)                               | MVP      |
| External port      | 1884 (Docker-mapped)                           | MVP      |
| TLS                | TLS 1.3 mandatory in production                | Production |
| Local development  | Plain TCP (no TLS)                             | MVP      |

### QoS Levels

| Message Type   | QoS Level | Rationale                                          |
|----------------|-----------|---------------------------------------------------|
| Telemetry      | QoS 1     | At-least-once; acceptable duplicate handling       |
| Commands       | QoS 2     | Exactly-once; prevents duplicate command execution |
| Heartbeat      | QoS 0     | Fire-and-forget; missed heartbeats are detected by timeout |

### Architecture

- Multi-tenant: all topics are scoped by `tenantId`
- Device authentication required for all connections
- ACL enforcement at the broker level

---

## 2. Topic Structure

All topics follow a standardized naming convention:

```
pyrosense/{tenantId}/{deviceId}/{category}/{type}
```

### Topic Definitions

| Topic                                                     | Direction          | Description                              | QoS |
|-----------------------------------------------------------|--------------------|------------------------------------------|-----|
| `pyrosense/{tenantId}/{deviceId}/telemetry/measurements`  | Device -> Platform | Periodic sensor data                     | 1   |
| `pyrosense/{tenantId}/{deviceId}/telemetry/events`        | Device -> Platform | Discrete events (arc detected, door open)| 1   |
| `pyrosense/{tenantId}/{deviceId}/status/heartbeat`        | Device -> Platform | Device alive signal                      | 0   |
| `pyrosense/{tenantId}/{deviceId}/status/health`           | Device -> Platform | Device health report                     | 1   |
| `pyrosense/{tenantId}/{deviceId}/command/config`          | Platform -> Device | Configuration push                       | 2   |
| `pyrosense/{tenantId}/{deviceId}/command/firmware`        | Platform -> Device | Firmware update commands                 | 2   |
| `pyrosense/{tenantId}/broadcast/config`                   | Platform -> Devices| Tenant-wide configuration broadcast      | 2   |

### Topic ACL Rules

Each device is authorized to:
- **Publish** to: `pyrosense/{its-tenant}/{its-id}/telemetry/#`, `pyrosense/{its-tenant}/{its-id}/status/#`
- **Subscribe** to: `pyrosense/{its-tenant}/{its-id}/command/#`, `pyrosense/{its-tenant}/broadcast/#`

Devices cannot publish or subscribe to topics belonging to other devices or tenants.

---

## 3. Payload Format (JSON)

All payloads are JSON-encoded UTF-8 strings. Every message includes a unique `messageId` (UUID v4), `deviceId`, `tenantId`, and ISO 8601 `timestamp`.

### Telemetry Measurement Payload

Published to `pyrosense/{tenantId}/{deviceId}/telemetry/measurements` at a configurable interval (default: 5 seconds for active monitoring, 60 seconds for standby).

```json
{
  "messageId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "deviceId": "dev-8a3b2c1d-4e5f-6789-abcd-ef0123456789",
  "tenantId": "tenant-1a2b3c4d-5e6f-7890-abcd-ef0123456789",
  "timestamp": "2025-03-10T14:00:00Z",
  "measurements": {
    "temperature": { "value": 42.5, "unit": "CELSIUS" },
    "current": { "value": 15.2, "unit": "AMPERE" },
    "vibration": { "value": 0.8, "unit": "G" },
    "humidity": { "value": 65.0, "unit": "PERCENT" }
  },
  "metadata": {
    "firmwareVersion": "1.2.3",
    "rssi": -45,
    "batteryLevel": 85
  }
}
```

### Event Payload

Published to `pyrosense/{tenantId}/{deviceId}/telemetry/events` when a discrete event occurs.

```json
{
  "messageId": "a1b2c3d4-e5f6-7890-abcd-ef0123456789",
  "deviceId": "dev-8a3b2c1d-4e5f-6789-abcd-ef0123456789",
  "tenantId": "tenant-1a2b3c4d-5e6f-7890-abcd-ef0123456789",
  "timestamp": "2025-03-10T14:05:12Z",
  "eventType": "ARC_DETECTED",
  "severity": "HIGH",
  "data": {
    "arcDuration_ms": 150,
    "peakCurrent": 45.2,
    "location": "phase_L1"
  },
  "metadata": {
    "firmwareVersion": "1.2.3",
    "rssi": -42
  }
}
```

Supported event types: `ARC_DETECTED`, `DOOR_OPEN`, `DOOR_CLOSED`, `OVERCURRENT`, `OVERTEMPERATURE`, `DEVICE_RESTART`, `CALIBRATION_COMPLETE`.

### Heartbeat Payload

Published to `pyrosense/{tenantId}/{deviceId}/status/heartbeat` at a fixed interval (default: 60 seconds).

```json
{
  "messageId": "b2c3d4e5-f6a7-8901-bcde-f01234567890",
  "deviceId": "dev-8a3b2c1d-4e5f-6789-abcd-ef0123456789",
  "tenantId": "tenant-1a2b3c4d-5e6f-7890-abcd-ef0123456789",
  "timestamp": "2025-03-10T14:01:00Z",
  "uptimeSeconds": 86400,
  "freeMemoryBytes": 524288,
  "rssi": -45
}
```

### Health Report Payload

Published to `pyrosense/{tenantId}/{deviceId}/status/health` periodically (default: every 5 minutes). Retained message.

```json
{
  "messageId": "c3d4e5f6-a7b8-9012-cdef-012345678901",
  "deviceId": "dev-8a3b2c1d-4e5f-6789-abcd-ef0123456789",
  "tenantId": "tenant-1a2b3c4d-5e6f-7890-abcd-ef0123456789",
  "timestamp": "2025-03-10T14:05:00Z",
  "status": "HEALTHY",
  "diagnostics": {
    "cpuUsagePercent": 32,
    "memoryUsagePercent": 61,
    "storageUsagePercent": 15,
    "temperatureInternal": 38.2,
    "uptimeSeconds": 86400,
    "lastRebootReason": "POWER_CYCLE"
  },
  "firmware": {
    "version": "1.2.3",
    "lastUpdateTimestamp": "2025-02-20T03:00:00Z"
  }
}
```

### Command Payload

Published by the platform to `pyrosense/{tenantId}/{deviceId}/command/config` or `command/firmware`.

```json
{
  "messageId": "d4e5f6a7-b8c9-0123-def0-123456789012",
  "commandType": "UPDATE_CONFIG",
  "timestamp": "2025-03-10T14:10:00Z",
  "parameters": {
    "telemetryInterval": 10,
    "heartbeatInterval": 30,
    "measurementFields": ["temperature", "current", "vibration"]
  },
  "responseRequired": true,
  "timeoutSeconds": 30
}
```

Firmware command example:

```json
{
  "messageId": "e5f6a7b8-c9d0-1234-ef01-234567890123",
  "commandType": "FIRMWARE_UPDATE",
  "timestamp": "2025-03-10T15:00:00Z",
  "parameters": {
    "version": "1.3.0",
    "downloadUrl": "https://firmware.pyrosense.io/v1.3.0/device-fw.bin",
    "sha256": "a1b2c3d4e5f6...full-hash",
    "forceUpdate": false
  },
  "responseRequired": true,
  "timeoutSeconds": 300
}
```

---

## 4. Device Authentication

### MVP: Username/Password

Each device authenticates using credentials generated during the provisioning process and stored in the device registry.

| Field      | Value                                |
|------------|--------------------------------------|
| Username   | `{deviceId}`                         |
| Password   | `{deviceToken}` (base64-encoded)     |
| Client ID  | `pyrosense-{deviceId}`               |

- Token: 32 bytes of cryptographic randomness, generated at provisioning
- Storage (server): SHA-256 hash only (plaintext never persisted)
- Storage (device): plaintext token in secure flash memory
- Expiry: configurable (default: 365 days)

### Planned: X.509 Client Certificates

Future releases will support mutual TLS with X.509 client certificates for stronger device identity verification. This eliminates the need for password-based authentication and provides hardware-bound identity when paired with secure elements (TPM/HSM).

### Credential Rotation

Devices receive credential rotation commands via `pyrosense/{tenantId}/{deviceId}/command/config`:

1. Platform sends `ROTATE_CREDENTIALS` command
2. Device acknowledges receipt
3. Platform generates new token, stores both old and new hashes (grace period: 24h)
4. New token delivered to device via command topic
5. Device reconnects with new token
6. Old token invalidated after grace period or successful new authentication

### ACL Enforcement

- Devices can only publish/subscribe to their own topics (tenant ID + device ID must match)
- Broker-level ACL validation on every PUBLISH and SUBSCRIBE
- Unauthorized access attempts are logged and trigger security events

---

## 5. Message Integrity & Idempotency

### Message Identification

Every message includes a unique `messageId` field (UUID v4). This enables:
- Deduplication at the ingestion layer
- End-to-end message tracing
- Correlation of command-response pairs

### Deduplication

- **Storage**: Redis SET with `messageId` as key
- **TTL**: 24 hours
- **Behavior on duplicate**: message acknowledged (PUBACK sent) but not processed further
- **Scope**: per-device deduplication (key: `{deviceId}:{messageId}`)

### Retained Messages

The following topics use MQTT retained messages so that the latest value is always available to new subscribers:

- `pyrosense/{tenantId}/{deviceId}/status/health` -- latest device health
- `pyrosense/{tenantId}/{deviceId}/status/heartbeat` -- latest heartbeat

---

## 6. Heartbeat & Connectivity

### Heartbeat Configuration

| Parameter                | Value       | Notes                                    |
|--------------------------|-------------|------------------------------------------|
| Heartbeat interval       | 60 seconds  | Configurable per device via command topic |
| Offline threshold        | 3 missed heartbeats (180s) | Device marked OFFLINE      |
| LWT topic                | `pyrosense/{tenantId}/{deviceId}/status/heartbeat` | Published on ungraceful disconnect |
| LWT payload              | `{"messageId":"...","deviceId":"...","tenantId":"...","timestamp":"...","status":"DISCONNECTED","reason":"UNGRACEFUL"}` | |
| LWT QoS                  | 1           | Ensures delivery to platform             |
| LWT retain               | true        | Latest status always available           |

### Last Will and Testament (LWT)

On connection, each device registers a Last Will message. If the device disconnects ungracefully (network loss, crash, power failure), the broker automatically publishes the LWT message, enabling immediate offline detection.

### Connectivity State Machine

```
ONLINE -> (3 missed heartbeats) -> OFFLINE
OFFLINE -> (heartbeat received) -> ONLINE
```

### Platform Events

When connectivity state changes, the platform publishes internal domain events:

- **DeviceOnlineEvent**: emitted when a device reconnects (heartbeat received after OFFLINE state)
- **DeviceOfflineEvent**: emitted after 3 missed heartbeats (180 seconds)

These events trigger:
- Device status update in the registry
- Notification to operators (configurable per tenant)
- Dashboard real-time update via WebSocket

---

## 7. Data Flow

```
+--------+       +----------------+       +-------------------+       +-------+
| Device | ----> | MQTT Broker    | ----> | Ingestion Service | ----> | Kafka |
|        |       | (Mosquitto)    |       |                   |       |       |
+--------+       +----------------+       +-------------------+       +-------+
                   port 1883/1884                                        |
                                                                         |
                                                         +---------------+---------------+
                                                         |               |               |
                                                         v               v               v
                                                  +-----------+   +----------+   +-----------+
                                                  | Signal    |   | Risk     |   | Alert     |
                                                  | Analysis  |   | Scoring  |   | Service   |
                                                  +-----------+   +----------+   +-----------+
                                                         |               |
                                                         v               v
                                                  +-----------------------------------+
                                                  |         TimescaleDB              |
                                                  +-----------------------------------+
```

### Flow Description

1. **Device** publishes telemetry/events/heartbeat to MQTT broker
2. **MQTT Broker** (Mosquitto) authenticates device, enforces ACL, routes messages
3. **Ingestion Service** subscribes to `pyrosense/+/+/telemetry/#` and `pyrosense/+/+/status/#`
4. Ingestion service validates, deduplicates, and publishes to **Kafka** topics
5. Downstream consumers (Signal Analysis, Risk Scoring, Alert Service) process from Kafka
6. Processed data is stored in **TimescaleDB** for time-series queries

---

## 8. Rate Limiting & Backpressure

### Device-Level Rate Limits

| Constraint                 | Value        | Action on Violation                        |
|----------------------------|--------------|--------------------------------------------|
| Max publish rate (telemetry)| 1 msg/sec   | Excess messages dropped, metric incremented|
| Max publish rate (events)  | 10 msg/sec   | Excess messages queued, alert if sustained |
| Max payload size           | 8 KB         | Message rejected (DISCONNECT)              |

### Broker-Level Rate Limiting

Rate limiting is enforced at the Mosquitto broker via a custom plugin:
- Per-client message rate tracking
- Configurable thresholds per message category
- Graceful rejection with MQTT reason codes

### Ingestion Service Backpressure

- Bounded internal queue (capacity: 1000 messages)
- When queue is full: ingestion service stops consuming from MQTT (TCP backpressure)
- Mosquitto buffers messages in its internal queue (max inflight: 20 per client)
- If broker buffer fills: messages are dropped per QoS semantics (QoS 0 dropped first)

---

## 9. Error Handling

### Malformed Payload

- JSON parsing failure: message dropped, `ingestion_errors_total{reason="deserialization"}` metric incremented
- Schema validation failure: message dropped, `ingestion_errors_total{reason="validation"}` metric incremented
- All dropped messages logged with device ID and raw payload (truncated) for debugging

### Authentication Failure

- Invalid credentials: connection rejected with MQTT CONNACK reason code `0x05` (Not authorized)
- Event logged: `security.authentication_failed` with device ID, IP address, timestamp
- Repeated failures (>5 in 1 minute): IP temporarily blocked

### QoS 1 Retry Behavior

- Broker retries on no PUBACK from ingestion service
- Max retries: 3
- Backoff: exponential (1s, 2s, 4s)
- After max retries: message stored in broker persistent queue for later delivery

### QoS 2 Retry Behavior (Commands)

- Full QoS 2 handshake: PUBLISH -> PUBREC -> PUBREL -> PUBCOMP
- Timeout per step: 10 seconds
- Max retries: 5
- On failure: command marked as FAILED, operator notified

---

## 10. Security Considerations

### Topic Security

- No sensitive data (tokens, credentials, PII) in topic names
- Topic names contain only UUIDs (not human-readable device names)
- Wildcard subscriptions restricted to platform services only

### Network Security

- **Production**: mandatory TLS 1.3 (port 8883)
- **Development**: plain TCP allowed (port 1883/1884)
- Network segmentation: MQTT broker deployed in DMZ
- Broker only accessible from internal services and authenticated devices

### Data Security

- Payload encryption at rest: planned (AES-256-GCM)
- Payload signing: HMAC-SHA256 using device token (planned for production)
- Audit trail: all command messages logged with full payload

### Monitoring

- Failed authentication attempts tracked and alerted
- Anomalous publish patterns detected (sudden rate increase, unusual topics)
- Broker health monitored: connected clients, message throughput, queue depth

---

## 11. IoT Simulator

The platform includes a Docker-based IoT simulator for development, testing, and demonstrations.

### Service Configuration

| Property              | Value                          |
|-----------------------|--------------------------------|
| Docker service name   | `pyrosense-iot-simulator`      |
| Image                 | Built from project Dockerfile  |
| Network               | Same Docker network as broker  |
| Broker connection     | `mqtt://mosquitto:1883`        |

### Capabilities

- Generates realistic telemetry data for N configurable simulated devices
- Each simulated device behaves as a real device: connects, authenticates, publishes telemetry, sends heartbeats
- Configurable parameters:
  - `DEVICE_COUNT`: number of simulated devices (default: 10)
  - `TELEMETRY_INTERVAL_MS`: milliseconds between telemetry messages (default: 5000)
  - `HEARTBEAT_INTERVAL_MS`: milliseconds between heartbeats (default: 60000)
  - `ANOMALY_INJECTION_RATE`: probability of anomalous readings per message (default: 0.05)

### Anomaly Injection

The simulator can inject realistic anomalies for testing the analysis pipeline:
- Temperature spikes (gradual increase simulating overheating)
- Current surges (sudden high-current events)
- Arc-like signal patterns (high-frequency noise bursts)
- Connectivity drops (simulated device going offline)

### Use Cases

- **Load testing**: simulate hundreds of devices to stress-test ingestion pipeline
- **Demo**: provide realistic data for sales demonstrations
- **Integration testing**: verify end-to-end message flow from device to dashboard
- **ML training**: generate labeled data with known anomalies for model training

---

## 12. Limitations (MVP)

### Protocol Limitations

- No MQTT 5.0 features: shared subscriptions, topic aliases, flow control, user properties
- No WebSocket transport (planned for browser-based device management)
- Plain TCP in development environment (TLS production-only)
- No message-level encryption (transport-level only via TLS)

### Hardware Limitations

- **No hardware certification**: devices are not certified for IEC 61439 (low-voltage switchgear assemblies) or NF C 15-100 (French electrical installation standard)
- **Simulated devices only**: no real sensor hardware integration in MVP
- **No secure element**: device tokens stored in plain flash (TPM/HSM planned)
- **No intrinsic safety certification**: not rated for hazardous environments (ATEX/IECEx)

### Scalability Limitations

- Single Mosquitto broker instance (no clustering in MVP)
- Broker state not replicated (single point of failure)
- Device limit: tested up to 1000 concurrent connections (production target: 10,000+)

### Planned Improvements

| Feature                        | Target Phase | Description                                      |
|--------------------------------|--------------|--------------------------------------------------|
| MQTT 5.0                       | Phase 2      | Shared subscriptions, flow control               |
| X.509 certificates             | Phase 2      | Hardware-bound device identity                   |
| Broker clustering              | Phase 2      | High availability with EMQX or HiveMQ           |
| Payload encryption             | Phase 2      | AES-256-GCM at message level                    |
| WebSocket transport            | Phase 3      | Browser-based device management                  |
| Real hardware integration      | Phase 3      | ESP32/STM32 firmware for physical sensors        |
| Hardware certification         | Phase 3      | IEC 61439, NF C 15-100 compliance               |
