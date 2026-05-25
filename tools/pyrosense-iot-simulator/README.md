# PyroSense IoT Simulator

Standalone IoT device simulator for testing the PyroSense AI Platform end-to-end without real ESP32 hardware.

## Features

- Simulates multiple tenants, buildings, and devices
- Generates realistic electrical telemetry data (10 metrics)
- 7 configurable scenarios for testing the full pipeline
- MQTT and REST publishing
- Time acceleration for multi-day simulations
- Interactive CLI

## Scenarios

| Scenario | Description | What it tests |
|----------|-------------|---------------|
| `NORMAL` | Stable electrical values with natural noise | Baseline building, false positive rejection |
| `INSULATION_DEGRADATION_PROGRESSIVE` | Gradual THD and leakage current increase over 200 ticks | Drift detection, trend analysis |
| `LOOSE_CONNECTION` | Random voltage drops with temperature spikes (15% probability) | Spike detection, micro-arc patterns |
| `MICRO_ARC_RECURRENT` | Recurring arc events (25% probability per tick) | Micro-arc detection, recurrence scoring |
| `OVERLOAD` | Sinusoidal current exceeding 1.5-2x nominal | Overload alerting, thermal prediction |
| `TEMPERATURE_RISE` | Progressive temperature from 25C to 65C over 150 ticks | Temperature trend detection |
| `DEVICE_OFFLINE` | No telemetry published | Heartbeat monitoring, offline detection |

## Quick Start

### Build

```bash
cd tools/pyrosense-iot-simulator
mvn clean package
```

### Run

```bash
java -jar target/pyrosense-iot-simulator-1.0.0.jar
```

### CLI Commands

```
1. start    - Start simulation
2. stop     - Stop simulation
3. scenario - Choose scenario
4. status   - Show status
5. devices  - List devices
6. config   - Show configuration
7. exit     - Exit
```

## CLI Options

```
--mqtt-host HOST       MQTT broker host (default: localhost)
--mqtt-port PORT       MQTT broker port (default: 1883)
--rest-url URL         REST ingestion URL (default: http://localhost:8083)
--tenants N            Number of tenants (default: 2)
--buildings N          Buildings per tenant (default: 2)
--devices N            Devices per building (default: 3)
--interval SECONDS     Publish interval in seconds (default: 5)
--acceleration FACTOR  Time acceleration factor (default: 1)
--days N               Simulation duration in days (default: 1)
--use-mqtt true|false  Enable MQTT publishing (default: true)
--use-rest true|false  Enable REST publishing (default: false)
```

## Examples

### Test baseline building (2 hours accelerated)

```bash
java -jar target/pyrosense-iot-simulator-1.0.0.jar \
  --tenants 1 --devices 5 --acceleration 60 --days 1 --interval 1
```

### Test with REST fallback only

```bash
java -jar target/pyrosense-iot-simulator-1.0.0.jar \
  --use-mqtt false --use-rest true --rest-url http://localhost:8083
```

### Stress test (many devices)

```bash
java -jar target/pyrosense-iot-simulator-1.0.0.jar \
  --tenants 5 --buildings 4 --devices 10 --interval 1
```

## Docker

### Build

```bash
docker build -t pyrosense-iot-simulator .
```

### Run (connected to PyroSense network)

```bash
docker run --rm -it --network pyrosense-network \
  pyrosense-iot-simulator \
  --mqtt-host mosquitto --tenants 3 --devices 5
```

## Telemetry Payload

Each reading contains:

| Field | Description | Normal range |
|-------|-------------|--------------|
| `rmsCurrent` | RMS current (A) | 8-12 |
| `rmsVoltage` | RMS voltage (V) | 225-235 |
| `activePower` | Active power (W) | 2000-2600 |
| `reactivePower` | Reactive power (VAR) | 150-250 |
| `powerFactor` | Power factor (0-1) | 0.93-0.97 |
| `thd` | Total harmonic distortion (%) | 2-4 |
| `temperatureCelsius` | Temperature (C) | 22-28 |
| `hfNoiseLevel` | High-frequency noise level | 0.05-0.15 |
| `microArcCount` | Micro-arc events per interval | 0 |
| `transientCount` | Transient events per interval | 0-2 |

## MQTT Topic Format

```
pyrosense/{tenantId}/{deviceId}/telemetry
```

## Architecture

```
src/main/java/com/pyrosense/simulator/
├── cli/           → SimulatorCli (interactive CLI, arg parsing)
├── config/        → SimulatorConfig (all parameters)
├── domain/        → SimulatedTenant, SimulatedBuilding, SimulatedDevice, SimulationEngine
├── publisher/     → TelemetryPublisher interface, MqttTelemetryPublisher, RestTelemetryPublisher
└── scenario/      → ScenarioEngine (7 scenarios, realistic data generation)
```

## Tests

```bash
mvn test
```

- `TelemetryReadingTest` — builder and record tests
- `SimulatedDeviceTest` — device creation, MQTT topic
- `SimulatedTenantTest` — tenant hierarchy
- `SimulatorConfigTest` — configuration defaults and builder
- `SimulationEngineTest` — start/stop, publishing, scenarios
- `ScenarioEngineTest` — all 7 scenarios with statistical assertions
