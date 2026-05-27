# PyroSense MVP 3 — Architecture Firmware ESP32-S3

**Date :** 2026-05-27
**Statut :** Specification technique (pre-implementation)
**MCU cible :** ESP32-S3-WROOM-1 (N16R8) — Dual-core Xtensa LX7 @ 240MHz, 16MB Flash, 8MB PSRAM
**Framework :** ESP-IDF 5.x (FreeRTOS)

---

## Table des Matieres

1. [Recommandation ESP-IDF vs Arduino](#1-recommandation-esp-idf-vs-arduino)
2. [Architecture firmware](#2-architecture-firmware)
3. [Diagramme de modules](#3-diagramme-de-modules)
4. [Structure de dossiers](#4-structure-de-dossiers)
5. [Etats du device](#5-etats-du-device)
6. [Types de payload](#6-types-de-payload)
7. [Pseudo-code des taches FreeRTOS](#7-pseudo-code-des-taches-freertos)
8. [Strategie d'erreurs](#8-strategie-derreurs)
9. [Strategie de logs](#9-strategie-de-logs)
10. [Strategie de test](#10-strategie-de-test)
11. [Strategie OTA future](#11-strategie-ota-future)

---

## 1. Recommandation ESP-IDF vs Arduino

### Verdict : ESP-IDF 5.x (FreeRTOS natif)

| Critere | ESP-IDF | Arduino (ESP32) |
|---------|:-------:|:---------------:|
| Controle FreeRTOS | **Complet** (tasks, queues, semaphores, core pinning) | Partiel (loop() masque le scheduler) |
| Partitionnement flash | **Natif** (partition table custom, OTA A/B) | Limite (Arduino OTA basique) |
| MQTT 5.0 | **esp-mqtt 5.0** (user properties, topic alias) | PubSubClient = MQTT 3.1.1 seulement |
| TLS/mTLS | **mbedTLS integre** (hardware accelere) | mbedTLS aussi, mais config complexe |
| Secure Boot + Flash Encryption | **Supporte nativement** (efuses) | Non supporte |
| NVS (Non-Volatile Storage) | **API stable** (chiffre optionnel) | Preferences.h (wrapper simplifie) |
| Taille binaire | Configurable (menu config) | Overhead Arduino core (~200KB) |
| Multi-core (dual core) | **xTaskCreatePinnedToCore** | loop() single core, manual ESP32 API |
| DMA / I2S / ADC continu | **API directe** (adc_continuous) | Non expose nativement |
| Communaute industrielle | Large (produits certifies) | Hobbyiste / prototypage rapide |
| Debugging | **JTAG + OpenOCD** (breakpoints, watchpoints) | Serial.println() principalement |
| Toolchain | CMake + Ninja (reproductible) | Arduino IDE / PlatformIO |
| Courbe d'apprentissage | **Plus raide** | Plus douce |

### Justification detaillee

1. **FreeRTOS necessaire** : Le firmware PyroSense requiert 4+ taches concurrentes avec des priorites differentes, des queues inter-taches, et du core pinning (acquisition temps reel sur Core 1, traitement et communication sur Core 0). Arduino cache FreeRTOS derriere `loop()` ce qui interdit ce niveau de controle.

2. **MQTT 5.0 obligatoire** : Le protocole edge-cloud utilise les MQTT 5.0 user properties (cert-fp, fw-ver, payload-ver, compress) qui ne sont pas supportees par PubSubClient.

3. **Secure Boot + Flash Encryption** : La securite device (anti-clonage, protection firmware) necessite les fonctionnalites hardware de l'ESP32-S3 (efuses, secure boot v2) uniquement accessibles via ESP-IDF.

4. **ADC continu (DMA)** : L'echantillonnage a 860 SPS (ADS1115 externe) voire 16kHz (futur ADC rapide) requiert le driver `adc_continuous` d'ESP-IDF pour un sampling sans jitter.

5. **OTA A/B** : Le firmware OTA avec rollback automatique (3 boot failures → revert) est une fonctionnalite native d'ESP-IDF non reproductible proprement sous Arduino.

6. **Tests unitaires** : ESP-IDF fournit un framework de test natif (`unity`) integre au build system, avec execution sur target ou host.

### Compromis accepte

- Courbe d'apprentissage plus raide → mitigue par la documentation exhaustive et le design modulaire.
- Build plus lent → mitigue par build incremental et ccache.
- Pas de `Serial.println()` rapide → remplace par ESP_LOG* (filtrable, categorise, desactivable).

---

## 2. Architecture firmware

### Vision d'ensemble

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           APPLICATION FIRMWARE                                    │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                        ORCHESTRATION (main_app)                              ││
│  │  - State machine device lifecycle                                            ││
│  │  - Task creation + supervision                                               ││
│  │  - Error escalation + recovery                                               ││
│  └───────────┬────────────────┬───────────────────┬────────────────────┬───────┘│
│              │                │                   │                    │         │
│  ┌───────────▼──────┐ ┌──────▼────────┐ ┌───────▼──────────┐ ┌──────▼───────┐ │
│  │   ACQUISITION    │ │  PROCESSING   │ │  COMMUNICATION   │ │ DIAGNOSTICS  │ │
│  │                  │ │               │ │                  │ │              │ │
│  │ sensors/         │ │ signal_proc/  │ │ connectivity/    │ │ diagnostics/ │ │
│  │ - current        │ │ - rms         │ │ - wifi           │ │ - health     │ │
│  │ - temperature    │ │ - thd         │ │ - mqtt           │ │ - heartbeat  │ │
│  │ - voltage        │ │ - transient   │ │ - reconnect      │ │ - self_test  │ │
│  │ - hf_detect      │ │ - arc_detect  │ │ - backoff        │ │ - watchdog   │ │
│  │                  │ │ - smoothing   │ │                  │ │              │ │
│  │ Freq: continues  │ │ - quality     │ │ telemetry/       │ │ Freq: 60s   │ │
│  │ Core: 1          │ │               │ │ - payload        │ │ Core: 0     │ │
│  │ Prio: 4          │ │ Freq: 1-10s   │ │ - versioning     │ │ Prio: 1     │ │
│  │                  │ │ Core: 0       │ │ - timestamp      │ │              │ │
│  └────────┬─────────┘ │ Prio: 3       │ │ - validation     │ └──────────────┘ │
│           │            └───────┬───────┘ │                  │                   │
│           │ raw_queue          │         │ storage/         │                   │
│           │ (64 items)         │         │ - buffer         │                   │
│           └───────────────────▶│         │ - file_queue     │                   │
│                                │         │ - retry          │                   │
│                     feat_queue │         │                  │                   │
│                     (32 items) │         │ security/        │                   │
│                                └────────▶│ - device_id      │                   │
│                                          │ - credentials    │                   │
│                                          │ - hmac           │                   │
│                                          │ - nonce          │                   │
│                                          └──────────────────┘                   │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                           PLATFORM (HAL / BSP)                               ││
│  │  boot/ — config/ — hal/ (I2C, SPI, GPIO, ADC, NVS, SPIFFS, WiFi, Timer)     ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                           ESP-IDF 5.x (FreeRTOS, mbedTLS, lwIP, esp-mqtt)    ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Principes d'architecture

| Principe | Application |
|----------|-------------|
| **Separation of concerns** | Chaque module a une responsabilite unique. Aucun module ne connait les details d'implementation d'un autre. |
| **Dependency inversion** | Les modules metier (processing, telemetry) dependent d'interfaces abstraites (HAL), pas d'implementations hardware. |
| **Testability** | Chaque module expose un header public avec des fonctions pures autant que possible. Le HAL est mockable pour tests host. |
| **Fail-safe** | Un module en erreur n'entraine pas la chute des autres. Watchdog par task. Degradation gracieuse. |
| **Zero secrets hardcodes** | Tous les secrets (WiFi, MQTT creds, device keys) en NVS chiffre ou secure element. Jamais dans le code source. |
| **Configuration par environnement** | NVS pour runtime config. `sdkconfig` pour build-time. Pas de `#ifdef DEV` dans le code metier. |
| **Observability** | Chaque module publie des metriques et des logs structures. Health aggrege l'etat global. |
| **Idempotency** | Les messages sont identifies par `messageId` (UUID) + `sequence` (monotonique). Les retransmissions sont sures. |

### Couches architecturales

```
┌─────────────────────────────────────────────────────────────┐
│ Layer 4: APPLICATION (orchestration, state machine)          │
│          Depend de: Layer 3                                  │
│          Ne depend PAS de: ESP-IDF directement              │
├─────────────────────────────────────────────────────────────┤
│ Layer 3: SERVICES (sensors, processing, telemetry,          │
│          connectivity, security, storage, diagnostics)       │
│          Depend de: Layer 2 (HAL interfaces)                │
│          Ne depend PAS de: ESP-IDF directement              │
├─────────────────────────────────────────────────────────────┤
│ Layer 2: HAL / BSP (hardware abstraction layer)             │
│          Encapsule: I2C, SPI, GPIO, ADC, NVS, SPIFFS,      │
│          WiFi, Timer, UART                                   │
│          Depend de: Layer 1 (ESP-IDF)                       │
├─────────────────────────────────────────────────────────────┤
│ Layer 1: ESP-IDF 5.x (FreeRTOS, mbedTLS, lwIP, esp-mqtt,   │
│          drivers, partition manager, OTA, secure boot)       │
└─────────────────────────────────────────────────────────────┘
```

**Regle stricte** : Seul le Layer 2 (HAL) importe des headers ESP-IDF. Les Layers 3 et 4 utilisent des types et fonctions definis dans `hal/` et `common/`. Cela permet de compiler et tester les modules metier sur un host Linux/Mac sans ESP32.

---

## 3. Diagramme de modules

### Interactions inter-modules

```
                      ┌─────────────────┐
                      │   main_app.c    │
                      │  (orchestrator) │
                      └────────┬────────┘
                               │ creates + supervises
              ┌────────────────┼────────────────────────────┐
              │                │                │            │
              ▼                ▼                ▼            ▼
    ┌─────────────────┐ ┌───────────┐ ┌─────────────┐ ┌──────────┐
    │ task_acquisition│ │task_proc  │ │task_comms    │ │task_diag │
    │ (Core 1, P4)   │ │(Core 0,P3)│ │(Core 0, P2) │ │(Core0,P1)│
    └────────┬────────┘ └─────┬─────┘ └──────┬──────┘ └─────┬────┘
             │                │               │              │
             │ uses           │ uses          │ uses         │ uses
             ▼                ▼               ▼              ▼
    ┌────────────────┐ ┌───────────────┐ ┌────────────┐ ┌──────────┐
    │  sensors/      │ │signal_proc/   │ │connectivity│ │diagnostics│
    │  - current.h   │ │- rms.h        │ │- wifi.h    │ │- health.h│
    │  - temp.h      │ │- thd.h        │ │- mqtt.h    │ │- hbeat.h │
    │  - voltage.h   │ │- transient.h  │ │- backoff.h │ │- selftest│
    │  - hf_detect.h │ │- arc.h        │ │            │ │- wdog.h  │
    │               │ │- smoothing.h  │ │telemetry/  │ └──────────┘
    │               │ │- quality.h    │ │- payload.h │
    └───────┬───────┘ └───────┬───────┘ │- version.h │
            │                 │         │- timestamp.h│
            │                 │         │- validate.h│
            │                 │         │            │
            │                 │         │security/   │
            │                 │         │- device_id.h│
            │                 │         │- creds.h   │
            │                 │         │- hmac.h    │
            │                 │         │- nonce.h   │
            │                 │         │- anti_rep.h│
            │                 │         │            │
            │                 │         │storage/    │
            │                 │         │- buffer.h  │
            │                 │         │- fqueue.h  │
            │                 │         │- retry.h   │
            │                 │         └─────┬──────┘
            │                 │               │
            ▼                 ▼               ▼
    ┌─────────────────────────────────────────────────────┐
    │                     hal/                              │
    │  i2c.h | spi.h | gpio.h | adc.h | nvs.h | spiffs.h │
    │  wifi.h | timer.h | uart.h | crypto_hw.h            │
    └─────────────────────────────────────────────────────┘
            │
            ▼
    ┌─────────────────────────────────────────────────────┐
    │                   ESP-IDF 5.x                        │
    └─────────────────────────────────────────────────────┘
```

### Communication inter-modules (Queues FreeRTOS)

| Queue | Producteur | Consommateur | Taille | Type element |
|-------|-----------|--------------|:------:|--------------|
| `raw_samples_queue` | task_acquisition | task_processing | 64 | `raw_sample_block_t` (1s de samples) |
| `feature_queue` | task_processing | task_comms | 32 | `feature_frame_t` (features 5s) |
| `event_queue` | task_processing | task_comms | 16 | `electrical_event_t` (urgences) |
| `command_queue` | task_comms (MQTT sub) | main_app | 8 | `device_command_t` (cloud → device) |
| `health_queue` | task_diag | task_comms | 4 | `health_report_t` (heartbeat) |
| `log_queue` | tous modules | task_diag | 64 | `log_entry_t` (structured log) |

### Semaphores et Mutex

| Resource | Type | Protege |
|----------|------|---------|
| `nvs_mutex` | Mutex | Acces concurrent a NVS (config, credentials) |
| `spiffs_mutex` | Mutex | Acces concurrent au buffer SPIFFS |
| `i2c_mutex` | Mutex | Bus I2C partage (ADS1115 + ATECC608B) |
| `state_mutex` | Mutex | Device state machine transitions |
| `seq_counter_sem` | Binary semaphore | Sequence monotonique atomique |

---

## 4. Structure de dossiers

```
pyrosense-firmware/
├── CMakeLists.txt                      # Top-level CMake (ESP-IDF project)
├── sdkconfig.defaults                  # Default configuration (non-secret)
├── sdkconfig.defaults.dev              # Dev overrides (verbose logs, no secure boot)
├── sdkconfig.defaults.prod             # Prod overrides (secure boot, flash encrypt, min logs)
├── partitions.csv                      # Partition table (OTA A/B + NVS + SPIFFS)
├── version.txt                         # Firmware version (semantic versioning)
├── CHANGELOG.md                        # Release notes
│
├── main/
│   ├── CMakeLists.txt
│   ├── main.c                          # Entry point → boot sequence → create tasks
│   └── Kconfig.projbuild               # Menu config options
│
├── components/
│   ├── boot/
│   │   ├── CMakeLists.txt
│   │   ├── include/
│   │   │   └── boot.h                  # boot_init(), boot_check_firmware_version()
│   │   └── src/
│   │       ├── boot.c                  # System init sequence
│   │       ├── config_loader.c         # Load NVS config → config_t struct
│   │       └── firmware_version.c      # Version check, OTA status report
│   │
│   ├── hal/
│   │   ├── CMakeLists.txt
│   │   ├── include/
│   │   │   ├── hal_i2c.h              # I2C bus abstraction
│   │   │   ├── hal_spi.h              # SPI bus abstraction
│   │   │   ├── hal_gpio.h            # GPIO pin abstraction
│   │   │   ├── hal_adc.h             # ADC reading abstraction
│   │   │   ├── hal_nvs.h             # NVS key-value store
│   │   │   ├── hal_spiffs.h          # SPIFFS file system
│   │   │   ├── hal_wifi.h            # WiFi connection
│   │   │   ├── hal_timer.h           # Hardware timers
│   │   │   ├── hal_uart.h            # UART (debug)
│   │   │   └── hal_crypto.h          # Hardware crypto (SHA, AES, RNG)
│   │   ├── src/
│   │   │   ├── hal_i2c_esp32.c       # ESP-IDF i2c_master implementation
│   │   │   ├── hal_spi_esp32.c       # ESP-IDF spi_master implementation
│   │   │   ├── hal_gpio_esp32.c
│   │   │   ├── hal_adc_esp32.c       # adc_continuous driver
│   │   │   ├── hal_nvs_esp32.c
│   │   │   ├── hal_spiffs_esp32.c
│   │   │   ├── hal_wifi_esp32.c
│   │   │   ├── hal_timer_esp32.c
│   │   │   └── hal_crypto_esp32.c    # mbedTLS hardware acceleration
│   │   └── mock/
│   │       ├── hal_i2c_mock.c         # Mock pour tests host
│   │       ├── hal_spi_mock.c
│   │       ├── hal_gpio_mock.c
│   │       ├── hal_adc_mock.c
│   │       ├── hal_nvs_mock.c
│   │       ├── hal_spiffs_mock.c
│   │       └── hal_wifi_mock.c
│   │
│   ├── sensors/
│   │   ├── CMakeLists.txt
│   │   ├── include/
│   │   │   ├── sensor_current.h       # current_init(), current_read_rms()
│   │   │   ├── sensor_temperature.h   # temp_init(), temp_read_celsius()
│   │   │   ├── sensor_voltage.h       # voltage_init(), voltage_read_rms()
│   │   │   └── sensor_hf_detect.h     # hf_init(), hf_get_noise_level()
│   │   └── src/
│   │       ├── sensor_current.c       # ADS1115 driver, CT calibration
│   │       ├── sensor_temperature.c   # MAX31865 driver, PT100 conversion
│   │       ├── sensor_voltage.c       # ZMPT101B driver (placeholder si absent)
│   │       └── sensor_hf_detect.c     # LM393 ISR + zero-crossing rate
│   │
│   ├── signal_processing/
│   │   ├── CMakeLists.txt
│   │   ├── include/
│   │   │   ├── sig_rms.h             # rms_compute(samples, n) → float
│   │   │   ├── sig_thd.h             # thd_compute(samples, n) → thd_result_t
│   │   │   ├── sig_transient.h       # transient_detect(samples) → uint16_t count
│   │   │   ├── sig_arc.h             # arc_compute_energy(samples) → float
│   │   │   ├── sig_smoothing.h       # ema_update(state, value, alpha) → float
│   │   │   └── sig_quality.h         # quality_compute(metrics) → uint8_t
│   │   └── src/
│   │       ├── sig_rms.c             # sum-of-squares, sqrt(sum/N)
│   │       ├── sig_thd.c             # FFT 1024, Hanning window, harmonics
│   │       ├── sig_transient.c       # Threshold crossing counter
│   │       ├── sig_arc.c             # HF band energy integration
│   │       ├── sig_smoothing.c       # EMA filter, moving average
│   │       └── sig_quality.c         # Multi-criteria scoring (0-100)
│   │
│   ├── telemetry/
│   │   ├── CMakeLists.txt
│   │   ├── include/
│   │   │   ├── telemetry_payload.h    # payload_build(features) → buffer
│   │   │   ├── telemetry_version.h    # PAYLOAD_VERSION, version_string()
│   │   │   ├── telemetry_timestamp.h  # timestamp_now(), timestamp_validate()
│   │   │   └── telemetry_validate.h   # validate_frame(frame) → bool
│   │   └── src/
│   │       ├── telemetry_payload.c    # CBOR encoding (tinycbor)
│   │       ├── telemetry_version.c    # Version field injection
│   │       ├── telemetry_timestamp.c  # NTP sync + monotonic counter
│   │       └── telemetry_validate.c   # Range checks, completeness
│   │
│   ├── connectivity/
│   │   ├── CMakeLists.txt
│   │   ├── include/
│   │   │   ├── conn_wifi.h           # wifi_connect(), wifi_status()
│   │   │   ├── conn_mqtt.h           # mqtt_init(), mqtt_publish(), mqtt_subscribe()
│   │   │   ├── conn_reconnect.h      # reconnect_schedule(), reconnect_reset()
│   │   │   ├── conn_backoff.h        # backoff_next_delay() → ms
│   │   │   └── conn_offline.h        # offline_enter(), offline_exit(), offline_mode()
│   │   └── src/
│   │       ├── conn_wifi.c           # WiFi STA, event handler, RSSI monitor
│   │       ├── conn_mqtt.c           # esp-mqtt 5.0, mTLS, user properties
│   │       ├── conn_reconnect.c      # Reconnection state machine
│   │       ├── conn_backoff.c        # Exponential backoff with jitter
│   │       └── conn_offline.c        # Offline detection, mode transitions
│   │
│   ├── security/
│   │   ├── CMakeLists.txt
│   │   ├── include/
│   │   │   ├── sec_device_id.h       # device_id_get() → char*
│   │   │   ├── sec_credentials.h     # creds_load(), creds_store(), creds_wipe()
│   │   │   ├── sec_hmac.h            # hmac_sign(payload, len) → hmac[32]
│   │   │   ├── sec_nonce.h           # nonce_generate() → uint8_t[16]
│   │   │   ├── sec_anti_replay.h     # replay_check(seq) → bool
│   │   │   └── sec_element.h         # atecc_init(), atecc_sign() (future)
│   │   └── src/
│   │       ├── sec_device_id.c       # Read from NVS or efuse
│   │       ├── sec_credentials.c     # NVS encrypted namespace, cert/key load
│   │       ├── sec_hmac.c            # mbedTLS HMAC-SHA256
│   │       ├── sec_nonce.c           # Hardware RNG (esp_random)
│   │       ├── sec_anti_replay.c     # Sliding window (64-bit bitmap)
│   │       └── sec_element.c         # ATECC608B stub → real driver later
│   │
│   ├── storage/
│   │   ├── CMakeLists.txt
│   │   ├── include/
│   │   │   ├── store_buffer.h        # buffer_init(), buffer_push(), buffer_pop()
│   │   │   ├── store_fqueue.h        # fqueue_enqueue(), fqueue_dequeue(), fqueue_count()
│   │   │   └── store_retry.h         # retry_schedule(), retry_get_next()
│   │   └── src/
│   │       ├── store_buffer.c        # RAM ring buffer (fast, volatile)
│   │       ├── store_fqueue.c        # SPIFFS file queue (persistent, 4MB)
│   │       └── store_retry.c         # Retry logic with exponential backoff
│   │
│   ├── diagnostics/
│   │   ├── CMakeLists.txt
│   │   ├── include/
│   │   │   ├── diag_health.h         # health_collect() → health_report_t
│   │   │   ├── diag_heartbeat.h      # heartbeat_build() → buffer
│   │   │   ├── diag_selftest.h       # selftest_run() → selftest_result_t
│   │   │   └── diag_watchdog.h       # wdog_init(), wdog_feed(), wdog_register_task()
│   │   └── src/
│   │       ├── diag_health.c         # CPU%, heap, stack HWM, temp interne, uptime
│   │       ├── diag_heartbeat.c      # CBOR heartbeat payload construction
│   │       ├── diag_selftest.c       # ADC loopback, NVS read/write, SPIFFS free
│   │       └── diag_watchdog.c       # Task watchdog subscription
│   │
│   └── common/
│       ├── CMakeLists.txt
│       ├── include/
│       │   ├── pyro_types.h          # feature_frame_t, electrical_event_t, etc.
│       │   ├── pyro_config.h         # config_t, config_get(), config_set()
│       │   ├── pyro_errors.h         # pyro_err_t enum, error context struct
│       │   ├── pyro_log.h            # PYRO_LOGI, PYRO_LOGW, PYRO_LOGE macros
│       │   └── pyro_constants.h      # Timing, sizes, thresholds (from config)
│       └── src/
│           ├── pyro_config.c         # Config management (NVS backed)
│           ├── pyro_errors.c         # Error registry, callback registration
│           └── pyro_log.c            # Structured logging (JSON optional)
│
├── test/
│   ├── CMakeLists.txt                 # Host-based test build
│   ├── unit/
│   │   ├── test_rms.c                # RMS computation (known inputs → expected outputs)
│   │   ├── test_thd.c                # THD computation (pure sine, known harmonics)
│   │   ├── test_transient.c          # Transient detection (injected spikes)
│   │   ├── test_arc.c                # Arc energy (synthetic HF burst)
│   │   ├── test_quality.c            # Signal quality scoring (boundary cases)
│   │   ├── test_payload.c            # CBOR encode/decode roundtrip
│   │   ├── test_hmac.c               # HMAC sign/verify (known vectors)
│   │   ├── test_nonce.c              # Nonce uniqueness, anti-replay window
│   │   ├── test_buffer.c             # Ring buffer push/pop, overflow
│   │   ├── test_fqueue.c             # File queue FIFO, persistence
│   │   ├── test_backoff.c            # Exponential backoff progression
│   │   ├── test_timestamp.c          # Timestamp monotonicity, drift detection
│   │   ├── test_validate.c           # Payload validation (range, completeness)
│   │   └── test_state_machine.c      # State transitions (valid + invalid)
│   ├── integration/
│   │   ├── test_sensor_to_feature.c  # Full pipeline: mock ADC → features
│   │   ├── test_feature_to_payload.c # features → CBOR → validate
│   │   ├── test_offline_buffer.c     # Offline → buffer → drain → verify order
│   │   └── test_mqtt_publish.c       # Feature → MQTT publish (mock broker)
│   └── target/
│       ├── test_adc_read.c           # On-target: ADS1115 real read
│       ├── test_temp_read.c          # On-target: MAX31865 real read
│       ├── test_wifi_connect.c       # On-target: WiFi association
│       ├── test_mqtt_connect.c       # On-target: MQTT+TLS handshake
│       └── test_nvs_creds.c          # On-target: NVS write/read credentials
│
├── tools/
│   ├── provision.py                   # Device provisioning script (serial)
│   ├── flash_credentials.py          # Inject certs/keys via serial + NVS
│   ├── monitor_payload.py            # Subscribe MQTT, decode CBOR, display
│   ├── signal_generator.py           # Control DDS AD9833 for test signals
│   └── firmware_sign.py              # Sign firmware binary (ECDSA P-256)
│
├── certs/
│   ├── .gitkeep                       # Certificates never committed
│   └── README.md                      # Instructions: how to generate/inject certs
│
└── docs/
    ├── pinout.md                      # GPIO assignments
    ├── calibration.md                 # Sensor calibration procedure
    └── flashing.md                    # Build + flash instructions
```

### Conventions de nommage

| Element | Convention | Exemple |
|---------|-----------|---------|
| Fichiers source | `module_function.c` | `sig_rms.c` |
| Headers | `module_function.h` | `sig_rms.h` |
| Fonctions publiques | `module_verb_noun()` | `rms_compute()` |
| Fonctions privees (static) | `_verb_noun()` | `_apply_window()` |
| Types (structs) | `module_noun_t` | `feature_frame_t` |
| Enums | `PYRO_MODULE_VALUE` | `PYRO_STATE_ACTIVE` |
| Constantes | `PYRO_MODULE_NOUN` | `PYRO_MQTT_MAX_PAYLOAD` |
| Macros | `PYRO_VERB_NOUN()` | `PYRO_LOGI()` |
| Queues | `module_noun_queue` | `feature_queue` |

---

## 5. Etats du device

### Diagramme d'etats

```
                            ┌─────────────────────────────────────────────────┐
                            │                                                 │
    POWER ON                ▼                                                 │
       │            ┌──────────────┐                                          │
       └───────────▶│   BOOTING    │                                          │
                    │              │                                          │
                    │ - HW init    │                                          │
                    │ - NVS load   │                                          │
                    │ - Self-test  │                                          │
                    └──────┬───────┘                                          │
                           │                                                  │
              ┌────────────┼─────────────┐                                   │
              │ (no creds) │             │ (has creds)                        │
              ▼            │             ▼                                    │
    ┌──────────────────┐   │   ┌─────────────────┐                          │
    │  PROVISIONING    │   │   │  CONNECTING      │                          │
    │                  │   │   │                  │◀─────────────────┐       │
    │ - AP mode        │   │   │ - WiFi connect   │                  │       │
    │ - Wait config    │   │   │ - MQTT connect   │                  │       │
    │ - CSR generate   │   │   │ - mTLS handshake │                  │       │
    │ - Enroll API     │   │   │ - Session resume │                  │       │
    │ - Store certs    │   │   └────────┬─────────┘                  │       │
    └────────┬─────────┘   │            │                             │       │
             │             │            │ (connected)                 │       │
             │ (enrolled)  │            ▼                             │       │
             │             │   ┌─────────────────┐                   │       │
             └─────────────┴──▶│    ACTIVE       │                   │       │
                               │                 │                   │       │
                               │ - Sampling      │                   │       │
                               │ - Processing    │                   │       │
                               │ - Publishing    │                   │       │
                               │ - Heartbeating  │                   │       │
                               └───┬─────┬───────┘                   │       │
                                   │     │                           │       │
                    (WiFi lost)    │     │  (cert revoked /          │       │
                                   │     │   command REVOKE)         │       │
                                   ▼     │                           │       │
                    ┌──────────────────┐  │                          │       │
                    │OFFLINE_BUFFERING │  │                          │       │
                    │                  │  │                          │       │
                    │ - Keep sampling  │  │                          │       │
                    │ - Buffer SPIFFS  │  │                          │       │
                    │ - Try reconnect  │  │                          │       │
                    │ - Degrade modes  │  │                          │       │
                    └──────┬───┬───────┘  │                          │       │
                           │   │          │                          │       │
              (reconnected)│   │(72h+ or  │                          │       │
                           │   │ critical │                          │       │
                           │   │ failure) │                          │       │
                           │   ▼          ▼                          │       │
                           │ ┌──────────────────┐  ┌────────────┐   │       │
                           │ │   DEGRADED       │  │  REVOKED   │   │       │
                           │ │                  │  │            │   │       │
                           │ │ - Minimal ops    │  │ - Wipe creds│  │       │
                           │ │ - Only urgent    │  │ - Stop MQTT │  │       │
                           │ │   events detect  │  │ - LED rouge │  │       │
                           │ │ - No features tx │  │ - Await     │  │       │
                           │ │ - Await recovery │  │   re-enroll │  │       │
                           │ └───────┬──────────┘  └────────────┘   │       │
                           │         │                               │       │
                           │         │ (manual reset                 │       │
                           │         │  or OTA fix)                  │       │
                           │         ▼                               │       │
                           │ ┌──────────────────┐                   │       │
                           │ │     ERROR        │                   │       │
                           │ │                  │                   │       │
                           │ │ - Log error ctx  │                   │       │
                           │ │ - Attempt recov  │                   │       │
                           │ │ - If fatal: reboot                   │       │
                           │ │ - Count reboots  │                   │       │
                           │ └───────┬──────────┘                   │       │
                           │         │ (recovery success)           │       │
                           │         └──────────────────────────────┘       │
                           │                                                │
                           └────────────────────────────────────────────────┘
                                   (drain buffer then resume ACTIVE)
```

### Table de transitions

| Etat source | Evenement | Etat destination | Action |
|-------------|-----------|------------------|--------|
| — | Power ON | BOOTING | HW init, NVS load, self-test |
| BOOTING | Self-test OK + no credentials | PROVISIONING | Start AP mode, LED bleu clignotant |
| BOOTING | Self-test OK + credentials found | CONNECTING | WiFi + MQTT connect |
| BOOTING | Self-test FAIL (3x boot loop) | ERROR | Log, LED rouge rapide, await OTA |
| PROVISIONING | Enrollment success + cert stored | CONNECTING | Stop AP, start STA mode |
| PROVISIONING | Timeout 10 min | ERROR | LED rouge, await manual reset |
| CONNECTING | MQTT CONNACK received | ACTIVE | Start sampling, LED vert fixe |
| CONNECTING | 5 consecutive failures | OFFLINE_BUFFERING | Start buffering, LED orange |
| ACTIVE | WiFi disconnect > 30s | OFFLINE_BUFFERING | Start buffering |
| ACTIVE | MQTT disconnect > 60s | OFFLINE_BUFFERING | Start buffering |
| ACTIVE | Command REVOKE received | REVOKED | Wipe creds, disconnect |
| ACTIVE | Fatal sensor error | ERROR | Stop sampling, log context |
| ACTIVE | OTA command received | BOOTING | Download, verify, apply, reboot |
| OFFLINE_BUFFERING | WiFi + MQTT restored | ACTIVE | Drain buffer (10 msg/s) |
| OFFLINE_BUFFERING | 72h elapsed | DEGRADED | Reduce to events-only |
| OFFLINE_BUFFERING | Buffer full (100%) | DEGRADED | FIFO eviction, urgent only |
| DEGRADED | Connectivity restored | ACTIVE | Drain remaining, resume full |
| DEGRADED | No recovery 7 days | ERROR | Force reboot, factory reset option |
| ERROR | Recovery routine success | CONNECTING | Retry connection |
| ERROR | 5 consecutive boot failures | ERROR (stuck) | Await OTA or factory reset |
| REVOKED | Re-enrollment triggered | PROVISIONING | New CSR, new enrollment |

### Sous-etats OFFLINE_BUFFERING

| Mode | Declencheur | Comportement |
|------|-------------|--------------|
| NOMINAL | Buffer < 50% | Features 5s + events + heartbeat local |
| REDUCED | Buffer 50-75% OR offline > 24h | Features 30s + events + heartbeat 5min |
| MINIMAL | Buffer 75-90% OR offline > 48h | Features 5min + events seulement |
| EMERGENCY | Buffer > 90% OR offline > 72h | Events urgents seulement, FIFO eviction anciens |

---

## 6. Types de payload

### 6.1 Feature Frame (periodic, every 5s)

```c
// Taille estimee: ~180 bytes CBOR avant compression, ~120 bytes apres LZ4
typedef struct {
    uint8_t  version;             // PAYLOAD_VERSION = 2
    char     message_id[37];      // UUID v4 string
    uint32_t sequence;            // Monotonic counter (NVS backed, survives reboot)
    uint32_t timestamp_unix;      // Seconds since epoch (NTP synced)
    uint16_t timestamp_ms;        // Milliseconds within second
    bool     is_drain;            // true = replayed from buffer

    struct {
        float rms[3];             // Amperes RMS per phase (L1, L2, L3)
        float peak[3];            // Amperes peak per phase
        float crest_factor;       // Peak / RMS (dimensionless)
        float zero_crossing_hz;   // Frequency (should be ~50 Hz)
    } current;

    struct {
        float power_factor;       // 0.0 - 1.0 (NaN if V unavailable)
        float active_power_w;     // Watts (NaN if V unavailable)
    } power;

    struct {
        float sensor[2];          // °C connection points
        float ambient;            // °C ambient
        float rate_of_change;     // °C/min (5-min sliding window)
    } temperature;

    struct {
        float thd_percent;        // Total harmonic distortion %
        float harmonics[4];       // H3, H5, H7, H9 in %
    } harmonics;                  // Updated every 10s, repeated until next

    struct {
        float arc_energy;         // 0.0 - 1.0 (HF band energy)
        uint16_t transient_count; // Events per second
        float hf_noise_level;     // 0.0 - 1.0 (normalized)
    } arc;

    uint8_t signal_quality;       // 0 - 100 (self-diagnostic)
    uint8_t hmac[32];             // HMAC-SHA256 over all preceding fields
} feature_frame_t;
```

### 6.2 Electrical Event (urgent, immediate)

```c
// Taille estimee: ~300 bytes JSON (events restent en JSON pour lisibilite debug)
typedef struct {
    uint8_t  version;
    char     message_id[37];
    uint32_t sequence;
    uint32_t timestamp_unix;
    uint16_t timestamp_ms;

    enum {
        EVENT_ARC_DETECTED = 1,
        EVENT_OVERLOAD = 2,
        EVENT_OVERHEATING = 3,
        EVENT_VOLTAGE_SAG = 4,
        EVENT_VOLTAGE_SWELL = 5,
        EVENT_PHASE_LOSS = 6,
    } event_type;

    enum {
        SEVERITY_LOW = 1,
        SEVERITY_MEDIUM = 2,
        SEVERITY_HIGH = 3,
        SEVERITY_CRITICAL = 4,
    } severity;

    union {
        struct {
            float arc_energy;
            float peak_amplitude;
            uint32_t duration_us;
            uint8_t phase;          // 0=L1, 1=L2, 2=L3
        } arc;
        struct {
            float current_rms;
            float rated_current;
            float overload_ratio;   // current / rated
        } overload;
        struct {
            float temperature;
            float rate_of_change;
            float ambient;
        } overheating;
    } data;

    // Context snapshot at event time
    struct {
        float rms_l1;
        float temperature;
        uint8_t signal_quality;
    } context;

    uint8_t hmac[32];
} electrical_event_t;
```

### 6.3 Heartbeat (status, every 60s)

```c
// Taille estimee: ~50 bytes CBOR, QoS 0
typedef struct {
    uint8_t  version;
    uint32_t sequence;
    uint32_t timestamp_unix;
    uint32_t uptime_seconds;
    uint32_t free_heap_bytes;
    int8_t   wifi_rssi_dbm;
    uint8_t  buffer_percent;      // 0 - 100
    uint8_t  signal_quality;      // 0 - 100
    uint8_t  device_state;        // enum device_state_t
    uint8_t  cpu_percent;         // 0 - 100
    int8_t   internal_temp_c;     // ESP32 die temperature
} heartbeat_t;
```

### 6.4 Health Report (extended, every 5 min)

```c
// Taille estimee: ~200 bytes CBOR, QoS 1
typedef struct {
    uint8_t  version;
    uint32_t sequence;
    uint32_t timestamp_unix;

    struct {
        uint32_t uptime_seconds;
        uint32_t reboot_count;       // Since last OTA
        uint8_t  last_reboot_reason; // ESP_RST_* enum
    } system;

    struct {
        uint32_t free_heap;
        uint32_t min_free_heap;      // All-time low (heap watermark)
        uint16_t largest_free_block;
        uint8_t  fragmentation_pct;
    } memory;

    struct {
        uint32_t spiffs_used_bytes;
        uint32_t spiffs_total_bytes;
        uint32_t queued_messages;
        uint32_t dropped_messages;   // Since last report
    } storage;

    struct {
        int8_t   rssi_dbm;
        uint32_t disconnect_count;   // Since boot
        uint32_t mqtt_publish_ok;    // Since last report
        uint32_t mqtt_publish_fail;
        uint16_t avg_latency_ms;     // PUBACK round-trip
    } connectivity;

    struct {
        bool     adc_ok;
        bool     temp_ok;
        bool     nvs_ok;
        bool     spiffs_ok;
        uint8_t  signal_quality;
    } selftest;

    char firmware_version[16];        // "1.2.3"
    uint8_t hmac[32];
} health_report_t;
```

### 6.5 Device Command (cloud → device, subscribed)

```c
typedef struct {
    uint8_t  version;
    char     command_id[37];         // For ack/nack response
    uint32_t timestamp_unix;

    enum {
        CMD_UPDATE_CONFIG = 1,       // Change intervals, thresholds
        CMD_ROTATE_CERT = 2,         // Certificate rotation trigger
        CMD_REBOOT = 3,              // Graceful reboot
        CMD_OTA_AVAILABLE = 4,       // New firmware ready
        CMD_FACTORY_RESET = 5,       // Wipe all + re-provision
        CMD_SET_CALIBRATION = 6,     // Update sensor calibration
        CMD_REVOKE = 7,              // Device revocation
    } command_type;

    union {
        struct {
            uint16_t feature_interval_s;
            uint16_t stats_interval_s;
            uint16_t heartbeat_interval_s;
            float    arc_threshold;
            float    overload_ratio;
            float    temp_rate_threshold;
        } config;
        struct {
            char challenge_nonce[32];
            uint16_t timeout_seconds;
        } rotate_cert;
        struct {
            char firmware_url[128];
            char expected_sha256[65];
            uint32_t size_bytes;
        } ota;
        struct {
            float gain[3];           // Per-phase calibration
            float offset[3];
            float temp_offset;
        } calibration;
    } payload;
} device_command_t;
```

### Encoding et transport

| Payload | Encoding | Compression | QoS | Topic pattern |
|---------|----------|-------------|:---:|---------------|
| Feature Frame | CBOR | LZ4 | 1 | `pyrosense/{tid}/{did}/features/periodic` |
| Electrical Event | JSON | None | 1 | `pyrosense/{tid}/{did}/events/electrical` |
| Heartbeat | CBOR | None | 0 | `pyrosense/{tid}/{did}/status/heartbeat` |
| Health Report | CBOR | None | 1 | `pyrosense/{tid}/{did}/status/health` |
| Command (sub) | JSON | None | 2 | `pyrosense/{tid}/{did}/command/#` |
| Command Ack | JSON | None | 1 | `pyrosense/{tid}/{did}/command/ack` |

---

## 7. Pseudo-code des taches FreeRTOS

### 7.1 Task Acquisition (Core 1, Priority 4)

```c
// Responsabilite: lire les capteurs, produire des blocs d'echantillons bruts
// Periode: continue (timer ISR declenche lecture)
// Stack: 4096 bytes
// Watchdog: 5s timeout

void task_acquisition(void *params) {
    sensor_current_init();       // ADS1115 config, I2C
    sensor_temperature_init();   // MAX31865 config, SPI
    sensor_voltage_init();       // ZMPT101B ou stub
    sensor_hf_detect_init();     // GPIO ISR setup

    raw_sample_block_t block;
    uint32_t sample_index = 0;
    TickType_t last_temp_read = 0;

    while (true) {
        wdog_feed(TASK_ACQUISITION);

        // ADC sampling (860 SPS via ADS1115 I2C, or timer-triggered)
        // Accumulate samples into block (1 second worth)
        for (int i = 0; i < SAMPLES_PER_BLOCK; i++) {
            block.current_l1[i] = sensor_current_read_raw(PHASE_L1);
            block.current_l2[i] = sensor_current_read_raw(PHASE_L2);
            block.current_l3[i] = sensor_current_read_raw(PHASE_L3);

            if (sensor_voltage_available()) {
                block.voltage[i] = sensor_voltage_read_raw();
            }

            // Precise timing (hardware timer callback or busy wait)
            vTaskDelayUntil(&last_wake, pdMS_TO_TICKS(1000 / SAMPLE_RATE_HZ));
        }

        // Temperature: every 5 seconds (slower sensor)
        if (xTaskGetTickCount() - last_temp_read > pdMS_TO_TICKS(5000)) {
            block.temp_sensor[0] = sensor_temperature_read(SENSOR_1);
            block.temp_sensor[1] = sensor_temperature_read(SENSOR_2);
            block.temp_ambient = sensor_temperature_read(AMBIENT);
            last_temp_read = xTaskGetTickCount();
        }

        // HF detection: read ISR counter
        block.transient_count = sensor_hf_detect_get_count_and_reset();
        block.hf_zcr = sensor_hf_detect_get_zcr();

        block.timestamp = timestamp_now();
        block.sample_count = SAMPLES_PER_BLOCK;

        // Send to processing task (non-blocking, drop oldest if full)
        if (xQueueSend(raw_samples_queue, &block, 0) != pdTRUE) {
            PYRO_LOGW("ACQ", "raw_queue full, dropping oldest block");
            raw_sample_block_t discard;
            xQueueReceive(raw_samples_queue, &discard, 0);
            xQueueSend(raw_samples_queue, &block, 0);
        }
    }
}
```

### 7.2 Task Processing (Core 0, Priority 3)

```c
// Responsabilite: calculer features a partir des echantillons bruts
// Periode: reactive (driven by raw_samples_queue)
// Stack: 8192 bytes (FFT needs stack space)
// Watchdog: 15s timeout (FFT can take ~100ms)

void task_processing(void *params) {
    // Accumulation state
    float rms_accumulator[3] = {0};
    uint8_t rms_count = 0;
    uint32_t fft_sample_buffer[FFT_SIZE];  // 1024 samples for THD
    uint16_t fft_buffer_pos = 0;
    float thd_last = NAN;
    float harmonics_last[4] = {NAN};
    TickType_t last_feature_publish = 0;
    TickType_t last_thd_compute = 0;

    // Smoothing state
    ema_state_t ema_rms[3], ema_temp[3], ema_hf;
    smoothing_init_all(&ema_rms, &ema_temp, &ema_hf);

    while (true) {
        raw_sample_block_t block;
        if (xQueueReceive(raw_samples_queue, &block, pdMS_TO_TICKS(2000)) != pdTRUE) {
            PYRO_LOGW("PROC", "No raw data for 2s, sensor issue?");
            wdog_feed(TASK_PROCESSING);
            continue;
        }
        wdog_feed(TASK_PROCESSING);

        // --- RMS computation (every 1s block) ---
        float rms_l1 = rms_compute(block.current_l1, block.sample_count);
        float rms_l2 = rms_compute(block.current_l2, block.sample_count);
        float rms_l3 = rms_compute(block.current_l3, block.sample_count);

        rms_accumulator[0] = ema_update(&ema_rms[0], rms_l1);
        rms_accumulator[1] = ema_update(&ema_rms[1], rms_l2);
        rms_accumulator[2] = ema_update(&ema_rms[2], rms_l3);

        // --- Peak and crest factor ---
        float peak_l1 = peak_detect(block.current_l1, block.sample_count);
        float crest = peak_l1 / fmaxf(rms_l1, 0.001f);

        // --- Zero crossing (frequency estimation) ---
        float zc_hz = zero_crossing_rate(block.current_l1, block.sample_count, SAMPLE_RATE_HZ);

        // --- Power factor (if voltage available) ---
        float pf = NAN, active_power = NAN;
        if (block.voltage[0] != 0) {
            pf = power_factor_compute(block.voltage, block.current_l1, block.sample_count);
            float v_rms = rms_compute(block.voltage, block.sample_count);
            active_power = v_rms * rms_l1 * pf;
        }

        // --- Temperature rate of change ---
        float temp_rate = temperature_rate_compute(block.temp_sensor[0]);

        // --- Accumulate FFT buffer (for 10s THD) ---
        memcpy(&fft_sample_buffer[fft_buffer_pos], block.current_l1,
               MIN(block.sample_count, FFT_SIZE - fft_buffer_pos) * sizeof(uint32_t));
        fft_buffer_pos += block.sample_count;

        if (fft_buffer_pos >= FFT_SIZE) {
            // THD computation (every ~10 seconds or when buffer full)
            thd_result_t thd_result = thd_compute(fft_sample_buffer, FFT_SIZE);
            thd_last = thd_result.thd_percent;
            memcpy(harmonics_last, thd_result.harmonics, sizeof(harmonics_last));
            fft_buffer_pos = 0;
            last_thd_compute = xTaskGetTickCount();
        }

        // --- Arc / HF detection ---
        float arc_energy = arc_compute_energy(block.transient_count, block.hf_zcr);
        float hf_level = ema_update(&ema_hf, (float)block.hf_zcr / 1000.0f);

        // --- Urgent event detection (immediate publish) ---
        if (arc_energy > config_get()->arc_threshold && block.transient_count > 5) {
            electrical_event_t event = build_arc_event(arc_energy, peak_l1, block);
            xQueueSend(event_queue, &event, pdMS_TO_TICKS(100));
            PYRO_LOGW("PROC", "ARC_DETECTED energy=%.3f tc=%d", arc_energy, block.transient_count);
        }
        if (rms_l1 > config_get()->rated_current * config_get()->overload_ratio) {
            electrical_event_t event = build_overload_event(rms_l1, config_get()->rated_current);
            xQueueSend(event_queue, &event, pdMS_TO_TICKS(100));
        }
        if (temp_rate > config_get()->temp_rate_threshold) {
            electrical_event_t event = build_overheating_event(block.temp_sensor[0], temp_rate);
            xQueueSend(event_queue, &event, pdMS_TO_TICKS(100));
        }

        // --- Build feature frame (every 5s) ---
        if (xTaskGetTickCount() - last_feature_publish > pdMS_TO_TICKS(FEATURE_INTERVAL_MS)) {
            feature_frame_t frame = {
                .version = PAYLOAD_VERSION,
                .sequence = sequence_next(),
                .timestamp_unix = timestamp_now(),
                .timestamp_ms = timestamp_ms(),
                .is_drain = false,
                .current = { .rms = {rms_accumulator[0], rms_accumulator[1], rms_accumulator[2]},
                             .peak = {peak_l1, 0, 0},
                             .crest_factor = crest,
                             .zero_crossing_hz = zc_hz },
                .power = { .power_factor = pf, .active_power_w = active_power },
                .temperature = { .sensor = {block.temp_sensor[0], block.temp_sensor[1]},
                                 .ambient = block.temp_ambient,
                                 .rate_of_change = temp_rate },
                .harmonics = { .thd_percent = thd_last,
                               .harmonics = {harmonics_last[0], harmonics_last[1],
                                             harmonics_last[2], harmonics_last[3]} },
                .arc = { .arc_energy = arc_energy,
                         .transient_count = block.transient_count,
                         .hf_noise_level = hf_level },
                .signal_quality = quality_compute(&rms_l1, &block, &ema_rms[0]),
            };

            generate_message_id(frame.message_id);
            hmac_sign((uint8_t*)&frame, offsetof(feature_frame_t, hmac), frame.hmac);

            xQueueSend(feature_queue, &frame, pdMS_TO_TICKS(100));
            last_feature_publish = xTaskGetTickCount();
        }
    }
}
```

### 7.3 Task Communications (Core 0, Priority 2)

```c
// Responsabilite: publier features/events via MQTT, gerer buffer offline
// Periode: reactive (driven by feature_queue + event_queue)
// Stack: 6144 bytes (TLS needs memory)
// Watchdog: 30s timeout (reconnection can take time)

void task_comms(void *params) {
    conn_wifi_init();
    conn_mqtt_init();
    conn_offline_init();
    store_buffer_init();
    store_fqueue_init();

    while (true) {
        wdog_feed(TASK_COMMS);

        device_state_t state = state_machine_current();

        if (state == STATE_OFFLINE_BUFFERING || state == STATE_DEGRADED) {
            // --- OFFLINE MODE: buffer to SPIFFS ---
            feature_frame_t frame;
            while (xQueueReceive(feature_queue, &frame, 0) == pdTRUE) {
                pyro_err_t err = store_fqueue_enqueue(&frame, sizeof(frame), PRIORITY_NORMAL);
                if (err == PYRO_ERR_STORAGE_FULL) {
                    store_fqueue_evict_oldest();
                    store_fqueue_enqueue(&frame, sizeof(frame), PRIORITY_NORMAL);
                }
            }
            // Events get priority in buffer
            electrical_event_t event;
            while (xQueueReceive(event_queue, &event, 0) == pdTRUE) {
                store_fqueue_enqueue(&event, sizeof(event), PRIORITY_HIGH);
            }

            // Try reconnection with backoff
            if (conn_reconnect_should_try()) {
                conn_wifi_connect();
                if (conn_wifi_is_connected()) {
                    conn_mqtt_connect();
                    if (conn_mqtt_is_connected()) {
                        state_machine_transition(EVENT_RECONNECTED);
                        // Begin drain (handled in ACTIVE path below)
                    }
                }
                conn_reconnect_schedule(conn_backoff_next_delay());
            }

            vTaskDelay(pdMS_TO_TICKS(1000));
            continue;
        }

        if (state == STATE_ACTIVE) {
            // --- DRAIN MODE: replay buffered messages first ---
            if (store_fqueue_count() > 0 && conn_mqtt_is_connected()) {
                uint8_t drain_batch = 0;
                while (store_fqueue_count() > 0 && drain_batch < DRAIN_BATCH_SIZE) {
                    uint8_t buf[512];
                    size_t len;
                    uint8_t priority;
                    store_fqueue_peek(buf, &len, &priority);

                    feature_frame_t *frame = (feature_frame_t*)buf;
                    frame->is_drain = true;

                    uint8_t cbor_buf[256];
                    size_t cbor_len = telemetry_payload_encode(frame, cbor_buf, sizeof(cbor_buf));

                    pyro_err_t err = conn_mqtt_publish(
                        topic_features(), cbor_buf, cbor_len, QOS_1, /*retain=*/false);

                    if (err == PYRO_OK) {
                        store_fqueue_dequeue();
                        drain_batch++;
                    } else {
                        break;  // Network issue, retry later
                    }
                    vTaskDelay(pdMS_TO_TICKS(100));  // 10 msg/s throttle
                }
            }

            // --- LIVE: publish real-time features ---
            feature_frame_t frame;
            if (xQueueReceive(feature_queue, &frame, pdMS_TO_TICKS(100)) == pdTRUE) {
                uint8_t cbor_buf[256];
                size_t cbor_len = telemetry_payload_encode(&frame, cbor_buf, sizeof(cbor_buf));

                // LZ4 compress
                uint8_t compressed[256];
                size_t comp_len = lz4_compress(cbor_buf, cbor_len, compressed, sizeof(compressed));

                pyro_err_t err = conn_mqtt_publish_with_props(
                    topic_features(), compressed, comp_len, QOS_1,
                    /* user_props: */ "payload-ver", "2",
                    "encoding", "cbor",
                    "compress", "lz4",
                    "fw-ver", firmware_version_string(),
                    "cert-fp", sec_credentials_cert_fingerprint()
                );

                if (err != PYRO_OK) {
                    // Publish failed → buffer locally
                    store_fqueue_enqueue(&frame, sizeof(frame), PRIORITY_NORMAL);
                    if (consecutive_failures++ > MAX_CONSECUTIVE_FAILURES) {
                        state_machine_transition(EVENT_MQTT_DISCONNECT);
                    }
                } else {
                    consecutive_failures = 0;
                    conn_reconnect_reset();
                }
            }

            // --- EVENTS: immediate publish (priority) ---
            electrical_event_t event;
            if (xQueueReceive(event_queue, &event, 0) == pdTRUE) {
                uint8_t json_buf[512];
                size_t json_len = telemetry_event_encode_json(&event, json_buf, sizeof(json_buf));

                pyro_err_t err = conn_mqtt_publish(
                    topic_events(), json_buf, json_len, QOS_1, /*retain=*/false);

                if (err != PYRO_OK) {
                    store_fqueue_enqueue(&event, sizeof(event), PRIORITY_HIGH);
                }
            }
        }
    }
}
```

### 7.4 Task Diagnostics (Core 0, Priority 1)

```c
// Responsabilite: health monitoring, heartbeat, self-test, watchdog
// Periode: 60s (heartbeat), 300s (health report), on-demand (self-test)
// Stack: 4096 bytes
// Watchdog: 120s timeout (self-test can be slow)

void task_diagnostics(void *params) {
    diag_watchdog_init();
    diag_selftest_run_startup();  // Initial self-test at boot

    TickType_t last_heartbeat = 0;
    TickType_t last_health_report = 0;

    while (true) {
        wdog_feed(TASK_DIAG);

        uint32_t now = xTaskGetTickCount();

        // --- Heartbeat (every 60s) ---
        if (now - last_heartbeat > pdMS_TO_TICKS(config_get()->heartbeat_interval_ms)) {
            heartbeat_t hb = {
                .version = PAYLOAD_VERSION,
                .sequence = sequence_next(),
                .timestamp_unix = timestamp_now(),
                .uptime_seconds = esp_timer_get_time() / 1000000,
                .free_heap_bytes = esp_get_free_heap_size(),
                .wifi_rssi_dbm = conn_wifi_get_rssi(),
                .buffer_percent = store_fqueue_usage_percent(),
                .signal_quality = quality_last_score(),
                .device_state = state_machine_current(),
                .cpu_percent = diag_health_cpu_percent(),
                .internal_temp_c = diag_health_internal_temp(),
            };

            if (state_machine_current() == STATE_ACTIVE) {
                uint8_t cbor_buf[64];
                size_t len = diag_heartbeat_encode(&hb, cbor_buf, sizeof(cbor_buf));
                conn_mqtt_publish(topic_heartbeat(), cbor_buf, len, QOS_0, /*retain=*/true);
            }
            last_heartbeat = now;
        }

        // --- Health report (every 5 min) ---
        if (now - last_health_report > pdMS_TO_TICKS(config_get()->health_interval_ms)) {
            health_report_t report = diag_health_collect();
            hmac_sign((uint8_t*)&report, offsetof(health_report_t, hmac), report.hmac);

            if (state_machine_current() == STATE_ACTIVE) {
                uint8_t cbor_buf[256];
                size_t len = diag_heartbeat_encode_health(&report, cbor_buf, sizeof(cbor_buf));
                conn_mqtt_publish(topic_health(), cbor_buf, len, QOS_1, /*retain=*/false);
            }
            last_health_report = now;
        }

        // --- Watchdog check (all tasks alive?) ---
        for (int t = 0; t < TASK_COUNT; t++) {
            if (!wdog_is_task_alive(t)) {
                PYRO_LOGE("DIAG", "Task %d unresponsive, escalating", t);
                error_escalate(PYRO_ERR_TASK_TIMEOUT, t);
            }
        }

        // --- Periodic self-test (every 1h) ---
        static uint32_t selftest_counter = 0;
        if (++selftest_counter >= 60) {  // 60 × 60s = 1h
            selftest_result_t result = diag_selftest_run();
            if (!result.all_pass) {
                PYRO_LOGW("DIAG", "Self-test partial failure: adc=%d temp=%d nvs=%d",
                          result.adc_ok, result.temp_ok, result.nvs_ok);
            }
            selftest_counter = 0;
        }

        vTaskDelay(pdMS_TO_TICKS(60000));  // Check every 60s
    }
}
```

### 7.5 Main Application (Orchestrator)

```c
// Responsabilite: boot sequence, task creation, state machine, error handling
// Ce n'est PAS une task FreeRTOS — c'est app_main() qui cree les tasks

void app_main(void) {
    // === BOOT SEQUENCE ===
    pyro_err_t err;

    // 1. Hardware init (minimal)
    err = boot_init_hardware();   // GPIO, buses I2C/SPI, NVS
    if (err != PYRO_OK) goto fatal;

    // 2. Load configuration from NVS
    err = boot_load_config();     // Intervals, thresholds, device_id
    if (err != PYRO_OK) goto fatal;

    // 3. Check firmware version + OTA status
    boot_check_firmware_version();  // Logs version, marks OTA success if pending

    // 4. Initialize state machine
    state_machine_init(config_has_credentials() ? STATE_CONNECTING : STATE_PROVISIONING);

    // 5. Self-test (non-fatal, just report)
    selftest_result_t st = diag_selftest_run_startup();
    if (!st.all_pass) {
        PYRO_LOGW("MAIN", "Startup self-test issues detected");
    }

    // === STATE-DEPENDENT INITIALIZATION ===
    device_state_t initial_state = state_machine_current();

    if (initial_state == STATE_PROVISIONING) {
        // Start provisioning mode (WiFi AP, HTTP server for enrollment)
        provisioning_start();
        // Block until enrolled or timeout
        if (!provisioning_wait_complete(PROVISIONING_TIMEOUT_MS)) {
            state_machine_transition(EVENT_PROVISIONING_TIMEOUT);
            goto fatal;
        }
        state_machine_transition(EVENT_ENROLLED);
    }

    // === CONNECT ===
    conn_wifi_connect();
    if (!conn_wifi_wait_connected(WIFI_CONNECT_TIMEOUT_MS)) {
        state_machine_transition(EVENT_WIFI_FAIL);
        // Continue in offline mode — tasks will buffer
    } else {
        conn_mqtt_connect();
        if (conn_mqtt_wait_connected(MQTT_CONNECT_TIMEOUT_MS)) {
            state_machine_transition(EVENT_CONNECTED);
        } else {
            state_machine_transition(EVENT_MQTT_FAIL);
        }
    }

    // === CREATE FREERTOS TASKS ===
    xTaskCreatePinnedToCore(task_acquisition, "acq",  4096, NULL, 4, &task_acq_handle,  1);
    xTaskCreatePinnedToCore(task_processing,  "proc", 8192, NULL, 3, &task_proc_handle, 0);
    xTaskCreatePinnedToCore(task_comms,       "comm", 6144, NULL, 2, &task_comm_handle, 0);
    xTaskCreatePinnedToCore(task_diagnostics, "diag", 4096, NULL, 1, &task_diag_handle, 0);

    // Register tasks with watchdog
    wdog_register_task(TASK_ACQUISITION, task_acq_handle,  5000);
    wdog_register_task(TASK_PROCESSING,  task_proc_handle, 15000);
    wdog_register_task(TASK_COMMS,       task_comm_handle, 30000);
    wdog_register_task(TASK_DIAG,        task_diag_handle, 120000);

    // === COMMAND HANDLER LOOP (main task becomes command processor) ===
    while (true) {
        device_command_t cmd;
        if (xQueueReceive(command_queue, &cmd, pdMS_TO_TICKS(5000)) == pdTRUE) {
            handle_command(&cmd);
        }
        // Check for state transitions needing orchestration
        check_pending_transitions();
    }

fatal:
    PYRO_LOGE("MAIN", "Fatal boot error: %d", err);
    state_machine_transition(EVENT_FATAL_ERROR);
    // LED rouge rapide, await OTA recovery
    while (true) {
        led_blink(LED_RED, 200);
        vTaskDelay(pdMS_TO_TICKS(5000));
        esp_restart();  // Auto-reboot every 5s (OTA bootloader may catch)
    }
}
```

---

## 8. Strategie d'erreurs

### Taxonomie des erreurs

```c
typedef enum {
    // --- Severity: RECOVERABLE (auto-recovery, no intervention) ---
    PYRO_ERR_NONE = 0,
    PYRO_ERR_SENSOR_TIMEOUT,         // Sensor read timeout → retry
    PYRO_ERR_SENSOR_RANGE,           // Value out of range → mark invalid
    PYRO_ERR_QUEUE_FULL,             // Queue overflow → drop oldest
    PYRO_ERR_MQTT_PUBLISH_FAIL,      // Publish fail → buffer locally
    PYRO_ERR_WIFI_DISCONNECT,        // WiFi lost → reconnect with backoff
    PYRO_ERR_NTP_SYNC_FAIL,          // Time sync fail → use last known offset
    PYRO_ERR_CBOR_ENCODE_FAIL,       // Encoding fail → skip this frame

    // --- Severity: DEGRADED (service impact, needs attention) ---
    PYRO_ERR_SENSOR_PERMANENT,       // Sensor dead → degrade signal quality
    PYRO_ERR_BUFFER_NEAR_FULL,       // > 75% buffer → reduce sampling
    PYRO_ERR_MQTT_DISCONNECT,        // Persistent MQTT loss → offline mode
    PYRO_ERR_NVS_CORRUPT,            // NVS read error → use defaults
    PYRO_ERR_SPIFFS_CORRUPT,         // FS error → format + restart buffer
    PYRO_ERR_TASK_TIMEOUT,           // Task watchdog → log + attempt restart

    // --- Severity: FATAL (requires reboot or human intervention) ---
    PYRO_ERR_BOOT_FAIL,              // Hardware init failed
    PYRO_ERR_OTA_FAIL,               // OTA apply failed → rollback
    PYRO_ERR_CREDENTIALS_CORRUPT,    // Certs unreadable → re-provision
    PYRO_ERR_SECURE_ELEMENT_FAIL,    // ATECC608B unresponsive → reboot
    PYRO_ERR_MEMORY_EXHAUSTED,       // Heap depleted → emergency reboot
    PYRO_ERR_BOOT_LOOP,              // 3+ consecutive boot failures → factory reset
} pyro_err_t;
```

### Politique de traitement par severite

| Severite | Reaction | Escalade si persistant |
|----------|----------|------------------------|
| RECOVERABLE | Log warning, retry, continue | 10 consecutive → DEGRADED |
| DEGRADED | Log error, reduce functionality, notify via health report | 1h unresolved → FATAL |
| FATAL | Log critical, attempt recovery once, reboot | 3 reboots → factory reset mode |

### Pattern de traitement

```c
// Chaque module retourne pyro_err_t
// L'appelant decide de la politique

pyro_err_t err = sensor_current_read_rms(&value);
switch (err) {
    case PYRO_OK:
        break;  // Normal path
    case PYRO_ERR_SENSOR_TIMEOUT:
        error_log(err, "current_read L1");
        error_increment_counter(ERR_COUNTER_SENSOR);
        value = NAN;  // Mark as invalid, signal_quality will reflect
        break;
    case PYRO_ERR_SENSOR_PERMANENT:
        error_escalate(err, PHASE_L1);
        state_machine_transition(EVENT_SENSOR_FAIL);
        break;
    default:
        error_escalate(err, 0);
}
```

### Error context (pour debug post-mortem)

```c
typedef struct {
    pyro_err_t code;
    uint32_t   timestamp;
    uint8_t    module_id;        // PYRO_MODULE_SENSORS, _PROC, _COMMS, etc.
    uint8_t    task_id;
    uint32_t   line;             // __LINE__
    uint32_t   extra;            // Module-specific context
} error_context_t;

// Ring buffer of last 32 errors, persisted in NVS
// Included in health_report for cloud-side diagnosis
```

### Reboot et recovery

| Condition | Action | Trace |
|-----------|--------|-------|
| 1 reboot spontane | Log reason (ESP_RST_*), continue normally | `reboot_count` in health |
| 3 reboots en 5 min | Disable non-essential features, start minimal mode | Log "boot_loop_detected" |
| 5 reboots en 10 min | Factory reset WiFi config, keep device credentials | Log "factory_reset_partial" |
| OTA applied but boot fails | Rollback to previous partition (ESP-IDF auto-rollback) | `ota_rollback` event |

---

## 9. Strategie de logs

### Niveaux de log

| Niveau | Macro | Usage | En production |
|--------|-------|-------|:-------------:|
| ERROR | `PYRO_LOGE(tag, fmt, ...)` | Erreurs necessitant attention | Oui |
| WARNING | `PYRO_LOGW(tag, fmt, ...)` | Situations anormales recuperables | Oui |
| INFO | `PYRO_LOGI(tag, fmt, ...)` | Transitions d'etat, evenements importants | Oui |
| DEBUG | `PYRO_LOGD(tag, fmt, ...)` | Details de fonctionnement | Non (dev only) |
| VERBOSE | `PYRO_LOGV(tag, fmt, ...)` | Traces detaillees (values, buffers) | Non |

### Implementation

```c
// Wrapper autour ESP_LOG* avec contexte structure
#define PYRO_LOGE(tag, fmt, ...) \
    do { \
        ESP_LOGE(tag, "[%s:%d] " fmt, __func__, __LINE__, ##__VA_ARGS__); \
        log_queue_push(LOG_ERROR, tag, fmt, ##__VA_ARGS__); \
    } while(0)
```

### Tags par module

| Module | Tag | Exemples de messages |
|--------|-----|---------------------|
| boot/ | `BOOT` | "NVS loaded, device_id=abc123", "OTA rollback detected" |
| sensors/ | `SENS` | "ADS1115 init OK @860SPS", "PT100 read timeout" |
| signal_processing/ | `PROC` | "ARC_DETECTED energy=0.45", "THD=8.2%" |
| telemetry/ | `TELE` | "Payload v2 encoded, 142 bytes", "Validation fail: rms out of range" |
| connectivity/ | `CONN` | "WiFi connected RSSI=-42", "MQTT disconnected, backoff 30s" |
| security/ | `SEC` | "HMAC verified OK", "Cert expires in 59 days" |
| storage/ | `STOR` | "Buffer 45% full, 312 messages queued", "FIFO eviction: 10 oldest dropped" |
| diagnostics/ | `DIAG` | "Health: heap=184K, cpu=23%, uptime=3d", "Self-test: ADC OK, TEMP OK" |
| state machine | `SM` | "Transition ACTIVE → OFFLINE_BUFFERING (WiFi lost)" |

### Strategie de persistence des logs

| Destination | Quand | Retention |
|-------------|-------|-----------|
| UART (serial) | Dev mode (sdkconfig.dev) | Temps reel, pas de persistence |
| RAM ring buffer (64 entries) | Toujours | Derniers 64 logs ERROR/WARNING |
| NVS (derniers 8 fatals) | Apres reboot | Crash context, survives reboot |
| MQTT topic (`.../status/log`) | Si connecte + level >= WARNING | Cloud collects, 30 jours retention |

### Regles strictes

1. **Pas de log dans les ISR** — ISR set un flag, la task log.
2. **Pas de log bloquant** — `log_queue_push()` est non-bloquant (drop si full).
3. **Pas de donnees sensibles** — Jamais de credentials, cert content, ou HMAC keys dans les logs.
4. **Pas de log en boucle rapide** — Rate-limiting: max 1 log/s par tag+level (throttle counter).
5. **Pas de strings dynamiques** — Format compile-time seulement (securite + taille flash).

### Configuration runtime

```c
// Via NVS (changeable par commande cloud CMD_UPDATE_CONFIG)
typedef struct {
    uint8_t uart_level;       // LOG_LEVEL_DEBUG (dev) or LOG_LEVEL_NONE (prod)
    uint8_t mqtt_level;       // LOG_LEVEL_WARNING (prod)
    uint8_t buffer_level;     // LOG_LEVEL_ERROR (always)
    bool    include_timestamp;
    bool    include_heap_free;
} log_config_t;
```

---

## 10. Strategie de test

### Pyramide de tests firmware

```
            ┌───────────┐
            │  Target   │  Tests on real ESP32 hardware
            │  Tests    │  (5-10 tests, slow, manual trigger)
            │  (HW)     │
            ├───────────┤
            │Integration│  Full pipeline mock-to-mock
            │  Tests    │  (15-20 tests, host-based)
            │  (Host)   │
            ├───────────┤
            │           │
            │   Unit    │  Pure functions, no hardware
            │   Tests   │  (50+ tests, host-based, < 1s total)
            │  (Host)   │
            │           │
            └───────────┘
```

### Unit Tests (Host-Based)

**Execution :** Sur PC de dev (Linux/Mac/WSL), sans ESP32.
**Framework :** Unity (integre ESP-IDF) + CMock (pour HAL mocks).
**Build :** CMake host build (`idf.py -T test` ou CMake standalone).

| Module | Tests | Strategie |
|--------|:-----:|-----------|
| sig_rms | 8 | Sine wave connue → RMS attendu. DC offset. Saturation. Zeros. |
| sig_thd | 6 | Sinus pur (THD=0). Sinus + H3 50% (THD=50%). Bruit blanc. |
| sig_transient | 5 | Zero crossings. Spikes injectees. Threshold edges. |
| sig_arc | 4 | HF burst synthetique. Pas de signal. Saturation. |
| sig_quality | 6 | All OK (score=100). Each degradation individually. Combined. |
| telemetry_payload | 5 | Encode → decode roundtrip. Version field. Edge cases (NaN). |
| telemetry_validate | 4 | Valid frame. Out-of-range values. Missing fields. Future version. |
| sec_hmac | 3 | Known test vectors (RFC 4231). Tampered payload → verify fail. |
| sec_nonce | 3 | Uniqueness (1000 generates, no duplicates). Anti-replay window. |
| sec_anti_replay | 4 | In-window accept. Out-of-window reject. Replay detect. Window slide. |
| store_buffer | 5 | Push/pop FIFO. Overflow wrap. Empty pop. Full push. Size. |
| store_fqueue | 5 | Enqueue/dequeue. Persistence (simulated). Priority ordering. |
| conn_backoff | 4 | Progression (1s, 2s, 4s, ..., max). Jitter range. Reset. |
| telemetry_timestamp | 3 | Monotonic increment. Drift detection. Overflow handling. |
| state_machine | 8 | Each valid transition. Invalid transitions rejected. Guard conditions. |
| **TOTAL** | **~70** | |

### Integration Tests (Host-Based)

**Execution :** Sur PC, avec HAL mocks complets.
**But :** Valider les interactions entre modules sans hardware.

| Test | Description |
|------|-------------|
| sensor_to_feature | Mock ADC retourne sine wave → feature_frame contient RMS correct |
| feature_to_payload | feature_frame → CBOR encode → validate → decode → verify fields |
| offline_buffer_drain | Simuler offline → buffer N frames → reconnect → drain in order |
| mqtt_publish_retry | Publish fail → buffer → retry OK → dequeue |
| state_transitions_full | BOOTING → PROVISIONING → CONNECTING → ACTIVE → OFFLINE → ACTIVE |
| event_detection_arc | Inject high transient_count + arc_energy → electrical_event emitted |
| event_detection_temp | Inject rapid temperature rise → overheating event emitted |
| config_update | Receive CMD_UPDATE_CONFIG → intervals change → verify new timing |
| hmac_end_to_end | Build frame → sign → tamper → verify fails. Sign → verify passes. |
| heartbeat_content | Generate heartbeat → verify all fields populated correctly |

### Target Tests (On-Hardware)

**Execution :** Sur ESP32-S3 reel, via `idf.py flash monitor`.
**But :** Valider les drivers hardware et la communication reelle.
**Declenchement :** Manuel, apres flash du firmware de test.

| Test | Prerequis | Validation |
|------|-----------|------------|
| ADC read | ADS1115 connecte, signal connu sur input | Valeur lue ±5% de la valeur attendue |
| Temperature read | MAX31865 + PT100, temperature ambiante connue | Lecture ±1°C vs thermometre reference |
| NVS write/read | — | Ecriture cle → reboot → relecture identique |
| SPIFFS write/read | — | Ecrire fichier → lire → comparer. Free space correct. |
| WiFi connect | AP disponible (credentials en NVS) | Association < 10s, RSSI lisible |
| MQTT connect | Broker Mosquitto local, certs pre-provisioned | CONNACK recu, publish OK, subscribe OK |
| MQTT mTLS | Broker avec mTLS, device cert + CA chain | Handshake OK, publish OK |
| GPIO ISR | Generateur signal sur pin ISR | Compteur fronts ±2% vs frequence connue |
| Deep sleep / wake | — | Entrer deep sleep → wake timer → NVS intact |
| OTA flash | Serveur HTTP avec firmware signe | Download → verify → apply → reboot → new version |

### CI/CD Integration

```
┌─────────────────────────────────────────────────────────┐
│ Push to branch                                           │
│                                                          │
│ 1. Build firmware (idf.py build)                        │
│    → Verify compilation OK for all sdkconfig variants   │
│                                                          │
│ 2. Run host unit tests (cmake --build test && ctest)    │
│    → 70+ tests, < 5 seconds                            │
│                                                          │
│ 3. Run host integration tests                           │
│    → 10 tests, < 10 seconds                            │
│                                                          │
│ 4. Static analysis (cppcheck, clang-tidy)               │
│    → Zero warnings policy                               │
│                                                          │
│ 5. Size check (binary < 1.5MB, partitions fit)          │
│    → Fail if OTA partition exceeded                     │
│                                                          │
│ 6. [Manual] Flash to target + run target tests          │
│    → Before merge to main only                          │
└─────────────────────────────────────────────────────────┘
```

### Couverture cible

| Couche | Objectif | Mesure |
|--------|:--------:|--------|
| signal_processing/ | > 90% | Fonctions pures, faciles a tester |
| telemetry/ | > 85% | Encode/decode/validate |
| security/ | > 90% | Crypto paths critiques |
| storage/ | > 80% | Ring buffer, file queue |
| connectivity/ | > 60% | Beaucoup de code I/O, mock-dependent |
| sensors/ | > 50% | Drivers hardware, teste surtout on-target |
| hal/ | N/A | Tested indirectement via target tests |

---

## 11. Strategie OTA future

### Architecture OTA

```
┌────────────────┐        ┌────────────────┐        ┌────────────────┐
│  BUILD SERVER  │        │  DEVICE SVC    │        │    DEVICE      │
│  (CI/CD)       │        │  (Cloud)       │        │   (ESP32-S3)   │
└───────┬────────┘        └───────┬────────┘        └───────┬────────┘
        │                         │                         │
        │ 1. Build firmware       │                         │
        │ 2. Sign (ECDSA P-256)   │                         │
        │ 3. Upload artifact      │                         │
        │─────────────────────────▶                         │
        │                         │                         │
        │                         │ 4. Select target devices│
        │                         │    (canary → 10% → all) │
        │                         │                         │
        │                         │ 5. MQTT Command:        │
        │                         │    CMD_OTA_AVAILABLE     │
        │                         │    {url, sha256, size}   │
        │                         │────────────────────────▶│
        │                         │                         │
        │                         │                 6. Download (HTTPS)
        │                         │                 7. Verify SHA-256
        │                         │                 8. Verify ECDSA sig
        │                         │                 9. Write to OTA_1
        │                         │                10. Set boot partition
        │                         │                11. Reboot
        │                         │                         │
        │                         │◀──── Heartbeat ─────────│
        │                         │  (new fw_version)       │
        │                         │                         │
        │                         │ 12. Mark OTA success    │
        │                         │     (or rollback if no  │
        │                         │      heartbeat in 5min) │
```

### Partition table

```
# Name,   Type, SubType,  Offset,   Size,    Flags
nvs,      data, nvs,      0x9000,   0x6000,
otadata,  data, ota,      0xF000,   0x2000,
phy_init, data, phy,      0x11000,  0x1000,
ota_0,    app,  ota_0,    0x20000,  0x1C0000, # 1.75 MB (firmware A)
ota_1,    app,  ota_1,    0x1E0000, 0x1C0000, # 1.75 MB (firmware B)
nvs_keys, data, nvs_keys, 0x3A0000, 0x1000,   # NVS encryption keys
spiffs,   data, spiffs,   0x3A1000, 0x400000, # 4 MB buffer storage
coredump, data, coredump, 0x7A1000, 0x10000,  # Core dump partition
```

### Securite OTA

| Mesure | Implementation |
|--------|----------------|
| **Signature firmware** | ECDSA P-256, cle publique flashee en efuse (immutable) |
| **Transport** | HTTPS (TLS 1.2+) vers CDN/S3 signe |
| **Integrite** | SHA-256 du binaire verifie AVANT ecriture flash |
| **Anti-downgrade** | Version monotonique (NVS counter, refuse version inferieure) |
| **Rollback automatique** | Si pas de `esp_ota_mark_app_valid()` dans les 60s → revert |
| **Secure Boot v2** | Bootloader verifie signature de chaque partition app au boot |
| **Flash Encryption** | AES-256-XTS (efuse key, transparent pour le firmware) |

### Rollout progressif

| Phase | Cible | Duree observation | Critere de succes |
|-------|:-----:|:-----------------:|-------------------|
| Canary | 1 device | 24h | Heartbeat OK, no errors, features publishing |
| 10% | 10% du parc | 48h | < 1% rollback, metrics stables |
| 50% | 50% du parc | 24h | Pas de regression detectee |
| 100% | Tous | — | Deployment complet |

### Rollback conditions automatiques

- Pas de heartbeat dans les 5 minutes post-reboot
- 3 reboots consecutifs post-OTA (boot loop detection)
- Signal quality drop > 30 points pour > 50% des devices mis a jour
- Explicit rollback command from cloud

### Contraintes firmware OTA

1. **Taille max binaire :** 1.75 MB (partition OTA)
2. **RAM pendant download :** Buffer de 4KB (streaming, pas de full download en RAM)
3. **Resume :** Si download interrompu, reprendre au dernier offset (Range header)
4. **Timing :** OTA preferee pendant periode calme (nuit, configurable)
5. **Pas d'OTA pendant event :** Si arc/overload en cours, reporter l'OTA

---

## Annexes

### A. Ressources memoire (budget)

| Resource | Disponible | Utilise (estime) | Marge |
|----------|:----------:|:----------------:|:-----:|
| Flash (code) | 1.75 MB (par partition) | ~800 KB | 54% libre |
| PSRAM | 8 MB | ~2 MB (FFT buffers, TLS) | 75% libre |
| Internal SRAM | 512 KB | ~300 KB (stacks, queues, heap) | 41% libre |
| SPIFFS | 4 MB | Variable (buffer) | — |
| NVS | 24 KB | ~8 KB (config, creds ref) | 67% libre |

### B. Allocation des stacks FreeRTOS

| Task | Stack | Justification |
|------|:-----:|---------------|
| task_acquisition | 4096 B | I2C/SPI calls, sample buffers |
| task_processing | 8192 B | FFT 1024 points (4KB float array), temp vars |
| task_comms | 6144 B | TLS buffers, CBOR encoding, LZ4 |
| task_diagnostics | 4096 B | Health report building, self-test |
| main (command handler) | 4096 B | Default ESP-IDF main task |
| **TOTAL** | **~27 KB** | Internal SRAM |

### C. Pinout ESP32-S3 (Option A)

| GPIO | Fonction | Module | Bus |
|:----:|----------|--------|-----|
| 1 | I2C SDA | ADS1115 | I2C0 |
| 2 | I2C SCL | ADS1115 | I2C0 |
| 10 | SPI MOSI | MAX31865 | SPI2 |
| 11 | SPI MISO | MAX31865 | SPI2 |
| 12 | SPI CLK | MAX31865 | SPI2 |
| 13 | SPI CS0 | MAX31865 #1 | SPI2 |
| 14 | SPI CS1 | MAX31865 #2 | SPI2 |
| 15 | GPIO Input | LM393 arc detect ISR | — |
| 16 | GPIO Output | LED Status (RGB NeoPixel) | — |
| 17 | I2C SDA | ATECC608B (future) | I2C1 |
| 18 | I2C SCL | ATECC608B (future) | I2C1 |
| 21 | UART TX | Debug console | UART0 |
| — | WiFi antenna | Internal PCB antenna | — |

### D. Dependencies externes (composants ESP-IDF)

| Composant | Version | Role |
|-----------|---------|------|
| esp-mqtt | 2.x (ESP-IDF integ) | Client MQTT 5.0 |
| mbedTLS | 3.x (ESP-IDF integ) | TLS 1.3, HMAC, SHA-256, ECDSA |
| tinycbor | 0.6+ (component registry) | CBOR encode/decode |
| lz4 | 1.9+ (component registry) | LZ4 compression |
| esp_timer | ESP-IDF | Hardware timers, precision timing |
| nvs_flash | ESP-IDF | Non-volatile storage (chiffre) |
| esp_spiffs | ESP-IDF | File system (buffer) |
| esp_wifi | ESP-IDF | WiFi STA |
| esp_ota_ops | ESP-IDF | OTA update management |
| esp_http_client | ESP-IDF | OTA download (HTTPS) |
| driver/i2c | ESP-IDF | I2C master (ADS1115, ATECC608B) |
| driver/spi | ESP-IDF | SPI master (MAX31865) |
| driver/gpio | ESP-IDF | GPIO + ISR (arc detect, LED) |
| unity | ESP-IDF | Test framework |

### E. Configuration par environnement

| Parametre | Dev (sdkconfig.dev) | Prod (sdkconfig.prod) |
|-----------|--------------------|-----------------------|
| Log level UART | DEBUG | NONE |
| Log level MQTT | DEBUG | WARNING |
| Secure Boot | Disabled | Enabled (v2) |
| Flash Encryption | Disabled | Enabled (AES-256-XTS) |
| JTAG | Enabled | Disabled (efuse) |
| Watchdog timeout | 30s (relax) | 5s (strict) |
| WiFi power save | Disabled | Enabled (modem sleep) |
| Core dump | UART (immediate) | Flash (post-mortem) |
| Assertions | Enabled (abort) | Enabled (reboot) |
| Stack overflow check | Canary + pattern | Canary only |
| Feature interval | 5s | 5s (configurable) |
| Heartbeat interval | 10s (debug fast) | 60s |
| OTA check interval | 60s (fast test) | 3600s (1h) |

### F. Glossaire

| Terme | Definition |
|-------|------------|
| HAL | Hardware Abstraction Layer — isole le code metier du hardware |
| NVS | Non-Volatile Storage — key-value store dans flash |
| SPIFFS | SPI Flash File System — systeme de fichiers simple |
| CT | Current Transformer — pince amperometrique |
| THD | Total Harmonic Distortion — distorsion harmonique totale |
| RMS | Root Mean Square — valeur efficace |
| ISR | Interrupt Service Routine — routine d'interruption |
| EMA | Exponential Moving Average — lissage exponentiel |
| CBOR | Concise Binary Object Representation — encodage binaire compact |
| mTLS | Mutual TLS — authentification bidirectionnelle |
| ECDSA | Elliptic Curve Digital Signature Algorithm |
| CSR | Certificate Signing Request — demande de certificat |
| OTA | Over-The-Air — mise a jour firmware a distance |
| DMA | Direct Memory Access — transfert sans CPU |
