# PyroSense MVP 3 — Architecture Edge-Cloud Detaillee

**Date :** 2026-05-27
**Statut :** Specification technique (pre-implementation)
**Scope :** Communication capteur physique ↔ plateforme backend

---

## Table des Matieres

1. [Diagramme textuel edge/cloud](#1-diagramme-textuel-edgecloud)
2. [Sequences completes](#2-sequences-completes)
3. [Responsabilites edge vs cloud](#3-responsabilites-edge-vs-cloud)
4. [Calculs edge (capteur)](#4-calculs-edge-capteur)
5. [Calculs cloud (backend)](#5-calculs-cloud-backend)
6. [Strategie zero donnees brutes](#6-strategie-zero-donnees-brutes)
7. [Strategie de synchronisation du temps](#7-strategie-de-synchronisation-du-temps)
8. [Strategie offline 72h](#8-strategie-offline-72h)
9. [Strategie de compression](#9-strategie-de-compression)
10. [Strategie de versioning payload](#10-strategie-de-versioning-payload)
11. [Strategie de compatibilite firmware/backend](#11-strategie-de-compatibilite-firmwarebackend)
12. [Risques et mitigations](#12-risques-et-mitigations)

---

## 1. Diagramme textuel edge/cloud

### Vue d'ensemble complete

```
╔══════════════════════════════════════════════════════════════════════════════════╗
║                        EDGE — TABLEAU ELECTRIQUE (rail DIN)                      ║
║                                                                                  ║
║  ┌────────────────────────────────────────────────────────────────────────────┐ ║
║  │                    CAPTEURS PHYSIQUES (passifs)                             │ ║
║  │                                                                            │ ║
║  │  CT(L1) ──┐    CT(L2) ──┐    CT(L3) ──┐    PT100 ─┐    PT100 ─┐         │ ║
║  │  (30A)    │    (30A)    │    (30A)    │    (#1)   │    (#2)   │         │ ║
║  │           │             │             │           │           │         │ ║
║  │  ArcDet ──┤             │             │           │           │         │ ║
║  │  (LM393)  │             │             │           │           │         │ ║
║  └───────────┼─────────────┼─────────────┼───────────┼───────────┼─────────┘ ║
║              │ analog      │ analog      │ analog    │ SPI       │ SPI       ║
║              ▼             ▼             ▼           ▼           ▼           ║
║  ┌────────────────────────────────────────────────────────────────────────────┐ ║
║  │                         GATEWAY (ESP32-S3-WROOM-1)                         │ ║
║  │                                                                            │ ║
║  │  ┌──────────────────────────────────────────────────────────────────────┐ │ ║
║  │  │ COUCHE ACQUISITION (Core 1, priorite 4)                              │ │ ║
║  │  │                                                                      │ │ ║
║  │  │  ADS1115 (I2C) ──▶ DMA buffer 16K samples ──▶ ISR declenchement 1s │ │ ║
║  │  │  MAX31865 (SPI) ──▶ Lecture 0.2 Hz (toutes les 5s)                  │ │ ║
║  │  │  GPIO arc detect ──▶ ISR front montant ──▶ compteur + timestamp      │ │ ║
║  │  └─────────────────────────────────┬────────────────────────────────────┘ │ ║
║  │                                    │ features_queue (64 items)             │ ║
║  │                                    ▼                                       │ ║
║  │  ┌──────────────────────────────────────────────────────────────────────┐ │ ║
║  │  │ COUCHE TRAITEMENT (Core 0, priorite 3)                               │ │ ║
║  │  │                                                                      │ │ ║
║  │  │  ┌────────────────┐  ┌────────────────┐  ┌─────────────────────┐   │ │ ║
║  │  │  │ RMS (1s)       │  │ FFT 1024pts    │  │ Detection urgence   │   │ │ ║
║  │  │  │ Crest factor   │  │ THD (10s)      │  │ - Arc (HF energy)   │   │ │ ║
║  │  │  │ Zero-crossing  │  │ H3,H5,H7,H9   │  │ - Surcharge (>1.2In)│   │ │ ║
║  │  │  │ Power factor   │  │ HF noise band  │  │ - Echauffement      │   │ │ ║
║  │  │  └────────────────┘  └────────────────┘  │   (dT/dt > 2°C/min)│   │ │ ║
║  │  │                                           └─────────────────────┘   │ │ ║
║  │  │                                                                      │ │ ║
║  │  │  Outputs:                                                            │ │ ║
║  │  │  - FeatureFrame (chaque 5s) ──────────────────┐                     │ │ ║
║  │  │  - UrgentEvent (immediat si seuil depasse) ───┤                     │ │ ║
║  │  │  - StatsFrame (chaque 60s) ───────────────────┤                     │ │ ║
║  │  └───────────────────────────────────────────────┼──────────────────────┘ │ ║
║  │                                                   │ publish_queue (128)    │ ║
║  │                                                   ▼                        │ ║
║  │  ┌──────────────────────────────────────────────────────────────────────┐ │ ║
║  │  │ COUCHE COMMUNICATION (Core 0, priorite 2)                            │ │ ║
║  │  │                                                                      │ │ ║
║  │  │  ┌─────────────────┐  ┌──────────────────┐  ┌────────────────────┐ │ │ ║
║  │  │  │ MQTT 5.0 Client │  │ Buffer Manager   │  │ Signature & Time  │ │ │ ║
║  │  │  │ - TLS 1.3       │  │ - SPIFFS 4MB     │  │ - HMAC-SHA256     │ │ │ ║
║  │  │  │ - X.509 mTLS    │  │ - FIFO drain     │  │ - NTP sync        │ │ │ ║
║  │  │  │ - QoS 1 (feat)  │  │ - Priorite event │  │ - Monotonic seq   │ │ │ ║
║  │  │  │ - QoS 1 (event) │  │ - 72h capacity   │  │ - ATECC608B sign  │ │ │ ║
║  │  │  │ - QoS 0 (heart) │  │ - Compression LZ4│  │                   │ │ │ ║
║  │  │  └────────┬────────┘  └──────────────────┘  └────────────────────┘ │ │ ║
║  │  └───────────┼──────────────────────────────────────────────────────────┘ │ ║
║  │              │                                                              │ ║
║  │  ┌───────────┼──────────────────────────────────────────────────────────┐ │ ║
║  │  │ COUCHE SANTE (Core 0, priorite 1)                                    │ │ ║
║  │  │ - Self-diagnostic (CPU, RAM, flash, T° interne)                      │ │ ║
║  │  │ - OTA check + apply (A/B partitions)                                 │ │ ║
║  │  │ - Certificate rotation check                                          │ │ ║
║  │  │ - Watchdog feed                                                       │ │ ║
║  │  └──────────────────────────────────────────────────────────────────────┘ │ ║
║  └──────────────┼──────────────────────────────────────────────────────────────┘ ║
╚═════════════════╪════════════════════════════════════════════════════════════════╝
                  │
                  │  WiFi (primary) / 4G LTE-M (fallback)
                  │  TLS 1.3 + mTLS X.509
                  │  MQTT 5.0
                  │
╔═════════════════╪════════════════════════════════════════════════════════════════╗
║                 │              CLOUD (Kubernetes)                                 ║
║                 ▼                                                                 ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐║
║  │                        EMQX BROKER (2 noeuds)                                 │║
║  │  - mTLS terminaison (X.509 device cert validation)                           │║
║  │  - ACL par device (topic authorization)                                       │║
║  │  - Rate limiting (1 msg/s features, 10 msg/s events)                         │║
║  │  - Shared subscriptions ($share/ingestion/...)                                │║
║  │  - Session persistence (24h expiry)                                           │║
║  └──────────────────────────────┬───────────────────────────────────────────────┘║
║                                 │ Internal (no TLS, pod network)                  ║
║                                 ▼                                                 ║
║  ┌──────────────────────────────────────────────────────────────────────────────┐║
║  │  INGESTION SERVICE (adapter/in/messaging/MqttInboundAdapter)                  │║
║  │                                                                               │║
║  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐ │║
║  │  │ CBOR/JSON    │  │ Schema       │  │ Device       │  │ Idempotency     │ │║
║  │  │ Decoder      │  │ Validation   │  │ Authorization│  │ (Redis 24h TTL) │ │║
║  │  │ (payload v)  │  │ (features v) │  │ (X.509 cert) │  │ (messageId)     │ │║
║  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────────┘ │║
║  │         └──────────────────┴──────────────────┴──────────────────┘            │║
║  │                                          │                                    │║
║  │                          Kafka: telemetry-features (6 partitions)             │║
║  └──────────────────────────────────────────┼───────────────────────────────────┘║
║                                             │                                     ║
║          ┌──────────────────────────────────┼──────────────────────────┐          ║
║          │                                  │                          │          ║
║          ▼                                  ▼                          ▼          ║
║  ┌────────────────────┐   ┌────────────────────────┐   ┌──────────────────────┐ ║
║  │ SIGNAL ANALYSIS    │   │ RISK SCORING           │   │ FEATURE STORE        │ ║
║  │                    │   │                        │   │                      │ ║
║  │ - Welford baseline │   │ - 6-factor composite   │   │ - TimescaleDB write  │ ║
║  │ - Z-score anomaly  │   │ - Trend detection      │   │ - Continuous agg.    │ ║
║  │ - Drift detection  │   │ - Explainability       │   │ - Parquet export     │ ║
║  │ - Cross-correlation│   │ - ML shadow (optional) │   │ - Label management   │ ║
║  │ - Calibration comp │   │ - Feedback adjustment  │   │ - Retention policy   │ ║
║  └────────┬───────────┘   └────────────┬───────────┘   └──────────────────────┘ ║
║           │ anomaly-detected            │ risk-assessed                           ║
║           └──────────────┬──────────────┘                                        ║
║                          ▼                                                        ║
║  ┌────────────────────────────────────────────────────────────────────────────┐  ║
║  │ ALERTING → NOTIFICATION → MAINTENANCE → REPORTING → DASHBOARD             │  ║
║  │ (lifecycle)  (multi-canal)  (interventions)  (PDF)      (read-model)       │  ║
║  └────────────────────────────────────────────────────────────────────────────┘  ║
╚══════════════════════════════════════════════════════════════════════════════════╝
```

### Architecture hexagonale cote backend — Ports/Adapters concernes

```
ingestion-service/
├── adapter/
│   ├── in/
│   │   └── messaging/
│   │       ├── MqttInboundAdapter.java        ← DRIVEN by EMQX (shared subscription)
│   │       ├── CborFeatureDecoder.java        ← Decodage CBOR features
│   │       └── PayloadVersionRouter.java      ← Routing par version payload
│   └── out/
│       ├── persistence/
│       │   └── JdbcFeatureRepository.java     ← TimescaleDB insert batch
│       ├── messaging/
│       │   └── KafkaFeaturePublisher.java     ← Publish vers telemetry-features
│       ├── cache/
│       │   └── RedisIdempotencyAdapter.java   ← Dedup messageId 24h
│       └── security/
│           └── X509DeviceAuthAdapter.java     ← Validation cert fingerprint
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── IngestFeaturesUseCase.java     ← Port entree features
│   │   │   └── IngestEventUseCase.java        ← Port entree events urgents
│   │   └── out/
│   │       ├── FeatureRepositoryPort.java     ← Stockage time-series
│   │       ├── FeatureEventPublisherPort.java ← Publication Kafka
│   │       ├── DeviceAuthorizationPort.java   ← Validation device
│   │       └── IdempotencyPort.java           ← Deduplication
│   └── usecase/
│       ├── IngestFeaturesService.java         ← Orchestration ingestion
│       └── IngestEventService.java            ← Orchestration events urgents
└── domain/
    ├── model/
    │   ├── FeatureFrame.java                  ← Aggregate: features 5s
    │   ├── ElectricalEvent.java               ← Entity: event urgent
    │   └── DeviceCapability.java              ← VO: capacites declarees
    └── valueobject/
        ├── PayloadVersion.java                ← VO: version schema
        ├── SignalQuality.java                 ← VO: qualite mesure
        └── EdgeTimestamp.java                 ← VO: horodatage edge + drift
```

---

## 2. Sequences completes

### 2.1 Provisioning device

```
┌────────────┐        ┌────────────┐        ┌────────────────┐        ┌──────────┐
│  DEVICE    │        │  TECHNICIEN│        │ DEVICE SERVICE │        │   CA     │
│  (ESP32)   │        │  (app web) │        │ (Spring Boot)  │        │  (PKI)   │
└─────┬──────┘        └─────┬──────┘        └───────┬────────┘        └────┬─────┘
      │                     │                        │                      │
      │ Power ON            │                        │                      │
      │ (LED bleu clignotant)                       │                      │
      │                     │                        │                      │
      │                     │  1. POST /devices      │                      │
      │                     │     {serial, tenantId,  │                      │
      │                     │      buildingId,        │                      │
      │                     │      panelId,           │                      │
      │                     │      publicKey (EC P256)}                     │
      │                     │──────────────────────▶│                      │
      │                     │                        │ validate + store     │
      │                     │                        │ state = REGISTERED   │
      │                     │◀──────────────────────│                      │
      │                     │  201 {deviceId,         │                      │
      │                     │       enrollmentToken}   │                      │
      │                     │                        │                      │
      │  2. WiFi config     │                        │                      │
      │     (AP provisioning│ou WPS)                 │                      │
      │◀────────────────────│                        │                      │
      │                     │                        │                      │
      │  3. Generate CSR (ATECC608B)                 │                      │
      │     CN={deviceId}, O={tenantId}              │                      │
      │     signed by device private key             │                      │
      │                     │                        │                      │
      │──────── POST /devices/{id}/enroll ──────────▶│                      │
      │         {csr, enrollmentToken,               │                      │
      │          capabilities: {                     │                      │
      │            firmwareVersion,                   │                      │
      │            payloadVersion: "2.0",            │                      │
      │            sensors: [CT_30A, PT100, ARC],    │                      │
      │            features: [RMS,THD,PF,ARC,TEMP], │                      │
      │            bufferHours: 72,                  │                      │
      │            compressionSupported: true        │                      │
      │          }}                                   │                      │
      │                     │                        │                      │
      │                     │                        │  4. Validate CSR     │
      │                     │                        │     (key matches     │
      │                     │                        │      registered key) │
      │                     │                        │                      │
      │                     │                        │─── Sign CSR ────────▶│
      │                     │                        │                      │
      │                     │                        │◀── X.509 cert ───────│
      │                     │                        │    (validity 2 years)│
      │                     │                        │                      │
      │◀──── 200 {certificate, caChain,  ───────────│                      │
      │           mqttEndpoint, mqttPort,            │                      │
      │           ntpServers: [...],                 │                      │
      │           telemetryConfig: {                 │                      │
      │             featureIntervalSec: 5,           │                      │
      │             statsIntervalSec: 60,            │                      │
      │             heartbeatIntervalSec: 60,        │                      │
      │             healthIntervalSec: 300           │                      │
      │           },                                 │                      │
      │           negotiatedCapabilities: {          │                      │
      │             payloadEncoding: "CBOR",         │                      │
      │             compressionAlgo: "LZ4",          │                      │
      │             maxPayloadBytes: 512             │                      │
      │           }}                                 │                      │
      │                     │                        │                      │
      │  5. Store cert in ATECC608B                  │                      │
      │  6. Connect MQTT (mTLS)                      │                      │
      │  7. Publish first heartbeat                  │                      │
      │─────────── heartbeat ───────────────────────▶│                      │
      │                     │                        │ state = PROVISIONED  │
      │                     │                        │ (after 10 heartbeats │
      │  8. LED vert fixe                            │  → state = LEARNING) │
      │                     │                        │                      │
```

### 2.2 Heartbeat (nominal)

```
┌────────────┐        ┌────────────┐        ┌────────────────┐
│  DEVICE    │        │ EMQX BROKER│        │INGESTION SVC   │
└─────┬──────┘        └─────┬──────┘        └───────┬────────┘
      │                     │                        │
      │  Every 60s:         │                        │
      │  QoS 0, retain=true │                        │
      │                     │                        │
      │  PUBLISH            │                        │
      │  topic: pyrosense/{tid}/{did}/status/heartbeat
      │  payload (CBOR, ~50 bytes):                  │
      │  {                   │                        │
      │    v: 2,             │  (payload version)    │
      │    seq: 84201,       │  (monotonic counter)  │
      │    ts: 1726401600,   │  (unix epoch seconds) │
      │    up: 259200,       │  (uptime seconds)     │
      │    mem: 184320,      │  (free heap bytes)    │
      │    rssi: -42,        │  (WiFi dBm)          │
      │    buf: 0,           │  (buffer % used)     │
      │    dq: 95            │  (data quality 0-100)│
      │  }                   │                        │
      │─────────────────────▶│                        │
      │                     │  Forward (shared sub)  │
      │                     │───────────────────────▶│
      │                     │                        │ Update device status
      │                     │                        │ Check offline timer
      │                     │                        │ Publish: device-heartbeats (Kafka)
      │                     │                        │
      │  [If 3 missed (180s)]                       │
      │                     │                        │ DeviceOfflineEvent → Kafka
      │                     │                        │ → Notification Service
      │                     │                        │ → Dashboard update
```

### 2.3 Telemetry (features periodiques)

```
┌────────────┐        ┌────────────┐        ┌────────────────┐        ┌─────────────┐
│  DEVICE    │        │ EMQX BROKER│        │INGESTION SVC   │        │ KAFKA       │
└─────┬──────┘        └─────┬──────┘        └───────┬────────┘        └──────┬──────┘
      │                     │                        │                        │
      │  Every 5s:          │                        │                        │
      │  QoS 1              │                        │                        │
      │                     │                        │                        │
      │  PUBLISH            │                        │                        │
      │  topic: pyrosense/{tid}/{did}/features/periodic                      │
      │  user-properties:    │                        │                        │
      │    cert-fp: "a3b2..."│                        │                        │
      │    fw-ver: "1.0.3"   │                        │                        │
      │    payload-ver: "2"  │                        │                        │
      │    encoding: "cbor"  │                        │                        │
      │    compress: "lz4"   │                        │                        │
      │                     │                        │                        │
      │  payload (CBOR+LZ4, ~120 bytes compressed):  │                        │
      │  {                   │                        │                        │
      │    v: 2,             │                        │                        │
      │    mid: "uuid",      │  (message ID)         │                        │
      │    seq: 84201,       │  (monotonic)          │                        │
      │    ts: 1726401605,   │                        │                        │
      │    drain: false,     │  (not from buffer)    │                        │
      │    sig: "hmac...",   │  (HMAC-SHA256)        │                        │
      │    f: {              │  (features)           │                        │
      │      i: [14.82, 15.01, 14.95],  (RMS A L1/L2/L3)                    │
      │      ip: [21.1, 21.3, 21.0],    (Peak A)    │                        │
      │      cf: 1.42,       │  (crest factor)      │                        │
      │      zc: 100.0,      │  (zero-crossing Hz)  │                        │
      │      pf: 0.97,       │  (power factor)      │                        │
      │      pa: 3456.7,     │  (active power W)    │                        │
      │      t: [38.2, 36.8],│  (temperatures °C)   │                        │
      │      ta: 24.1,       │  (ambient °C)        │                        │
      │      thd: 8.2,       │  (THD %)             │                        │
      │      h: [5.1,3.2,1.8,0.9], (H3,H5,H7,H9 %)│                        │
      │      arc: 0.0012,    │  (arc energy 50-100kHz)                       │
      │      tc: 0,          │  (transient count 1s)│                        │
      │      hf: 0.003,      │  (HF noise level)    │                        │
      │      sq: 95           │  (signal quality 0-100)                       │
      │    }                 │                        │                        │
      │  }                   │                        │                        │
      │─────────────────────▶│                        │                        │
      │                     │───────────────────────▶│                        │
      │◀── PUBACK ──────────│                        │                        │
      │                     │                        │                        │
      │                     │                        │ 1. Decompress LZ4     │
      │                     │                        │ 2. Decode CBOR        │
      │                     │                        │ 3. Route by payload v │
      │                     │                        │ 4. Validate schema    │
      │                     │                        │ 5. Verify HMAC        │
      │                     │                        │ 6. Check device auth  │
      │                     │                        │    (cert-fp match)    │
      │                     │                        │ 7. Dedup (messageId)  │
      │                     │                        │ 8. Check timestamp    │
      │                     │                        │    (±5min tolerance)  │
      │                     │                        │ 9. Store TimescaleDB  │
      │                     │                        │10. Publish Kafka      │
      │                     │                        │──────────────────────▶│
      │                     │                        │  topic: telemetry-features
      │                     │                        │  key: {deviceId}      │
      │                     │                        │  headers: tenantId,   │
      │                     │                        │    traceId, payloadV  │
```

### 2.4 Event urgent (detection locale)

```
┌────────────┐        ┌────────────┐        ┌────────────────┐        ┌────────────────┐
│  DEVICE    │        │ EMQX BROKER│        │INGESTION SVC   │        │SIGNAL ANALYSIS │
└─────┬──────┘        └─────┬──────┘        └───────┬────────┘        └───────┬────────┘
      │                     │                        │                         │
      │  [Arc detect ISR]   │                        │                         │
      │  GPIO interrupt     │                        │                         │
      │  HF energy >        │                        │                         │
      │  threshold           │                        │                         │
      │                     │                        │                         │
      │  IMMEDIATE PUBLISH   │                        │                         │
      │  QoS 1, priority     │                        │                         │
      │  topic: pyrosense/{tid}/{did}/events/electrical                        │
      │  payload (JSON, ~300 bytes):                  │                         │
      │  {                   │                        │                         │
      │    "v": 2,           │                        │                         │
      │    "mid": "uuid",    │                        │                         │
      │    "seq": 84202,     │                        │                         │
      │    "ts": "2026-09-15T14:05:12.034Z",          │                         │
      │    "eventType": "ARC_DETECTED",               │                         │
      │    "severity": "HIGH",│                        │                         │
      │    "data": {          │                        │                         │
      │      "arcEnergy": 0.45,                       │                         │
      │      "peakAmplitude": 2.3,                    │                         │
      │      "durationUs": 150,│                       │                         │
      │      "phase": "L1",    │                       │                         │
      │      "contextRms": 14.82,                     │                         │
      │      "contextTemp": 38.2                      │                         │
      │    },                 │                        │                         │
      │    "sig": "hmac..."   │                        │                         │
      │  }                   │                        │                         │
      │─────────────────────▶│                        │                         │
      │                     │───────────────────────▶│                         │
      │◀── PUBACK ──────────│                        │                         │
      │                     │                        │ Validate + dedup       │
      │                     │                        │ Kafka: electrical-events│
      │                     │                        │────────────────────────▶│
      │                     │                        │                         │
      │                     │                        │                         │ Correlate with
      │                     │                        │                         │ baseline + history
      │                     │                        │                         │
      │                     │                        │                         │ If pattern confirmed:
      │                     │                        │                         │ Kafka: anomaly-detected
      │                     │                        │                         │──▶ Risk Scoring
      │                     │                        │                         │    → Alerting
      │                     │                        │                         │    → Notification
```

### 2.5 Reconnect apres offline (buffer drain)

```
┌────────────┐        ┌────────────┐        ┌────────────────┐
│  DEVICE    │        │ EMQX BROKER│        │INGESTION SVC   │
└─────┬──────┘        └─────┬──────┘        └───────┬────────┘
      │                     │                        │
      │  [WiFi restored]    │                        │
      │  MQTT CONNECT       │                        │
      │  (clean_start=false,│                        │
      │   session_expiry=24h)                        │
      │─────────────────────▶│                        │
      │◀── CONNACK ─────────│                        │
      │                     │                        │
      │  Resume session      │                        │
      │  Check pending QoS 1 │                        │
      │                     │                        │
      │  [Buffer drain mode] │                        │
      │  Rate: 10 msg/s      │                        │
      │  (throttled to avoid │                        │
      │   broker overload)   │                        │
      │                     │                        │
      │  PUBLISH (drain=true)│                        │
      │  topic: .../features/periodic                │
      │  {                   │                        │
      │    v: 2,             │                        │
      │    mid: "uuid-old-1",│                        │
      │    seq: 83000,       │  (old sequence)       │
      │    ts: 1726315200,   │  (timestamp from 24h ago)
      │    drain: true,      │  ← CRITICAL FLAG      │
      │    f: {...}          │                        │
      │  }                   │                        │
      │─────────────────────▶│───────────────────────▶│
      │◀── PUBACK ──────────│                        │ Accept old timestamp
      │                     │                        │ (drain=true → relaxed
      │  ... (repeat until   │                        │  time validation:
      │   buffer empty) ...  │                        │  72h window instead
      │                     │                        │  of 5min)
      │  PUBLISH (drain=true)│                        │
      │  {seq: 83001, ...}   │                        │
      │─────────────────────▶│───────────────────────▶│ Store with original ts
      │◀── PUBACK ──────────│                        │ (not arrival time)
      │                     │                        │
      │  ... buffer empty ... │                       │
      │                     │                        │
      │  PUBLISH (drain=false)                       │
      │  {seq: 84500, ts: NOW, drain: false}         │
      │─────────────────────▶│───────────────────────▶│ Back to normal mode
      │                     │                        │ (strict 5min window)
      │                     │                        │
      │  [Normal operation   │                        │
      │   resumes]           │                        │
```

### 2.6 Rotation credentials (certificat X.509)

```
┌────────────┐        ┌────────────────┐        ┌──────────┐
│  DEVICE    │        │ DEVICE SERVICE │        │   CA     │
└─────┬──────┘        └───────┬────────┘        └────┬─────┘
      │                        │                      │
      │  [60 jours avant       │                      │
      │   expiration cert]     │                      │
      │                        │                      │
      │                        │  Scheduled check:    │
      │                        │  devices with cert   │
      │                        │  expiring < 60 days  │
      │                        │                      │
      │  MQTT Command:          │                      │
      │  topic: .../command/config                    │
      │  {                      │                      │
      │    "commandType":       │                      │
      │      "ROTATE_CERT",     │                      │
      │    "challengeNonce":    │                      │
      │      "random-32-bytes", │                      │
      │    "timeoutSeconds": 300│                      │
      │  }                      │                      │
      │◀────────────────────────│                      │
      │                        │                      │
      │  1. Generate new CSR    │                      │
      │     (new keypair in     │                      │
      │      ATECC608B slot 2)  │                      │
      │  2. Sign CSR + nonce    │                      │
      │     with CURRENT key    │                      │
      │                        │                      │
      │  POST /devices/{id}/rotate-cert              │
      │  {csr, nonce,           │                      │
      │   signedByCurrentKey}   │                      │
      │─────────────────────────▶                      │
      │                        │ Verify current       │
      │                        │ cert signature       │
      │                        │ Verify nonce         │
      │                        │                      │
      │                        │── Sign new CSR ─────▶│
      │                        │                      │
      │                        │◀─ New cert ──────────│
      │                        │                      │
      │◀── 200 {newCert,       │                      │
      │         caChain}        │                      │
      │                        │                      │
      │  3. Store new cert in   │                      │
      │     ATECC608B           │                      │
      │  4. Disconnect MQTT     │                      │
      │  5. Reconnect with      │                      │
      │     new cert            │                      │
      │─── MQTT CONNECT (new cert) ──▶                │
      │                        │                      │
      │                        │ Verify new cert     │
      │                        │ Revoke old cert      │
      │                        │ Update CRL           │
      │                        │ state = ACTIVE       │
      │◀── CONNACK ────────────│                      │
      │                        │                      │
      │  [Grace period: 7 days │                      │
      │   old cert still valid │                      │
      │   (in case of failure)]│                      │
```

---

## 3. Responsabilites edge vs cloud

### Matrice de responsabilites

| Fonction | EDGE (firmware) | CLOUD (backend) | Raison du placement |
|----------|:---------------:|:---------------:|---------------------|
| Echantillonnage ADC 16kHz | **X** | | Latence, volume (16K samples/s impossible en cloud) |
| Calcul RMS (1s) | **X** | | Reduction 16000:1, temps reel |
| Calcul THD (FFT 10s) | **X** | | Reduction donnees, trop volumineux en raw |
| Detection arc (HF energy) | **X** | | Latence < 100ms requise pour event urgent |
| Detection surcharge (>1.2In) | **X** | | Reaction immediate, pas de round-trip cloud |
| Detection echauffement rapide | **X** | | dT/dt > 2°C/min = urgence locale |
| Power factor | **X** | | Calcul local a partir I + V (si disponible) |
| Signal quality score | **X** | | Self-diagnostic capteur (bruit, saturation) |
| Horodatage NTP | **X** | | Precision sub-seconde, autonome |
| Signature HMAC | **X** | | Integrite end-to-end |
| Buffer offline (SPIFFS) | **X** | | Resilience reseau |
| Compression LZ4 | **X** | | Economie bande passante |
| Baseline (Welford) | | **X** | Necessite historique long (30j+), multi-device |
| Drift detection | | **X** | Comparaison inter-devices, tendance longue |
| Z-score anomaly | | **X** | Depend de la baseline cloud |
| Scoring de risque (6 facteurs) | | **X** | Combinaison multi-source, poids ajustables |
| Correlation multi-jours | | **X** | Necessite stockage time-series complet |
| Correlation multi-devices | | **X** | Cross-device impossible depuis un seul firmware |
| Feedback loop (ajust. seuils) | | **X** | Input electricien via app, logique metier complexe |
| ML shadow mode | | **X** | GPU/CPU cloud, dataset complet requis |
| Rapports PDF | | **X** | Templates, aggregation, telechargement |
| Alerting lifecycle | | **X** | Deduplication, escalade, SLA, multi-canal |
| Calibration coefficients | | → **X** | Calcules dans le cloud, envoyes au device via command |
| OTA firmware decision | | **X** | Controle centralise, rollout progressif |
| Retention / purge donnees | | **X** | Politique configurable, compliance |

### Principes de separation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│  EDGE CALCULE :                          CLOUD CALCULE :                    │
│                                                                             │
│  Tout ce qui necessite                   Tout ce qui necessite              │
│  - latence < 1s                          - historique > 10s                 │
│  - echantillonnage > 1Hz                 - cross-device                    │
│  - reaction immediate                    - logique metier complexe         │
│  - fonctionnement offline                - intervention humaine            │
│  - volume de donnees brutes              - ML / statistiques avancees      │
│    inenvoyable                           - rapports / visualisation        │
│                                                                             │
│  L'edge produit des FEATURES             Le cloud produit des DECISIONS    │
│  (nombres derives des signaux)           (alertes, scores, rapports)       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Calculs edge (capteur)

### 4.1 RMS (Root Mean Square) — courant et tension

```
Frequence de calcul : chaque 1 seconde
Entree : 16000 echantillons ADC (16kHz × 1s)
Sortie : float32 (Amperes RMS par phase)

Algorithme :
  sum_sq = 0
  for sample in window_1s:
      sum_sq += (sample × calibration_factor)²
  rms = sqrt(sum_sq / N)

Precision cible : < ±2% vs reference (Fluke i400s)
Calibration : coefficient par phase, stocke en NVS
```

### 4.2 THD (Total Harmonic Distortion)

```
Frequence de calcul : chaque 10 secondes
Entree : 10 × 16000 = 160000 echantillons (moyenne sur FFT 1024 × ~156 fenetres)
Sortie : float32 (% THD), float32[4] (H3, H5, H7, H9 en %)

Algorithme :
  Fenetre de Hanning sur 1024 points
  FFT reelle (rfft) → 512 bins complexes
  Frequence fondamentale (50Hz) → bin 10 (si fs=16kHz, N=1024: f_res = 15.625 Hz)
  Ajustement: bin fondamental = round(50/15.625) = 3 (recalculer avec Fs exact)
  
  Note: avec Fs=16000, N=1024 → resolution = 15.625 Hz
  Bin 50Hz = 50/15.625 ≈ 3.2 → interpolation parabole
  
  THD = sqrt(sum(Hn²)) / H1 × 100%
  Hn = amplitude bin(n × f_fund) pour n = 3, 5, 7, 9, 11, 13

  Moyenne sur 10 FFT par intervalle 10s

Precision cible : < ±1% absolu vs analyseur de puissance
```

### 4.3 Temperature

```
Frequence d'acquisition : 0.2 Hz (toutes les 5s)
Capteur : MAX31865 + PT100 classe A
Sortie : float32 (°C), float32 (delta T vs ambient), float32 (rate of change °C/min)

Rate of change :
  dT/dt = (T_now - T_5min_ago) / 5.0  [°C/min]
  (fenetre glissante 5 minutes, 60 echantillons)

Alerte edge si : dT/dt > 2.0 °C/min → event OVERHEATING
```

### 4.4 microArcCount (detection micro-arc)

```
Detection : ISR sur GPIO (comparateur LM393, seuil analogique)
Metrique : compteur de fronts par seconde + energie HF

Algorithme :
  1. Comparateur LM393 detecte transitoire > seuil (reglable par potentiometre ou DAC)
  2. ISR GPIO compte les fronts → transientCount (par seconde)
  3. En parallele, FFT bande 50-100 kHz sur ADC (si Fs=200kHz ADC rapide)
     OU estimation via comptage de zero-crossings HF sur fenetre 1ms
  4. arcEnergy = integrale(amplitude²) dans bande 50-100 kHz (normalise 0-1)

Sortie dans feature frame :
  - tc (transientCount) : uint16, fronts par seconde (0-65535)
  - arc (arcEnergy) : float32, energie normalisee (0.0 - 1.0)

Seuil urgence edge : arcEnergy > 0.3 ET tc > 5 → event ARC_DETECTED
```

### 4.5 hfNoiseLevel (bruit haute frequence)

```
Frequence de calcul : chaque 5 secondes
Bande d'interet : 2 kHz - 150 kHz (partielles, EMI, arcs naissants)

Algorithme :
  Si ADC rapide disponible (200kHz) :
    FFT sur buffer HF → integrale energie [2k-150k]
    hfNoiseLevel = sqrt(sum(|FFT[bin]|²)) pour bins dans [2kHz, 150kHz]
    Normalise 0-1 par rapport a un seuil max (calibration)

  Si ADC rapide non disponible :
    Estimation via zero-crossing rate :
    zcr = count(sign changes) / window_size
    Si zcr >> 100 Hz → presence de composantes HF
    hfNoiseLevel ≈ (zcr - 100) / 1000  (heuristique, calibration labo)

Sortie : float32 (0.0 - 1.0)
```

### 4.6 powerFactor

```
Frequence de calcul : chaque 1 seconde
Prerequis : mesure simultanee courant ET tension (necessite sonde tension)

Algorithme (si V disponible) :
  P_active = (1/N) × sum(V[n] × I[n])   pour N=16000 echantillons
  P_apparente = V_rms × I_rms
  PF = P_active / P_apparente

Si tension non disponible (version simplifiee CT-only) :
  PF estimé via dephasage fondamental :
  Phase_shift = angle(FFT_I[50Hz]) - angle(FFT_V[50Hz])
  PF ≈ cos(phase_shift)
  OU PF = non disponible (envoye comme null/NaN)

Sortie : float32 (0.0 - 1.0) ou null si non calcule
```

### 4.7 signalQuality (auto-diagnostic)

```
Frequence de calcul : chaque 5 secondes
But : le capteur evalue la fiabilite de ses propres mesures

Score (0-100) calcule comme :
  sq = 100
  sq -= 20 si (ADC_saturation_count > 0 dans derniere seconde)
  sq -= 15 si (variance_RMS < epsilon → signal constant = capteur bloque)
  sq -= 15 si (noise_floor > threshold → bruit EMI excessif)
  sq -= 10 si (temperature_interne_ESP32 > 75°C → throttling possible)
  sq -= 10 si (free_heap < 20% → risque instabilite)
  sq -= 10 si (WiFi_RSSI < -80 dBm → communication degradee)
  sq -= 10 si (NTP_drift > 2s → horodatage imprecis)
  sq -= 10 si (buffer_usage > 50% → donnees potentiellement anciennes)

Sortie : uint8 (0-100)
Impact cloud : si sq < 60, le scoring attenue le poids de ce device
```

### 4.8 Resume features frame (5s)

```c
typedef struct {
    uint8_t  version;          // payload version (2)
    char     messageId[37];    // UUID v4
    uint32_t sequence;         // monotonic counter
    uint32_t timestamp;        // unix epoch seconds
    bool     bufferDrain;      // true if replayed from SPIFFS
    // Current (3 phases)
    float    rms_L1;           // Amperes RMS
    float    rms_L2;
    float    rms_L3;
    float    peak_L1;          // Amperes peak
    float    crestFactor;      // peak / RMS (sans unite)
    float    zeroCrossingRate; // Hz
    // Power
    float    powerFactor;      // 0-1 (ou NaN si non dispo)
    float    activePower;      // Watts
    // Temperature
    float    temp1;            // °C connection #1
    float    temp2;            // °C connection #2
    float    tempAmbient;      // °C ambient
    float    tempRateOfChange; // °C/min
    // Harmonics (updated every 10s, repeated until next update)
    float    thd;              // %
    float    h3, h5, h7, h9;  // % per harmonic
    // Arc / HF
    float    arcEnergy;        // 0-1 normalized
    uint16_t transientCount;   // fronts/s
    float    hfNoiseLevel;     // 0-1 normalized
    // Quality
    uint8_t  signalQuality;    // 0-100
    // Signature
    uint8_t  hmac[32];         // HMAC-SHA256
} FeatureFrame;
```

---

## 5. Calculs cloud (backend)

### 5.1 Baseline (Welford online algorithm)

```
Service : signal-analysis-service
Port in : UpdateBaselineUseCase
Donnees : FeatureFrame stream (Kafka consumer)
Periode : mis a jour a chaque feature recue

Algorithme (par device, par metrique) :
  n += 1
  delta = value - mean
  mean += delta / n
  delta2 = value - mean
  M2 += delta × delta2
  variance = M2 / (n - 1)
  stddev = sqrt(variance)

Fenetre : 30 jours glissants (pondere exponentiellement)
Learning period : 7 jours minimum (pas d'anomaly detect avant)
Output : {mean, stddev, min, max, percentiles} par feature par device
```

### 5.2 Drift detection

```
Service : signal-analysis-service
Algorithme : regression lineaire sur fenetre 7 jours

slope = linear_regression(feature_values, timestamps)
Si |slope| > threshold → DRIFT_DETECTED

Types de drift :
- UPWARD : pente positive significative (echauffement progressif)
- DOWNWARD : pente negative (derive capteur)
- STEP : changement brutal > 3σ (nouvel equipement installe)

Distinction capteur vs reel :
- Si drift detecte sur 1 seul device dans un panel → probablement capteur
- Si drift detecte sur 2+ devices proches → probablement reel
```

### 5.3 Scoring de risque (6 facteurs)

```
Service : risk-scoring-service
Port in : CalculateRiskScoreUseCase
Input : anomaly-detected events (Kafka)

RiskScore = (micro_arc    × 0.30)
          + (thd_drift    × 0.20)
          + (temperature  × 0.20)
          + (transient    × 0.10)
          + (hf_noise     × 0.10)
          + (reliability  × 0.10)

Chaque facteur normalise [0, 100] :
  micro_arc   = f(arcEnergy, transientCount, frequency, recurrence)
  thd_drift   = f(THD vs baseline, slope, duration)
  temperature = f(deltaT vs ambient, rate_of_change, absolute)
  transient   = f(transientCount vs baseline, pattern)
  hf_noise    = f(hfNoiseLevel vs baseline, duration)
  reliability = f(signalQuality, deviceAge, missedHeartbeats)

Modifiers :
  × recency_weight (last hour > last day > last week)
  × repetition_boost (×1.5 si >3 occurrences)
  × trend_multiplier (DEGRADING: ×1.3, IMPROVING: ×0.7)
  × dataQuality_weight (sq < 60 → ×0.5 attenuation)
```

### 5.4 Correlation multi-jours

```
Service : signal-analysis-service
Pattern : Kafka consumer + TimescaleDB queries

Correlations recherchees :
1. Temperature + courant : echauffement disproportionne → resistance anormale
2. THD + arc energy : degradation isolant progressive
3. Transitoires + heure : pattern lie a des equipements specifiques
4. Multi-device meme tableau : propagation de perturbation
5. Saisonnalite : profil ete vs hiver (chauffage, climatisation)
6. Jour/nuit : baseline diurne vs nocturne

Implementation :
  - Continuous aggregates TimescaleDB (1min, 15min, 1h, 1j)
  - Cross-correlation function sur fenetres 7j
  - Pearson coefficient entre features (alerte si nouveau >0.8)
```

### 5.5 Rapports et feedback loop

```
Rapports :
  Service : reporting-service
  Input : aggregats TimescaleDB + labels electricien
  Output : PDF (OpenPDF) + Kafka event (report-generated)

Feedback loop :
  Service : signal-analysis-service + risk-scoring-service
  Input : ElectricalDefectConfirmedEvent / FalsePositiveConfirmedEvent (Kafka)
  
  Defaut confirme → threshold × 0.9 (plus sensible pour ce pattern)
  Faux positif → threshold × 1.15 (moins sensible)
  3 FP consecutifs meme device → threshold × 1.3
  
  Stockage : table adjustment_history (device_id, pattern, old_threshold, new_threshold, reason, date)
```

---

## 6. Strategie zero donnees brutes

### Principe fondamental

> **Aucun echantillon ADC brut ne transite entre le device et le cloud.**
> Seuls des nombres derives (features, events, stats) sont transmis.

### Reduction a chaque couche

| Couche | Entree | Sortie | Facteur reduction |
|--------|--------|--------|:-----------------:|
| ADC → buffer DMA | 16000 samples/s × 16-bit × 3 phases | 96 000 bytes/s | — |
| Buffer → RMS (1s) | 96 000 bytes | 3 × float32 = 12 bytes | **8 000:1** |
| Buffer → FFT/THD (10s) | 960 000 bytes | 5 × float32 = 20 bytes | **48 000:1** |
| Feature frame (5s) | All features | ~180 bytes CBOR | N/A |
| Compression LZ4 | 180 bytes | ~120 bytes | 1.5:1 |
| **Total ADC → cloud** | 480 000 bytes/s | ~24 bytes/s | **20 000:1** |

### Ce qui ne sort JAMAIS du device

| Donnee | Raison | Alternative envoyee |
|--------|--------|---------------------|
| Echantillons ADC bruts (16-bit samples) | Volume (5.5 Go/jour/device) | RMS, THD, features derivees |
| Formes d'onde (waveform capture) | Vie privee + volume | arcEnergy, transientCount |
| Profil de consommation instantane | Peut reveler occupation | Puissance moyenne (15min) |
| Spectre complet (512 bins FFT) | Volume + inutile pour scoring | THD + H3/H5/H7/H9 |
| Echantillons temperature 0.2Hz bruts | Redondant | T° moyenne 5s + rate of change |

### Exceptions (mode debug terrain, temporaire)

En mode debug (active par commande platform, duree max 1h, electricien present) :

```json
{
  "commandType": "ENABLE_DEBUG_CAPTURE",
  "parameters": {
    "durationMinutes": 10,
    "captureMode": "WAVEFORM_SNAPSHOT",
    "samplesPerCapture": 1024,
    "captureIntervalSec": 60
  }
}
```

Un snapshot de 1024 echantillons (~2 KB) est envoye chaque minute pendant 10 min maximum. Usage : diagnostic terrain par electricien quand un defaut est suspecte mais non confirme.

---

## 7. Strategie de synchronisation du temps

### Exigences

| Exigence | Valeur | Raison |
|----------|:------:|--------|
| Precision absolue | < ±1 seconde | Correlation avec evenements batiment |
| Precision relative (inter-devices) | < ±100 ms | Correlation multi-devices meme tableau |
| Resilience (NTP indisponible) | Derive < 5s/jour | Crystal ESP32 = ~20 ppm |
| Detection de desync | Alerte si drift > 2s | Scoring time-aware |

### Architecture de synchronisation

```
┌────────────────────────────────────────────────────────────────────────────┐
│ DEVICE (ESP32)                                                              │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ TIME SUBSYSTEM                                                        │  │
│  │                                                                       │  │
│  │  1. NTP Client (SNTP)                                                │  │
│  │     - Servers: pool.ntp.org + platform-provided NTP                  │  │
│  │     - Sync interval: 1 heure (nominal), 10 min (si drift > 500ms)   │  │
│  │     - Fallback: 2nd NTP server si 1er timeout (5s)                   │  │
│  │                                                                       │  │
│  │  2. RTC (ESP32 internal + optional DS3231 external)                  │  │
│  │     - Maintient l'heure entre reboots                                │  │
│  │     - Battery backup (DS3231) si coupure alimentation                │  │
│  │                                                                       │  │
│  │  3. Monotonic Sequence Counter                                        │  │
│  │     - uint32_t, never resets (stored in NVS)                         │  │
│  │     - Garantit l'ordre meme si horloge desynchronisee                │  │
│  │     - Incremente a chaque message publie                             │  │
│  │                                                                       │  │
│  │  4. Drift Monitor                                                     │  │
│  │     - Compare NTP response vs local clock                            │  │
│  │     - Si |drift| > 2s → flag dans heartbeat (dq -= 10)              │  │
│  │     - Si |drift| > 30s → force re-sync + log event                  │  │
│  │                                                                       │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────┐
│ CLOUD (Ingestion Service)                                                   │
│                                                                             │
│  Reception :                                                                │
│  1. Extraire timestamp du payload (ts_device)                              │
│  2. Capturer timestamp d'arrivee (ts_arrival)                              │
│  3. Calculer clock_offset = ts_arrival - ts_device                         │
│  4. Validation :                                                            │
│     - Si drain=false ET |clock_offset| > 5 min → REJECT (clock desync)    │
│     - Si drain=true ET |clock_offset| > 72h → REJECT (trop ancien)        │
│     - Si drain=true ET |clock_offset| ≤ 72h → ACCEPT (avec flag)          │
│  5. Stockage : ts_device (temps de l'evenement), ts_ingested (arrivee)     │
│  6. Metrique : ingestion.clock_offset_seconds (histogram par device)       │
│                                                                             │
│  Compensation :                                                              │
│  - Les calculs de baseline utilisent TOUJOURS ts_device                    │
│  - Les SLA/delais utilisent ts_ingested                                    │
│  - Le dashboard affiche ts_device avec indication "(buffered)" si drain    │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

### Sequence counter (garantie d'ordre)

Chaque message contient un `seq` monotonique (uint32). Le cloud :
- Detecte les trous (messages perdus)
- Reordonne les messages arrives hors ordre (buffer drain)
- Alerte si gap > 100 (perte significative)

---

## 8. Strategie offline 72h

### Capacite de stockage

```
Flash SPIFFS : 4 Mo

Taille feature frame CBOR + LZ4 : ~120 bytes
Taille event JSON : ~300 bytes
Taille heartbeat CBOR : ~50 bytes

Features (1 frame / 5s) : 120 × 12 × 60 × 24 = 2 488 320 bytes/jour ≈ 2.4 Mo/jour
Events (estime 50/jour) : 300 × 50 = 15 000 bytes/jour ≈ 15 Ko/jour
Heartbeats : NON BUFFERISES (QoS 0, pas critique)

Total par jour : ~2.4 Mo
Capacite 4 Mo : ~1.7 jour en mode nominal

Pour atteindre 72h (3 jours) :
  → Mode degradé active automatiquement apres 24h offline
```

### Mode degradé (offline > 24h)

| Duree offline | Mode | Features freq | Events | Heartbeat |
|:-------------:|------|:-------------:|:------:|:---------:|
| 0-24h | NOMINAL | 1/5s (100%) | Tous | Non buffered |
| 24-48h | REDUCED | 1/30s (17%) | Tous | Non buffered |
| 48-72h | MINIMAL | 1/60s (8%) | Seulement CRITICAL | Non buffered |
| > 72h | EMERGENCY | 1/5min (1.7%) | Seulement CRITICAL | Non buffered |

### Gestion du buffer SPIFFS

```
┌─────────────────────────────────────────────────────────────┐
│ SPIFFS (4 Mo)                                                │
│                                                              │
│  Structure fichiers :                                        │
│  /buffer/                                                    │
│    features_20260915_140000.bin  (batch 1000 frames)        │
│    features_20260915_141000.bin                              │
│    events_20260915.bin          (append, 1 fichier/jour)    │
│                                                              │
│  Metadata (NVS) :                                            │
│    buffer_head_seq: 84100  (plus ancien non-envoye)         │
│    buffer_tail_seq: 84500  (dernier ecrit)                  │
│    buffer_bytes_used: 2400000                               │
│    oldest_timestamp: 1726315200                             │
│                                                              │
│  Politique eviction (si buffer plein) :                      │
│    1. Supprimer features les plus anciennes (FIFO)          │
│    2. JAMAIS supprimer events (priorite absolue)            │
│    3. Reduire frequence si proche de plein (mode REDUCED)   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Drain apres reconnexion

```
┌───────────────────────────────────────────────────────────────────────┐
│ DRAIN ALGORITHM                                                        │
│                                                                        │
│  1. Reconnexion MQTT detectee                                         │
│  2. Publier heartbeat immediatement (signal "je suis de retour")      │
│  3. Attendre CONNACK + subscribe ACK                                  │
│  4. Commencer drain :                                                  │
│     a. Events d'abord (priorite haute, tous)                          │
│     b. Features ensuite (FIFO, chronologique)                         │
│  5. Rate limiting drain : 10 messages/seconde                         │
│     (eviter de surcharger le broker et l'ingestion)                   │
│  6. Intercaler messages temps reel entre batch drain :                 │
│     drain 9 messages + 1 message temps reel (ratio 9:1)              │
│  7. Chaque message drain porte : drain=true, timestamp original        │
│  8. A la fin du drain : publier event BUFFER_DRAIN_COMPLETE            │
│  9. Reprendre mode normal (drain=false)                               │
│                                                                        │
│  Duree drain estimee :                                                 │
│  - 24h de buffer (~17280 features) : 17280/10 = 28 min              │
│  - 72h de buffer (~14400 features en mode mixte) : ~24 min           │
│                                                                        │
└───────────────────────────────────────────────────────────────────────┘
```

### Interaction avec le cloud pendant drain

Le cloud (ingestion-service) :
- Accepte les timestamps anciens (72h) si `drain=true`
- Stocke avec le timestamp original du device (pas le timestamp d'arrivee)
- Marque les enregistrements `source=BUFFER_DRAIN` dans TimescaleDB
- Le signal-analysis n'execute PAS de detection anomaly sur les messages draines (baseline recalculee apres)
- Le scoring ne genere PAS d'alerte sur les donnees draines (evite faux positifs retro-actifs)
- EXCEPTION : si un event CRITICAL est draine, il genere une alerte (avec mention "detected offline, reported late")

---

## 9. Strategie de compression

### Choix de LZ4 (frame format)

| Critere | LZ4 | zlib/deflate | Zstandard | Raison du choix |
|---------|:---:|:----------:|:---------:|-----------------|
| Vitesse compression (ESP32) | 200 MB/s | 30 MB/s | 100 MB/s | LZ4 gagne |
| RAM requise | 16 KB | 256 KB | 1 MB | ESP32 = RAM limitee |
| Ratio compression (CBOR features) | 1.4:1 | 1.8:1 | 1.7:1 | LZ4 acceptable |
| Latence | < 1ms / frame | 5-10ms | 2-5ms | LZ4 negligeable |
| Complexite implementation | Faible | Moyenne | Haute | LZ4 simple |
| Decompression (cloud, Java) | lz4-java | java.util.zip | zstd-jni | Tous disponibles |

### Application de la compression

| Type de message | Compression | Raison |
|-----------------|:-----------:|--------|
| Features periodiques (CBOR) | LZ4 | Haute frequence, benefice cumule |
| Stats (CBOR) | LZ4 | Meme raison |
| Events (JSON) | Non | Basse frequence, lisibilite debug, taille deja petite |
| Heartbeat (CBOR) | Non | Ultra-petit (50 bytes), overhead LZ4 frame > gain |
| Health report (JSON) | Non | Basse frequence |
| Commands (JSON, cloud→device) | Non | Basse frequence, lisibilite |

### Negociation

Lors du provisioning, le device declare `compressionSupported: true` dans ses capabilities.
Le cloud repond avec `compressionAlgo: "LZ4"` (ou `"NONE"` si desactive).

Le header MQTT 5.0 user property `compress: "lz4"` indique au cloud que le payload est compresse.
Si absent → payload non-compresse (backward compat).

### Taille sur le fil

```
Feature frame non-compresse (CBOR) : ~180 bytes
Feature frame compresse (CBOR+LZ4) : ~120 bytes (estimation -33%)
+ overhead MQTT 5.0 header : ~80 bytes (topic + properties + QoS)

Total par message feature : ~200 bytes sur le fil
A 1 msg/5s : 200 × 12/min × 60 × 24 = ~3.5 Mo/jour/device

10 devices : ~35 Mo/jour (upload)
```

---

## 10. Strategie de versioning payload

### Schema versioning

Chaque payload contient un champ `v` (version) en premier octet :

```
v=1 : MVP 2 (simulateur, JSON, mesures brutes)     [LEGACY]
v=2 : MVP 3 (capteur reel, CBOR, features)         [CURRENT]
v=3 : Future (ajout capteurs, nouveaux champs)      [RESERVED]
```

### Regles de versioning

| Regle | Detail |
|-------|--------|
| **Additive only** | Une nouvelle version peut AJOUTER des champs, jamais en supprimer |
| **Unknown fields ignored** | Le cloud ignore les champs inconnus (forward compat) |
| **Missing fields = default** | Un champ absent est traite avec sa valeur par defaut documentee |
| **Version in every message** | Pas d'etat de session — chaque message est auto-descriptif |
| **No negotiation required** | Le cloud traite tous les v supportes simultanement |

### Routage par version (cloud)

```java
// adapter/in/messaging/PayloadVersionRouter.java
public interface PayloadVersionRouter {
    FeatureFrame route(byte[] rawPayload, int version);
}

// Implementations :
// - V1PayloadDecoder : JSON telemetry (legacy simulator)
// - V2PayloadDecoder : CBOR features (real sensor)
// - V3PayloadDecoder : future extension
```

### Schema registry (documentation, pas runtime)

Pas de Confluent Schema Registry pour les payloads MQTT (trop lourd pour IoT).
A la place, un document `docs/payload-schema-registry.md` avec :
- Schema JSON pour chaque version (validation)
- Changelog (quels champs ajoutes en v2, v3...)
- Mapping champs v1 → v2 pour backward compat

### Exemple backward compat v1 → v2

```
v1 payload (simulateur, JSON) :
{
  "measurements": {"temperature": {"value": 42.5}, "current": {"value": 15.2}}
}

v2 payload (capteur, CBOR) :
{
  "v": 2,
  "f": {"t": [42.5, null], "i": [15.2, 15.0, 14.8], "thd": 8.2, ...}
}

Le cloud maintient les deux decodeurs actifs simultanement.
Le simulateur continue de fonctionner en v1.
Les capteurs reels utilisent v2.
Pas de migration forcee.
```

---

## 11. Strategie de compatibilite firmware/backend

### Capability Negotiation

Lors du provisioning, le device declare ses capacites :

```json
{
  "capabilities": {
    "firmwareVersion": "1.0.3",
    "payloadVersion": "2.0",
    "payloadEncoding": ["CBOR", "JSON"],
    "compressionAlgorithms": ["LZ4", "NONE"],
    "sensors": ["CT_30A_x3", "PT100_x2", "ARC_DETECTOR"],
    "features": ["RMS", "PEAK", "CREST", "ZCR", "PF", "THD", "H3", "H5", "H7", "H9", "ARC_ENERGY", "TRANSIENT_COUNT", "HF_NOISE", "TEMP", "SIGNAL_QUALITY"],
    "bufferCapacityHours": 72,
    "maxPayloadBytes": 512,
    "otaSupported": true,
    "secureBootEnabled": true,
    "cryptoChipPresent": true
  }
}
```

Le cloud repond avec une configuration negociee :

```json
{
  "negotiatedConfig": {
    "payloadEncoding": "CBOR",
    "compressionAlgo": "LZ4",
    "featureIntervalSec": 5,
    "statsIntervalSec": 60,
    "heartbeatIntervalSec": 60,
    "healthIntervalSec": 300,
    "maxPayloadBytes": 512,
    "requiredFeatures": ["RMS", "TEMP", "THD", "ARC_ENERGY", "SIGNAL_QUALITY"],
    "optionalFeatures": ["PF", "H3", "H5", "H7", "H9", "HF_NOISE"]
  }
}
```

### Contrats de compatibilite

| Regle | Description | Consequence |
|-------|-------------|-------------|
| **Cloud supports N, N-1, N-2** | Le cloud decode 3 versions payload simultanement | Pas de mise a jour firmware forcee |
| **Firmware never breaks** | Une feature declaree comme supported est toujours envoyee | Cloud peut compter dessus |
| **Missing features = null** | Si un feature declare "optional" est absent, le cloud s'adapte | Scoring degrade gracefully |
| **New firmware, old cloud** | Nouveau firmware envoie champs inconnus → cloud les ignore | Pas de blocage |
| **Old firmware, new cloud** | Cloud recoit payload v ancien → decode avec ancien decoder | Pas de blocage |
| **Config push (command topic)** | Cloud peut modifier intervalles, features actives, modes | Pas de reflash necessaire |

### Matrice de compatibilite

```
             Cloud v1.0    Cloud v1.1    Cloud v2.0
             (payload v1)  (v1+v2)       (v1+v2+v3)
             ────────────  ────────────  ────────────
FW v1.0       ✓             ✓             ✓
(payload v1)  (nominal)     (compat)      (compat)

FW v2.0       ✗ (upgrade    ✓             ✓
(payload v2)   cloud first) (nominal)     (compat)

FW v3.0       ✗             ✗ (upgrade    ✓
(payload v3)                 cloud first) (nominal)
```

**Regle d'or : toujours deployer le cloud AVANT le firmware.**
Le cloud supporte les anciennes versions. Le firmware ne peut pas parler a un cloud trop ancien.

### Rollout firmware progressif

```
1. Deploy cloud v1.1 (supporte v1 + v2)     → Tous devices v1 continuent a fonctionner
2. Push firmware v2.0 a 1 device (canary)    → 24h monitoring
3. Si OK : push a 10% des devices            → 48h monitoring
4. Si OK : push a 50%                        → 48h monitoring
5. Si OK : push a 100%                       → Cloud peut deprecier v1 dans 6 mois
```

### Deprecation timeline

| Etape | Action | Delai |
|-------|--------|:-----:|
| New version released | v_new supported en parallele | Jour 0 |
| Old version deprecated | Warning logs, metric counter | +3 mois |
| Old version EOL | Force OTA vers v_new | +6 mois |
| Old decoder removed | Code cleanup | +9 mois |

---

## 12. Risques et mitigations

### 12.1 Risques architecture edge

| # | Risque | Probabilite | Impact | Mitigation |
|---|--------|:-----------:|:------:|-----------|
| EA1 | Buffer SPIFFS insuffisant pour 72h en mode nominal | Haute | Perte donnees apres 40h | Mode degradé auto (reduce freq) + flash externe 16Mo |
| EA2 | FFT 1024pts depasse budget CPU ESP32 (Core 0) | Moyenne | THD non calcule | FFT asynchrone, priorite inferieure, fallback ZCR estimation |
| EA3 | Conflit DMA entre ADC et WiFi TX | Moyenne | Corruption donnees | Core pinning (ADC=Core1, WiFi=Core0), buffer intermediaire |
| EA4 | Watchdog trigger pendant FFT longue | Faible | Reboot intempestif | Feed WDT dans boucle FFT, timeout WDT = 10s |
| EA5 | Flash encryption ralentit SPIFFS | Moyenne | Latence buffer | Benchmark labo, desactiver encryption SPIFFS si < perf (encrypt uniquement NVS) |
| EA6 | NTP non accessible (reseau filtre) | Moyenne | Horodatage derive | Fallback: correction cloud (ts_arrival - known_offset), DS3231 RTC backup |
| EA7 | ESP32-S3 RAM insuffisante (PSRAM needed) | Faible | Crash OOM | Version N16R8 (8Mo PSRAM), budget RAM defini par task |
| EA8 | CT saturation sur charges > 30A | Moyenne | Mesures incorrectes | Detection saturation firmware (sq -= 20), alerte maintenance |

### 12.2 Risques architecture cloud

| # | Risque | Probabilite | Impact | Mitigation |
|---|--------|:-----------:|:------:|-----------|
| CA1 | EMQX broker crash (single point) | Faible | Perte ingestion temps reel | 2 noeuds cluster, buffer device 72h, Prometheus alert |
| CA2 | Kafka consumer lag > 5 min | Moyenne | Alertes retardees | Auto-scaling consumers, 6 partitions, monitoring lag |
| CA3 | TimescaleDB full (retention policy fail) | Faible | Insert refuse | Alertes disk 80%, compression policy, monitoring |
| CA4 | Redis down (cache) | Moyenne | Dedup fail → duplicates | Circuit breaker, fallback DB-based dedup, eventual consistency OK |
| CA5 | Ingestion service OOM (buffer drain flood) | Moyenne | Service restart | Rate limit drain (10 msg/s par device), bounded queue (1000), backpressure |
| CA6 | Payload v inconnu (future firmware) | Faible | Messages rejetes | Forward compat (ignore unknown), log warning, metric |
| CA7 | Clock desync device → scoring incorrect | Moyenne | Fausses anomalies | Tolerance ±5min, correction offset, flag dans scoring |
| CA8 | DLQ accumulation (validation failures) | Moyenne | Ops load | Monitoring DLQ depth, auto-retry ExponentialBackOff, alert |

### 12.3 Risques communication

| # | Risque | Probabilite | Impact | Mitigation |
|---|--------|:-----------:|:------:|-----------|
| CO1 | WiFi instable locaux techniques (metal, EMI) | Haute | Deconnexions frequentes | 4G LTE-M fallback, buffer 72h, antenna externe |
| CO2 | TLS handshake lent (ESP32 + ATECC608B) | Moyenne | Reconnexion > 5s | Session resumption TLS 1.3, ATECC608B hardware crypto |
| CO3 | Broker rejette cert (CRL check fail) | Faible | Device bloque | Grace period 7j, OCSP stapling, fallback CRL cache |
| CO4 | MQTT session expiry (24h offline) | Moyenne | Session perdue, re-subscribe | clean_start=false, session_expiry=86400, auto-reconnect |
| CO5 | Payload trop gros (> maxPayloadBytes negocié) | Faible | Message refuse par broker | Validation taille firmware-side, truncation features si depasse |
| CO6 | HMAC mismatch (clock drift → signature invalide) | Faible | Message rejete | HMAC sur payload only (pas timestamp), retry, log |

### 12.4 Risques compatibilite

| # | Risque | Probabilite | Impact | Mitigation |
|---|--------|:-----------:|:------:|-----------|
| CP1 | Firmware v2 deploye avant cloud v1.1 | Moyenne | Messages rejetes | Procedure : cloud d'abord, firmware ensuite. CI/CD check. |
| CP2 | Rollback firmware → ancien payload | Faible | Cloud recoit v1 inatendu | Cloud supporte v1+v2+v3 simultanement |
| CP3 | CBOR library mismatch (encoding quirks) | Faible | Decode fail | Tests d'integration firmware ↔ cloud dans CI, pinning CBOR lib version |
| CP4 | Feature manquante dans payload (sensor defaillant) | Moyenne | Scoring incomplet | Graceful degradation : scoring partiel avec confidence reduite |
| CP5 | Configuration push non recue (device offline) | Moyenne | Device config desynchronisee | Retained message sur command topic, re-push a reconnexion |

### 12.5 Matrice impact × probabilite

```
                    Impact
                    CRITIQUE    HAUT       MOYEN      FAIBLE
                  ┌───────────┬──────────┬──────────┬──────────┐
  Probabilite     │           │          │          │          │
  HAUTE           │           │ CO1      │ EA1      │          │
                  │           │          │ EA5      │          │
                  ├───────────┼──────────┼──────────┼──────────┤
  MOYENNE         │           │ CA5      │ EA2,EA3  │ CA4      │
                  │           │ CP1      │ CA2,CA7  │ CO4      │
                  │           │          │ CO2,CP4  │          │
                  ├───────────┼──────────┼──────────┼──────────┤
  FAIBLE          │           │ CA1      │ EA4,EA7  │ CA6      │
                  │           │          │ CA3,CA8  │ CO5,CO6  │
                  │           │          │ CO3,CP2  │ CP3      │
                  └───────────┴──────────┴──────────┴──────────┘

Priorite traitement : HAUTE×HAUT > HAUTE×MOYEN > MOYENNE×HAUT > ...
```

---

## Annexe : Ports et Adapters — Vue complete

### Ports IN (driving) concernes par l'edge-cloud

| Port | Service | Caller | Methode |
|------|---------|--------|---------|
| `IngestFeaturesUseCase` | ingestion | MqttInboundAdapter | `ingest(FeatureFrame)` |
| `IngestEventUseCase` | ingestion | MqttInboundAdapter | `ingestEvent(ElectricalEvent)` |
| `RecordHeartbeatUseCase` | device | MqttInboundAdapter | `recordHeartbeat(Heartbeat)` |
| `EnrollDeviceUseCase` | device | REST controller | `enroll(CSR, capabilities)` |
| `RotateCertificateUseCase` | device | REST controller | `rotateCert(CSR, nonce)` |
| `UpdateBaselineUseCase` | signal-analysis | Kafka consumer | `update(FeatureFrame)` |
| `DetectAnomalyUseCase` | signal-analysis | Kafka consumer | `detect(FeatureFrame)` |
| `CalculateRiskScoreUseCase` | risk-scoring | Kafka consumer | `calculate(AnomalyEvent)` |
| `SendCommandUseCase` | device | REST controller / scheduler | `sendCommand(Command)` |

### Ports OUT (driven) concernes par l'edge-cloud

| Port | Service | Implementation | Technologie |
|------|---------|---------------|-------------|
| `FeatureRepositoryPort` | ingestion | JdbcFeatureRepository | TimescaleDB batch insert |
| `FeatureEventPublisherPort` | ingestion | KafkaFeaturePublisher | Kafka (telemetry-features) |
| `IdempotencyPort` | ingestion | RedisIdempotencyAdapter | Redis SET + TTL 24h |
| `DeviceAuthorizationPort` | ingestion | X509DeviceAuthAdapter | Cert fingerprint lookup |
| `MqttCommandPublisherPort` | device | EmqxCommandPublisher | EMQX MQTT publish QoS 2 |
| `CertificateAuthorityPort` | device | CloudKmsCaAdapter | AWS KMS / GCP KMS signing |
| `CrlPublisherPort` | device | S3CrlPublisher | S3 bucket (CRL file) |
| `BaselineRepositoryPort` | signal-analysis | JdbcBaselineRepository | PostgreSQL |
| `AnomalyEventPublisherPort` | signal-analysis | KafkaAnomalyPublisher | Kafka (anomaly-detected) |
| `FeatureStorePort` | signal-analysis | TimescaleFeatureStore | TimescaleDB continuous agg |

---

## Annexe : Device Capability Negotiation — Workflow detaille

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ CAPABILITY NEGOTIATION                                                       │
│                                                                              │
│  Device declares:                     Cloud responds:                        │
│  ─────────────────                    ──────────────                         │
│  sensors: [CT_30A_x3, PT100_x2]      requiredFeatures: [RMS, TEMP, THD]    │
│  features: [RMS, THD, TEMP, PF, ...]  optionalFeatures: [PF, HF_NOISE]     │
│  payloadEncoding: [CBOR, JSON]        payloadEncoding: CBOR                 │
│  compression: [LZ4, NONE]            compressionAlgo: LZ4                   │
│  bufferCapacity: 72h                  featureInterval: 5s                   │
│  maxPayload: 512B                     maxPayloadBytes: 512                  │
│                                                                              │
│  Scenarios de negociation :                                                  │
│                                                                              │
│  1. Device declare PF mais pas de capteur tension                           │
│     → Cloud met PF en optionalFeatures (sera null/NaN)                      │
│                                                                              │
│  2. Device ne supporte pas LZ4                                              │
│     → Cloud repond compressionAlgo: NONE                                    │
│     → Cloud ajuste maxPayload en consequence                                │
│                                                                              │
│  3. Device ancienne generation (v1, JSON only)                              │
│     → Cloud repond payloadEncoding: JSON                                    │
│     → Simulateur compatible                                                  │
│                                                                              │
│  4. Device avec capteur supplementaire (ex: vibration future)               │
│     → Device declare feature "VIBRATION"                                    │
│     → Cloud ne connait pas → ignore, stocke raw, pas de scoring             │
│     → Apres cloud update : scoring integre vibration                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Annexe : Protocole firmware — Protection contre les pannes

```
┌────────────────────────────────────────────────────────────────────────────┐
│ FIRMWARE RESILIENCE                                                          │
│                                                                              │
│  1. WATCHDOG HIERARCHY                                                       │
│     ├── Hardware WDT (8s timeout) : fed by watchdog task                    │
│     ├── Task WDT (5s timeout) : per-task, detect stuck                      │
│     └── Interrupt WDT (300ms) : detect ISR stuck                            │
│                                                                              │
│  2. BOOT SEQUENCE SAFETY                                                     │
│     ├── Secure boot verify (reject unsigned)                                │
│     ├── Self-test (ADC, flash, ATECC608B, RAM)                             │
│     ├── If self-test FAIL : reboot in SAFE MODE (no acquisition, OTA only) │
│     └── If 3 consecutive boot fail : rollback to factory partition          │
│                                                                              │
│  3. MEMORY PROTECTION                                                        │
│     ├── Stack canary per task (overflow detection)                          │
│     ├── Heap corruption detection (heap_caps_check_integrity)              │
│     ├── PSRAM ECC (error correcting code)                                   │
│     └── Budget RAM : total 512KB SRAM + 8MB PSRAM                          │
│         ├── Task stacks : 4×8KB = 32KB                                     │
│         ├── DMA buffers : 2×32KB = 64KB                                    │
│         ├── FFT workspace : 16KB                                            │
│         ├── MQTT buffers : 16KB                                             │
│         ├── FreeRTOS : 24KB                                                 │
│         ├── mbedTLS : 48KB                                                  │
│         └── Free : 328KB (marge suffisante)                                │
│                                                                              │
│  4. FLASH PROTECTION                                                         │
│     ├── Wear leveling SPIFFS (distribue les ecritures)                     │
│     ├── CRC32 sur chaque fichier buffer                                    │
│     ├── NVS partition avec backup (2 copies)                                │
│     └── Ecriture atomique (rename pattern)                                  │
│                                                                              │
│  5. COMMUNICATION RESILIENCE                                                 │
│     ├── Reconnect backoff : 1s, 2s, 4s, 8s, 16s, 30s, 60s (cap)          │
│     ├── WiFi fallback : disconnect → scan → reconnect → 4G if fail        │
│     ├── MQTT keepalive : 60s (detect broken TCP)                           │
│     └── TLS session cache (eviter full handshake a chaque reconnect)       │
│                                                                              │
└────────────────────────────────────────────────────────────────────────────┘
```
