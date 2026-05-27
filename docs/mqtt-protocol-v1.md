# PyroSense MQTT Protocol v1 — Specification

**Date :** 2026-05-27
**Version :** 1.0
**Statut :** Implementation reference
**Scope :** Contrat MQTT entre firmware ESP32-S3 et backend Java/Spring Boot

---

## Table des Matieres

1. [Topics MQTT](#1-topics-mqtt)
2. [Payloads](#2-payloads)
3. [Securite](#3-securite)
4. [Regles de validation backend](#4-regles-de-validation-backend)
5. [QoS et fiabilite](#5-qos-et-fiabilite)
6. [Erreurs et rejets](#6-erreurs-et-rejets)
7. [Compatibilite et versioning](#7-compatibilite-et-versioning)
8. [Exemples JSON complets](#8-exemples-json-complets)

---

## 1. Topics MQTT

### Structure generale

```
pyrosense/v1/{tenantId}/{deviceId}/{messageType}
```

| # | Topic | Direction | QoS | Description |
|---|-------|:---------:|:---:|-------------|
| 1 | `pyrosense/v1/{tenantId}/{deviceId}/telemetry` | Device → Cloud | 1 | Features extraites (periodique, chaque 5s) |
| 2 | `pyrosense/v1/{tenantId}/{deviceId}/heartbeat` | Device → Cloud | 0 | Status device (chaque 60s) |
| 3 | `pyrosense/v1/{tenantId}/{deviceId}/events` | Device → Cloud | 1 | Evenements electriques urgents |
| 4 | `pyrosense/v1/provisioning/{claimToken}` | Device → Cloud | 1 | Demande d'enrollment |
| 5 | `pyrosense/v1/{tenantId}/{deviceId}/commands` | Cloud → Device | 2 | Commandes cloud (config, OTA, revoke) |
| 6 | `pyrosense/v1/{tenantId}/{deviceId}/command-acks` | Device → Cloud | 1 | Acquittement de commandes |

### Regles

- `{tenantId}` : UUID v4 ou slug alphanumérique (max 64 chars)
- `{deviceId}` : UUID v4 ou identifiant unique (max 64 chars)
- `{claimToken}` : Token ephemere genere lors du pre-enregistrement (24h TTL)
- Wildcard ACL : un device ne peut publier que sur SES topics (verifiable par cert CN)

---

## 2. Payloads

### 2.1 TelemetryPayload (Device → Cloud)

```json
{
  "schemaVersion": "1.0",
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "deviceId": "pyro-dev-001",
  "tenantId": "tenant-lab-01",
  "timestamp": "2026-05-27T14:30:05.123Z",
  "sequenceNumber": 4201,
  "firmwareVersion": "0.1.0",
  "samplingWindowMs": 1000,
  "features": {
    "rmsCurrent": 14.52,
    "rmsVoltage": 230.1,
    "activePower": 3245.8,
    "reactivePower": 312.4,
    "powerFactor": 0.94,
    "thd": 4.7,
    "temperatureCelsius": 39.5,
    "hfNoiseLevel": 0.18,
    "microArcCount": 0,
    "transientCount": 1,
    "signalQuality": 0.97
  },
  "security": {
    "nonce": "6839ab0100001069a3f2bc01",
    "signature": "a1b2c3d4e5f6..."
  }
}
```

| Champ | Type | Obligatoire | Validation |
|-------|------|:-----------:|------------|
| schemaVersion | string | Oui | "1.0" (versions supportees: ["1.0"]) |
| messageId | string (UUID) | Oui | UUID v4 format, unique par message |
| deviceId | string | Oui | Doit correspondre au topic |
| tenantId | string | Oui | Doit correspondre au topic |
| timestamp | ISO-8601 | Oui | Max 5 min dans le futur, max 72h dans le passe (si drain) |
| sequenceNumber | uint32 | Oui | Monotone croissant par device |
| firmwareVersion | string | Oui | Semver |
| samplingWindowMs | uint16 | Oui | 1-60000 |
| features.rmsCurrent | float | Oui | 0-500 A |
| features.rmsVoltage | float | Oui | 0-1000 V |
| features.activePower | float | Oui | 0-500000 W |
| features.reactivePower | float | Oui | 0-500000 VAR |
| features.powerFactor | float | Oui | 0.0-1.0 |
| features.thd | float | Oui | 0-100 % |
| features.temperatureCelsius | float | Oui | -40 - 200 °C |
| features.hfNoiseLevel | float | Oui | 0.0-1.0 |
| features.microArcCount | uint16 | Oui | 0-65535 |
| features.transientCount | uint16 | Oui | 0-65535 |
| features.signalQuality | float | Oui | 0.0-1.0 |
| security.nonce | string (hex) | Oui | 24 chars hex, unique, non reutilisable |
| security.signature | string (hex) | Oui | HMAC-SHA256 sur payload sans le champ signature |

### 2.2 HeartbeatPayload (Device → Cloud)

```json
{
  "schemaVersion": "1.0",
  "deviceId": "pyro-dev-001",
  "tenantId": "tenant-lab-01",
  "timestamp": "2026-05-27T14:30:00Z",
  "sequenceNumber": 701,
  "firmwareVersion": "0.1.0",
  "state": "ACTIVE",
  "uptimeSeconds": 259200,
  "freeHeapBytes": 184320,
  "wifiRssiDbm": -42,
  "bufferUsagePercent": 0,
  "signalQuality": 0.95,
  "cpuPercent": 23,
  "internalTempCelsius": 45
}
```

| Champ | Type | Obligatoire | Validation |
|-------|------|:-----------:|------------|
| schemaVersion | string | Oui | "1.0" |
| deviceId | string | Oui | Match topic |
| tenantId | string | Oui | Match topic |
| timestamp | ISO-8601 | Oui | Max 5 min tolerance |
| sequenceNumber | uint32 | Oui | Monotone croissant |
| firmwareVersion | string | Oui | Semver |
| state | enum | Oui | BOOTING, PROVISIONING, CONNECTING, ACTIVE, OFFLINE_BUFFERING, DEGRADED, ERROR |
| uptimeSeconds | uint32 | Oui | >= 0 |
| freeHeapBytes | uint32 | Oui | >= 0 |
| wifiRssiDbm | int8 | Oui | -120 to 0 |
| bufferUsagePercent | uint8 | Oui | 0-100 |
| signalQuality | float | Oui | 0.0-1.0 |
| cpuPercent | uint8 | Non | 0-100 |
| internalTempCelsius | int8 | Non | -40 to 120 |

### 2.3 DeviceEventPayload (Device → Cloud)

```json
{
  "schemaVersion": "1.0",
  "messageId": "660e8400-e29b-41d4-a716-446655440001",
  "deviceId": "pyro-dev-001",
  "tenantId": "tenant-lab-01",
  "timestamp": "2026-05-27T14:30:12.034Z",
  "sequenceNumber": 4202,
  "firmwareVersion": "0.1.0",
  "eventType": "ARC_DETECTED",
  "severity": "HIGH",
  "data": {
    "arcEnergy": 0.45,
    "peakAmplitude": 2.3,
    "durationUs": 150,
    "phase": "L1"
  },
  "context": {
    "rmsCurrent": 14.82,
    "temperatureCelsius": 38.2,
    "signalQuality": 0.92
  },
  "security": {
    "nonce": "6839ab0200001070b4e3cd02",
    "signature": "b2c3d4e5f6a7..."
  }
}
```

| Champ | Type | Validation |
|-------|------|------------|
| eventType | enum | ARC_DETECTED, OVERLOAD, OVERHEATING, VOLTAGE_SAG, VOLTAGE_SWELL, PHASE_LOSS |
| severity | enum | LOW, MEDIUM, HIGH, CRITICAL |
| data | object | Contenu depend de eventType |
| context | object | Snapshot mesures au moment de l'event |

### 2.4 ProvisioningRequestPayload (Device → Cloud)

```json
{
  "schemaVersion": "1.0",
  "claimToken": "abc123def456...",
  "deviceSerial": "PYRO-2026-00042",
  "firmwareVersion": "0.1.0",
  "capabilities": {
    "sensors": ["CT_30A", "PT100", "ARC_DETECT"],
    "features": ["RMS", "THD", "PF", "ARC", "TEMP"],
    "payloadVersion": "1.0",
    "bufferHours": 72,
    "compressionSupported": false
  }
}
```

### 2.5 ProvisioningResponsePayload (Cloud → Device)

```json
{
  "schemaVersion": "1.0",
  "status": "ENROLLED",
  "deviceId": "pyro-dev-001",
  "tenantId": "tenant-lab-01",
  "mqttConfig": {
    "brokerUri": "mqtts://mqtt.pyrosense.io",
    "port": 8883,
    "keepaliveSec": 60
  },
  "telemetryConfig": {
    "featureIntervalMs": 5000,
    "heartbeatIntervalMs": 60000,
    "healthIntervalMs": 300000
  },
  "securityConfig": {
    "hmacKeyHex": "a1b2c3...64chars",
    "signatureRequired": true
  }
}
```

### 2.6 DeviceCommandPayload (Cloud → Device)

```json
{
  "schemaVersion": "1.0",
  "commandId": "cmd-550e8400-e29b-41d4",
  "commandType": "UPDATE_CONFIG",
  "issuedAt": "2026-05-27T14:30:00Z",
  "expiresAt": "2026-05-27T14:35:00Z",
  "payload": {
    "featureIntervalMs": 10000,
    "heartbeatIntervalMs": 120000
  }
}
```

| commandType | Description | Payload |
|-------------|-------------|---------|
| UPDATE_CONFIG | Changer intervalles/seuils | `{featureIntervalMs, heartbeatIntervalMs, arcThreshold, ...}` |
| REBOOT | Reboot gracieux | `{}` |
| OTA_AVAILABLE | Firmware update dispo | `{firmwareUrl, sha256, sizeBytes}` |
| ROTATE_CERT | Rotation certificat | `{challengeNonce, timeoutSeconds}` |
| REVOKE | Revoquer le device | `{reason}` |
| SET_CALIBRATION | Calibration capteurs | `{gain: [...], offset: [...]}` |

### 2.7 CommandAckPayload (Device → Cloud)

```json
{
  "schemaVersion": "1.0",
  "commandId": "cmd-550e8400-e29b-41d4",
  "deviceId": "pyro-dev-001",
  "tenantId": "tenant-lab-01",
  "timestamp": "2026-05-27T14:30:01Z",
  "status": "ACCEPTED",
  "message": ""
}
```

| status | Signification |
|--------|---------------|
| ACCEPTED | Commande recue et en cours de traitement |
| COMPLETED | Commande executee avec succes |
| REJECTED | Commande refusee (raison dans message) |
| FAILED | Execution echouee (raison dans message) |

---

## 3. Securite

### 3.1 Signature HMAC-SHA256

Chaque message telemetry et event est signe :

1. **Construction du contenu a signer** : payload JSON complet SANS le champ `security.signature`
2. **Calcul** : `HMAC-SHA256(content_bytes, shared_secret_key)`
3. **Encodage** : hex lowercase (64 chars)

```
Signer:
  input  = {"schemaVersion":"1.0",...,"security":{"nonce":"...","signature":""}}
  key    = device_hmac_key (32 bytes)
  output = hex(HMAC-SHA256(input, key))
```

### 3.2 Anti-replay

| Mecanisme | Verification | Rejet si |
|-----------|-------------|----------|
| **Nonce** | Chaque nonce est unique et jamais reutilise | Nonce deja vu (Redis TTL 24h) |
| **Timestamp** | Horodatage du message | > 5 min dans le futur OU > 72h dans le passe |
| **SequenceNumber** | Monotone croissant par device | Sequence <= derniere sequence acceptee |

### 3.3 Validation topic/payload coherence

- `deviceId` dans le payload DOIT correspondre au `{deviceId}` du topic
- `tenantId` dans le payload DOIT correspondre au `{tenantId}` du topic
- Un device ne peut publier que sur ses propres topics (enforced par ACL broker)

### 3.4 Device revocation

- Si `device.status == REVOKED` dans le device-service → rejet immediat de tous les messages
- Le broker EMQX deconnecte le device (via API admin ou ACL update)
- Le device recoit le disconnect et passe en etat REVOKED

---

## 4. Regles de validation backend

### Pipeline de validation (ordre)

```
1. Payload size check      (< 8KB)
2. JSON parse              (syntaxe valide)
3. Schema version check    (version supportee)
4. Topic/payload coherence (deviceId, tenantId match)
5. Device authorization    (device actif, non revoque, tenant correct)
6. Signature verification  (HMAC-SHA256)
7. Anti-replay checks      (nonce unique, timestamp fresh, sequence monotone)
8. Idempotency check       (messageId non duplique, Redis 24h)
9. Field validation        (ranges, types, completude)
10. Accept → persist + publish Kafka
```

### Rejets et codes

| Etape | Raison rejet | Action |
|-------|-------------|--------|
| 1 | Payload > 8KB | Drop silencieux, increment counter |
| 2 | JSON invalide | DLQ + log |
| 3 | Schema version inconnue | DLQ + log warning |
| 4 | deviceId/tenantId mismatch | Reject + log security |
| 5 | Device REVOKED ou INACTIVE | Reject + publish DeviceRejectedEvent |
| 6 | Signature invalide | Reject + log security alert |
| 7a | Nonce reutilise | Reject (replay attack) |
| 7b | Timestamp trop vieux (>72h) | Reject (stale data) |
| 7c | Timestamp futur (>5min) | Reject (clock drift) |
| 7d | Sequence regress | Reject (out of order) |
| 8 | messageId duplique | Return DUPLICATE (idempotent) |
| 9 | Valeurs hors range | DLQ + log |
| 10 | Succes | Persist TimescaleDB + Kafka telemetry-features |

---

## 5. QoS et fiabilite

| Message type | QoS | Retain | Justification |
|:-------------|:---:|:------:|---------------|
| Telemetry | 1 | Non | Donnees importantes, delivery garanti au moins une fois |
| Heartbeat | 0 | Oui | Dernier etat connu, perte acceptable |
| Events | 1 | Non | Evenements critiques, delivery garanti |
| Provisioning | 1 | Non | One-shot enrollment, must succeed |
| Commands | 2 | Non | Exactement une fois (eviter double reboot/OTA) |
| Command Ack | 1 | Non | Confirmation, delivery garanti |

### Reconnexion (firmware)

```
Backoff exponentiel avec jitter :
  delay = min(base_delay * 2^attempt, max_delay) + random(0, jitter)
  
  base_delay = 1s
  max_delay  = 60s
  jitter     = random(0, 1000ms)
  
  Attempt 1: ~1-2s
  Attempt 2: ~2-3s
  Attempt 3: ~4-5s
  Attempt 4: ~8-9s
  Attempt 5: ~16-17s
  ...
  Attempt 6+: ~60s + jitter
```

### Buffer offline (firmware)

- Capacite : 1000 messages (RAM queue) ou 4MB SPIFFS (persistent)
- Priorite : CRITICAL > HIGH > NORMAL
- Eviction : FIFO sur messages NORMAL quand plein
- Drain : 10 msg/s apres reconnexion, events d'abord
- Flag `isDrain: true` dans le payload si message rejoue depuis buffer

---

## 6. Erreurs et rejets

### Codes d'erreur backend (publies sur DLQ Kafka)

| Code | Signification |
|------|---------------|
| `INVALID_SCHEMA_VERSION` | Version schema non supportee |
| `INVALID_JSON` | Payload non parseable |
| `TOPIC_PAYLOAD_MISMATCH` | deviceId/tenantId ne correspondent pas au topic |
| `DEVICE_REVOKED` | Device revoque |
| `DEVICE_NOT_FOUND` | Device inconnu |
| `INVALID_SIGNATURE` | Signature HMAC incorrecte |
| `REPLAY_DETECTED` | Nonce deja utilise |
| `TIMESTAMP_TOO_OLD` | Timestamp > 72h dans le passe |
| `TIMESTAMP_FUTURE` | Timestamp > 5min dans le futur |
| `SEQUENCE_REGRESSION` | Sequence inferieure ou egale a la derniere |
| `DUPLICATE_MESSAGE` | messageId deja traite |
| `FIELD_OUT_OF_RANGE` | Valeur hors bornes |
| `PAYLOAD_TOO_LARGE` | > 8KB |

---

## 7. Compatibilite et versioning

### Regles

1. **schemaVersion** est obligatoire dans chaque message
2. Le backend supporte les versions : `["1.0"]` (extensible)
3. Les nouvelles versions sont **additives** (nouveaux champs optionnels)
4. Un champ existant ne change JAMAIS de type ou de semantique
5. Firmware peut envoyer des champs inconnus → le backend les ignore
6. Backend repond avec la version demandee par le device

### Evolution future

| Version | Ajouts prevus |
|---------|---------------|
| 1.0 | Version actuelle (MVP 3) |
| 1.1 | + `compressionAlgo`, + `payloadEncoding` (CBOR), + harmonics detail |
| 2.0 | Breaking: CBOR obligatoire, X.509 mTLS (remplace HMAC) |

### Firmware version tracking

- Le backend stocke `firmwareVersion` par device
- Si un device publie avec une version differente → event `FirmwareVersionChanged`
- Le device-service maintient `lastKnownFirmwareVersion` et `capabilities`

---

## 8. Exemples JSON complets

### Telemetry — scenario nominal

```json
{
  "schemaVersion": "1.0",
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "deviceId": "pyro-dev-001",
  "tenantId": "tenant-lab-01",
  "timestamp": "2026-05-27T14:30:05.123Z",
  "sequenceNumber": 4201,
  "firmwareVersion": "0.1.0",
  "samplingWindowMs": 1000,
  "features": {
    "rmsCurrent": 14.52,
    "rmsVoltage": 230.1,
    "activePower": 3245.8,
    "reactivePower": 312.4,
    "powerFactor": 0.94,
    "thd": 4.7,
    "temperatureCelsius": 39.5,
    "hfNoiseLevel": 0.18,
    "microArcCount": 0,
    "transientCount": 1,
    "signalQuality": 0.97
  },
  "security": {
    "nonce": "6839ab0100001069a3f2bc01",
    "signature": "7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069"
  }
}
```

### Telemetry — buffer drain (message rejoue)

```json
{
  "schemaVersion": "1.0",
  "messageId": "660e8400-e29b-41d4-a716-446655440010",
  "deviceId": "pyro-dev-001",
  "tenantId": "tenant-lab-01",
  "timestamp": "2026-05-26T10:15:00Z",
  "sequenceNumber": 3800,
  "firmwareVersion": "0.1.0",
  "samplingWindowMs": 1000,
  "isDrain": true,
  "features": { "..." : "..." },
  "security": { "nonce": "...", "signature": "..." }
}
```

### Event — arc detecte

```json
{
  "schemaVersion": "1.0",
  "messageId": "770e8400-e29b-41d4-a716-446655440002",
  "deviceId": "pyro-dev-001",
  "tenantId": "tenant-lab-01",
  "timestamp": "2026-05-27T14:30:12.034Z",
  "sequenceNumber": 4202,
  "firmwareVersion": "0.1.0",
  "eventType": "ARC_DETECTED",
  "severity": "HIGH",
  "data": {
    "arcEnergy": 0.45,
    "peakAmplitude": 2.3,
    "durationUs": 150,
    "phase": "L1"
  },
  "context": {
    "rmsCurrent": 14.82,
    "temperatureCelsius": 38.2,
    "signalQuality": 0.92
  },
  "security": {
    "nonce": "6839ab0200001070b4e3cd02",
    "signature": "8a94b2c5d6e7f8091a2b3c4d5e6f70819293a4b5c6d7e8f9012345678abcdef0"
  }
}
```

### Command — update config

```json
{
  "schemaVersion": "1.0",
  "commandId": "cmd-a1b2c3d4-e5f6-7890",
  "commandType": "UPDATE_CONFIG",
  "issuedAt": "2026-05-27T14:30:00Z",
  "expiresAt": "2026-05-27T14:35:00Z",
  "payload": {
    "featureIntervalMs": 10000,
    "heartbeatIntervalMs": 120000,
    "arcThreshold": 0.25
  }
}
```
