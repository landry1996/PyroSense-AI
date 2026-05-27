# PyroSense Firmware — Getting Started

Guide de demarrage rapide pour le firmware ESP32-S3 PyroSense.

---

## 1. Prerequis

### Build Host (tests sans ESP32)

| Outil | Version | Installation |
|-------|---------|--------------|
| CMake | 3.16+ | `apt install cmake` / `brew install cmake` / `choco install cmake` |
| GCC ou Clang | C++17 | `apt install g++` / Xcode CLT / MSVC 2019+ |
| Git | 2.x | Deja installe |

### Build Target (ESP32-S3)

| Outil | Version | Installation |
|-------|---------|--------------|
| ESP-IDF | 5.x | [Guide officiel](https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/get-started/) |
| Python | 3.8+ | Inclus dans ESP-IDF install |
| USB Driver | CP210x ou CH340 | Selon dev board |

---

## 2. Structure du projet

```
firmware/pyrosense-device/
├── main/                    Code source firmware
│   ├── app_main.cpp         Point d'entree
│   ├── config/              Configuration + state machine
│   ├── sensors/             Interface capteur + mock
│   ├── signal_processing/   Extraction features
│   ├── telemetry/           Construction payload JSON
│   ├── connectivity/        WiFi + MQTT
│   ├── security/            HMAC + nonce
│   ├── storage/             Queue offline
│   ├── diagnostics/         Logs + self-test
│   └── utils/               Time + UUID
├── test/                    Tests host (sans ESP32)
├── CMakeLists.txt           Projet ESP-IDF
├── sdkconfig.defaults       Configuration SDK
└── partitions.csv           Table de partitions flash
```

---

## 3. Build et tests (Host — sans hardware)

```bash
cd firmware/pyrosense-device/test
mkdir -p build && cd build
cmake ..
cmake --build .
ctest --output-on-failure
```

Resultat attendu :
```
      Start  1: RmsCalculator
 1/7  Test #1: RmsCalculator ............   Passed
      Start  2: SignalQuality
 2/7  Test #2: SignalQuality ............   Passed
      Start  3: StateMachine
 3/7  Test #3: StateMachine .............   Passed
      Start  4: OfflineQueue
 4/7  Test #4: OfflineQueue .............   Passed
      Start  5: PayloadBuilder
 5/7  Test #5: PayloadBuilder ...........   Passed
      Start  6: NonceGenerator
 6/7  Test #6: NonceGenerator ...........   Passed
      Start  7: IntegrationPipeline
 7/7  Test #7: IntegrationPipeline ......   Passed

100% tests passed, 0 tests failed
```

---

## 4. Build pour ESP32-S3

### Premiere fois

```bash
# Installer ESP-IDF (si pas encore fait)
mkdir -p ~/esp && cd ~/esp
git clone --recursive https://github.com/espressif/esp-idf.git
cd esp-idf && ./install.sh esp32s3
source export.sh

# Configurer le projet
cd firmware/pyrosense-device
cp main/config/config.example.h main/config/config.h
# Editer config.h avec vos identifiants WiFi/MQTT

idf.py set-target esp32s3
idf.py build
```

### Flash et monitor

```bash
# Flash (adapter le port)
idf.py -p /dev/ttyUSB0 flash

# Monitor (logs serie)
idf.py -p /dev/ttyUSB0 monitor

# Flash + monitor en une commande
idf.py -p /dev/ttyUSB0 flash monitor
```

### Sortir du monitor : `Ctrl+]`

---

## 5. Configuration

### Fichier config.h (secrets)

Copier `main/config/config.example.h` → `main/config/config.h` :

```c
#define PYRO_DEVICE_ID        "pyro-lab-001"
#define PYRO_TENANT_ID        "tenant-dev-01"
#define PYRO_WIFI_SSID        "MonWiFi"
#define PYRO_WIFI_PASSWORD    "MotDePasse"
#define PYRO_MQTT_BROKER_URI  "mqtt://192.168.1.50"
#define PYRO_MQTT_PORT        1883
#define PYRO_HMAC_KEY         ""
#define PYRO_HMAC_ENABLED     false
```

**Ce fichier est dans .gitignore — il ne sera jamais commite.**

### Configuration runtime (NVS)

En production, la configuration sera chargee depuis NVS (Non-Volatile Storage) au boot.
Pour le prototype lab, les valeurs par defaut dans `device_config.cpp` sont utilisees.

---

## 6. Tester avec un broker MQTT local

```bash
# Lancer Mosquitto (Docker)
docker run -d --name mosquitto -p 1883:1883 eclipse-mosquitto:2

# S'abonner a tous les topics PyroSense
mosquitto_sub -h localhost -t "pyrosense/#" -v

# Dans un autre terminal, lancer le firmware (host build)
cd test/build
./test_integration_pipeline
```

Ou, pour tester le flux complet avec le backend PyroSense :
```bash
# Depuis la racine du projet
docker compose up -d  # Demarre Mosquitto + Kafka + backend
# Puis flash/run le firmware
```

---

## 7. Ajouter un vrai driver capteur

1. Creer une classe implementant `ISensorProvider` :

```cpp
// main/sensors/ads1115_provider.h
#pragma once
#include "sensors/sensor_interface.h"

class Ads1115Provider : public pyrosense::ISensorProvider {
public:
    bool init() override;
    pyrosense::SensorReading read() override;
    bool self_test() override;
    const char* provider_name() const override { return "ADS1115"; }
};
```

2. Implementer les methodes avec les appels I2C ESP-IDF.

3. Enregistrer dans `app_main.cpp` :

```cpp
// Remplacer MockSensorProvider par le vrai driver
auto real_provider = std::make_unique<Ads1115Provider>();
SensorRegistry::instance().register_provider(std::move(real_provider));
```

---

## 8. Workflow de developpement

```
1. Modifier le code dans main/
2. Compiler les tests host : cd test/build && cmake --build . && ctest
3. Si tests passent : idf.py build (compilation ESP32)
4. Flash et verifier sur hardware : idf.py flash monitor
5. Verifier les messages MQTT : mosquitto_sub -t "pyrosense/#"
```

---

## 9. Troubleshooting

| Probleme | Solution |
|----------|----------|
| `config.h not found` | Copier `config.example.h` → `config.h` |
| Build host echoue | Verifier CMake 3.16+ et compilateur C++17 |
| `idf.py: command not found` | `source ~/esp/esp-idf/export.sh` |
| Flash echoue | Verifier port USB (`ls /dev/ttyUSB*`), appuyer BOOT + EN |
| MQTT ne publie pas | Verifier `PYRO_MQTT_BROKER_URI`, tester avec `mosquitto_pub` |
| Self-test echoue | Normal si aucun capteur hardware connecte (mock always passes) |

---

## 10. Prochaines etapes

| Sprint | Tache | Status |
|--------|-------|--------|
| 3.1 | Driver ADS1115 (I2C, courant) | A faire |
| 3.1 | Driver MAX31865 (SPI, temperature) | A faire |
| 3.1 | FFT pour THD reel | A faire |
| 3.1 | MQTT TLS (mTLS si ATECC608B) | A faire |
| 3.3 | Buffer SPIFFS persistant | A faire |
| 3.3 | OTA A/B partitions | A faire |
| 3.3 | Secure Boot v2 | A faire |
