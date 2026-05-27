# MVP 3 — Cadrage Complet : Du Prototype Capteur au Pilote Terrain

**Date :** 2026-05-27
**Auteur :** Architecture IoT
**Statut :** Cadrage (pre-implementation)
**Prerequis :** MVP 2 complet, audit securite passe (4 vulnerabilites corrigees), 571+ tests backend passent.

---

## Table des Matieres

1. [Perimetre exact du MVP 3](#1-perimetre-exact-du-mvp-3)
2. [Objectifs mesurables](#2-objectifs-mesurables)
3. [Hors perimetre](#3-hors-perimetre)
4. [Architecture edge-cloud mise a jour](#4-architecture-edge-cloud-mise-a-jour)
5. [Composants hardware envisages](#5-composants-hardware-envisages)
6. [Architecture firmware](#6-architecture-firmware)
7. [Architecture backend d'integration capteur reel](#7-architecture-backend-dintegration-capteur-reel)
8. [Protocole MQTT definitif](#8-protocole-mqtt-definitif)
9. [Protocole d'enrolement device](#9-protocole-denrolement-device)
10. [Strategie de securite IoT](#10-strategie-de-securite-iot)
11. [Strategie de collecte terrain](#11-strategie-de-collecte-terrain)
12. [Strategie de validation qualite des donnees](#12-strategie-de-validation-qualite-des-donnees)
13. [Plan de test laboratoire](#13-plan-de-test-laboratoire)
14. [Plan pilote terrain](#14-plan-pilote-terrain)
15. [Risques](#15-risques)
16. [Roadmap detaillee du MVP 3](#16-roadmap-detaillee-du-mvp-3)
17. [Documents a creer dans /docs](#17-documents-a-creer-dans-docs)
18. [Mise a jour de TODO.md](#18-mise-a-jour-de-todomd)

---

## 1. Perimetre exact du MVP 3

### En une phrase

Le MVP 3 transforme la plateforme logicielle validee (MVP 2) en systeme physique mesurable : capteurs reels sur rail DIN, firmware MQTT securise, pipeline edge-cloud, et pilote sur 2 batiments (10 capteurs) pendant 3 mois.

### Axes principaux

| Axe | Description | Livrable concret |
|-----|-------------|-----------------|
| **Hardware** | Concevoir et assembler un module capteur electrique rail DIN < 200 EUR | 10 prototypes fonctionnels |
| **Firmware** | Developper le firmware embarque (ESP32-S3) avec MQTT 5.0, TLS, buffer local | Firmware v1.0 flashable OTA |
| **Edge processing** | Calcul local (RMS, THD, features) — seuls features/events/agregats remontent au cloud | Architecture edge validee en labo |
| **Backend integration** | Adapter ingestion-service pour payloads reels, provisioning X.509, device lifecycle | Services mis a jour et deployes |
| **Securite IoT** | Enrollment X.509, secure boot, firmware signing, tamper detection | Chaine de confiance operationnelle |
| **Calibration** | Etalonner seuils sur donnees physiques reelles en labo puis terrain | Seuils publies par type d'installation |
| **Pilote terrain** | Deployer 10 capteurs sur 2 batiments, collecter 3 mois, valider detection | Rapport pilote avec KPIs mesures |
| **ML preparation** | Feature store, export dataset, Isolation Forest en shadow mode | Model en parallele (sans impact prod) |
| **Cloud** | Deployer sur Kubernetes (cloud manage) avec TLS, backups, monitoring | Plateforme accessible HTTPS |

### Critere de succes global

Le MVP 3 est reussi si, apres 3 mois de pilote terrain :
- 10 capteurs fonctionnent avec uptime > 95%
- Au moins 1 defaut electrique reel detecte avant incident
- Taux de faux positifs < 20%
- Dataset de 500+ echantillons etiquetes constitue
- Aucun incident de securite sur site

---

## 2. Objectifs mesurables

### Objectifs techniques

| # | Objectif | Metrique | Cible | Methode de mesure |
|---|----------|----------|:-----:|-------------------|
| O1 | Capteur fonctionnel sur rail DIN | Nombre de prototypes operationnels | 10 | Test bench validation |
| O2 | Cout unitaire capteur | EUR BOM + assemblage | < 200 EUR | Factures composants |
| O3 | Firmware stable | MTBF (Mean Time Between Failures) | > 30 jours | Watchdog reboot counter |
| O4 | Uptime capteurs terrain | Heures online / heures totales | > 95% | Prometheus `device.online.ratio` |
| O5 | Latence ingestion end-to-end | Timestamp device → timestamp Kafka | < 5s (P95) | Tracing OpenTelemetry |
| O6 | Perte de messages | Messages perdus / messages emis | < 0.5% | Compteurs MQTT vs Kafka |
| O7 | Buffer local (offline) | Duree buffering sans perte | > 24h | Test coupure reseau |
| O8 | Provisioning device | Temps d'enrollment complet | < 5 min | Chrono installation |
| O9 | Secure boot operationnel | Boot non-signe rejete | 100% | Test injection firmware corrompu |
| O10 | TLS end-to-end | Connexion sans TLS rejetee | 100% | Test port plain TCP |

### Objectifs detection

| # | Objectif | Metrique | Cible pilote | Methode de mesure |
|---|----------|----------|:------------:|-------------------|
| D1 | Sensibilite (recall) | VP / (VP + FN) | > 80% | Validation electricien |
| D2 | Precision | VP / (VP + FP) | > 60% | Validation electricien |
| D3 | Taux faux positifs | FP / total alertes | < 20% | Statistique 3 mois |
| D4 | Temps de detection | Debut anomalie → alerte | < 24h | Logs + timestamp alerte |
| D5 | Temps anticipation | Alerte → defaillance confirmee | > 48h | Retro-analyse incidents |

### Objectifs operationnels

| # | Objectif | Metrique | Cible | Methode de mesure |
|---|----------|----------|:-----:|-------------------|
| P1 | Disponibilite plateforme | Uptime cloud | > 99% | Prometheus/Grafana |
| P2 | Electricien repond dans SLA | Interventions < 4h (CRITICAL) | > 80% | Metriques intervention |
| P3 | Gestionnaire utilise dashboard | Sessions/semaine | > 3 | Analytics |
| P4 | Dataset etiquete | Echantillons avec label electricien | > 500 | Count DB |
| P5 | ML shadow mode | Model deploye en parallele | 1 model | Artifact registry |

---

## 3. Hors perimetre

### Explicitement exclu du MVP 3

| Element | Raison d'exclusion | Quand ? |
|---------|-------------------|---------|
| Certification IEC 61439 / NF C 15-100 | Necessite produit final, pas prototype | MVP 4 (commercialisation) |
| Marquage CE (CEM, basse tension, RED) | Pre-assessment OK, certification formelle hors scope | MVP 4 |
| Manipulation 230V par non-professionnel | **Interdit.** Tout contact secteur = electricien habilite B2V+ | Permanent |
| Haute disponibilite multi-AZ | Sur-dimensionne pour 10 capteurs pilote | Production (100+ batiments) |
| Application mobile native | PWA responsive suffit pour le pilote | MVP 3.6 ou MVP 4 |
| ML en production (remplacement stats) | Pas assez de donnees — shadow mode seulement | Apres 6 mois de terrain |
| Federated learning cross-tenant | Necessite 50+ tenants | Production a grande echelle |
| Edge AI (inference sur ESP32) | ESP32-S3 limite — features seulement, pas de model inference | MVP 4 avec NPU |
| Multi-region cloud | Un seul cluster suffit pour le pilote | Production |
| Support 24/7 | Equipe reduite, monitoring + alertes suffisent | Production |
| Firmware OTA via LoRaWAN / NB-IoT | WiFi + 4G fallback suffisent en batiment | Produit commercial |
| Integration BMS (Modbus, BACnet) | Pas de BMS sur sites pilotes | Produit commercial |
| Facturation / billing tenant | Pilote gratuit, pas de monetisation | Produit commercial |

### Limites de securite strictes

- **Aucune manipulation du secteur 230V par un non-habilite** — tout raccordement = electricien habilite minimum B2V
- **Banc de test basse tension FIRST** — validation signal simule avant toute connexion reseau electrique reel
- **Validation electricien BEFORE terrain** — aucun deploiement terrain sans validation prealable en labo par un electricien qualifie

---

## 4. Architecture edge-cloud mise a jour

### Vue d'ensemble

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              SITE (Batiment)                                      │
│                                                                                  │
│  ┌──────────────────────────────────────────────┐                               │
│  │           TABLEAU ELECTRIQUE (rail DIN)        │                               │
│  │                                                │                               │
│  │  ┌────────┐ ┌────────┐ ┌────────┐            │                               │
│  │  │Capteur │ │Capteur │ │Capteur │  (max 6)   │                               │
│  │  │CT+Temp │ │CT+Temp │ │CT+Temp │            │                               │
│  │  └───┬────┘ └───┬────┘ └───┬────┘            │                               │
│  │      │ I2C/SPI   │          │                  │                               │
│  │      └───────────┼──────────┘                  │                               │
│  │                  │                              │                               │
│  │  ┌───────────────▼──────────────────────┐     │                               │
│  │  │        GATEWAY (ESP32-S3)             │     │                               │
│  │  │                                       │     │                               │
│  │  │  ┌─────────────────────────────────┐ │     │                               │
│  │  │  │ Edge Processing                  │ │     │                               │
│  │  │  │ - RMS calculation (1s window)   │ │     │                               │
│  │  │  │ - THD via FFT (10s window)      │ │     │                               │
│  │  │  │ - Feature extraction            │ │     │                               │
│  │  │  │ - Event detection (arc, surge)  │ │     │                               │
│  │  │  │ - Local buffer (SPIFFS, 24h)    │ │     │                               │
│  │  │  └─────────────────────────────────┘ │     │                               │
│  │  │                                       │     │                               │
│  │  │  ┌─────────────────────────────────┐ │     │                               │
│  │  │  │ Communication                    │ │     │                               │
│  │  │  │ - WiFi (primary)                │ │     │                               │
│  │  │  │ - 4G LTE-M (fallback)          │ │     │                               │
│  │  │  │ - TLS 1.3 + X.509 client cert  │ │     │                               │
│  │  │  │ - MQTT 5.0                      │ │     │                               │
│  │  │  └─────────────────────────────────┘ │     │                               │
│  │  └──────────────────────────────────────┘     │                               │
│  └──────────────────────────────────────────────┘                               │
│                          │                                                        │
│                          │ WiFi / 4G                                              │
└──────────────────────────┼────────────────────────────────────────────────────────┘
                           │
                           │ TLS 1.3 (MQTT 5.0)
                           │
┌──────────────────────────▼────────────────────────────────────────────────────────┐
│                              CLOUD (Kubernetes)                                     │
│                                                                                    │
│  ┌────────────────────┐     ┌─────────────────────┐    ┌───────────────────────┐ │
│  │ MQTT Broker (EMQX) │     │ Ingestion Service   │    │ Device Service        │ │
│  │ - mTLS terminaison │────▶│ - Validation schema │───▶│ - Provisioning X.509  │ │
│  │ - ACL per device   │     │ - Deduplication     │    │ - Lifecycle           │ │
│  │ - Rate limiting    │     │ - Kafka publish     │    │ - Heartbeat           │ │
│  └────────────────────┘     └─────────┬───────────┘    └───────────────────────┘ │
│                                        │                                           │
│                                        ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────────────────┐  │
│  │                        Kafka (KRaft mode)                                    │  │
│  │  Topics: telemetry-events | anomaly-detected | risk-assessed |               │  │
│  │          alert-created | intervention-events | notification-events            │  │
│  └───────┬─────────────────────┬────────────────────────┬──────────────────────┘  │
│          │                     │                        │                          │
│          ▼                     ▼                        ▼                          │
│  ┌──────────────┐    ┌──────────────┐         ┌──────────────────┐               │
│  │ Signal       │    │ Risk Scoring │         │ Alerting +       │               │
│  │ Analysis     │    │              │         │ Notification +   │               │
│  │ (Welford)    │    │ (6 factors)  │         │ Maintenance      │               │
│  └──────────────┘    └──────────────┘         └──────────────────┘               │
│                                                                                    │
│  ┌──────────────────────────────────────────────────────────────────────────┐     │
│  │  ML Pipeline (Shadow Mode)                                                │     │
│  │  ┌──────────────┐  ┌────────────────┐  ┌───────────────┐                │     │
│  │  │ Feature Store │  │ Model Training │  │ Shadow Scoring │                │     │
│  │  │ (export)      │  │ (weekly batch) │  │ (Isolation F.) │                │     │
│  │  └──────────────┘  └────────────────┘  └───────────────┘                │     │
│  └──────────────────────────────────────────────────────────────────────────┘     │
│                                                                                    │
│  ┌────────────────────────────────────────────────────────────────────────────┐   │
│  │  Storage                                                                    │   │
│  │  PostgreSQL 16 + TimescaleDB │ Redis 7 │ S3 (PDF, firmware, datasets)      │   │
│  └────────────────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────────────────┘
```

### Principes architecturaux edge-cloud

| Principe | Implementation | Raison |
|----------|---------------|--------|
| **Pas de donnees brutes vers le cloud** | Le gateway calcule RMS, THD, features localement. Seuls les resultats remontent. | Bande passante, confidentialite, latence |
| **Stockage local si offline** | Buffer SPIFFS 4 Mo (~24h de features a 1Hz) | Resilience reseau |
| **Replay apres reconnexion** | Drain buffer chronologique vers MQTT broker | Zero perte de donnees |
| **Events immediats (edge)** | Arc detecte localement → event publie sans attente | Latence critique pour securite |
| **Features periodiques** | Chaque 5s : RMS, T°. Chaque 60s : THD, stats glissantes | Compromis bande passante/precision |
| **Health telemetrie reduite** | Chaque 5min : sante device (CPU, RAM, flash, RSSI) | Monitoring sans surcharge |

### Differences avec MVP 2

| Aspect | MVP 2 (simulateur) | MVP 3 (capteur reel) |
|--------|--------------------|-----------------------|
| Source donnees | Simulateur Java MQTT | ESP32-S3 hardware |
| Frequence brute | 5s (payload complet) | 16kHz echantillonnage ADC, 1s features |
| Calcul | Tout dans le cloud | Edge: RMS, THD, events. Cloud: scoring, alerting |
| Protocole | MQTT 3.1.1, username/password | MQTT 5.0, X.509 mTLS |
| Broker | Mosquitto single | EMQX cluster (2 noeuds) |
| Payload | JSON complet depuis simulateur | CBOR compact (features uniquement) |
| Securite device | Token HMAC | X.509 + secure boot + firmware signing |
| Offline | N/A (simulateur toujours connecte) | Buffer local 24h + replay |

---

## 5. Composants hardware envisages

### Bill of Materials (BOM) — Module capteur v1

| Composant | Reference | Role | Prix unitaire (EUR) | Quantite |
|-----------|-----------|------|:-------------------:|:--------:|
| MCU + WiFi + BLE | ESP32-S3-WROOM-1 (N16R8) | Processing, comm, crypto | 4.50 | 1 |
| Capteur courant (CT) | YHDC SCT-013-030 (30A) | Mesure courant non-invasive | 8.00 | 3 (triphasé) |
| ADC haute resolution | ADS1115 (16-bit, 860 SPS) | Numerisation signal CT | 3.50 | 1 |
| Sonde temperature | MAX31865 + PT100 (classe A) | Temperature point de connexion | 12.00 | 2 |
| Detecteur transitoires | Comparateur LM393 + diviseur | Detection front rapide (micro-arc) | 2.00 | 1 |
| Module 4G fallback | SIM7080G (LTE-M / NB-IoT) | Communication backup | 18.00 | 1 |
| Alimentation | Hi-Link HLK-PM01 (5V/3W) | Alimentation depuis rail DIN | 4.00 | 1 |
| Regulateur | AMS1117-3.3 | 3.3V pour ESP32 | 0.30 | 1 |
| Crypto element | ATECC608B | Stockage cles X.509, ECDSA | 1.50 | 1 |
| Flash externe | W25Q128 (16 Mo) | Buffer local offline | 2.00 | 1 |
| Boitier rail DIN | 4M (72mm) plastique IP20 | Protection mecanique | 8.00 | 1 |
| Antenne WiFi | PCB antenna + UFL connector | Connectivite | 1.50 | 1 |
| LED statut | RGB diffuse 3mm | Indication visuelle | 0.20 | 1 |
| Connecteurs | Bornier a vis 5.08mm | Raccordement CT + sondes | 3.00 | 1 |
| PCB | 4 couches, FR4, 80x40mm | Circuit imprime | 15.00 | 1 |
| Passifs (R, C, L) | Divers | Filtrage, protection | 3.00 | lot |
| **TOTAL BOM** | | | **~86 EUR** | |
| Assemblage + test | | | ~30 EUR | |
| Marge erreur (20%) | | | ~23 EUR | |
| **COUT TOTAL UNITAIRE** | | | **~139 EUR** | |

### Justification des choix

| Choix | Raison | Alternative ecartee |
|-------|--------|---------------------|
| ESP32-S3 (pas STM32) | WiFi+BLE integre, crypto hardware (AES, SHA, RSA), PSRAM 8Mo, communaute | STM32 + module WiFi externe (plus complexe, plus cher) |
| CT non-invasif (pas shunt) | **Aucune coupure de circuit necessaire.** Pose par electricien sans consignation longue | Shunt: necessite coupure, intrusif |
| ADS1115 (pas ADC interne) | 16-bit a 860 SPS vs 12-bit ESP32 interne. Mesure THD necessite resolution | ADC interne ESP32: bruit, 12-bit insuffisant pour harmoniques |
| ATECC608B | Stockage prive des cles, ECDSA hardware, anti-extraction | Cle en flash: extractible si acces physique |
| MAX31865 + PT100 | Precision ±0.15°C, linearisation hardware, norme industrielle | Thermistance NTC: derive, moins precis, non lineaire |
| LTE-M fallback | Couverture interieure, faible conso, pas besoin de SIM data volumetrique | LoRa: trop faible debit pour features 1s. WiFi seul: risque coupure |
| Boitier 4M rail DIN | Standard electricien, montage rapide, IP20 suffisant en tableau | Boitier custom: delai, cout, pas standard |

### Contraintes physiques

| Contrainte | Valeur | Consequence design |
|------------|--------|-------------------|
| Dimensions max | 72 × 90 × 65 mm (4 modules DIN) | PCB 80×40mm double face |
| Temperature operationnelle | -10°C a +60°C | Composants automotive grade, pas de ventilateur |
| Alimentation | 230V rail DIN → HLK-PM01 → 5V → AMS1117 → 3.3V | Isolation galvanique HLK-PM01 |
| Consommation max | < 2W (0.5W typ, pic WiFi TX) | Compatible alimentation tableau |
| Nombre CTs par gateway | 3 (triphase) ou 6 (2 departs) | Multiplexeur ADC si > 4 canaux |
| Distance CT → gateway | < 2m (cable blinde) | Bruit EMI minimise |

### Schema bloc simplifie

```
                  ┌─────────────────────────────────────────────────────┐
    230V rail ──▶ │ HLK-PM01 (5V) ──▶ AMS1117 (3.3V) ──▶ ESP32-S3    │
                  │                                         │           │
    CT Phase L1 ──▶──────────▶ ADS1115 ──────I2C──────────▶│           │
    CT Phase L2 ──▶──────────▶ (ch0-ch2)                   │   WiFi    │──▶ MQTT Broker
    CT Phase L3 ──▶──────────▶                              │   (+4G)   │
                  │                                         │           │
    PT100 #1   ──▶──────────▶ MAX31865 ──────SPI──────────▶│           │
    PT100 #2   ──▶──────────▶ MAX31865                     │           │
                  │                                         │           │
    Arc detect ──▶──────────▶ LM393 ─────────GPIO─────────▶│           │
                  │                                         │           │
                  │            ATECC608B ─────I2C──────────▶│           │
                  │            W25Q128 ──────SPI───────────▶│           │
                  │            LED RGB ──────GPIO──────────▶│           │
                  └─────────────────────────────────────────────────────┘
```

---

## 6. Architecture firmware

### Stack logiciel

| Couche | Technologie | Role |
|--------|-------------|------|
| RTOS | ESP-IDF 5.x (FreeRTOS) | Multitache, drivers, networking |
| Communication | ESP-MQTT (MQTT 5.0) | Publication features/events |
| Crypto | mbedTLS + ATECC608B driver | TLS 1.3, X.509, ECDSA |
| Storage | SPIFFS + NVS | Buffer offline + configuration |
| OTA | ESP-IDF OTA (A/B partitions) | Mise a jour securisee |
| Watchdog | Hardware WDT + Task WDT | Auto-reboot si freeze |

### Architecture en taches (FreeRTOS)

```
┌──────────────────────────────────────────────────────────────────────┐
│                        ESP32-S3 FreeRTOS Tasks                        │
│                                                                      │
│  ┌─────────────────┐  Priority: 5 (highest)                        │
│  │ WATCHDOG TASK   │  Period: 1s                                    │
│  │ - Feed HW WDT   │  Stack: 2048 bytes                            │
│  │ - Monitor tasks  │                                                │
│  └─────────────────┘                                                │
│                                                                      │
│  ┌─────────────────┐  Priority: 4                                   │
│  │ ACQUISITION TASK│  Period: ISR-driven (ADC DMA) + 1s processing  │
│  │ - ADC sampling   │  Stack: 8192 bytes                            │
│  │ - RMS calc       │  Core: 1 (dedicateé)                          │
│  │ - Event detect   │                                                │
│  └────────┬────────┘                                                │
│           │ Queue (features_queue, 64 items)                         │
│           ▼                                                          │
│  ┌─────────────────┐  Priority: 3                                   │
│  │ PROCESSING TASK │  Period: event-driven (queue consumer)          │
│  │ - Feature extract│  Stack: 8192 bytes                            │
│  │ - THD (FFT 1024)│  Core: 0                                       │
│  │ - Statistics     │                                                │
│  └────────┬────────┘                                                │
│           │ Queue (publish_queue, 128 items)                         │
│           ▼                                                          │
│  ┌─────────────────┐  Priority: 2                                   │
│  │ COMMUNICATION   │  Period: event-driven (queue consumer)          │
│  │ TASK            │  Stack: 8192 bytes                              │
│  │ - MQTT publish   │  Core: 0                                       │
│  │ - Buffer mgmt    │                                                │
│  │ - Reconnect      │                                                │
│  └─────────────────┘                                                │
│                                                                      │
│  ┌─────────────────┐  Priority: 1 (lowest)                          │
│  │ HEALTH TASK     │  Period: 5 min                                  │
│  │ - Self-diag      │  Stack: 4096 bytes                            │
│  │ - Memory check   │                                                │
│  │ - Flash usage    │                                                │
│  │ - OTA check      │                                                │
│  └─────────────────┘                                                │
└──────────────────────────────────────────────────────────────────────┘
```

### Pipeline de traitement signal (edge)

```
ADC (16kHz, 16-bit)
    │
    ▼
┌──────────────────────────────────┐
│ ACQUISITION (ISR + DMA)          │
│ - Buffer circulaire 16K samples  │
│ - Declenchement chaque 1s        │
└──────────────┬───────────────────┘
               │ 16000 samples / seconde
               ▼
┌──────────────────────────────────┐
│ FEATURE EXTRACTION (1s window)   │
│ - RMS courant (3 phases)         │
│ - RMS tension                    │
│ - Puissance active/reactive      │
│ - Facteur de puissance           │
│ - Crete / RMS ratio (crest)      │
│ - Zero crossing rate             │
└──────────────┬───────────────────┘
               │
               ▼
┌──────────────────────────────────┐
│ FEATURE EXTRACTION (10s window)  │
│ - THD via FFT 1024 points        │
│ - Harmoniques H3, H5, H7, H9    │
│ - Spectre HF (2-150 kHz)        │
│ - Energie bande arc (50-100kHz)  │
└──────────────┬───────────────────┘
               │
               ▼
┌──────────────────────────────────┐
│ EVENT DETECTION (continu)        │
│ - Transitoire > seuil → ARC     │
│ - dT/dt > 2°C/min → OVERHEAT   │
│ - I > 1.2 × In → OVERCURRENT    │
│ - Crest factor > 3 → DISTORTION │
└──────────────┬───────────────────┘
               │
               ├──▶ publish_queue (features periodiques)
               └──▶ publish_queue (events immediats, priorite haute)
```

### Partition table (flash 16 Mo)

| Partition | Type | Taille | Usage |
|-----------|------|:------:|-------|
| nvs | data | 24 KB | Configuration, WiFi credentials |
| otadata | data | 8 KB | OTA boot selection |
| phy_init | data | 4 KB | PHY calibration |
| factory | app | 4 MB | Firmware factory (fallback) |
| ota_0 | app | 4 MB | Firmware slot A |
| ota_1 | app | 4 MB | Firmware slot B |
| spiffs | data | 4 MB | Buffer offline (features/events) |

### Strategie OTA (Over-The-Air)

| Etape | Action | Securite |
|-------|--------|----------|
| 1 | Platform publie commande `FIRMWARE_UPDATE` | Signe ECDSA (cle platform) |
| 2 | Device verifie signature commande | ATECC608B verify |
| 3 | Device telecharge firmware via HTTPS | TLS + SHA-256 du binaire verifie |
| 4 | Ecriture dans partition inactive (A ou B) | Verification integrite post-ecriture |
| 5 | Reboot sur nouvelle partition | esp_ota_set_boot_partition |
| 6 | Validation (10 heartbeats consecutifs OK) | Rollback automatique si echec |
| 7 | Confirmation a la platform | Event `FIRMWARE_UPDATED` |
| 8 | Ancienne partition marquee invalide | Securite: pas de downgrade |

### Gestion offline

```
ONLINE                    OFFLINE                   RECONNECT
  │                          │                          │
  │ features_queue ──▶ MQTT  │ features_queue ──▶ SPIFFS│ SPIFFS drain ──▶ MQTT
  │                          │ (append fichier horodate)│ (FIFO, throttled)
  │                          │                          │
  │ buffer_level: 0%         │ buffer_level: 0-100%     │ buffer_level: 100% → 0%
  │                          │                          │
  │                          │ Si buffer > 90%:         │ Debit drain: 10 msg/s
  │                          │ - Reduire frequence      │ (pas de flood broker)
  │                          │ - Garder events, dropper │
  │                          │   features anciennes     │ Resume temps reel
  │                          │                          │ quand buffer vide
```

### Etats firmware (state machine)

```
                    ┌──────────────┐
     Power On ────▶ │  BOOTING     │
                    └──────┬───────┘
                           │ Secure boot OK + Self-test OK
                           ▼
                    ┌──────────────┐
                    │ PROVISIONING │ (si pas de certificat)
                    └──────┬───────┘
                           │ Cert installe dans ATECC608B
                           ▼
                    ┌──────────────┐
                    │  CONNECTING  │
                    └──────┬───────┘
                           │ MQTT CONNACK received
                           ▼
                    ┌──────────────┐
                    │  LEARNING    │ (7 premiers jours)
                    └──────┬───────┘
                           │ Baseline stable
                           ▼
                    ┌──────────────┐         ┌──────────────┐
                    │   ACTIVE     │◀───────▶│   OFFLINE    │
                    │ (nominal)    │ Deconn. │ (buffering)  │
                    └──────┬───────┘         └──────────────┘
                           │
                           │ Commande OTA
                           ▼
                    ┌──────────────┐
                    │  UPDATING    │
                    └──────┬───────┘
                           │ OK ──▶ ACTIVE
                           │ FAIL ──▶ ROLLBACK ──▶ ACTIVE (ancien firmware)
```

---

## 7. Architecture backend d'integration capteur reel

### Modifications a apporter aux services existants

#### Ingestion Service — Adaptations

| Modification | Raison | Effort |
|-------------|--------|:------:|
| Support CBOR (en plus de JSON) | Payload compact edge, economie bande passante | 2 jours |
| Validation schema features (pas raw samples) | Payload redefini (features pas mesures brutes) | 1 jour |
| Decodage metadata firmware v2 | Nouveaux champs: `bufferDrainMode`, `edgeVersion`, `cryptoChipId` | 1 jour |
| Gestion replay buffer (messages anciens) | Timestamps potentiellement old de 24h — ne pas rejeter | 1 jour |
| Metric: `ingestion_buffer_drain_total` | Differencier messages temps reel vs replay | 0.5 jour |
| Support MQTT 5.0 user properties | Extraction device-cert-fingerprint, correlation-id | 1 jour |

#### Device Service — Adaptations

| Modification | Raison | Effort |
|-------------|--------|:------:|
| Provisioning X.509 (CSR workflow) | Remplacement username/password par certificat | 1 semaine |
| Device state: `LEARNING` | Nouveau state entre ACTIVATED et ACTIVE | 2 jours |
| Firmware registry (versions, rollback history) | Tracking firmware par device | 3 jours |
| Certificate lifecycle (rotation, revocation, CRL) | Securite long terme | 1 semaine |
| Hardware metadata (BOM version, PCB revision, crypto chip serial) | Tracabilite | 2 jours |

#### Signal Analysis Service — Adaptations

| Modification | Raison | Effort |
|-------------|--------|:------:|
| Input: features pre-calculees (pas raw) | Edge fait le DSP, cloud recoit features | 2 jours |
| Baseline validation edge vs cloud | Comparer RMS edge et baseline cloud pour drift detection | 3 jours |
| Nouveau facteur: `arc_energy_band` (50-100kHz) | Capteur reel fournit spectre HF | 2 jours |
| Calibration coefficients par device (compensation CT, T offset) | Chaque capteur a ses biais | 3 jours |
| Mode `LEARNING` (7j sans alertes, baseline construction) | Adaptation au terrain | 1 jour (exists partiellement) |

#### Nouveau service : Feature Store Service (optionnel MVP 3.5)

| Responsabilite | Implementation |
|----------------|----------------|
| Export telemetrie vers Parquet | Job batch quotidien, S3 output |
| Rolling statistics (1h, 6h, 24h, 7j) | Materialized views TimescaleDB |
| Label management (electricien feedback) | REST API + Kafka consumer |
| Dataset generation (train/test split) | Job batch hebdomadaire |

### Schema d'integration

```
┌───────────────┐     ┌──────────────────┐     ┌────────────────────┐
│ ESP32 Gateway │────▶│ EMQX Broker      │────▶│ Ingestion Service  │
│ (CBOR/MQTT5)  │     │ (mTLS, ACL)      │     │ (CBOR decode,      │
│               │     │                  │     │  validate features, │
│               │     │                  │     │  Kafka publish)     │
└───────────────┘     └──────────────────┘     └─────────┬──────────┘
                                                          │
                              Kafka: telemetry-features    │
                      ┌───────────────────────────────────┘
                      │
        ┌─────────────▼────────────┐
        │                          │
        ▼                          ▼
┌──────────────────┐    ┌──────────────────┐
│ Signal Analysis  │    │ Feature Store    │
│ (baseline,       │    │ (export, labels, │
│  z-score,        │    │  dataset)        │
│  anomaly detect) │    │                  │
└────────┬─────────┘    └──────────────────┘
         │
         │ Kafka: anomaly-detected
         ▼
┌──────────────────┐
│ Risk Scoring     │
│ (6 factors +     │
│  edge events)    │
└────────┬─────────┘
         │
         │ Kafka: risk-assessed
         ▼
┌──────────────────┐     ┌──────────────────┐
│ Alerting Service │────▶│ Maintenance +    │
│                  │     │ Notification     │
└──────────────────┘     └──────────────────┘
```

### Nouveau payload features (remplace telemetry measurements)

```json
{
  "messageId": "uuid-v4",
  "deviceId": "dev-xxx",
  "tenantId": "tenant-xxx",
  "timestamp": "2026-09-15T14:00:05Z",
  "payloadType": "FEATURES",
  "edgeVersion": "1.0.3",
  "bufferDrain": false,
  "features": {
    "current": {
      "rms_L1": 14.82,
      "rms_L2": 15.01,
      "rms_L3": 14.95,
      "peak_L1": 21.1,
      "crest_factor": 1.42,
      "zero_crossing_rate": 100.0
    },
    "power": {
      "active_W": 3456.7,
      "reactive_VAR": 892.3,
      "power_factor": 0.97
    },
    "temperature": {
      "connection_1": 38.2,
      "connection_2": 36.8,
      "ambient": 24.1,
      "delta_T_1": 14.1,
      "rate_of_change_1": 0.3
    },
    "harmonics": {
      "thd_percent": 8.2,
      "h3": 5.1,
      "h5": 3.2,
      "h7": 1.8,
      "h9": 0.9
    },
    "arc_detection": {
      "energy_50_100kHz": 0.0012,
      "transient_count_1s": 0,
      "max_amplitude": 0.0
    }
  }
}
```

---

## 8. Protocole MQTT definitif

### Migration MQTT 3.1.1 → MQTT 5.0

| Feature MQTT 5.0 | Usage PyroSense | Benefice |
|-------------------|-----------------|----------|
| User Properties | `cert-fingerprint`, `fw-version`, `edge-version` | Metadata sans modifier payload |
| Reason Codes | Diagnostic deconnexion (rate limit, ACL, quota) | Meilleur troubleshooting |
| Topic Aliases | Reduction overhead par message (topic → alias int) | Economie bande passante |
| Flow Control (receive max) | Limiter inflight messages par device | Prevention surcharge broker |
| Shared Subscriptions | `$share/ingestion-group/pyrosense/+/+/telemetry/#` | Load balancing consumers |
| Session Expiry | 24h (aligne avec buffer local) | Reconnexion propre |
| Message Expiry | 48h (features), never (events CRITICAL) | Purge donnees obsoletes |

### Topics definitifs (MVP 3)

| Topic | Direction | Payload | QoS | Retain | Freq |
|-------|-----------|---------|:---:|:------:|------|
| `pyrosense/{tid}/{did}/features/periodic` | Device → Cloud | CBOR features 1s | 1 | No | 1/s ou 1/5s |
| `pyrosense/{tid}/{did}/features/stats` | Device → Cloud | CBOR stats glissantes | 1 | No | 1/60s |
| `pyrosense/{tid}/{did}/events/electrical` | Device → Cloud | JSON event (arc, surge) | 1 | No | Event-driven |
| `pyrosense/{tid}/{did}/status/heartbeat` | Device → Cloud | CBOR heartbeat compact | 0 | Yes | 1/60s |
| `pyrosense/{tid}/{did}/status/health` | Device → Cloud | JSON health report | 1 | Yes | 1/5min |
| `pyrosense/{tid}/{did}/command/config` | Cloud → Device | JSON config update | 2 | No | On-demand |
| `pyrosense/{tid}/{did}/command/firmware` | Cloud → Device | JSON OTA command | 2 | No | On-demand |
| `pyrosense/{tid}/{did}/command/calibrate` | Cloud → Device | JSON calibration params | 2 | No | On-demand |
| `pyrosense/{tid}/broadcast/config` | Cloud → All devices tenant | JSON tenant config | 2 | No | Rare |

### Payload encoding

| Type de message | Encoding | Raison |
|-----------------|----------|--------|
| Features periodiques | CBOR | Compact (~40% vs JSON), haute frequence |
| Events electriques | JSON | Lisibilite debug, basse frequence |
| Heartbeat | CBOR | Ultra-compact (< 50 bytes) |
| Health report | JSON | Lisibilite, basse frequence |
| Commands | JSON | Lisibilite, basse frequence |

### CBOR features payload (exemple hexadecimal)

```
Taille estimee CBOR features/periodic: ~180 bytes
Taille equivalente JSON: ~450 bytes
Compression: 60%

A 1 feature/seconde, 10 devices:
- CBOR: 10 × 180 × 86400 = ~149 Mo/jour
- JSON: 10 × 450 × 86400 = ~373 Mo/jour
```

### ACL rules (EMQX)

```
# Device ACL (per device, generated at provisioning)
{allow, {client, "pyrosense-${deviceId}"}, publish, [
    "pyrosense/${tenantId}/${deviceId}/features/#",
    "pyrosense/${tenantId}/${deviceId}/events/#",
    "pyrosense/${tenantId}/${deviceId}/status/#"
]}.

{allow, {client, "pyrosense-${deviceId}"}, subscribe, [
    "pyrosense/${tenantId}/${deviceId}/command/#",
    "pyrosense/${tenantId}/broadcast/#"
]}.

{deny, all}.

# Ingestion service ACL (shared subscription)
{allow, {client, "ingestion-service-*"}, subscribe, [
    "$share/ingestion/pyrosense/+/+/features/#",
    "$share/ingestion/pyrosense/+/+/events/#",
    "$share/ingestion/pyrosense/+/+/status/#"
]}.
```

### QoS et retry definitifs

| Scenario | QoS | Retry device | Retry broker | Fallback |
|----------|:---:|:------------:|:------------:|----------|
| Feature periodique | 1 | 3× backoff 1/2/4s | N/A | Buffer SPIFFS |
| Event electrique | 1 | 5× backoff 1/2/4/8/16s | N/A | Buffer SPIFFS (priorite haute) |
| Heartbeat | 0 | Aucun | N/A | Perte acceptee (detection = absence) |
| Config command | 2 | N/A | 5× backoff 2/4/8/16/32s | Mark FAILED, notify operator |
| Firmware OTA | 2 | N/A | 3× backoff 10/30/60s | Mark FAILED, retry manuellement |

---

## 9. Protocole d'enrolement device

### Vue d'ensemble du lifecycle

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  MANUFACTURED │────▶│  REGISTERED  │────▶│ PROVISIONED  │────▶│   ACTIVE     │
│  (usine/labo) │     │  (platform)  │     │ (certificat) │     │  (terrain)   │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
```

### Etape 1 : Manufacturing (usine ou assemblage labo)

| Action | Responsable | Artefact produit |
|--------|-------------|-----------------|
| Assemblage PCB + boitier | Technicien | Module physique |
| Flash firmware factory | Technicien | Partition factory ecrite |
| Generation keypair ATECC608B | Firmware (premier boot) | Cle privee locked dans ATECC608B |
| Export public key + chip serial | Firmware (premier boot) | Fichier `device_identity.json` |
| Etiquetage QR code (serial) | Technicien | QR sur boitier |
| Test fonctionnel automatise | Banc de test | Rapport PASS/FAIL |

### Etape 2 : Registration (platform — technicien IT)

| Action | Interface | Validation |
|--------|-----------|-----------|
| Scan QR code (ou saisie serial) | App mobile / web admin | Serial existe dans manifest usine |
| Association tenant + batiment + tableau | Formulaire web | Tenant existe, batiment existe |
| Upload public key du device | API REST `POST /devices/{id}/register` | Key format EC P-256 valide |
| Assignation circuit(s) monitore(s) | Formulaire web | Circuit non deja assigne |
| Generation CSR template | Platform | CSR pre-rempli avec CN=deviceId, O=tenantId |

### Etape 3 : Provisioning (installation terrain — electricien + technicien IT)

```
┌────────────────┐                    ┌────────────────┐                 ┌──────────────┐
│    DEVICE      │                    │   PLATFORM     │                 │   CA (PKI)   │
│   (ESP32)      │                    │  (Device Svc)  │                 │              │
└───────┬────────┘                    └───────┬────────┘                 └──────┬───────┘
        │                                      │                                 │
        │ 1. Boot en mode PROVISIONING         │                                 │
        │    (LED clignote bleu)               │                                 │
        │                                      │                                 │
        │ 2. Connexion WiFi provisioning       │                                 │
        │    (AP temporaire ou WPS)            │                                 │
        │                                      │                                 │
        │ 3. Generate CSR (ATECC608B)          │                                 │
        │────────── POST /devices/{id}/csr ────▶                                 │
        │           (CSR signed by device key) │                                 │
        │                                      │ 4. Validate CSR                  │
        │                                      │    (serial match,                │
        │                                      │     tenant match,                │
        │                                      │     key match registered)        │
        │                                      │                                  │
        │                                      │──── Sign CSR ────────────────────▶
        │                                      │                                  │
        │                                      │◀─── Certificate (X.509) ─────────│
        │                                      │     (validity: 2 years)          │
        │                                      │                                  │
        │◀───── Certificate + CA chain ────────│                                 │
        │       + broker endpoint              │                                 │
        │       + MQTT config                  │                                 │
        │                                      │                                 │
        │ 5. Store cert in ATECC608B           │                                 │
        │                                      │                                 │
        │ 6. Connect to MQTT broker            │                                 │
        │    (mTLS with new certificate)       │                                 │
        │                                      │                                 │
        │ 7. Publish first heartbeat           │                                 │
        │──────── heartbeat ──────────────────▶│                                 │
        │                                      │                                 │
        │                                      │ 8. Mark device PROVISIONED      │
        │◀───── ACK (device state OK) ────────│                                 │
        │                                      │                                 │
        │ 9. LED vert fixe = OK                │                                 │
```

### Etape 4 : Activation (automatique apres validation terrain)

| Critere | Verification | Auto/Manuel |
|---------|-------------|:-----------:|
| 10 heartbeats consecutifs recus | Platform counter | Auto |
| Premier feature payload valide | Ingestion service validates | Auto |
| Temperature dans plage attendue | Feature: T° ambient 10-50°C | Auto |
| Courant non-nul (circuit actif) | Feature: RMS > 0.1A | Auto |
| Photo installation uploadee | Technicien via app | Manuel |
| Circuit label confirme | Technicien via app | Manuel |

Apres validation : device passe en state `LEARNING` (7 jours, baseline construction, pas d'alertes).

### Rotation certificat

| Parametre | Valeur |
|-----------|--------|
| Validite certificat | 2 ans |
| Renouvellement automatique | 60 jours avant expiration |
| Grace period (ancien + nouveau valide) | 7 jours |
| Revocation | CRL publiee sur S3 + OCSP endpoint |
| Revocation immediate | Commande admin `POST /devices/{id}/revoke` |

### Securite enrollment

| Menace | Mitigation |
|--------|-----------|
| Faux device s'enrole | Public key must match celle registree a l'usine |
| Replay CSR | Nonce inclus dans CSR challenge |
| MITM pendant provisioning | Canal HTTPS pour CSR upload, certificat platform verifie |
| Extraction cle privee device | ATECC608B: cle non-exportable, secure element hardware |
| Enrollment sans autorisation | Device doit etre `REGISTERED` dans platform avant CSR accepte |

---

## 10. Strategie de securite IoT

### Defence in depth (couches)

```
┌───────────────────────────────────────────────────────────────┐
│ Couche 1: PHYSIQUE                                             │
│ - Boitier scelle (vis inviolables)                            │
│ - Tamper switch (detection ouverture)                         │
│ - Effacement cles si tamper detecte                           │
└───────────────────────────────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────────────────────────────┐
│ Couche 2: HARDWARE CRYPTO                                      │
│ - ATECC608B: cle privee non-exportable                        │
│ - Secure boot: seul firmware signe demarre                    │
│ - Flash encryption: SPIFFS chiffre AES-256                    │
└───────────────────────────────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────────────────────────────┐
│ Couche 3: FIRMWARE                                             │
│ - Signature ECDSA P-256 de chaque release                     │
│ - Anti-downgrade (monotonic counter in eFuse)                 │
│ - Watchdog hardware (reboot si freeze)                        │
│ - Stack canaries + heap overflow detection                    │
└───────────────────────────────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────────────────────────────┐
│ Couche 4: COMMUNICATION                                        │
│ - TLS 1.3 obligatoire (pas de fallback)                       │
│ - mTLS (certificat client X.509)                              │
│ - Certificate pinning (CA root seulement)                     │
│ - MQTT 5.0 avec ACL strictes                                  │
└───────────────────────────────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────────────────────────────┐
│ Couche 5: CLOUD                                                │
│ - Broker dans VPC prive (pas d'accès Internet direct)         │
│ - Rate limiting par device (1 msg/s features, 10 msg/s events)│
│ - Anomaly detection connexion (geo, horaire, volume)          │
│ - Revocation immediate (CRL + disconnect force)               │
└───────────────────────────────────────────────────────────────┘
```

### Matrice des menaces IoT

| Menace | Vecteur | Couche defense | Implementation |
|--------|---------|:-------------:|----------------|
| Firmware malveillant | Flash physique | 2+3 | Secure boot + ECDSA verify |
| Extraction credentials | Dump flash | 2 | ATECC608B (non-exportable) + flash encryption |
| Usurpation device | MQTT avec faux ID | 4 | mTLS X.509 (cert = identite) |
| Injection donnees | Publish faux payloads | 4+5 | ACL + schema validation + rate limit |
| Man-in-the-middle | WiFi sniffing | 4 | TLS 1.3 + certificate pinning |
| Denial of Service | Flood MQTT | 5 | Rate limit + throttle + ban IP |
| Downgrade firmware | OTA avec vieux binaire | 3 | eFuse monotonic counter |
| Physical tampering | Ouverture boitier | 1 | Tamper switch + key zeroize |
| Supply chain | Composant compromis | 2 | Sourcing certifie + attestation ATECC608B |
| Replay attack | Rejeu messages anciens | 4+5 | Timestamp + messageId dedup (24h TTL) |

### Politique de gestion des cles

| Type de cle | Stockage | Rotation | Revocation |
|-------------|----------|:--------:|-----------|
| Device private key (ECDSA P-256) | ATECC608B slot 0 (non-exportable) | Jamais (liee au hardware) | Revoke certificat |
| Device certificate (X.509) | ATECC608B slot 1 | 2 ans (auto-renew 60j avant) | CRL + OCSP |
| Platform CA key | HSM cloud (AWS KMS / GCP KMS) | 5 ans | Nouvelle CA + re-sign tous devices |
| Firmware signing key | HSM cloud | 3 ans | Nouvelle cle + eFuse update |
| MQTT broker cert (server) | K8s secret (cert-manager) | 90 jours (Let's Encrypt) | Auto-renew |
| WiFi credentials (provisioning) | NVS encrypted | A chaque reset | N/A |

### Incident response IoT

| Scenario | Detection | Action automatique | Action manuelle |
|----------|-----------|-------------------|-----------------|
| Device compromis suspecte | Payload anomal (schema, volume, timing) | Rate limit → suspend → alert | Investigate, revoke si confirme |
| Certificat vole | Connexion simultanee 2 clients meme cert | Disconnect les 2, alert CRITICAL | Revoke cert, re-provision device |
| Firmware corrompu | Watchdog reboot loop (> 3 en 10min) | Rollback partition factory | Flash manuel si factory echoue |
| Tamper detecte | GPIO tamper switch interrupt | Zeroize keys, publish TAMPER event, stop | Remplacement device |
| DDoS MQTT | Rate limit depasse, latence broker > 5s | Drop excess, alert ops | Scale broker, ban source |

---

## 11. Strategie de collecte terrain

### Phases de collecte

| Phase | Duree | Mode | Donnees collectees | Alertes |
|-------|:-----:|------|-------------------|:-------:|
| **Installation** | Jour 0 | Manuel | Photo, labels, metadata installation | Non |
| **Baseline (learning)** | Jours 1-30 | Automatique | Features 1/s, pas d'evenements | Non |
| **Observation** | Jours 31-45 | Automatique | Features + events | Internes seulement (pas de notif) |
| **Actif** | Jours 46+ | Automatique | Features + events + alertes | Oui (electricien + gestionnaire) |

### Granularite de collecte

| Donnee | Frequence acquisition | Frequence remontee cloud | Retention raw | Retention agregee |
|--------|:--------------------:|:-----------------------:|:-------------:|:-----------------:|
| Courant RMS (3 phases) | 1 Hz (features edge) | 1/5s (config) | 90 jours | Permanent (1min avg) |
| Temperature | 0.2 Hz | 1/5s | 90 jours | Permanent (5min avg) |
| THD + harmoniques | 0.1 Hz | 1/60s | 90 jours | Permanent (10min avg) |
| Arc energy band | 1 Hz | 1/5s | 90 jours | Permanent (1min max) |
| Puissance (P, Q, PF) | 1 Hz | 1/60s | 30 jours | Permanent (15min avg) |
| Events electriques | Event-driven | Immediat | Permanent | N/A |
| Device health | 1/5min | 1/5min | 30 jours | Permanent (1h avg) |

### Volume de donnees estime (10 capteurs)

| Donnee | Taille/msg | Msgs/jour/capteur | Volume/jour (10 cap) | Volume/mois |
|--------|:----------:|:-----------------:|:-------------------:|:-----------:|
| Features periodic (1/5s) | 180 B (CBOR) | 17 280 | 31 Mo | 933 Mo |
| Features stats (1/60s) | 300 B | 1 440 | 4.3 Mo | 129 Mo |
| Events electriques | 500 B (JSON) | ~50 (estime) | 0.25 Mo | 7.5 Mo |
| Heartbeat (1/60s) | 50 B (CBOR) | 1 440 | 0.72 Mo | 21.6 Mo |
| Health (1/5min) | 400 B | 288 | 1.15 Mo | 34.5 Mo |
| **TOTAL** | | | **~37 Mo/jour** | **~1.1 Go/mois** |

### Strategie de retention (TimescaleDB)

```sql
-- Politique de compression (apres 7 jours)
SELECT add_compression_policy('telemetry_features', INTERVAL '7 days');

-- Politique de retention (raw = 90 jours)
SELECT add_retention_policy('telemetry_features', INTERVAL '90 days');

-- Continuous aggregate (permanent, 1 minute)
CREATE MATERIALIZED VIEW telemetry_1min
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 minute', timestamp) AS bucket,
    device_id,
    avg(current_rms_l1) AS avg_current_l1,
    max(current_rms_l1) AS max_current_l1,
    avg(temperature_1) AS avg_temp_1,
    max(temperature_1) AS max_temp_1,
    avg(thd_percent) AS avg_thd,
    max(arc_energy) AS max_arc_energy
FROM telemetry_features
GROUP BY bucket, device_id;
```

### Metadata contextuelles a collecter (par site)

| Categorie | Champs | Source | Quand |
|-----------|--------|--------|-------|
| Batiment | Type, age, surface, nb etages, usage | Gestionnaire (formulaire) | Installation |
| Installation electrique | Age, type tableau, marque, dernier controle | Electricien (formulaire) | Installation |
| Circuit monitore | Section cable, longueur, protection amont, charge nominale | Electricien (formulaire) | Installation |
| Equipements aval | Type (moteur, CVC, eclairage, IT), puissance nominale | Electricien (formulaire) | Installation |
| Conditions ambiantes | T° exterieure (API meteo), occupation | Automatique + gestionnaire | Continu |
| Interventions | Date, type, resultat, electricien | App (workflow intervention) | Chaque intervention |

---

## 12. Strategie de validation qualite des donnees

### Validation multicouche

```
COUCHE 1: EDGE (firmware)          COUCHE 2: INGESTION (cloud)         COUCHE 3: ANALYSIS (cloud)
─────────────────────────          ─────────────────────────           ─────────────────────────
• Range check ADC                  • Schema CBOR/JSON valid            • Coherence temporelle
• Saturation detection             • Timestamp plausible (±5min)       • Baseline deviation plausible
• Self-test capteur (zero)         • Device ID connu + ACTIVE          • Cross-device correlation
• NaN / Inf rejection              • Tenant match certificate          • Drift detection (lent)
• Checksum buffer local            • Deduplication (messageId)         • Isolation capteur defaillant
                                   • Rate limit (anti-flood)
                                   • Feature range [min, max]
```

### Regles de validation par feature

| Feature | Min | Max | Alerte qualite si | Action |
|---------|:---:|:---:|-------------------|--------|
| current_rms (A) | 0.0 | 200.0 | > 100A sur CT 30A | Flag `SENSOR_SATURATED` |
| temperature (°C) | -20.0 | 120.0 | > 85°C | Flag `EXTREME_VALUE` + alert |
| thd_percent (%) | 0.0 | 100.0 | > 50% | Flag `HIGH_THD` |
| power_factor | 0.0 | 1.0 | < 0.5 prolonge | Flag `POWER_QUALITY_ISSUE` |
| arc_energy | 0.0 | 1.0 | — | No quality flag (detection input) |
| crest_factor | 1.0 | 10.0 | > 5.0 | Flag `WAVEFORM_DISTORTION` |

### Detection capteur defaillant

| Symptome | Detection | Classification | Action |
|----------|-----------|:-------------:|--------|
| Valeur constante > 1h | Variance = 0 sur fenetre 1h | `SENSOR_STUCK` | Alert maintenance, exclure du scoring |
| Derive lente unidirectionnelle | Regression lineaire pente > threshold | `SENSOR_DRIFT` | Alert maintenance, compensation |
| Bruit excessif | Variance > 10× baseline | `SENSOR_NOISY` | Alert maintenance, filtrage renforce |
| Valeur aberrante isolee | |z-score| > 10 (point unique) | `OUTLIER` | Rejeter point, pas d'alerte |
| Deconnexion capteur | Valeur = 0 alors que circuit actif | `SENSOR_DISCONNECTED` | Alert immediate |
| Incoherence cross-canal | T° monte mais courant stable | `CROSS_CHECK_FAIL` | Investiguer (peut etre reel) |

### Scoring de qualite par device

Chaque device a un `dataQualityScore` (0-100) mis a jour en continu :

```
DataQualityScore = 100
  - (outliers_24h × 2)           # -2 par outlier
  - (stuck_hours × 10)           # -10 par heure de valeur constante
  - (drift_detected × 20)       # -20 si derive detectee
  - (disconnection_events × 5)  # -5 par deconnexion capteur
  - (schema_errors_24h × 3)     # -3 par erreur de schema
```

| Score | Classification | Impact sur scoring risque |
|:-----:|:-------------:|--------------------------|
| 80-100 | EXCELLENT | Scoring normal |
| 60-79 | DEGRADED | Scoring + bandeau "qualite reduite" |
| 40-59 | POOR | Alertes supprimees, notification maintenance |
| 0-39 | FAILED | Device exclu du scoring, intervention requise |

### Calibration croisee

| Methode | Quand | Objectif |
|---------|-------|----------|
| Zero-current check | Installation (circuit off) | Offset ADC calibration |
| Known-load test | Installation (charge connue) | Gain CT calibration |
| Inter-device comparison | Continu (si 2+ devices meme tableau) | Detect derive relative |
| Reference mensuelle | Electricien (1×/mois pilote) | Pince ampero reference vs capteur |
| Temperature cross-check | Continu | PT100 #1 vs #2 (max delta 3°C) |

---

## 13. Plan de test laboratoire

### Objectifs du test labo

1. Valider le hardware (assemblage, robustesse, precision)
2. Valider le firmware (features, communication, buffer, OTA)
3. Valider l'integration end-to-end (device → cloud → alerte)
4. Calibrer les seuils de detection sur signaux connus
5. Mesurer les faux positifs sur scenarios normaux
6. Valider le comportement offline/reconnexion

### Environnement de test

| Equipement | Specification | Usage |
|------------|--------------|-------|
| Banc electrique basse tension | 24V AC, 10A max, 3 phases | **SECURITE : pas de 230V sans electricien** |
| Generateur de signal | AWG 16-bit, 1 MHz | Injection signaux calibres |
| Charge programmable | 0-10A, cos φ reglable | Simulation charges |
| Perturbateur EMI | EN 61000-4-4 burst generator | Test immunite |
| Chambre thermique | -10°C a +70°C | Test temperature |
| Oscilloscope | 4 voies, 200 MHz, 1 GS/s | Validation mesures |
| Multimetre de reference | Keithley 6.5 digits | Calibration reference |
| Pince amperometrique ref | Fluke i400s | Reference courant |

### Scenarios de test

#### Phase Labo 1 : Validation hardware (semaine 1-2)

| # | Test | Entree | Resultat attendu | Critere PASS |
|---|------|--------|-------------------|-------------|
| H1 | Precision courant | 1A, 5A, 10A, 20A, 30A (reference) | RMS correct | Erreur < ±2% |
| H2 | Precision temperature | 20°C, 40°C, 60°C, 80°C (ref) | Lecture correcte | Erreur < ±0.5°C |
| H3 | Linearite CT | Rampe 0-30A | Courbe lineaire | R² > 0.999 |
| H4 | Bruit ADC (zero) | Pas de courant, circuit off | RMS < seuil bruit | < 0.05A |
| H5 | Bande passante | Sweep 50Hz - 150kHz | Reponse plate | -3dB > 100kHz |
| H6 | Consommation | Monitoring alimentation | < 2W nominal | Mesure wattmetre |
| H7 | Temperature interne | Fonctionnement 24h, T° ambiante 40°C | Pas de throttle | CPU < 85°C |
| H8 | Flash endurance | 10000 cycles ecriture SPIFFS | Pas de corruption | Read-back verify |
| H9 | EMI immunite | Burst IEC 61000-4-4, 2kV | Pas de reboot, donnees OK | No data loss |
| H10 | Reset / Watchdog | Kill task acquisition | Reboot < 5s, reprise | Auto-recovery |

#### Phase Labo 2 : Validation firmware (semaine 2-3)

| # | Test | Scenario | Resultat attendu | Critere PASS |
|---|------|----------|-------------------|-------------|
| F1 | Boot time | Power-on → premier heartbeat | < 10s | Timer |
| F2 | Provisioning | CSR → cert → MQTT connect | < 2 min total | Timer |
| F3 | Feature accuracy | Signal connu → features | RMS/THD corrects | Compare oscilloscope |
| F4 | THD measurement | Signal 50Hz + H3(10%) + H5(5%) | THD = 11.18% | Erreur < ±1% |
| F5 | Arc detection | Burst 50kHz, 100µs, 0.5A | Event ARC_DETECTED | Detection < 100ms |
| F6 | Buffer offline | Deconnexion WiFi 1h | 3600 features buffered | Count SPIFFS files |
| F7 | Buffer drain | Reconnexion apres 1h offline | Drain complet < 10min | Count messages broker |
| F8 | OTA update | Push firmware v1.0.1 | Update OK, rollback test | Version check |
| F9 | OTA rollback | Push firmware corrompu | Rollback auto | Boot ancienne version |
| F10 | Concurrent tasks | Full load (acq + comm + health) | Pas de watchdog trigger | 24h stability |
| F11 | Memory leak | 72h continuous operation | Heap stable | Free heap variance < 5% |
| F12 | WiFi reconnect | Coupure AP 5 min | Reconnexion < 30s | Timer |

#### Phase Labo 3 : Integration end-to-end (semaine 3-4)

| # | Test | Scenario | Validation |
|---|------|----------|-----------|
| E1 | Telemetrie → Dashboard | Device actif → verifier dashboard | Valeurs coherentes, latence < 5s |
| E2 | Event → Alerte | Injection arc → alerte generee | Alerte visible dans < 30s |
| E3 | Baseline learning | 7 jours simules (time accelere) | Baseline stable, pas de fausse alerte |
| E4 | Detection surcharge | Rampe courant 100% → 150% In | Alerte OVERCURRENT generee |
| E5 | Detection echauffement | Rampe T° +2°C/min | Alerte TEMPERATURE_RISE |
| E6 | Detection micro-arc | Bursts repetes (pattern realiste) | Score risque monte, alerte si seuil |
| E7 | Faux positif: demarrage moteur | Appel courant 6×In, 3s | Pas de fausse alerte |
| E8 | Faux positif: commutation | On/off charge 10A | Pas de fausse alerte |
| E9 | Multi-device | 3 devices simultanes | Platform gere sans perte |
| E10 | Device offline → online | Coupure 2h → retour | Buffer drain, donnees integres |
| E11 | Intervention workflow | Alerte → electricien confirme defaut | Event feedback, seuil ajuste |
| E12 | Load test | 10 devices simules, 1 reel | Pas de degradation |

#### Phase Labo 4 : Calibration seuils (semaine 4)

| # | Defaut injecte | Signal | Seuil initial | Seuil ajuste |
|---|----------------|--------|:-------------:|:------------:|
| C1 | Connexion lache | Resistance variable 0.1-1Ω en serie | z-score > 3 | A mesurer |
| C2 | Surcharge progressive | Courant +10%/heure | 120% In | A mesurer |
| C3 | Echauffement | Source chaleur sur connexion | ΔT > 15°C/baseline | A mesurer |
| C4 | Micro-arc (simule) | Burst generator 50-100kHz | Energy > threshold | A mesurer |
| C5 | Degradation isolement | THD injection progressive | ΔTHD > 5% vs baseline | A mesurer |
| C6 | Perte de neutre | Desequilibre triphase force | Unbalance > 30% | A mesurer |

### Criteres de sortie laboratoire (gate review)

| Critere | Seuil | Obligatoire |
|---------|:-----:|:-----------:|
| Tests hardware: pass rate | > 95% (H1-H10) | Oui |
| Tests firmware: pass rate | 100% (F1-F12) | Oui |
| Tests integration: pass rate | > 90% (E1-E12) | Oui |
| Precision courant RMS | < ±2% | Oui |
| Precision temperature | < ±0.5°C | Oui |
| Detection arc (recall sur signaux injectes) | > 90% | Oui |
| Faux positifs sur scenarios normaux | < 5% | Oui |
| Stabilite 72h continue | Pas de reboot, pas de leak | Oui |
| Validation par electricien qualifie | Avis ecrit favorable | Oui |
| OTA fonctionnel + rollback | Teste 3× OK | Oui |

### Securite labo

| Regle | Application |
|-------|-------------|
| **Pas de 230V sans electricien habilite B2V** | Banc basse tension (24V) pour tous tests initiaux |
| Tests 230V uniquement en phase finale | Electricien present, procedures de consignation |
| EPI obligatoires pour tests HT | Gants isolants, lunettes, tapis isolant |
| Banc de test equipe d'un differentiel 30mA | Protection des personnes |
| Extincteur classe E accessible | Risque electrique |

---

## 14. Plan pilote terrain

### Criteres de selection des sites

| Critere | Obligatoire | Ideal |
|---------|:-----------:|:-----:|
| Accord ecrit proprietaire/gestionnaire | Oui | — |
| Tableau electrique accessible (pas encastre) | Oui | — |
| Rail DIN avec espace libre (4 modules min) | Oui | — |
| WiFi fiable dans local technique | Oui | Ethernet |
| Pas de travaux prevus (6 mois) | Oui | — |
| Electricien qualifie disponible pour installation | Oui | — |
| Electricien disponible pour validations alertes | Oui | — |
| Batiment > 5 ans (installations vieillissantes) | Non | Oui |
| Mix tertiaire + residentiel | Non | Oui |
| Proprietaire motive / early-adopter | Non | Oui |
| Contrat maintenance electrique existant | Non | Oui |

### Configuration du pilote

| Parametre | Valeur |
|-----------|--------|
| Nombre de sites | 2 batiments |
| Capteurs par site | 5 (1 TGBT + 4 departs principaux) |
| Total capteurs | 10 |
| Duree pilote | 6 mois (dont 1 mois baseline) |
| Phase active (alertes) | 5 mois |
| Electricien assigne | 1 (temps partiel, SLA 4h CRITICAL) |
| Gestionnaire referent | 1 par site |
| Revue hebdomadaire | Lundi matin (15 min, dashboard review) |
| Revue mensuelle | Rapport KPIs + electricien + gestionnaire |

### Chronologie du pilote

```
Semaine   1    2    3    4    5    6    7    8    ...   24
          ├────┼────┼────┼────┼────┼────┼────┼────────┤
          │INSTALL│  BASELINE (30j)  │    ACTIF         │
          │ Site 1│  Learning mode   │    Alertes ON    │
          │ Site 2│  Pas d'alertes   │    Feedback loop │
          │       │  Pas de notifs   │    Rapports      │
          │       │                  │                   │
          │       │  Validation:     │  Revues:          │
          │       │  - heartbeats OK │  - Hebdo (15min) │
          │       │  - features OK   │  - Mensuelle     │
          │       │  - baseline      │  - Trim. (KPIs)  │
          │       │    converge      │                   │
```

### Procedure d'installation terrain (par site)

| Etape | Duree | Responsable | Prerequis |
|-------|:-----:|-------------|-----------|
| 1. Visite prealable (audit tableau) | 1h | Electricien | Accord gestionnaire |
| 2. Planning installation | — | Coordinateur | Disponibilite electricien + IT |
| 3. Preparation materiel (5 modules) | 2h | Technicien | Modules testes en labo |
| 4. Consignation tableau | 10 min | Electricien B2V | Accord exploitant |
| 5. Pose CTs + sondes T° + gateway | 30 min/capteur | Electricien | Consignation |
| 6. Remise sous tension | 5 min | Electricien | Pose terminee |
| 7. Provisioning devices (enrollment) | 5 min/device | Technicien IT | Devices sous tension |
| 8. Validation telemetrie (dashboard) | 10 min | Technicien IT | Provisioning OK |
| 9. Photos + etiquetage circuits | 15 min | Electricien | — |
| 10. Formation gestionnaire (dashboard) | 30 min | Technicien IT | Installation complete |
| **Total par site (5 capteurs)** | **~4h** | | |

### Protocole de validation des alertes (terrain)

| Severity | Delai intervention | Action electricien | Livrable |
|----------|:-----------------:|-------------------|----------|
| CRITICAL | < 4h | Inspection immediate | Diagnostic + photo + resultat dans app |
| WARNING (recurrent, > 3) | < 48h | Inspection programmee | Diagnostic dans app |
| WARNING (isole) | < 7 jours | Observation dans app | Note explicative |
| INFO | Aucune | — | — |

### KPIs pilote a mesurer

| KPI | Source | Cible mois 1 | Cible mois 3 | Cible mois 6 |
|-----|--------|:------------:|:------------:|:------------:|
| Uptime capteurs | Prometheus | > 90% | > 95% | > 97% |
| Perte messages | Compteurs MQTT/Kafka | < 2% | < 1% | < 0.5% |
| Faux positifs | Feedback electricien | < 40% | < 20% | < 15% |
| Alertes traitees dans SLA | Metriques intervention | > 50% | > 80% | > 90% |
| Defauts detectes | Electricien confirme | ≥ 0 | ≥ 1 | ≥ 2 |
| Dashboard usage (gestionnaire) | Analytics | 1×/sem | 3×/sem | 3×/sem |
| Data quality score moyen | Metrique calculee | > 70 | > 80 | > 85 |

### Criteres d'arret du pilote (stop criteria)

| Critere | Seuil | Action |
|---------|:-----:|--------|
| Incident securite (choc electrique, debut incendie) | 1 | **Arret immediat**, investigation |
| Capteur defaillant endommageant installation | 1 | Retrait capteur, investigation |
| Faux positifs > 50% apres ajustement (mois 3+) | Continu | Review algorithmes, possible retrait |
| Gestionnaire demande retrait | 1 site | Retrait du site |
| Aucun defaut detecte en 6 mois ET electricien confirme installation saine | — | Succes (installation saine = pas de defaut a detecter) |

### Go / No-Go pilote terrain

| Question | Reponse requise | Source |
|----------|:--------------:|--------|
| Gate review labo passee ? | Oui | Rapport labo |
| Contrat pilote signe (non-substitution, RGPD) ? | Oui | Juridique |
| Assurance RC Pro pilote ? | Oui | Assureur |
| Electricien identifie et disponible ? | Oui | Planning |
| Sites valides (visite prealable) ? | Oui | Rapport visite |
| Infrastructure cloud deployee et testee ? | Oui | DevOps |
| Firmware v1.0 stable (72h labo) ? | Oui | Rapport labo |
| Formation electricien et gestionnaire planifiee ? | Oui | Calendrier |

---

## 15. Risques

### 15.1 Risques electriques

| # | Risque | Probabilite | Impact | Mitigation |
|---|--------|:-----------:|:------:|-----------|
| RE1 | Choc electrique lors de l'installation | Faible | CRITIQUE (humain) | Electricien B2V+ obligatoire, consignation, EPI |
| RE2 | Court-circuit cause par un capteur defaillant | Tres faible | HAUT | Isolation galvanique CT (non-invasif), fusible capteur |
| RE3 | Echauffement du module capteur | Faible | MOYEN | Dissipation < 2W, test thermique labo, protection thermique interne |
| RE4 | Interference EMI avec equipements sensibles | Moyen | MOYEN | Tests CEM IEC 61000 en labo, filtrage, blindage |
| RE5 | Fausse securite (defaut non detecte) | Moyen | HAUT | Clause non-substitution, maintien inspections periodiques |
| RE6 | Manipulation 230V par personnel non-habilite | N/A | CRITIQUE | **INTERDIT.** Procedure + formation + clause contractuelle |

### 15.2 Risques techniques

| # | Risque | Probabilite | Impact | Mitigation |
|---|--------|:-----------:|:------:|-----------|
| RT1 | EMI perturbe mesures dans tableau | Haute | Donnees inexploitables | Blindage, filtrage analogique, validation labo avec perturbateurs |
| RT2 | Derive capteur non detectee | Moyenne | Fausse securite | Calibration croisee, auto-diagnostic, inspection mensuelle |
| RT3 | WiFi instable locaux techniques | Haute | Pertes de donnees | 4G LTE-M fallback, buffer local 24h |
| RT4 | Baseline instable (saisons, travaux) | Moyenne | Faux positifs | Fenetre glissante 30j, detection changement de regime |
| RT5 | Volume donnees depasse stockage | Moyenne | Arret ingestion | Retention policy, monitoring disk, alertes 80% |
| RT6 | Firmware OTA echoue | Faible | Capteur bricked | A/B partitions, rollback auto, watchdog, flash factory |
| RT7 | Modele ML degrade apres deploy | Moyenne | Plus de FP | Shadow mode 30j obligatoire, rollback auto si F1 < baseline |
| RT8 | Incompatibilite payload firmware ↔ ingestion | Moyenne | Donnees rejetees | Schema versioning, tests integration labo, feature flags |
| RT9 | ESP32 memory corruption (PSRAM instability) | Faible | Crash | ECC PSRAM (N16R8), test stress 72h, watchdog |
| RT10 | Broker EMQX down | Faible | Perte temps reel | 2 noeuds cluster, buffer device 24h, Prometheus alert |

### 15.3 Risques securite

| # | Risque | Probabilite | Impact | Mitigation |
|---|--------|:-----------:|:------:|-----------|
| RS1 | Capteur compromis (firmware malveillant) | Faible | Donnees falsifiees | Secure boot, firmware signing, ATECC608B |
| RS2 | MQTT broker expose sur Internet | Moyenne | Injection donnees | mTLS, broker dans VPC, WAF |
| RS3 | Vol certificat (extraction flash) | Moyenne | Usurpation device | ATECC608B (non-exportable), tamper detection |
| RS4 | Man-in-the-middle WiFi | Moyenne | Interception | TLS 1.3 end-to-end, certificate pinning |
| RS5 | DDoS MQTT | Moyenne | Indisponibilite | Rate limit, auto-scale, ban IP |
| RS6 | Acces physique capteur | Haute | Manipulation | Tamper switch, vis inviolables, alert |
| RS7 | Supply chain (composant backdoor) | Faible | Backdoor hardware | Sourcing certifie, audit ATECC608B attestation |
| RS8 | Lateral movement (capteur → reseau interne) | Faible | Pivot reseau | Isolation VLAN, firewall rules, pas de route vers LAN |

### 15.4 Risques reglementaires

| # | Risque | Probabilite | Impact | Mitigation |
|---|--------|:-----------:|:------:|-----------|
| RR1 | Non-conformite IEC 61439 pour commercialisation | Haute (si pas anticipe) | Interdit de vendre | Design for compliance, pre-assessment labo certifie |
| RR2 | Responsabilite si incident malgre surveillance | Moyenne | Poursuites | Clause non-substitution, RC Pro, avertissement explicite |
| RR3 | RGPD donnees consommation = donnees personnelles | Moyenne | Mise en demeure | Pas de lien direct personne ↔ circuit, DPO designe, consentement |
| RR4 | Directive RED (radio WiFi/BLE) | Haute | Pas de CE | Tests conformite RED des le proto |
| RR5 | Non-conformite CEM (emissions) | Haute | Echec CE | Tests EMC labo accredite en phase proto |
| RR6 | Installation par non-habilite | Faible | Responsabilite | Exigence B2V documentee, checklist, contrat |
| RR7 | Donnees electriques utilisees pour facturation non-autorisee | Faible | Legal | Pas de sous-comptage certifie MID, disclaimer |
| RR8 | Non-respect droit du travail (monitoring employes) | Moyenne | Legal | Anonymisation, pas de suivi individuel, information CSE |

### 15.5 Risques projet / business

| # | Risque | Probabilite | Impact | Mitigation |
|---|--------|:-----------:|:------:|-----------|
| RB1 | Pas de partenaire hardware | Moyenne | Bloque tout | Identifier 3 options (labo univ, startup, freelance) |
| RB2 | Site pilote refuse apres signature | Faible | Retard 2 mois | 2 sites en backup |
| RB3 | Electricien non disponible | Moyenne | Retard validation | 2 electriciens identifies |
| RB4 | Budget hardware depasse | Moyenne | Redesign | Marge 20% dans BOM |
| RB5 | Aucun defaut detecte en pilote (install trop recente) | Haute | ROI non demontrable | Metrique "risque detecte", comparaison inspection manuelle |
| RB6 | Cloud cost overrun (donnees > previsions) | Faible | Budget | Alertes billing, compression agressive, downsampling |

---

## 16. Roadmap detaillee du MVP 3

### Vue timeline (12 mois)

```
Mois     1       2       3       4       5       6       7       8       9      10      11      12
         ├───────┼───────┼───────┼───────┼───────┼───────┼───────┼───────┼───────┼───────┼───────┤
Sprint   │  3.0  │     3.1       │  3.2  │     3.3       │          3.4          │  3.5  │  3.6  │
         │ Cloud │  Hardware +    │ Data  │    Labo       │     Pilote terrain    │  ML   │ Scale │
         │ Infra │  Firmware      │ Pipe  │               │                       │Shadow │       │
         │       │                │       │               │                       │       │       │
Gates:   │       │                │       │   GATE LABO   │    GATE TERRAIN       │       │ GATE  │
         │       │                │       │   (go/no-go)  │    (go/no-go)         │       │ FINAL │
```

### Sprint 3.0 — Infrastructure Cloud (Mois 1, 4 semaines)

| Tache | Responsable | Effort | Dependance | Livrable |
|-------|-------------|:------:|:----------:|----------|
| Choix cloud provider (AWS vs GCP) | CTO | 2j | — | ADR documente |
| Terraform bootstrap (VPC, subnets, IAM) | DevOps | 1 sem | Choix cloud | Infra code |
| Kubernetes cluster (EKS/GKE, 3 nodes) | DevOps | 1 sem | Terraform | Cluster operationnel |
| PostgreSQL manage + TimescaleDB extension | DevOps | 3j | K8s | DB accessible |
| Redis manage | DevOps | 1j | K8s | Cache operationnel |
| Kafka manage (MSK ou Confluent Cloud) | DevOps | 3j | K8s | Topics crees |
| EMQX cluster (2 noeuds, Helm chart) | DevOps | 3j | K8s | Broker MQTT operationnel |
| DNS + TLS wildcard (cert-manager) | DevOps | 2j | K8s | HTTPS actif |
| Helm charts 11 services | DevOps | 1 sem | K8s | Deployement OK |
| CI/CD pipeline (GitHub Actions → K8s) | DevOps | 3j | Helm | Auto-deploy on push |
| Keycloak cloud + realm pyrosense | Backend | 3j | K8s | Auth fonctionnelle |
| Monitoring (Prometheus + Grafana + Loki) | DevOps | 3j | K8s | Dashboards operationnels |
| Backup automatise (DB + config) | DevOps | 2j | DB | Backup + test restore |
| Load test 50 devices simules | Backend | 3j | Deploy | Rapport perf baseline |

**Cout cloud estime Mois 1 :** 500-800 EUR (petit cluster, usage faible)

### Sprint 3.1 — Hardware + Firmware (Mois 2-3, 6 semaines)

| Tache | Responsable | Effort | Dependance | Livrable |
|-------|-------------|:------:|:----------:|----------|
| Selection composants (BOM finalisee) | Hardware eng | 1 sem | — | BOM document |
| Commande composants | Hardware eng | — | BOM | Composants recus (2-3 sem lead) |
| Schema electronique (KiCad) | Hardware eng | 1 sem | BOM | Schematic + review |
| Prototype breadboard (3 exemplaires) | Hardware eng | 1 sem | Composants | Proto fonctionnel |
| PCB design (4 couches, 80×40mm) | Hardware eng | 1 sem | Schema | Gerbers |
| Fabrication PCB | — | 2 sem (lead) | Gerbers | PCBs recues |
| Assemblage 10 modules | Hardware eng | 1 sem | PCB + composants | 10 modules |
| Firmware: acquisition ADC (DMA, 16kHz) | Firmware eng | 1 sem | Proto breadboard | Task ACQUISITION |
| Firmware: feature extraction (RMS, THD, FFT) | Firmware eng | 2 sem | Acquisition | Task PROCESSING |
| Firmware: MQTT 5.0 + TLS + ATECC608B | Firmware eng | 2 sem | Proto | Task COMMUNICATION |
| Firmware: buffer SPIFFS + drain | Firmware eng | 1 sem | Communication | Offline OK |
| Firmware: OTA (A/B + rollback) | Firmware eng | 1 sem | Base firmware | OTA fonctionnel |
| Firmware: state machine + watchdog | Firmware eng | 3j | Base firmware | States + recovery |
| Firmware: provisioning mode | Firmware eng | 3j | ATECC608B | Enrollment fonctionnel |
| Tests unitaires firmware (Unity framework) | Firmware eng | 1 sem | Features | 80% coverage |
| Integration test firmware ↔ ingestion-service | Backend + Firmware | 3j | Firmware stable | Payload valide |

**Cout hardware estime :** 3000-5000 EUR (composants + PCB + outillage)

### Sprint 3.2 — Data Pipeline (Mois 4, 4 semaines)

| Tache | Responsable | Effort | Dependance | Livrable |
|-------|-------------|:------:|:----------:|----------|
| Adapter ingestion-service: CBOR decoder | Backend | 2j | — | CBOR support |
| Adapter ingestion-service: schema features | Backend | 2j | — | Validation OK |
| Adapter ingestion-service: buffer drain mode | Backend | 1j | — | Replay OK |
| Adapter device-service: X.509 provisioning | Backend | 1 sem | — | CSR workflow |
| Adapter device-service: state LEARNING | Backend | 2j | — | Nouveau state |
| Adapter signal-analysis: features input | Backend | 2j | — | Calcul OK |
| Adapter signal-analysis: calibration coefficients | Backend | 3j | — | Per-device calibration |
| Feature store: export Parquet (batch job) | Backend/Data | 1 sem | — | Export S3 |
| Feature store: rolling aggregates (TimescaleDB) | Backend/Data | 3j | — | Materialized views |
| Feedback endpoint enrichi (photo, mesures terrain) | Backend | 3j | — | API mise a jour |
| Tests integration adaptes (Testcontainers) | Backend | 1 sem | Adaptations | Tests passent |
| PKI setup (CA interne, cert-manager, CRL) | DevSecOps | 1 sem | — | CA operationnelle |

### Sprint 3.3 — Laboratoire (Mois 4-5, 4 semaines)

| Tache | Responsable | Effort | Dependance | Livrable |
|-------|-------------|:------:|:----------:|----------|
| Setup banc de test basse tension | Hardware eng | 3j | Equipement | Banc operationnel |
| Tests hardware (H1-H10) | Hardware eng | 1 sem | Banc + modules | Rapport pass/fail |
| Tests firmware (F1-F12) | Firmware eng | 1 sem | Tests hardware OK | Rapport pass/fail |
| Tests integration (E1-E12) | Backend + Firmware | 1 sem | Tests firmware OK | Rapport pass/fail |
| Calibration seuils (C1-C6) | Data + Firmware | 1 sem | Integration OK | Seuils documentes |
| Validation par electricien qualifie | Electricien | 1j | Tests OK | Avis ecrit |
| Rapport de sortie labo | Tous | 2j | Tout | Rapport gate review |
| **GATE REVIEW LABO** | Comite | 1j | Rapport | **GO / NO-GO terrain** |

### Sprint 3.4 — Pilote Terrain (Mois 5-10, 6 mois)

| Tache | Responsable | Effort | Dependance | Livrable |
|-------|-------------|:------:|:----------:|----------|
| Signature contrat pilote (2 sites) | Business/Juridique | — | Gate labo OK | Contrat signe |
| Assurance RC Pro pilote | Juridique | — | Contrat | Police active |
| Visite prealable sites | Electricien | 2×1h | Contrat | Rapport visite |
| Installation site 1 (5 capteurs) | Electricien + IT | 4h | Visite OK | Devices online |
| Installation site 2 (5 capteurs) | Electricien + IT | 4h | Visite OK | Devices online |
| Formation electricien (app + protocole) | IT | 2h | Installation | Electricien autonome |
| Formation gestionnaire (dashboard + rapports) | IT | 1h/site | Installation | Gestionnaire autonome |
| Baseline learning (30 jours) | Automatique | 30j | Installation | Baselines stables |
| **GATE TERRAIN** (activation alertes) | Comite | 1j | Baseline OK | **GO alertes** |
| Monitoring quotidien (alertes, heartbeats) | IT | Continu | Activation | Logs monitoring |
| Revue hebdomadaire (dashboard) | Gestionnaire + IT | 15min/sem | Activation | Notes hebdo |
| Revue mensuelle (KPIs) | Tous | 1h/mois | Activation | Rapport mensuel |
| Rapport pilote 3 mois | Data + Business | 1 sem | 3 mois actifs | Rapport intermediaire |
| Rapport pilote final (6 mois) | Tous | 2 sem | 6 mois | Rapport final + decision |

### Sprint 3.5 — ML Shadow Mode (Mois 9-10, 4 semaines)

| Tache | Responsable | Effort | Dependance | Livrable |
|-------|-------------|:------:|:----------:|----------|
| Export dataset etiquete (Parquet) | Data eng | 3j | 3+ mois de donnees | Dataset sur S3 |
| Feature engineering (rolling stats) | Data scientist | 1 sem | Dataset | Features calculees |
| Train Isolation Forest | Data scientist | 1 sem | Features | Model v1 |
| Train Autoencoder (alternative) | Data scientist | 1 sem | Features | Model v2 |
| Evaluation comparative (stats vs ML) | Data scientist | 3j | Models | Rapport metrics |
| Deploiement shadow mode (predictions sans action) | Backend + Data | 1 sem | Model choisi | Predictions loggees |
| Dashboard ML metrics (precision/recall/F1 vs stats) | Frontend + Backend | 3j | Shadow mode | Dashboard visible |
| Decision promotion (si ML > stats sur 30j) | Comite | 1j | 30j shadow | Decision documentee |

### Sprint 3.6 — Scaling + Bilan (Mois 11-12, 4 semaines)

| Tache | Responsable | Effort | Dependance | Livrable |
|-------|-------------|:------:|:----------:|----------|
| Extension 10 batiments (50 capteurs) si KPIs OK | Tous | 4 sem | Pilote reussi | 50 capteurs online |
| Load test K6 (100 devices simules + 10 reels) | Backend | 1 sem | Extension | Rapport perf |
| PWA mobile (alertes, confirmation electricien) | Frontend | 3 sem | — | PWA deployee |
| Pre-assessment certification (labo certifie) | Business + Hardware | — | Extension reussie | Rapport pre-assessment |
| Rapport final MVP 3 | Tous | 1 sem | Tout | Decision MVP 4 |
| **GATE FINALE MVP 3** | Comite | 1j | Rapport | **GO / NO-GO MVP 4** |

### Budget previsionnel MVP 3 (12 mois)

| Poste | Mois 1-3 | Mois 4-6 | Mois 7-12 | Total |
|-------|:--------:|:--------:|:---------:|:-----:|
| Cloud infrastructure | 1 500 EUR | 2 500 EUR | 5 000 EUR | 9 000 EUR |
| Hardware (composants, PCB, assemblage) | 5 000 EUR | 1 000 EUR | 2 000 EUR | 8 000 EUR |
| Electricien (installation + validations) | — | 2 000 EUR | 2 000 EUR | 4 000 EUR |
| Data scientist (temps partiel) | — | — | 15 000 EUR | 15 000 EUR |
| Certification pre-assessment | — | — | 8 000 EUR | 8 000 EUR |
| Juridique (contrat, RC Pro, RGPD) | 1 000 EUR | 2 000 EUR | 1 000 EUR | 4 000 EUR |
| Equipement labo (banc test, instruments) | 3 000 EUR | — | — | 3 000 EUR |
| **Total** | **10 500 EUR** | **7 500 EUR** | **33 000 EUR** | **51 000 EUR** |

### Chemin critique

```
[Partenaire HW] ─▶ [Composants] ─▶ [Proto] ─▶ [Firmware] ─▶ [Labo] ─▶ [Terrain] ─▶ [ML]
  Sem 1-2          Sem 3-5        Sem 5-7    Sem 5-10     Sem 11-14  Sem 15-40   Sem 36-44

Chemin critique: HARDWARE → FIRMWARE → LABO → TERRAIN
Le cloud (Sprint 3.0) est parallelise et non bloquant.
```

---

## 17. Documents a creer dans /docs

| # | Document | Contenu | Quand |
|---|----------|---------|-------|
| 1 | `docs/mvp3-cadrage.md` | Ce document (cadrage complet) | Maintenant |
| 2 | `docs/hardware-specification.md` | BOM detaillee, schemas, boitier, contraintes thermiques | Sprint 3.1 |
| 3 | `docs/firmware-architecture.md` | Tasks FreeRTOS, state machine, OTA, buffer, API interne | Sprint 3.1 |
| 4 | `docs/edge-processing.md` | Pipeline DSP, FFT, feature extraction, calibration | Sprint 3.1 |
| 5 | `docs/device-enrollment-protocol.md` | CSR workflow, X.509, PKI, rotation, revocation | Sprint 3.2 |
| 6 | `docs/iot-security-strategy.md` | Defence in depth, menaces, cles, incident response | Sprint 3.2 |
| 7 | `docs/mqtt-protocol-v2.md` | MQTT 5.0, topics, CBOR, ACL, QoS, shared subscriptions | Sprint 3.2 |
| 8 | `docs/data-quality-framework.md` | Validation multicouche, scoring qualite, calibration | Sprint 3.2 |
| 9 | `docs/lab-test-plan.md` | Scenarios, criteres, equipement, securite, rapport template | Sprint 3.3 |
| 10 | `docs/lab-test-report.md` | Resultats, mesures, calibration, avis electricien | Sprint 3.3 (fin) |
| 11 | `docs/pilot-deployment-guide.md` | Procedure installation, checklist, formation, contacts | Sprint 3.4 |
| 12 | `docs/pilot-contract-template.md` | Clauses type: non-substitution, RGPD, responsabilite | Sprint 3.4 |
| 13 | `docs/feature-store-design.md` | Export, rolling stats, labels, dataset generation | Sprint 3.5 |
| 14 | `docs/ml-shadow-mode.md` | Architecture shadow, comparaison, criteres promotion | Sprint 3.5 |
| 15 | `docs/pilot-report-template.md` | Template rapport mensuel et trimestriel KPIs | Sprint 3.4 |
| 16 | `docs/mvp3-adr-log.md` | Architecture Decision Records du MVP 3 | Continu |

---

## 18. Mise a jour de TODO.md

### Ajouts proposes a TODO.md

```markdown
### Phase 23 — MVP 3 : Infrastructure Cloud (Sprint 3.0)
- [ ] ADR choix cloud provider (AWS vs GCP)
- [ ] Terraform bootstrap (VPC, subnets, IAM, K8s)
- [ ] PostgreSQL manage + TimescaleDB
- [ ] Redis manage
- [ ] Kafka manage
- [ ] EMQX cluster Helm chart (2 noeuds)
- [ ] DNS + TLS wildcard (cert-manager + Let's Encrypt)
- [ ] Helm charts 11 services
- [ ] CI/CD GitHub Actions → K8s
- [ ] Keycloak realm pyrosense (cloud)
- [ ] Monitoring (Prometheus + Grafana + Loki cloud)
- [ ] Backup automatise + test restore
- [ ] Load test baseline (50 devices simules)

### Phase 24 — MVP 3 : Hardware + Firmware (Sprint 3.1)
- [ ] BOM finalisee et commandee
- [ ] Schema electronique KiCad
- [ ] PCB design 4 couches (80×40mm)
- [ ] Fabrication + assemblage 10 modules
- [ ] Firmware: acquisition ADC DMA 16kHz
- [ ] Firmware: feature extraction (RMS, THD, FFT 1024)
- [ ] Firmware: MQTT 5.0 + TLS 1.3 + ATECC608B
- [ ] Firmware: buffer SPIFFS + drain
- [ ] Firmware: OTA A/B + rollback
- [ ] Firmware: state machine + watchdog
- [ ] Firmware: mode provisioning (CSR)
- [ ] Tests unitaires firmware (Unity framework)
- [ ] Integration test firmware ↔ ingestion-service

### Phase 25 — MVP 3 : Backend Integration (Sprint 3.2)
- [ ] Ingestion-service: decodeur CBOR
- [ ] Ingestion-service: schema features v2
- [ ] Ingestion-service: support buffer drain (old timestamps)
- [ ] Device-service: provisioning X.509 (CSR workflow)
- [ ] Device-service: state LEARNING
- [ ] Device-service: firmware registry
- [ ] Device-service: certificate lifecycle (rotation, CRL)
- [ ] Signal-analysis: input features pre-calculees
- [ ] Signal-analysis: calibration coefficients per device
- [ ] Feature store: export Parquet (batch job S3)
- [ ] Feature store: continuous aggregates TimescaleDB
- [ ] PKI: CA interne + cert-manager + CRL
- [ ] Tests integration adaptes

### Phase 26 — MVP 3 : Laboratoire (Sprint 3.3)
- [ ] Setup banc de test basse tension
- [ ] Tests hardware H1-H10
- [ ] Tests firmware F1-F12
- [ ] Tests integration E1-E12
- [ ] Calibration seuils C1-C6
- [ ] Validation electricien qualifie (avis ecrit)
- [ ] Rapport de sortie labo
- [ ] GATE REVIEW LABO (go/no-go terrain)

### Phase 27 — MVP 3 : Pilote Terrain (Sprint 3.4)
- [ ] Contrat pilote signe (2 sites)
- [ ] Assurance RC Pro pilote
- [ ] Installation site 1 (5 capteurs)
- [ ] Installation site 2 (5 capteurs)
- [ ] Formation electricien + gestionnaires
- [ ] Baseline learning 30 jours
- [ ] GATE TERRAIN (activation alertes)
- [ ] Monitoring quotidien + revues hebdo
- [ ] Rapport pilote 3 mois
- [ ] Rapport pilote final 6 mois

### Phase 28 — MVP 3 : ML Shadow Mode (Sprint 3.5)
- [ ] Export dataset etiquete (Parquet → S3)
- [ ] Feature engineering (rolling stats 1h/6h/24h/7j)
- [ ] Train Isolation Forest
- [ ] Train Autoencoder (alternative)
- [ ] Evaluation comparative (stats vs ML)
- [ ] Deploiement shadow mode
- [ ] Dashboard ML metrics
- [ ] Decision promotion

### Phase 29 — MVP 3 : Scaling (Sprint 3.6)
- [ ] Extension 10 batiments (50 capteurs)
- [ ] Load test K6 (100 devices)
- [ ] PWA mobile (alertes + confirmation)
- [ ] Pre-assessment certification
- [ ] Rapport final MVP 3
- [ ] GATE FINALE (go/no-go MVP 4)
```

---

## Annexe A — Glossaire

| Terme | Definition |
|-------|-----------|
| CT | Current Transformer — capteur de courant non-invasif (pince) |
| THD | Total Harmonic Distortion — taux de distorsion harmonique |
| RMS | Root Mean Square — valeur efficace |
| DIN | Deutsches Institut fur Normung — standard de rail pour tableaux electriques |
| ATECC608B | Microchip secure element — stockage de cles cryptographiques |
| CBOR | Concise Binary Object Representation — format binaire compact (RFC 8949) |
| mTLS | mutual TLS — authentification bidirectionnelle (client + serveur) |
| CSR | Certificate Signing Request — demande de signature de certificat |
| OTA | Over-The-Air — mise a jour firmware a distance |
| SPIFFS | SPI Flash File System — systeme de fichiers pour flash SPI |
| LWT | Last Will and Testament — message MQTT envoye sur deconnexion |
| FFT | Fast Fourier Transform — transformee de Fourier rapide |
| NVS | Non-Volatile Storage — stockage persistant ESP-IDF |
| eFuse | Electronic fuse — fusible programmable une seule fois (anti-downgrade) |
| MTBF | Mean Time Between Failures — temps moyen entre pannes |
| B2V | Habilitation electrique — travaux au voisinage basse tension |

---

## Annexe B — References normatives

| Norme | Objet | Applicabilite MVP 3 |
|-------|-------|:-------------------:|
| IEC 61439 | Ensembles d'appareillage a basse tension | Pre-assessment seulement |
| NF C 15-100 | Installations electriques basse tension | Pre-assessment seulement |
| EN 61000-4-4 | Immunite aux transitoires rapides (burst) | Tests labo |
| EN 55032 | Emissions CEM equipements multimedia | Tests labo |
| RED 2014/53/EU | Directive equipements radioelectriques | Design for compliance |
| IEC 62443 | Securite cybersecurite systemes industriels | Guide securite IoT |
| RGPD | Protection donnees personnelles | Pilote (consentement site) |
| RFC 8949 | CBOR encoding | Implementation payload |
| RFC 8446 | TLS 1.3 | Communication device-cloud |
| OASIS MQTT 5.0 | Protocole messaging IoT | Communication principale |

---

## Annexe C — Contacts et roles

| Role | Responsabilite MVP 3 | Disponibilite requise |
|------|----------------------|-----------------------|
| Tech Lead / Architecte | Architecture, cadrage, review, decisions | Temps plein |
| Ingenieur firmware | ESP32, FreeRTOS, DSP, tests firmware | Temps plein (Mois 2-5) |
| Ingenieur hardware | BOM, PCB, assemblage, validation | Temps partiel (Mois 2-4) |
| DevOps / SRE | Cloud, K8s, CI/CD, monitoring | Temps partiel (Mois 1, puis support) |
| Backend developer | Adaptations services Java | Temps partiel (Mois 4-5) |
| Data scientist | Feature store, ML, evaluation | Temps partiel (Mois 9-12) |
| Electricien qualifie (B2V+) | Installation, validation, feedback | Ponctuel (labo + terrain) |
| Gestionnaire immobilier (pilote) | Feedback usage, acces sites | Ponctuel (hebdo) |
| Juridique | Contrat, RGPD, assurance | Ponctuel (Mois 4-5) |
