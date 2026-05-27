# Checklist Monitoring Quotidien — Pilote PyroSense AI

## Informations

| Champ | Valeur |
|-------|--------|
| Date | ____/____/________ |
| Operateur | |
| Heure debut | ___:___ |
| Heure fin | ___:___ |

---

## 1. Sante des Devices (08:00)

| Device | Statut | Dernier HB | RSSI | SignalQ | freeHeap | Anomalie |
|--------|:------:|-----------|:----:|:------:|:--------:|----------|
| PS-001 | ACTIVE / OFFLINE | ___:___ | ___ dBm | ___/100 | ___ KB | |
| PS-002 | ACTIVE / OFFLINE | ___:___ | ___ dBm | ___/100 | ___ KB | |
| PS-003 | ACTIVE / OFFLINE | ___:___ | ___ dBm | ___/100 | ___ KB | |
| PS-004 | ACTIVE / OFFLINE | ___:___ | ___ dBm | ___/100 | ___ KB | |
| PS-005 | ACTIVE / OFFLINE | ___:___ | ___ dBm | ___/100 | ___ KB | |
| PS-006 | ACTIVE / OFFLINE | ___:___ | ___ dBm | ___/100 | ___ KB | |
| PS-007 | ACTIVE / OFFLINE | ___:___ | ___ dBm | ___/100 | ___ KB | |
| PS-008 | ACTIVE / OFFLINE | ___:___ | ___ dBm | ___/100 | ___ KB | |
| PS-009 | ACTIVE / OFFLINE | ___:___ | ___ dBm | ___/100 | ___ KB | |
| PS-010 | ACTIVE / OFFLINE | ___:___ | ___ dBm | ___/100 | ___ KB | |

**Devices actifs** : ___ / 10
**Devices offline** : ___ / 10

---

## 2. Alertes en Attente (08:15)

| Heure alerte | Device | Type | Severite | Statut | Action |
|:------------:|--------|------|:--------:|:------:|--------|
| | | | WARNING / CRITICAL | OPEN / ACK | |
| | | | WARNING / CRITICAL | OPEN / ACK | |
| | | | WARNING / CRITICAL | OPEN / ACK | |

**Nouvelles alertes depuis hier** : ___
**Alertes CRITICAL non traitees** : ___ (si > 0 : escalade immediate)

---

## 3. Metriques Backend (08:30)

| Metrique | Valeur | Seuil | OK |
|----------|--------|:-----:|:--:|
| Ingestion messages recus (24h) | | > 8000 (10 dev × 6/min × 24h × 90%) | |
| Ingestion rejections (24h) | | < 1% des recus | |
| Ingestion latency P95 | | < 2s | |
| Analysis events processed (24h) | | > 0 | |
| Scoring calculations (24h) | | > 0 | |
| Backend error rate (24h) | | < 1% | |
| Kafka consumer lag max | | < 100 | |
| PostgreSQL connections | | < 80% max | |
| Disk usage | | < 80% | |

---

## 4. Actions Declenchees

| # | Action | Device | Raison | Statut |
|---|--------|--------|--------|--------|
| | | | | FAIT / EN COURS / ESCALADE |
| | | | | |
| | | | | |

---

## 5. Evenements Notables

```
(Tout evenement inhabituel : pic de charge, meteo extreme, travaux sur site, 
deconnexion gestionnaire, maintenance programmee, etc.)




```

---

## 6. Bilan Journalier (17:00)

| Question | Reponse |
|----------|---------|
| Tous les devices sont-ils actifs ? | OUI / NON (details : ___) |
| Y a-t-il des alertes CRITICAL non traitees ? | OUI / NON |
| Y a-t-il des alertes WARNING > 48h non traitees ? | OUI / NON |
| Le backend est-il stable ? | OUI / NON |
| Faut-il escalader quelque chose demain ? | OUI / NON (quoi : ___) |
| Faut-il planifier une intervention terrain ? | OUI / NON (quand : ___) |

---

## 7. Indicateurs Tendance (hebdomadaire — remplir le vendredi)

| KPI | Cette semaine | Semaine precedente | Tendance |
|-----|:-------------:|:------------------:|:--------:|
| Uptime moyen | ___% | ___% | ↑ / → / ↓ |
| Alertes generees | ___ | ___ | ↑ / → / ↓ |
| Faux positifs | ___ | ___ | ↑ / → / ↓ |
| Confirmations terrain | ___ | ___ | ↑ / → / ↓ |
| Signal quality moyen | ___/100 | ___/100 | ↑ / → / ↓ |
| Interventions planifiees | ___ | ___ | ↑ / → / ↓ |

---

## Signature Operateur

| | |
|---|---|
| Nom | |
| Date | |
| Signature | |

---

## Rappels

- Si device offline > 4h → suivre procedure section 16 du plan pilote
- Si alerte CRITICAL → suivre procedure section 15 du plan pilote
- Si doute → escalader au Tech Lead (Slack #pyrosense-pilot)
- Ne JAMAIS contacter le gestionnaire pour une alerte WARNING sans validation prealable
- En cas de danger electrique reel signale : rappeler de couper le disjoncteur + appeler le 18
