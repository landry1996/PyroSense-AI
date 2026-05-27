# PyroSense Device Firmware

ESP32-S3 firmware for the PyroSense predictive electrical fire prevention sensor.

**Status:** Skeleton / Lab prototype — Uses mock sensors. No real hardware drivers yet.

## Architecture

```
app_main.cpp (orchestrator)
    ├── config/         Device configuration, state machine (7 states)
    ├── sensors/        ISensorProvider interface + MockSensorProvider
    ├── signal_processing/  RMS, THD, signal quality extraction
    ├── telemetry/      JSON payload & heartbeat construction
    ├── connectivity/   WiFi + MQTT client (configurable)
    ├── security/       HMAC-SHA256 signing, nonce generation
    ├── storage/        Offline queue with priority eviction
    ├── diagnostics/    Logger, self-test
    └── utils/          Time, UUID utilities
```

## Prerequisites

### For Host Build (Tests)

- CMake 3.16+
- C++17 compiler (GCC 9+, Clang 10+, MSVC 2019+)

### For ESP32 Target Build

- ESP-IDF 5.x ([install guide](https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/get-started/))
- Python 3.8+
- ESP32-S3 DevKitC-1 (N16R8)

## Quick Start — Host Build (No ESP32 Required)

```bash
cd test
mkdir build && cd build
cmake ..
cmake --build .
ctest --output-on-failure
```

This compiles all modules and runs unit + integration tests on your PC.

## Quick Start — ESP32 Build

```bash
# 1. Copy config template
cp main/config/config.example.h main/config/config.h
# Edit config.h with your WiFi/MQTT credentials

# 2. Build
idf.py set-target esp32s3
idf.py build

# 3. Flash
idf.py -p /dev/ttyUSB0 flash monitor
```

## Configuration

Copy `main/config/config.example.h` to `main/config/config.h` and fill in:

| Parameter | Description |
|-----------|-------------|
| `PYRO_DEVICE_ID` | Unique device identifier |
| `PYRO_TENANT_ID` | Tenant/organization ID |
| `PYRO_WIFI_SSID` | WiFi network name |
| `PYRO_WIFI_PASSWORD` | WiFi password |
| `PYRO_MQTT_BROKER_URI` | MQTT broker address |
| `PYRO_HMAC_KEY` | HMAC-SHA256 key (hex, 64 chars) |

**config.h is in .gitignore — never commit secrets.**

## Device States

```
BOOTING → PROVISIONING (no creds) or CONNECTING (has creds)
CONNECTING → ACTIVE (connected) or OFFLINE_BUFFERING (no WiFi/MQTT)
ACTIVE → OFFLINE_BUFFERING (disconnect) or REVOKED (revoke command)
OFFLINE_BUFFERING → ACTIVE (reconnected) or DEGRADED (buffer full/72h)
DEGRADED → CONNECTING (recovery) or ERROR (fatal)
ERROR → CONNECTING (recovery success)
```

## Telemetry Payload

Published to `pyrosense/{deviceId}/features/periodic` every 5s:

```json
{
  "schemaVersion": "1.0",
  "deviceId": "pyro-dev-001",
  "tenantId": "tenant-lab-01",
  "timestamp": "2026-05-27T14:30:00Z",
  "sequenceNumber": 42,
  "firmwareVersion": "0.1.0",
  "samplingWindowMs": 1000,
  "features": {
    "rmsCurrent": 14.50,
    "rmsVoltage": 230.1,
    "activePower": 3200.5,
    "reactivePower": 300.2,
    "powerFactor": 0.950,
    "thd": 4.2,
    "temperatureCelsius": 39.0,
    "hfNoiseLevel": 0.050,
    "microArcCount": 0,
    "transientCount": 1,
    "signalQuality": 0.970
  },
  "security": {
    "nonce": "6839ab0100000001a3f2bc01",
    "signature": "disabled"
  }
}
```

## Tests

| Suite | Count | Description |
|-------|:-----:|-------------|
| test_rms | 6 | RMS computation (DC, sine, known values) |
| test_signal_quality | 6 | Quality scoring (perfect, degraded, combined) |
| test_state_machine | 10 | State transitions (valid + invalid + callbacks) |
| test_offline_queue | 8 | Queue FIFO, priority, eviction, capacity |
| test_payload | 5 | JSON structure, sequence, fields |
| test_nonce | 5 | Uniqueness, format, freshness |
| test_integration_pipeline | 3 | Full sensor→features→payload→publish pipeline |
| **Total** | **43** | |

## Adding Real Sensor Drivers

1. Create a new class implementing `ISensorProvider` (see `sensors/sensor_interface.h`)
2. Implement `init()`, `read()`, `self_test()`, `provider_name()`
3. Register it in `app_main.cpp` instead of `MockSensorProvider`

Example:
```cpp
#include "sensors/sensor_interface.h"

class Ads1115SensorProvider : public ISensorProvider {
    bool init() override { /* I2C setup */ }
    SensorReading read() override { /* ADC read */ }
    bool self_test() override { /* verify communication */ }
    const char* provider_name() const override { return "ADS1115+MAX31865"; }
};
```

## Project Structure

```
firmware/pyrosense-device/
├── CMakeLists.txt              ESP-IDF project file
├── sdkconfig.defaults          SDK configuration
├── partitions.csv              Flash partition table (OTA A/B + SPIFFS)
├── version.txt                 Firmware version
├── .gitignore                  Excludes secrets, build artifacts
├── main/
│   ├── CMakeLists.txt          Component registration
│   ├── app_main.cpp            Entry point & orchestrator
│   ├── config/                 Configuration & state machine
│   ├── sensors/                Sensor interface + mock provider
│   ├── signal_processing/      Feature extraction (RMS, THD, quality)
│   ├── telemetry/              Payload & heartbeat construction
│   ├── connectivity/           WiFi + MQTT
│   ├── security/               HMAC + nonce
│   ├── storage/                Offline queue
│   ├── diagnostics/            Logger + self-test
│   └── utils/                  Time + UUID
├── test/
│   ├── CMakeLists.txt          Host-based test build
│   └── unit/                   Unit & integration tests
└── docs/                       Pinout, calibration, flashing
```
