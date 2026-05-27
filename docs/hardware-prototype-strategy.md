# PyroSense MVP 3 — Strategie Hardware Prototype

**Date :** 2026-05-27
**Statut :** Cadrage hardware (pre-achat)
**Objectif :** Choisir et documenter le prototype laboratoire pour valider firmware + backend sans danger.

---

## Avertissement Securite

> **REGLE ABSOLUE :** Aucun developpeur non-habilite ne doit manipuler du 230V secteur.
>
> - Le prototype laboratoire fonctionne en basse tension (< 50V AC) ou avec des generateurs de signal isoles.
> - Toute connexion a un tableau electrique reel = electricien qualifie (habilitation B2V minimum).
> - Les tests sur reseau 230V ne commencent qu'apres validation complete du firmware en basse tension.
> - La certification (IEC 61439, NF C 15-100, CE) est hors scope MVP 3 mais anticipee dans le design.

---

## Table des Matieres

1. [Comparaison des 3 options](#1-comparaison-des-3-options)
2. [BOM indicative par option](#2-bom-indicative-par-option)
3. [Avantages et inconvenients](#3-avantages-et-inconvenients)
4. [Risques securite](#4-risques-securite)
5. [Risques mesure](#5-risques-mesure)
6. [Cout estimatif](#6-cout-estimatif)
7. [Complexite](#7-complexite)
8. [Recommandation MVP 3 (Sprint 3.1)](#8-recommandation-mvp-3-sprint-31)
9. [Recommandation MVP 3.1 (Sprint 3.3-3.4)](#9-recommandation-mvp-31-sprint-33-34)
10. [Recommandation pre-MVP 4 (Sprint 3.6+)](#10-recommandation-pre-mvp-4-sprint-36)
11. [Architecture des composants](#11-architecture-des-composants)
12. [Liste des interfaces](#12-liste-des-interfaces)
13. [Hors perimetre](#13-hors-perimetre)
14. [Validation par expert requis](#14-validation-par-expert-requise)

---

## 1. Comparaison des 3 options

### Vue synthetique

| Critere | Option A | Option B | Option C |
|---------|:--------:|:--------:|:--------:|
| **Nom** | Prototype labo basse tension | Prototype non-invasif supervise | Prototype avance pre-certification |
| **Tension de travail** | < 24V AC (generateur) | 230V secteur (electricien) | 230V secteur (electricien) |
| **Danger pour le dev** | Aucun | Aucun (electricien installe) | Aucun (electricien installe) |
| **Valide firmware** | Oui (complet) | Oui (complet) | Oui (complet) |
| **Valide backend** | Oui (complet) | Oui (complet) | Oui (complet) |
| **Donnees realistes** | Non (signal simule) | Oui (reseau reel) | Oui (reseau reel) |
| **Calibration reelle** | Partielle (reference connue) | Oui (vs pince ampero ref) | Oui (vs instrument certifie) |
| **Deploiement terrain** | Non | Oui (pilote limite) | Oui (pilote + pre-certif) |
| **Delai** | 2-3 semaines | 4-6 semaines | 10-14 semaines |
| **Cout (3 exemplaires)** | 150-250 EUR | 400-600 EUR | 2000-4000 EUR |
| **Complexite electronique** | Faible | Moyenne | Haute |
| **Prerequis** | Aucun | Electricien disponible | Electricien + ingenieur PCB |
| **Phase recommandee** | Sprint 3.1 (dev firmware) | Sprint 3.3-3.4 (labo + terrain) | Sprint 3.6 (pre-MVP 4) |

### Positionnement dans le temps

```
Sprint 3.1         Sprint 3.3          Sprint 3.4           Sprint 3.6
(firmware dev)     (labo)              (terrain)            (pre-certif)
     │                │                    │                     │
     ▼                ▼                    ▼                     ▼
┌──────────┐    ┌───────────┐      ┌───────────────┐    ┌──────────────┐
│ Option A │───▶│ Option A+ │─────▶│  Option B     │───▶│  Option C    │
│ Breadboard│    │ + banc BT │      │  Terrain      │    │  PCB + DIN   │
│ + signal  │    │ + ref CT  │      │  (electricien)│    │  Certifiable │
│ gen       │    │            │      │               │    │              │
└──────────┘    └───────────┘      └───────────────┘    └──────────────┘
  Valide:          Valide:             Valide:              Valide:
  - firmware       - precision         - donnees reelles    - form factor
  - protocol       - calibration       - EMI reelle        - isolation
  - backend        - seuils            - WiFi terrain      - CEM
  - offline        - detection         - baseline          - robustesse
```

---

## 2. BOM indicative par option

### Option A — Prototype laboratoire basse tension

| Composant | Reference | Role | Prix (EUR) | Qte |
|-----------|-----------|------|:----------:|:---:|
| MCU | ESP32-S3-DevKitC-1 (N16R8) | Dev board, USB, PSRAM 8Mo | 12.00 | 3 |
| ADC externe | ADS1115 module breakout (I2C) | 16-bit, 860 SPS, 4 channels | 5.00 | 3 |
| Capteur courant | ACS712-20A module | Effet Hall, isolé, basse tension OK | 4.00 | 9 |
| OU CT alternatif | ZMCT103C (5A micro-CT) | Transformateur 1000:1, basse tension | 3.00 | 9 |
| Sonde temperature | DS18B20 (module etanche) | Digital, 1-Wire, ±0.5°C | 3.00 | 6 |
| OU sonde T° (precision) | MAX31865 breakout + PT100 | RTD, ±0.15°C, SPI | 15.00 | 6 |
| Generateur de signal | DDS AD9833 module | Sinusoide 0-12 MHz, signal calibre | 8.00 | 1 |
| OU charge simulee | Resistances puissance 10W + relais | Charges commutables 1-10A @ 24V | 15.00 | 1 |
| Alimentation labo | Transformateur 230V→24V AC 50VA | Basse tension isolee | 20.00 | 1 |
| Alimentation ESP32 | USB-C (PC ou chargeur 5V) | Dev board alimente par USB | 0.00 | — |
| Breadboard + cables | Grande breadboard + jumpers | Prototypage rapide | 15.00 | 1 |
| Oscilloscope USB (optionnel) | Hantek 6022BE ou similaire | Verification signaux | 60.00 | 1 |
| Multimetre | Uni-T UT61E+ (True RMS) | Reference mesures | 45.00 | 1 |
| **TOTAL Option A** | | | **~150-250 EUR** | |

### Option B — Prototype non-invasif supervise

| Composant | Reference | Role | Prix (EUR) | Qte |
|-----------|-----------|------|:----------:|:---:|
| MCU | ESP32-S3-DevKitC-1 (N16R8) | Dev board | 12.00 | 3 |
| ADC externe | ADS1115 breakout (I2C) | 16-bit, 4 channels | 5.00 | 3 |
| CT split-core (30A) | YHDC SCT-013-030 | Non-invasif, burden 62Ω integre | 8.00 | 9 |
| Burden resistor | 33Ω 1% (si CT sans burden integre) | Conversion courant → tension | 0.50 | 9 |
| Module tension isole | ZMPT101B | Transformateur tension 220V→signal | 4.00 | 3 |
| Sonde T° (precision) | MAX31865 breakout + PT100 classe A | ±0.15°C, SPI | 15.00 | 6 |
| Crypto element | ATECC608B breakout (Adafruit) | Stockage cles, ECDSA | 8.00 | 3 |
| Flash externe | W25Q128 module SPI (16 Mo) | Buffer offline | 3.00 | 3 |
| Alimentation DIN | HLK-PM01 (5V/3W) | Alimentation depuis rail | 5.00 | 3 |
| Regulateur 3.3V | AMS1117-3.3 module | Pour ESP32 | 1.00 | 3 |
| Boitier prototype | Boitier plastique 100×68×50mm | Protection temporaire | 5.00 | 3 |
| Connecteurs | Bornier a vis 5.08mm (lot 10) | Raccordement capteurs | 5.00 | 1 |
| Pince ampero reference | Uni-T UT210E (True RMS clamp) | Calibration | 50.00 | 1 |
| **TOTAL Option B** | | | **~400-600 EUR** | |

### Option C — Prototype avance pre-certification

| Composant | Reference | Role | Prix (EUR) | Qte |
|-----------|-----------|------|:----------:|:---:|
| MCU | ESP32-S3-WROOM-1 (N16R8) module | Production module (pas dev board) | 4.50 | 10 |
| ADC externe | ADS1115IDGSR (MSOP-10) | CMS, meme perf que breakout | 3.50 | 10 |
| CT split-core (30A) | YHDC SCT-013-030 | Non-invasif | 8.00 | 30 |
| MAX31865 (CMS) | MAX31865ATP+ (TQFN) | RTD interface, production | 6.00 | 20 |
| PT100 classe A | Heraeus M-FK 422 (film mince) | Industriel, compact | 8.00 | 20 |
| ATECC608B | ATECC608B-MAHDA-S (UDFN) | Crypto, production | 1.50 | 10 |
| Flash | W25Q128JVSIQ (SOIC-8) | 16 Mo SPI NOR | 2.00 | 10 |
| Alimentation isolee | HLK-PM01 + filtrage CEM | 5V/3W + filtre LC + TVS | 8.00 | 10 |
| Regulateur | TPS63001 (buck-boost, bruit bas) | 3.3V ultra-low noise | 3.00 | 10 |
| Detecteur arc | LM393 + circuit analogique custom | Comparateur + filtre passe-haut | 2.00 | 10 |
| Module 4G (optionnel) | SIM7080G breakout | LTE-M fallback | 18.00 | 3 |
| PCB 4 couches | JLCPCB / PCBWay (80×40mm, FR4) | Production quality | 15.00 | 10 |
| Assemblage SMD | JLCPCB Assembly (ou main) | Soudure CMS | 30.00 | 10 |
| Boitier rail DIN 4M | Phoenix Contact ou Italtronic 4M | IP20, rail standard | 8.00 | 10 |
| Connecteurs industriels | Phoenix Contact MSTB 5.08 | Bornier a vis pro | 4.00 | 10 |
| Antenne WiFi | PCB antenna + U.FL + cable coax | Performant en armoire | 2.00 | 10 |
| Test CEM (pre-assessment) | Labo accredite (EN 55032, EN 61000-4-4) | Pre-screening emissions/immunite | 1500.00 | 1 |
| Certification pre-assessment | Bureau d'etudes (dossier technique) | Gap analysis vs IEC 61439 | 2000.00 | 1 |
| **TOTAL Option C** | | | **~2000-4000 EUR** | |

---

## 3. Avantages et inconvenients

### Option A — Prototype labo basse tension

| Avantages | Inconvenients |
|-----------|---------------|
| **Zero danger electrique** — tout en < 24V | Signal non-representatif du reseau 230V reel |
| Demarrage immediat (composants disponibles en 3-5 jours) | Pas de validation des seuils de detection sur signaux reels |
| Cout tres faible (~200 EUR pour 3 proto) | Pas deployable en terrain |
| Ideal pour deboguer firmware (breakpoints, traces) | EMI non testee (conditions labo propres) |
| Validation complete pipeline MQTT → Kafka → scoring | CT basse tension = pas les memes caracteristiques qu'un CT 30A reel |
| Permet de travailler 100% en autonomie (pas d'electricien) | Calibration non transposable au terrain |
| Breadboard = modifications faciles et rapides | Fragile (fils volants, pas de boitier) |
| Compatible simulateur existant (validation end-to-end) | Ne prouve pas la faisabilite physique reelle |

### Option B — Prototype non-invasif supervise

| Avantages | Inconvenients |
|-----------|---------------|
| Mesures sur reseau reel (donnees representatives) | Necessite un electricien pour installation |
| CT split-core = non-invasif (pas de coupure circuit) | Delai 4-6 semaines (composants + electricien) |
| Calibration possible vs pince ampero reference | Dev board ESP32 fragile (pas industrialise) |
| Validation EMI en conditions reelles | Pas de boitier DIN (prototype encombrant) |
| Deployable en pilote limite (1-2 tableaux) | ATECC608B en breakout (pas miniaturise) |
| Preparation directe du pilote terrain | Alimentation via HLK-PM01 = connexion 230V (electricien) |
| Signal quality scoring validable en conditions reelles | Pas de certification possible (proto non conforme) |
| Feedback loop avec electricien possible | Risque de deconnexion accidentelle (pas de fixation DIN) |

### Option C — Prototype avance pre-certification

| Avantages | Inconvenients |
|-----------|---------------|
| Form factor final (rail DIN 4M, industriel) | Cout eleve (2000-4000 EUR) |
| PCB 4 couches = fiabilite, reproductibilite | Delai long (10-14 semaines, PCB + assemblage + tests) |
| Composants CMS = miniaturise, moins de bruit | Necessite competence PCB design (KiCad/Altium) |
| Tests CEM possibles (pre-assessment) | Modification hardware = nouveau cycle PCB |
| Design for certification (anticipation IEC 61439) | Overkill pour validation firmware initiale |
| Peut servir de base pour produit pilote | Complexite electronique haute |
| 4G fallback integre | Necessite outillage (station soudure, reflow, etc.) |
| Production small-batch possible (10+) | Budget justifie seulement si Option B validee |

---

## 4. Risques securite

### Par option

| Risque | Option A | Option B | Option C |
|--------|:--------:|:--------:|:--------:|
| Choc electrique (dev) | **NUL** (< 24V) | **NUL** (electricien installe) | **NUL** (electricien installe) |
| Choc electrique (electricien) | N/A | FAIBLE (CT non-invasif) | FAIBLE (CT non-invasif) |
| Court-circuit provoque par proto | NUL | TRES FAIBLE (CT passif, pas de connexion galvanique) | TRES FAIBLE (idem + isolation renforcee) |
| Incendie proto (surchauffe) | TRES FAIBLE (basse tension) | FAIBLE (HLK-PM01 sur 230V) | TRES FAIBLE (design thermique valide) |
| Manipulation 230V par non-habilite | **IMPOSSIBLE** (pas de 230V) | Risque si procedures non respectees | Risque si procedures non respectees |
| Donnees sensibles exposees | FAIBLE (labo) | MOYEN (reseau reel → RGPD) | MOYEN (idem) |
| Device compromis → pivot reseau | FAIBLE (labo isole) | MOYEN (WiFi batiment) | FAIBLE (isolation VLAN) |

### Mesures de securite obligatoires

| Mesure | Option A | Option B | Option C |
|--------|:--------:|:--------:|:--------:|
| Transformateur d'isolation | Oui (230→24V) | N/A (CT non-invasif) | N/A |
| Differentiel 30mA sur banc | Recommande | Obligatoire sur tableau | Obligatoire |
| EPI (gants isolants) | Non | Oui (electricien) | Oui (electricien) |
| Consignation avant intervention | Non | Oui | Oui |
| Formation securite electrique | Non requise | B0 minimum pour dev present | B0 minimum |
| Extincteur classe E | Recommande | Obligatoire | Obligatoire |

---

## 5. Risques mesure

### Precision et fiabilite

| Risque | Probabilite | Impact | Options concernees | Mitigation |
|--------|:-----------:|:------:|:------------------:|-----------|
| CT sature (courant > pleine echelle) | Moyenne | Mesures fausses | A, B, C | Detection saturation firmware (sq flag), choix calibre adapte |
| Bruit ADC (EMI tableau electrique) | Haute | THD imprecis | B, C | Filtrage analogique (RC), blindage cable CT, oversampling |
| Derive thermique ADC | Faible | Offset lent | A, B, C | Calibration zero periodique, compensation temperature |
| Aliasing (Fs < 2×Fmax) | Moyenne | Harmoniques fantomes | A, B, C | Filtre anti-aliasing analogique (fc = Fs/2), Fs ≥ 2×Fmax |
| Interference entre canaux ADC (crosstalk) | Faible | Correlation fausse | B, C | Mux settling time, separation physique, ADC differentiel |
| PT100 auto-echauffement | Tres faible | +0.1°C erreur | B, C | Courant excitation faible (MAX31865 = 1mA), mesure pulsee |
| Non-linearite CT a faible courant | Moyenne | Erreur < 0.5A | B, C | Specification CT (linearite 10-100% In), calibration multi-point |
| Phase shift du CT (dephasage) | Moyenne | Erreur puissance active | B, C | CT faible dephasage (< 1°), compensation numerique |
| Longueur cable CT → bruit | Haute | Parasites | B, C | Cable blinde, < 2m, paire torsadee |
| Absence de reference tension (CT-only) | N/A | Pas de PF, pas de P active | A (pas de V), B (ZMPT101B) | Option B inclut sonde tension |

### Precision attendue par option

| Metrique | Option A | Option B | Option C |
|----------|:--------:|:--------:|:--------:|
| Courant RMS (precision) | ±5% (ACS712 = effet Hall) | ±2% (CT YHDC + ADS1115) | ±1% (CT + ADC + calibration) |
| Temperature (precision) | ±0.5°C (DS18B20) | ±0.15°C (PT100 classe A) | ±0.1°C (PT100 + MAX31865 calibre) |
| THD (precision) | ±3% (bruit ADC interne) | ±1% (ADS1115 16-bit) | ±0.5% (ADC + filtre anti-aliasing) |
| Power factor | Non disponible | ±0.03 (ZMPT101B + CT) | ±0.01 (precision isolee) |
| Detection arc (recall) | ~60% (pas de HF reel) | ~80% (signaux reels) | ~90% (circuit dedie + filtrage) |
| Frequence echantillonnage effective | 860 SPS (ADS1115 max) | 860 SPS | 16 kHz (ADC custom pipeline) |

---

## 6. Cout estimatif

### Par lot de prototypes

| | Option A (3 proto) | Option B (3 proto) | Option C (10 proto) |
|--|:------------------:|:------------------:|:-------------------:|
| Composants | 120 EUR | 350 EUR | 1500 EUR |
| PCB / breadboard | 15 EUR (breadboard) | 15 EUR (breadboard) | 150 EUR (JLCPCB) |
| Assemblage | 0 (fait main) | 0 (fait main) | 300 EUR (JLCPCB assembly) |
| Instruments de mesure | 100 EUR (multimetre) | 150 EUR (multimetre + pince) | 150 EUR |
| Equipement labo | 20 EUR (alim 24V) | 50 EUR (alim + connectique) | 50 EUR |
| Tests CEM | 0 | 0 | 1500 EUR |
| Pre-assessment certif | 0 | 0 | 2000 EUR |
| Electricien | 0 | 200 EUR (2h install) | 400 EUR (4h) |
| **TOTAL** | **~255 EUR** | **~765 EUR** | **~6050 EUR** |
| **Cout par prototype** | **~85 EUR** | **~255 EUR** | **~605 EUR** |

### Budget cumule (strategie progressive)

```
Sprint 3.1:  Option A = 255 EUR                    (total: 255 EUR)
Sprint 3.3:  Option B = 765 EUR (reutilise ESP32)  (total: 1020 EUR)
Sprint 3.6:  Option C = 6050 EUR                   (total: 7070 EUR)
                                                    ─────────────────
                                                    Budget total: ~7000 EUR
                                                    (vs 6050 EUR si on saute A→C directement,
                                                     mais avec 2 mois de retard et plus de risque)
```

---

## 7. Complexite

### Matrice de complexite

| Dimension | Option A | Option B | Option C |
|-----------|:--------:|:--------:|:--------:|
| Electronique | ★☆☆☆☆ | ★★☆☆☆ | ★★★★☆ |
| Firmware | ★★★☆☆ | ★★★☆☆ | ★★★★☆ |
| Mecanique | ☆☆☆☆☆ | ★☆☆☆☆ | ★★★☆☆ |
| Securite electrique | ☆☆☆☆☆ | ★★☆☆☆ | ★★★☆☆ |
| Logistique | ★☆☆☆☆ | ★★☆☆☆ | ★★★★☆ |
| Reglementaire | ☆☆☆☆☆ | ★☆☆☆☆ | ★★★☆☆ |
| Competences requises | Dev firmware | Dev firmware + electricien | Dev firmware + EE + electricien + PCB designer |

### Competences necessaires par option

| Competence | Option A | Option B | Option C |
|------------|:--------:|:--------:|:--------:|
| Programmation ESP-IDF / FreeRTOS | Requis | Requis | Requis |
| Electronique analogique (filtrage, ADC) | Basique | Intermediate | Avancee |
| Design PCB (KiCad/Altium) | Non | Non | Requis |
| Securite electrique (habilitation) | Non | B0 (presence) | B0 + B2V (electricien) |
| CEM / EMI (theorie + tests) | Non | Non | Requis ou sous-traite |
| Mecanique (boitier, thermique) | Non | Non | Intermediate |
| Soudure CMS (0402, QFN, MSOP) | Non | Non | Requis ou sous-traite |

---

## 8. Recommandation MVP 3 (Sprint 3.1)

### Choix : Option A — Prototype laboratoire basse tension

**Pourquoi :**
- Permet de demarrer le firmware immediatement (pas de blocage hardware)
- Zero risque electrique pour l'equipe de dev
- Valide 100% du pipeline : acquisition → features → MQTT → ingestion → Kafka → scoring
- Cout negligeable (< 300 EUR)
- Iterations rapides (breadboard = modifications en 5 min)

**Configuration recommandee (3 exemplaires) :**

```
ESP32-S3-DevKitC-1 (N16R8)
    │
    ├── I2C: ADS1115 breakout
    │         ├── CH0: ACS712-20A (simule courant L1)
    │         ├── CH1: ACS712-20A (simule courant L2)
    │         ├── CH2: ACS712-20A (simule courant L3)
    │         └── CH3: signal generateur (simule arc HF)
    │
    ├── SPI: MAX31865 breakout #1 + PT100
    │         (simule temperature connexion)
    │
    ├── SPI: MAX31865 breakout #2 + PT100
    │         (simule temperature ambiante, ou 2eme point)
    │
    ├── GPIO: bouton-poussoir
    │         (simule transitoire/arc pour tests)
    │
    └── USB: alimentation + debug (JTAG/UART)

Source de signal :
    - Generateur de fonction DDS (AD9833) : sinusoide 50Hz + harmoniques
    - OU transformateur 24V AC + charge resistive commutable
    - OU injection directe depuis PC (DAC USB)
```

**Livrables Sprint 3.1 avec Option A :**
- Firmware FreeRTOS complet (4 tasks)
- Pipeline acquisition → features → MQTT fonctionnel
- Buffer SPIFFS operationnel (test offline)
- OTA operationnel (A/B partitions)
- Integration test firmware ↔ ingestion-service (payload v2 valide)
- Benchmark : precision ADC, latence, stabilite 72h

**Limitation acceptee :**
- Les seuils de detection calibres sur Option A ne sont PAS transferables au terrain
- L'EMI n'est pas testee → faux sentiment de precision
- La detection d'arc est simulee (bouton ou signal gen), pas reelle

---

## 9. Recommandation MVP 3.1 (Sprint 3.3-3.4)

### Choix : Option B — Prototype non-invasif supervise

**Pourquoi :**
- Le firmware est valide (Sprint 3.1 termine)
- On a besoin de donnees reelles pour calibrer les seuils
- Le CT split-core YHDC est non-invasif (pas de coupure circuit)
- Cout raisonnable (~800 EUR pour 3 protos)
- Permet le pilote terrain (2 batiments, 5 capteurs/site)

**Configuration recommandee (3+7 exemplaires) :**

```
ESP32-S3-DevKitC-1 (N16R8)
    │
    ├── I2C: ADS1115 breakout
    │         ├── CH0: YHDC SCT-013-030 (L1) + burden 33Ω
    │         ├── CH1: YHDC SCT-013-030 (L2) + burden 33Ω
    │         ├── CH2: YHDC SCT-013-030 (L3) + burden 33Ω
    │         └── CH3: ZMPT101B (tension, si monophase)
    │
    ├── SPI: MAX31865 + PT100 (connexion barre cuivre)
    ├── SPI: MAX31865 + PT100 (ambient ou 2eme connexion)
    │
    ├── I2C: ATECC608B breakout (crypto, X.509)
    ├── SPI: W25Q128 (buffer 16 Mo)
    │
    ├── Alimentation: HLK-PM01 (230V→5V) → AMS1117 (3.3V)
    │   [INSTALLE PAR ELECTRICIEN UNIQUEMENT]
    │
    └── Boitier plastique IP20 (fixation provisoire dans armoire)
```

**Livrables Sprint 3.3-3.4 avec Option B :**
- 10 prototypes assembles et testes en labo (banc 24V d'abord)
- Validation par electricien qualifie (avis ecrit)
- Installation terrain par electricien sur 2 sites pilotes
- Calibration seuils sur signaux reels (30 jours de baseline)
- Premier dataset etiquete (electricien confirme/infirme alertes)

**Upgrade depuis Option A :**
- Reutilise les ESP32-S3-DevKitC du Sprint 3.1
- Remplace ACS712 par CT YHDC (precision superieure)
- Ajoute ATECC608B + W25Q128 (securite + buffer)
- Ajoute HLK-PM01 (alimentation rail DIN)
- Ajoute ZMPT101B (mesure tension pour PF)

---

## 10. Recommandation pre-MVP 4 (Sprint 3.6+)

### Choix : Option C — Prototype avance pre-certification

**Pourquoi :**
- Le pilote terrain a valide l'approche (donnees reelles, detection confirmee)
- Pour passer a 50+ capteurs, il faut un produit fiable et reproductible
- Un PCB dedie elimine les fils volants (cause #1 de pannes terrain)
- Le boitier DIN est obligatoire pour acceptation par les electriciens
- Les tests CEM sont necessaires avant de scaler

**Quand declencher :**
- Gate review Sprint 3.4 PASS (pilote fonctionne depuis 2+ mois)
- Au moins 1 defaut detecte OU taux FP < 20%
- Budget confirme pour 10 unites + tests CEM

**Configuration recommandee (10 exemplaires) :**

```
PCB custom 4 couches (80×40mm)
    │
    ├── ESP32-S3-WROOM-1 (N16R8) — module soude
    │
    ├── ADS1115 (MSOP-10, CMS)
    │   ├── 3× YHDC SCT-013-030 + filtre RC anti-aliasing
    │   └── 1× ZMPT101B (tension)
    │
    ├── 2× MAX31865 (TQFN) + PT100 M-FK 422
    │
    ├── LM393 + circuit analogique (detection HF)
    │   - Filtre passe-haut 10kHz
    │   - Comparateur seuil ajustable (DAC ou trimmer)
    │   - Compteur hardware (PCNT ESP32)
    │
    ├── ATECC608B (UDFN-8)
    ├── W25Q128 (SOIC-8)
    │
    ├── Alimentation isolee
    │   - HLK-PM01 (230V→5V, 3W)
    │   - Filtre EMI (inductance mode commun + capacites Y)
    │   - TVS protection surtension
    │   - TPS63001 (3.3V, low-noise, buck-boost)
    │
    ├── SIM7080G (optionnel, 3 sur 10 unites) — LTE-M fallback
    │
    ├── LED RGB (statut)
    ├── Bouton reset
    ├── Connecteur UFL (antenne WiFi externe)
    │
    └── Boitier Phoenix Contact ou Italtronic 4M DIN
        - Rail DIN standard 35mm
        - IP20
        - Ventilation passive
        - Etiquetage face avant
```

**Livrables Sprint 3.6 avec Option C :**
- 10 PCB assembles et testes
- Design review par ingenieur electronique
- Pre-assessment CEM (emissions + immunite burst)
- Gap analysis IEC 61439 (dossier technique)
- Deploiement 50 capteurs (10 batiments) si go

---

## 11. Architecture des composants

### Diagramme composants (toutes options)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            ARCHITECTURE COMPOSANTS                                │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                 CAPTEURS (passifs, isoles galvaniquement)                 │   │
│  │                                                                          │   │
│  │  ┌───────────────────────────────────────────────────────────────────┐  │   │
│  │  │ COURANT                                                            │  │   │
│  │  │                                                                    │  │   │
│  │  │  Option A: ACS712-20A (effet Hall, alimenté, 0-5V output)         │  │   │
│  │  │  Option B/C: YHDC SCT-013-030 (CT split-core, 30A:1V output)     │  │   │
│  │  │                                                                    │  │   │
│  │  │  Interface: signal analogique 0-1V AC → ADC (via burden + bias)   │  │   │
│  │  │  Isolation: CT = naturellement isole (couplage magnetique)         │  │   │
│  │  │  Conditionnement: burden resistor + DC bias Vref/2 + RC filter    │  │   │
│  │  └───────────────────────────────────────────────────────────────────┘  │   │
│  │                                                                          │   │
│  │  ┌───────────────────────────────────────────────────────────────────┐  │   │
│  │  │ TENSION (Option B/C seulement)                                     │  │   │
│  │  │                                                                    │  │   │
│  │  │  ZMPT101B (transformateur miniature 220V→2mV/V)                   │  │   │
│  │  │  Interface: signal analogique ±1V → ADC (via attenuation + bias)  │  │   │
│  │  │  Isolation: transformateur = isolation galvanique                   │  │   │
│  │  │  Note: necessaire pour calcul power factor et puissance active    │  │   │
│  │  └───────────────────────────────────────────────────────────────────┘  │   │
│  │                                                                          │   │
│  │  ┌───────────────────────────────────────────────────────────────────┐  │   │
│  │  │ TEMPERATURE                                                        │  │   │
│  │  │                                                                    │  │   │
│  │  │  Option A: DS18B20 (digital 1-Wire, ±0.5°C, simple)              │  │   │
│  │  │  Option B/C: MAX31865 + PT100 (RTD, SPI, ±0.15°C, industriel)   │  │   │
│  │  │                                                                    │  │   │
│  │  │  Interface: SPI (MAX31865) ou 1-Wire (DS18B20)                    │  │   │
│  │  │  Placement: contact direct sur barre cuivre / connexion boulonnee│  │   │
│  │  └───────────────────────────────────────────────────────────────────┘  │   │
│  │                                                                          │   │
│  │  ┌───────────────────────────────────────────────────────────────────┐  │   │
│  │  │ DETECTION ARC / TRANSITOIRES                                       │  │   │
│  │  │                                                                    │  │   │
│  │  │  Option A: bouton-poussoir (simulation) + signal gen               │  │   │
│  │  │  Option B: comparateur LM393 sur signal CT (seuil fixe)           │  │   │
│  │  │  Option C: LM393 + filtre passe-haut 10kHz + DAC seuil           │  │   │
│  │  │                                                                    │  │   │
│  │  │  Interface: GPIO interrupt (front montant) + compteur PCNT        │  │   │
│  │  └───────────────────────────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                      NUMERISATION (ADC)                                   │   │
│  │                                                                          │   │
│  │  ADS1115 (16-bit, 860 SPS, 4 channels, PGA, I2C)                       │   │
│  │                                                                          │   │
│  │  Configuration recommandee:                                              │   │
│  │    - Mode: continuous, 860 SPS, gain ±2.048V                            │   │
│  │    - CH0: CT L1 (burden output, biased Vref/2)                          │   │
│  │    - CH1: CT L2                                                          │   │
│  │    - CH2: CT L3                                                          │   │
│  │    - CH3: tension (ZMPT101B) ou arc detector                            │   │
│  │                                                                          │   │
│  │  Limitation: 860 SPS max (= 430 Hz Nyquist)                            │   │
│  │  → Suffisant pour fondamental 50Hz + harmoniques jusqu'a H7 (350Hz)    │   │
│  │  → Insuffisant pour detection HF (arcs 50kHz+)                          │   │
│  │  → Solution: detection HF par comparateur analogique (GPIO ISR)         │   │
│  │                                                                          │   │
│  │  Option C upgrade: ADC rapide (MCP3561, 24-bit, 153.6 kSPS)            │   │
│  │  → Permet FFT jusqu'a 75 kHz (detection arc dans le domaine numerique) │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                      TRAITEMENT (MCU)                                     │   │
│  │                                                                          │   │
│  │  ESP32-S3-WROOM-1 (N16R8)                                               │   │
│  │    - Dual-core Xtensa LX7 @ 240 MHz                                     │   │
│  │    - 512 KB SRAM + 8 MB PSRAM                                           │   │
│  │    - 16 MB Flash (partitions: factory + OTA_0 + OTA_1 + SPIFFS)         │   │
│  │    - WiFi 802.11 b/g/n + BLE 5.0                                       │   │
│  │    - Hardware crypto (AES, SHA, RSA)                                     │   │
│  │    - Secure boot v2 (RSA-3072)                                           │   │
│  │    - Flash encryption (AES-256-XTS)                                      │   │
│  │    - PCNT (pulse counter) pour transitoires                             │   │
│  │    - I2C, SPI, UART, GPIO, ADC interne (12-bit, backup)                │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                      STOCKAGE & SECURITE                                  │   │
│  │                                                                          │   │
│  │  ATECC608B (crypto element):                                             │   │
│  │    - Stockage cle privee ECDSA P-256 (non-exportable)                   │   │
│  │    - Signature/verification hardware                                     │   │
│  │    - Certificat X.509 storage (2 slots)                                 │   │
│  │    - Random number generator (TRNG)                                      │   │
│  │    - Anti-tamper (zeroize on detect)                                     │   │
│  │                                                                          │   │
│  │  W25Q128 (flash externe 16 Mo):                                          │   │
│  │    - Buffer offline (SPIFFS, 4 Mo partition)                             │   │
│  │    - Firmware backup (si flash interne insuffisante)                     │   │
│  │    - Log local (diagnostics, historique events)                          │   │
│  │    - Interface: SPI Quad (80 MHz)                                        │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                      COMMUNICATION                                        │   │
│  │                                                                          │   │
│  │  WiFi (primary):                                                          │   │
│  │    - 802.11 b/g/n (2.4 GHz)                                              │   │
│  │    - WPA2-Enterprise ou WPA2-PSK                                         │   │
│  │    - Antenne PCB ou UFL externe (Option C)                              │   │
│  │    - Portee: 10-30m en interieur (suffisant tableau → routeur)          │   │
│  │                                                                          │   │
│  │  4G LTE-M fallback (Option C, optionnel):                               │   │
│  │    - SIM7080G (LTE-M Cat-M1 + NB-IoT)                                  │   │
│  │    - SIM M2M (forfait data IoT ~2 EUR/mois)                            │   │
│  │    - Active si WiFi down > 5 min                                         │   │
│  │    - Debit: suffisant pour features (< 1 KB/s)                          │   │
│  │                                                                          │   │
│  │  MQTT 5.0 over TLS 1.3:                                                  │   │
│  │    - mTLS (certificat client X.509 dans ATECC608B)                      │   │
│  │    - Broker: EMQX (cloud)                                                │   │
│  │    - Session expiry: 24h (persistence QoS 1)                            │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                      ALIMENTATION                                         │   │
│  │                                                                          │   │
│  │  Option A: USB 5V (dev board) — pas de 230V                             │   │
│  │                                                                          │   │
│  │  Option B/C: HLK-PM01 (AC-DC module, 230V→5V, 3W)                      │   │
│  │    - Isolation: renforcee (3000V AC)                                     │   │
│  │    - Rendement: ~80%                                                     │   │
│  │    - Dissipation: < 0.6W                                                 │   │
│  │    - Protection: TVS + fusible + varistance (Option C)                  │   │
│  │    → AMS1117-3.3 (LDO, 3.3V, 1A max) pour ESP32                       │   │
│  │    → Option C: TPS63001 (buck-boost, low noise, meilleur pour ADC)      │   │
│  │                                                                          │   │
│  │  [INSTALLATION HLK-PM01 = ELECTRICIEN OBLIGATOIRE]                      │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 12. Liste des interfaces

### 12.1 Interface capteur courant

| Parametre | Option A | Option B/C |
|-----------|----------|-----------|
| Type capteur | ACS712-20A (effet Hall) | YHDC SCT-013-030 (CT split-core) |
| Plage mesure | 0-20A AC/DC | 0-30A AC |
| Signal sortie | 0-5V (Vcc/2 ± 100mV/A) | 0-1V AC (1V @ 30A) |
| Isolation | Integree (effet Hall) | Naturelle (couplage magnetique) |
| Conditionnement | Diviseur tension → 0-3.3V pour ADC | Burden 33Ω + DC bias Vdd/2 + RC filtre |
| Interface ADC | ADS1115 CH0-CH2 (single-ended) | ADS1115 CH0-CH2 (single-ended, biased) |
| Precision typique | ±1.5% (datasheet) mais ±5% en pratique | ±1% (avec calibration) |
| Bande passante | DC-80 kHz (-3dB) | 50 Hz - 1 kHz (CT limité, filtre RC) |
| Linearite | Bonne (10-100% FS) | Bonne > 10% In, degradee < 1A |

### Schematique conditionnement signal CT (Option B/C)

```
    CT Output (AC ±1V peak)
         │
         ├──── R_burden (33Ω, 1%) ────┐
         │                             │
         │                             ▼ GND
         │
         ├──── R1 (10kΩ) ──────┐
         │                      ├────── To ADS1115 (CH0)
         ├──── R2 (10kΩ) ──┐   │
         │                  │   │
         │                  ▼   │
         │                 Vdd/2│
         │                 (1.65V bias)
         │                      │
         └──── C_filter (100nF)─┘
              (anti-aliasing fc ≈ 160 Hz si R=10kΩ)
```

### 12.2 Interface capteur tension

| Parametre | Valeur |
|-----------|--------|
| Type | ZMPT101B (transformateur miniature) |
| Entree | 0-250V AC |
| Sortie | 0-5V AC (proportionnel, centre sur Vref/2) |
| Isolation | Transformateur (isolation renforcee) |
| Rapport | ~1/500 |
| Precision | ±2% (lineaire 50-250V) |
| Interface | ADS1115 CH3 (biased Vdd/2) |
| Disponibilite | Option B et C seulement |
| Usage | Power factor, puissance active, direction courant |

### 12.3 Interface temperature

| Parametre | Option A | Option B/C |
|-----------|----------|-----------|
| Type capteur | DS18B20 (digital) | PT100 classe A (RTD) |
| Interface | 1-Wire (GPIO, pull-up 4.7kΩ) | SPI (MAX31865) |
| Precision | ±0.5°C (-10 a +85°C) | ±0.15°C (-20 a +120°C) |
| Resolution | 12-bit (0.0625°C) | 15-bit (0.03°C) |
| Temps de reponse | ~750ms (probe inox) | ~200ms (film mince) |
| Nombre de capteurs | 2 (1-Wire bus, adresses uniques) | 2 (SPI, CS distincts) |
| Placement | Colle sur surface (conducteur thermique) | Contact barre cuivre ou borne |
| Avantage | Simple, pas de calibration | Industriel, precis, norme |
| Inconvenient | Moins precis, reaction lente | Plus complexe, cout superieur |

### 12.4 Interface stockage local

| Parametre | Valeur |
|-----------|--------|
| Flash interne ESP32 | 16 Mo (partition SPIFFS 4 Mo) |
| Flash externe (W25Q128) | 16 Mo supplementaires (SPI Quad) |
| Total buffer disponible | 4 Mo SPIFFS interne (priorite) + 12 Mo externe (extension) |
| Systeme de fichiers | SPIFFS (interne), LittleFS (externe, option) |
| Endurance | 100 000 cycles ecriture (wear leveling) |
| Interface | SPI @ 80 MHz (W25Q128), memoire mappee (interne) |
| Usage | Buffer features offline, log events, config backup |
| Protection | CRC32 par fichier, ecriture atomique (rename pattern) |
| Encryption | Option: AES-256 via flash encryption ESP32 (performance trade-off) |

### 12.5 Interface reseau

| Parametre | WiFi (primary) | 4G LTE-M (fallback, Option C) |
|-----------|----------------|-------------------------------|
| Module | ESP32-S3 integre | SIM7080G (externe, UART) |
| Frequence | 2.4 GHz | 700-2100 MHz (bandes LTE) |
| Portee | 10-30m interieur | Couverture cellulaire |
| Debit typique | 1-10 Mbps | 300 kbps (suffisant IoT) |
| Consommation TX | ~200 mA peak | ~300 mA peak |
| Authentification | WPA2-PSK ou Enterprise | SIM PIN |
| Interface broker | MQTT 5.0 / TLS 1.3 | MQTT 5.0 / TLS 1.3 (meme) |
| Basculement | — | Auto si WiFi down > 5 min |
| Cout mensuel | 0 (WiFi existant) | ~2 EUR/mois (SIM M2M) |
| SIM recommandee | — | 1NCE (10 ans, 500 Mo, 10 EUR) |

### 12.6 Interface securite device

| Parametre | Option A | Option B | Option C |
|-----------|----------|----------|----------|
| Crypto element | Non (dev mode) | ATECC608B breakout | ATECC608B UDFN (soude) |
| Cle privee | En flash (extractible) | ATECC608B slot 0 (non-exportable) | ATECC608B (idem) |
| Secure boot | Desactive (debug) | Active (RSA-3072) | Active |
| Flash encryption | Desactive (debug) | Active (AES-256-XTS) | Active |
| TLS | Optionnel (dev) | TLS 1.3 obligatoire | TLS 1.3 + cert pinning |
| Authentification MQTT | Username/password (dev) | X.509 mTLS | X.509 mTLS |
| Anti-tamper | Non | Non | Oui (switch detection ouverture) |
| Anti-downgrade | Non | eFuse counter (firmware) | eFuse counter |
| Debug (JTAG) | Active | Desactivable | Desactive en production |

---

## 13. Hors perimetre

### Explicitement exclu du prototype MVP 3

| Element | Raison | Quand |
|---------|--------|-------|
| PCB industrialise (Option C) avant validation Option B | Risque de redesign couteux | Apres gate Sprint 3.4 |
| Tests CEM en labo accredite | Budget + timing | Sprint 3.6 (si go) |
| Certification IEC 61439 / NF C 15-100 | Produit commercial seulement | MVP 4 |
| Marquage CE | Necessite CEM + RED + basse tension | MVP 4 |
| Boitier IP65+ (exterieur) | Tableaux interieurs IP20 suffisant | Produit commercial |
| Capteur tension triphase (3 ZMPT101B) | Monophase suffisant pour MVP 3 | Option C / MVP 4 |
| ADC rapide (>16 kSPS) pour FFT HF | Comparateur analogique suffit | Option C |
| LoRaWAN / NB-IoT exclusif | WiFi + 4G LTE-M suffisent en batiment | Produit commercial |
| Metering certifie MID (sous-comptage) | PyroSense n'est pas un compteur | Jamais (hors scope) |
| Mesure de tension directe (galvanique) | ZMPT101B isole suffit, pas de contact 230V | Design final si necessaire |
| Batterie de backup (UPS) | Alimentation rail DIN disponible, offline gere par buffer | Optionnel produit commercial |
| Detection de defaut d'arc conforme IEC 62606 | AFDD = produit certifie different | Partenariat eventuel |

### Non decides (a trancher Sprint 3.3)

| Decision | Options | Critere de choix |
|----------|---------|-----------------|
| Flash externe necessaire ? | W25Q128 vs SPIFFS interne seul | Test offline 72h : suffisant avec 4 Mo ? |
| 4G sur tous les prototypes ? | 3/10 vs 10/10 | Fiabilite WiFi constatee en labo |
| PT100 vs DS18B20 terrain ? | Precision vs simplicite | Exigence ΔT < 1°C suffisante ? |
| Antenne WiFi externe ? | PCB trace vs UFL + cable | RSSI mesure en armoire metallique |
| ADC unique (ADS1115) vs multiple ? | 1× 4ch vs 2× 4ch (8 entrees) | Besoin de > 4 canaux simultanes ? |

---

## 14. Validation par expert requise

### Ce qui DOIT etre valide par un electricien qualifie (B2V+)

| Element | Raison | Quand | Livrable attendu |
|---------|--------|-------|-----------------|
| Choix du CT (calibre, type) | Adequation avec l'installation cible | Avant achat Option B | Avis ecrit |
| Schema de raccordement CT sur barre | Securite + positionnement correct | Avant installation | Procedure ecrite |
| Alimentation HLK-PM01 (raccordement 230V) | Tout contact secteur = electricien | Installation | Realise par electricien |
| Placement sonde temperature | Point chaud pertinent (borne, barre, jeu de barres) | Installation | Choix valide in situ |
| Choix des departs a monitorer | Pertinence pour la detection (charges variees) | Avant installation | Liste circuits + justification |
| Validation apres mise sous tension | Verification courant mesure vs pince ampero | Installation | PV de mise en service |
| Conformite avec NF C 15-100 de l'installation existante | Le capteur ne doit pas degrader la conformite | Avant deploiement | Attestation |
| Tests sous charge (demande de courant elevee) | Verifier que le CT ne sature pas, pas de perturbation | Mise en service | Rapport mesures |

### Ce qui DOIT etre valide par un ingenieur electronique

| Element | Raison | Quand | Livrable attendu |
|---------|--------|-------|-----------------|
| Schema de conditionnement signal (bias, filtre) | Precision mesure, protection ADC | Avant assemblage Option B | Schema revise |
| Choix du burden resistor (valeur, puissance) | Inadequation → saturation ou bruit | Avant assemblage | Calcul documente |
| Filtre anti-aliasing (fc, ordre) | Eviter repliement spectral | Avant assemblage | Design note |
| Budget thermique (dissipation dans boitier) | Surchauffe → derive mesure, vieillissement | Option C (PCB) | Simulation thermique |
| Design PCB (routage, plans de masse, separation) | EMI propre, precision ADC | Option C | Revue design |
| Tests CEM (pre-screening) | Emissions < norme, immunite burst | Option C | Rapport mesures |
| Alimentation (bruit, regulation, ripple) | Bruit alim → bruit mesure | Option B/C | Mesure oscilloscope |

### Ce qui peut etre fait par l'equipe dev (sans expert)

| Element | Condition |
|---------|-----------|
| Assemblage Option A (breadboard basse tension) | Tension < 50V, aucun contact secteur |
| Programmation firmware (ESP-IDF, FreeRTOS) | Aucun risque electrique |
| Tests firmware sur Option A | Basse tension uniquement |
| Integration MQTT / backend | Pur logiciel |
| Assemblage Option B (composants basse tension) | Seulement les parties < 50V (ESP32, ADC, capteurs) |
| Tests logiciel sur Option B (avant mise sous tension) | USB alimente, pas de 230V |
| Design KiCad schema logique (Option C) | Soumis a review EE avant fabrication |

---

## Annexe A — Fournisseurs recommandes

| Composant | Fournisseur | Delai | Notes |
|-----------|-------------|:-----:|-------|
| ESP32-S3-DevKitC-1 | Mouser, DigiKey, AliExpress | 3-10j | Mouser/DigiKey = stock garanti |
| ADS1115 breakout | Adafruit (#1085), SparkFun | 5j | Adafruit = doc + support |
| YHDC SCT-013-030 | AliExpress, Seeed Studio | 5-15j | Commander en avance (delai Chine) |
| MAX31865 breakout | Adafruit (#3328) | 5j | Avec PT100 2-wire |
| ATECC608B breakout | Adafruit (#4314) | 5j | STEMMA QT / I2C |
| ZMPT101B module | AliExpress | 10-15j | Plusieurs fournisseurs, tester un lot |
| HLK-PM01 | AliExpress, Mouser | 5-15j | Verifier certification (Hi-Link officiel) |
| W25Q128 module | AliExpress, LCSC | 5-15j | Module breakout disponible |
| PCB fabrication | JLCPCB, PCBWay | 7-14j | JLCPCB = rapport qualite/prix |
| PCB assembly | JLCPCB Assembly | 14-21j | SMD seulement, through-hole main |
| Boitier DIN | RS Components, Farnell | 3-5j | Italtronic, Phoenix Contact |

### Commande recommandee Sprint 3.1 (immediate)

| Ref | Article | Qte | Prix | Fournisseur |
|-----|---------|:---:|:----:|-------------|
| 1 | ESP32-S3-DevKitC-1-N16R8 | 3 | 36 EUR | Mouser |
| 2 | Adafruit ADS1115 breakout | 3 | 15 EUR | Adafruit |
| 3 | ACS712-20A module | 9 | 36 EUR | AliExpress |
| 4 | Adafruit MAX31865 + PT100 | 6 | 90 EUR | Adafruit |
| 5 | DDS AD9833 module | 1 | 8 EUR | AliExpress |
| 6 | Breadboard + jumpers (kit) | 1 | 15 EUR | Amazon |
| 7 | Alimentation 24V AC 50VA | 1 | 20 EUR | RS Components |
| 8 | Resistances puissance (lot) | 1 | 10 EUR | Mouser |
| | **TOTAL Sprint 3.1** | | **~230 EUR** | |

---

## Annexe B — Tableau decision rapide

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                                                                               │
│  Question                        → Reponse                                   │
│  ─────────────────────────────── ─ ─────────────────────────────────────────│
│                                                                               │
│  "Je veux valider le firmware"   → Option A (basse tension, breadboard)      │
│                                                                               │
│  "Je veux des donnees reelles"   → Option B (electricien installe CT)        │
│                                                                               │
│  "Je veux preparer la prod"      → Option C (PCB, boitier DIN, tests CEM)   │
│                                                                               │
│  "Budget < 300 EUR"              → Option A                                  │
│                                                                               │
│  "Delai < 3 semaines"            → Option A                                  │
│                                                                               │
│  "Pilote terrain imminent"       → Option B (si firmware valide)             │
│                                                                               │
│  "50+ capteurs prevus"           → Option C (reproductibilite)               │
│                                                                               │
│  "Pas d'electricien disponible"  → Option A (seule option possible)          │
│                                                                               │
│  "Certification dans 12 mois"   → Option C (anticiper design for compliance)│
│                                                                               │
└───────────────────────────────────────────────────────────────────────────────┘
```

---

## Annexe C — Checklist achat avant Sprint 3.1

- [ ] Commander 3× ESP32-S3-DevKitC-1 (N16R8)
- [ ] Commander 3× ADS1115 breakout (Adafruit ou SparkFun)
- [ ] Commander 9× ACS712-20A module (ou ZMCT103C si dispo)
- [ ] Commander 6× MAX31865 breakout + PT100 (Adafruit)
- [ ] Commander 1× generateur de signal DDS (AD9833 ou equivalent)
- [ ] Commander 1× transformateur 24V AC (50VA min)
- [ ] Commander 1× kit breadboard + jumpers + resistances
- [ ] Verifier disponibilite multimetre True RMS
- [ ] Optionnel: oscilloscope USB (Hantek 6022BE ou similaire)
- [ ] Preparer poste de travail firmware (ESP-IDF 5.x installe, toolchain configuree)
