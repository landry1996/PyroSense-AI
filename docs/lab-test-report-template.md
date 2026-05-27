# Rapport de Test Laboratoire — PyroSense AI

## Ref : LAB-TEST-{YYYY-MM-DD}-{version}

---

## 1. Informations Generales

| Champ | Valeur |
|-------|--------|
| Date | {YYYY-MM-DD} |
| Heure debut | {HH:MM} |
| Heure fin | {HH:MM} |
| Duree totale | {X}h{XX}min |
| Lieu | {Salle / Labo} |
| Session N° | {numero sequentiel} |

### Equipe

| Role | Nom | Signature |
|------|-----|-----------|
| Developpeur firmware | | |
| Ingenieur hardware | | |
| Electricien (si present) | | |
| Observateur QA | | |

### Versions

| Composant | Version | Hash/Tag |
|-----------|---------|----------|
| Firmware | v{X.Y.Z} | {git short hash} |
| Backend (ingestion) | v{X.Y.Z} | {git short hash} |
| Backend (scoring) | v{X.Y.Z} | {git short hash} |
| Backend (analysis) | v{X.Y.Z} | {git short hash} |
| Protocole MQTT | v1 | — |
| Protocole de test | v{X} | {ref doc} |

### Materiel

| Equipement | Modele | N° serie | Calibration valide jusqu'au |
|------------|--------|----------|---------------------------|
| DUT (ESP32-S3) | DevKitC-1 N16R8 | | — |
| ADC | ADS1115 | | — |
| Sonde temperature | MAX31865 + PT100 | | |
| Generateur fonctions | | | |
| Oscilloscope | | | |
| Multimetre reference | | | |

---

## 2. Resume Executif

### Verdict global

| Resultat | |
|----------|---|
| **PASS / FAIL / PARTIAL** | {verdict} |
| Scenarios executes | {X} / 14 |
| Scenarios PASS | {X} |
| Scenarios FAIL | {X} |
| Scenarios PARTIAL | {X} |
| Scenarios non executes | {X} (raison : ...) |

### Points saillants

- {Point positif 1}
- {Point positif 2}
- {Point negatif / bloquant 1}
- {Point negatif / bloquant 2}

### Decision

- [ ] **GO terrain** : tous les criteres NP-01 a NP-10 satisfaits
- [ ] **NO-GO** : criteres bloquants non resolus (lister)
- [ ] **GO conditionnel** : passage possible apres correction de {anomalie(s)}

---

## 3. Resultats par Scenario

| ID | Scenario | Resultat | Duree | Commentaire |
|----|----------|:--------:|-------|-------------|
| SC-01 | Demarrage device | | | |
| SC-02 | Provisioning securise | | | |
| SC-03 | Heartbeat periodique | | | |
| SC-04 | Telemetrie normale | | | |
| SC-05 | Perte WiFi | | | |
| SC-06 | Reconnexion | | | |
| SC-07 | Buffer offline (stress) | | | |
| SC-08 | Donnees hors plage | | | |
| SC-09 | Signal bruite | | | |
| SC-10 | Temperature elevee simulee | | | |
| SC-11 | Micro-arc simule | | | |
| SC-12 | Transitoires simules | | | |
| SC-13 | Firmware version mismatch | | | |
| SC-14 | Credential revoked | | | |

---

## 4. Mesures Detaillees

### Precision des mesures

| Mesure | Valeur reference | Valeur capteur | Ecart | Seuil | Verdict |
|--------|-----------------|----------------|-------|-------|---------|
| RMS (1V @ 50Hz) | | | | ±5% | |
| THD (signal pur) | | | | < 2% | |
| Temperature (22°C) | | | | ±2°C | |
| Frequence (50 Hz) | | | | ±0.5 Hz | |

### Temps systeme

| Mesure | Valeur | Seuil | Verdict |
|--------|--------|-------|---------|
| Boot time | | < 15s | |
| Provisioning time | | < 5s | |
| Detection perte WiFi | | < 10s | |
| Reconnexion automatique | | < 30s | |
| Latence telemetrie E2E | | < 2s | |
| Drain buffer (60 msg) | | < 60s | |

### Stabilite (4h continu)

| Metrique | Debut | Fin | Derive | Verdict |
|----------|-------|-----|--------|---------|
| freeHeap | | | | < 1KB/h |
| Temperature PCB | | | | < 5°C drift |
| RSSI | | | | Stable ±3 dBm |
| Erreurs cumul | | | | 0 |

---

## 5. Anomalies Detectees

| ID Anomalie | Scenario | Severite | Description courte | Fiche |
|-------------|----------|----------|-------------------|-------|
| | | | | DEF-{XXX} |
| | | | | DEF-{XXX} |
| | | | | DEF-{XXX} |

---

## 6. Metriques Backend

### Ingestion

| Metrique | Valeur | Attendu |
|----------|--------|---------|
| Messages recus total | | |
| Messages rejetes | | 0 (hors scenarios rejet) |
| Latence moyenne | | < 500ms |
| Latence P99 | | < 2s |

### Analyse

| Metrique | Valeur | Attendu |
|----------|--------|---------|
| Anomalies detectees | | > 0 (scenarios 10-12) |
| Faux positifs (nominal) | | 0 |
| Temps traitement moyen | | < 1s |

### Scoring

| Metrique | Valeur | Attendu |
|----------|--------|---------|
| Score max observe | | > 60 (scenarios anomalie) |
| Score min observe (nominal) | | < 30 |
| Ajustements feedback | | N/A pour labo |

---

## 7. Conclusion et Recommandations

### Conclusion

{Paragraphe de synthese : le prototype est-il pret pour le terrain ? Quelles limitations ? Quel niveau de confiance ?}

### Actions correctives obligatoires (si NO-GO)

| # | Action | Responsable | Deadline |
|---|--------|-------------|----------|
| 1 | | | |
| 2 | | | |

### Recommandations (non bloquantes)

| # | Recommandation | Priorite |
|---|---------------|----------|
| 1 | | |
| 2 | | |

### Prochaine session

| Champ | Valeur |
|-------|--------|
| Date prevue | |
| Objectif | {Re-test apres corrections / nouveaux scenarios / endurance} |
| Pre-requis | |

---

## 8. Annexes

- [ ] Logs serie complets : `{chemin/fichier.log}`
- [ ] Export MQTT : `{chemin/mqtt-export.json}`
- [ ] Captures Grafana : `{chemin/screenshots/}`
- [ ] Captures oscilloscope : `{chemin/oscilloscope/}`
- [ ] Export Wireshark : `{chemin/capture.pcap}`
- [ ] Binaire firmware teste : `{chemin/firmware-vX.Y.Z.bin}` (SHA-256: `{hash}`)

---

## Approbation

| Role | Nom | Date | Signature |
|------|-----|------|-----------|
| Developpeur firmware | | | |
| Ingenieur hardware | | | |
| Observateur QA | | | |
| Tech Lead (approbation GO/NO-GO) | | | |
