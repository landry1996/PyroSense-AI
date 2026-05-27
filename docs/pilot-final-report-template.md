# Rapport Final Pilote — PyroSense AI (10 Capteurs)

## Ref : PILOT-REPORT-{YYYY-MM}

---

## 1. Resume Executif

### Decision

| | |
|---|---|
| **Verdict** | **GO / NO-GO / GO CONDITIONNEL** pour phase 100 capteurs |
| Date decision | {YYYY-MM-DD} |
| Decideurs | {noms et roles} |

### Chiffres cles

| Indicateur | Valeur |
|-----------|--------|
| Duree pilote | ___ mois |
| Devices deployes | ___ / 10 |
| Uptime moyen | ___% |
| Telemetrie valide | ___% |
| Alertes generees | ___ |
| Alertes confirmees (vrais positifs) | ___ |
| Faux positifs | ___ (___%) |
| Defauts detectes avant panne | ___ |
| Incidents securite | ___ |
| Satisfaction gestionnaire | ___/5 |

### Points forts

1. {point fort 1}
2. {point fort 2}
3. {point fort 3}

### Points faibles

1. {point faible 1}
2. {point faible 2}
3. {point faible 3}

---

## 2. Contexte et Objectifs Rappeles

### Objectifs initiaux

| Objectif | Atteint | Commentaire |
|----------|:-------:|-------------|
| Collecter donnees reelles pour calibration | OUI/NON/PARTIEL | |
| Valider robustesse hardware/firmware | OUI/NON/PARTIEL | |
| Valider pipeline complet capteur → alerte | OUI/NON/PARTIEL | |
| Evaluer taux faux positifs | OUI/NON/PARTIEL | |
| Recueillir feedback terrain | OUI/NON/PARTIEL | |
| Constituer dataset ML initial | OUI/NON/PARTIEL | |

### Perimetre reel vs prevu

| Element | Prevu | Reel | Ecart |
|---------|-------|------|-------|
| Devices deployes | 10 | | |
| Sites | 2-3 | | |
| Duree | 3-6 mois | | |
| Circuits monitores | 10-20 | | |

---

## 3. Deroulement Chronologique

| Semaine | Jalon / Evenement | Impact |
|:-------:|------------------|--------|
| S1 | | |
| S2 | | |
| S3-4 | | |
| S5-8 | | |
| S9-12 | | |
| S13+ | | |

### Incidents majeurs

| Date | Incident | Impact | Resolution | Duree |
|------|----------|--------|-----------|-------|
| | | | | |
| | | | | |

---

## 4. Resultats Techniques

### KPIs techniques — Bilan

| KPI | Cible | Resultat | Verdict |
|-----|:-----:|:--------:|:-------:|
| Uptime capteur | > 95% | ___% | PASS/FAIL |
| Telemetrie valide | > 99% | ___% | PASS/FAIL |
| Qualite signal moyenne | > 70 | ___ | PASS/FAIL |
| Latence E2E P95 | < 5s | ___s | PASS/FAIL |
| Buffer overflow | 0 | ___ | PASS/FAIL |
| Reconnexion moyenne | < 60s | ___s | PASS/FAIL |
| Memory stability | < 500 B/h | ___ B/h | PASS/FAIL |
| Backend disponibilite | > 99% | ___% | PASS/FAIL |

### Uptime par device

| Device | Uptime | Pannes | Cause principale |
|--------|:------:|:------:|-----------------|
| PS-001 | ___% | ___ | |
| PS-002 | ___% | ___ | |
| PS-003 | ___% | ___ | |
| PS-004 | ___% | ___ | |
| PS-005 | ___% | ___ | |
| PS-006 | ___% | ___ | |
| PS-007 | ___% | ___ | |
| PS-008 | ___% | ___ | |
| PS-009 | ___% | ___ | |
| PS-010 | ___% | ___ | |

### Problemes hardware rencontres

| # | Probleme | Devices affectes | Resolution |
|---|----------|:----------------:|-----------|
| | | | |

### Problemes firmware rencontres

| # | Probleme | Frequence | Fix applique | Version fix |
|---|----------|:---------:|-------------|:-----------:|
| | | | | |

---

## 5. Resultats Detection

### Vue d'ensemble

| Metrique | M1 | M2 | M3 | Total |
|----------|:--:|:--:|:--:|:-----:|
| Alertes generees | | | | |
| Alertes inspectees | | | | |
| Vrais positifs (CONFIRMED) | | | | |
| Faux positifs (FALSE_POSITIVE) | | | | |
| Inconclusifs | | | | |
| Taux faux positifs | ___% | ___% | ___% | ___% |

### Defauts reels detectes

| # | Date | Device | Type anomalie | Severite | Temps detection → inspection | Actions |
|---|------|--------|--------------|----------|:----------------------------:|---------|
| | | | | | | |
| | | | | | | |

### Evolution du taux de faux positifs

```
(Graphe ou tableau mensuel montrant la tendance — en baisse attendue)
```

### Types d'anomalies detectees

| Type | Occurrences | Confirmes | FP | Taux precision |
|------|:-----------:|:---------:|:--:|:--------------:|
| MICRO_ARC | | | | ___% |
| THD_DRIFT | | | | ___% |
| TEMPERATURE_RISING | | | | ___% |
| TRANSIENT_ABNORMAL | | | | ___% |
| HF_NOISE_ELEVATED | | | | ___% |

---

## 6. Resultats Operationnels

### Support

| Metrique | Valeur |
|----------|--------|
| Tickets L1 traites | |
| Tickets escalades L2 | |
| Interventions terrain (L3) | |
| Temps moyen resolution L1 | |
| Taux resolution L1 sans escalade | ___% |

### Satisfaction gestionnaire

| Question | Score (1-5) |
|----------|:-----------:|
| Clarte des communications | |
| Reactivite du support | |
| Pertinence des alertes | |
| Confiance dans le systeme | |
| Recommanderait a un pair | |
| **Moyenne** | **___/5** |

### Retours qualitatifs gestionnaire

```
(Citations et feedback libre du gestionnaire)


```

### Retours electricien

```
(Observations terrain, facilite d'usage du feedback, pertinence des alertes)


```

---

## 7. Analyse des Faux Positifs

### Causes identifiees

| Cause | Occurrences | % total FP | Correction appliquee |
|-------|:-----------:|:----------:|---------------------|
| Charge inhabituelle (moteur demarrage) | | | |
| Seuil trop sensible (THD) | | | |
| Bruit environnemental (HF) | | | |
| Conditions meteo (temperature) | | | |
| Autre | | | |

### Corrections de seuils appliquees

| Date | Parametre | Avant | Apres | Impact |
|------|-----------|:-----:|:-----:|--------|
| | | | | |

### Evolution

| Mois | Total alertes | FP | Taux FP | Tendance |
|------|:-------------:|:--:|:-------:|:--------:|
| M1 | | | ___% | — |
| M2 | | | ___% | ↑/→/↓ |
| M3 | | | ___% | ↑/→/↓ |

---

## 8. Analyse des Vrais Positifs

### Defauts detectes

| # | Description | Impact evite estime | Score risque | Actions correctives |
|---|-------------|--------------------:|:-----------:|---------------------|
| | | | | |

### Valeur demontree

| Metrique | Valeur |
|----------|--------|
| Defauts detectes en avance | |
| Cout estime incidents evites | |
| Temps moyen anticipation avant panne | |

---

## 9. Qualite du Dataset ML

| Metrique | Valeur | Cible |
|----------|:------:|:-----:|
| Volume telemetrie brute | ___ GB | > 5 GB |
| Labels confirmes (CONFIRMED_DEFECT) | | > 30 |
| Labels FALSE_POSITIVE | | > 10 |
| Labels NORMAL (baseline) | | > 1000 |
| Labels totaux exploitables | | > 50 |
| Qualite donnees (grade A+B) | ___% | > 80% |
| Diversite anomalies labelisees | ___ types | > 3 |
| Candidats dataset exportables | | > 100 |

### Exploitabilite pour ML

- [ ] Volume suffisant pour entrainement supervisé (> 50 labels confirmes)
- [ ] Distribution equilibree des classes
- [ ] Features stables et coherentes
- [ ] Pseudonymisation appliquee
- [ ] Export format Parquet/CSV pret

---

## 10. Bilan Financier

| Poste | Budget previsionnel | Cout reel | Ecart |
|-------|:-------------------:|:---------:|:-----:|
| Hardware (10 capteurs + spare) | | | |
| Installation (electricien) | | | |
| Infrastructure cloud (3-6 mois) | | | |
| Support / temps equipe | | | |
| Deplacement / logistique | | | |
| **TOTAL** | | | |

### Rapport cout / valeur

| | |
|---|---|
| Cout total pilote | |
| Valeur estimee incidents evites | |
| Ratio | |

---

## 11. Lecons Apprises

### Firmware

| # | Lecon | Action recommandee |
|---|-------|-------------------|
| | | |
| | | |

### Backend

| # | Lecon | Action recommandee |
|---|-------|-------------------|
| | | |
| | | |

### Process / Operations

| # | Lecon | Action recommandee |
|---|-------|-------------------|
| | | |
| | | |

### Humain / Communication

| # | Lecon | Action recommandee |
|---|-------|-------------------|
| | | |
| | | |

---

## 12. Recommandations pour Phase 100 Capteurs

### Pre-requis techniques

| # | Pre-requis | Statut actuel | Action |
|---|-----------|:-------------:|--------|
| | Firmware stable (0 crash/semaine) | | |
| | Taux FP < 5% stabilise | | |
| | OTA fonctionnel et teste | | |
| | Backend HA (multi-instance) | | |
| | ML v1 entraine et valide | | |

### Pre-requis operationnels

| # | Pre-requis | Statut actuel | Action |
|---|-----------|:-------------:|--------|
| | Procedure installation < 45 min | | |
| | Support L1 autonome (runbook) | | |
| | Dashboard gestionnaire operationnel | | |
| | Process feedback electricien rode | | |
| | Monitoring 24/7 ou alertes auto fiables | | |

### Pre-requis business

| # | Pre-requis | Statut actuel | Action |
|---|-----------|:-------------:|--------|
| | Modele economique valide | | |
| | Assurance RC Pro | | |
| | CGU / contrat pilote template | | |
| | Support commercial identifie | | |

### Planning propose phase 100

| Etape | Duree | Actions |
|-------|-------|---------|
| Preparation | 1 mois | Corrections firmware, HA backend, OTA, ML train |
| Deploiement progressif | 2 mois | 10→30→60→100 capteurs |
| Operation | 6 mois | Monitoring, feedback, ajustements |
| Bilan | 1 mois | Rapport, decision industrialisation |

---

## 13. Decision GO / NO-GO

### Criteres de succes — Bilan

| # | Critere | Seuil | Resultat | Verdict |
|---|---------|:-----:|:--------:|:-------:|
| CS-01 | Uptime > 95% | 95% | ___% | |
| CS-02 | Telemetrie valide > 99% | 99% | ___% | |
| CS-03 | Taux FP < 10% a M3 | 10% | ___% | |
| CS-04 | Au moins 1 defaut reel detecte | > 0 | ___ | |
| CS-05 | 0 incident securite | 0 | ___ | |
| CS-06 | Dataset > 30 labels | 30 | ___ | |
| CS-07 | Backend > 99% dispo | 99% | ___% | |
| CS-08 | Reconnexion < 60s | 60s | ___s | |
| CO-01 | Satisfaction > 3.5/5 | 3.5 | ___/5 | |
| CO-05 | 0 retrait mecontentement | 0 | ___ | |

### Decision

- [ ] **GO** : Tous les criteres CS atteints. Lancer phase 100 capteurs.
- [ ] **GO CONDITIONNEL** : Majorite CS atteints. Conditions : {lister les pre-requis manquants}
- [ ] **NO-GO** : Criteres bloquants non atteints. Actions : {lister}
- [ ] **PIVOT** : Reorientation du projet. Raisons : {lister}

### Justification

```
(Argumentaire detaille de la decision — 1 a 2 paragraphes)


```

---

## 14. Annexes

- [ ] Graphes Grafana (uptime, latence, alertes, signal quality) — periode pilote complete
- [ ] Registre devices final (statuts, pannes, remplacements)
- [ ] Historique des alertes (export CSV)
- [ ] Feedbacks terrain (export)
- [ ] PV installations
- [ ] PV retraits (si applicable)
- [ ] Questionnaires satisfaction
- [ ] Photos installations
- [ ] Historique des mises a jour firmware
- [ ] Rapport qualite donnees (grades, issues)

---

## Approbation

| Role | Nom | Date | Signature |
|------|-----|------|-----------|
| Tech Lead | | | |
| Chef projet | | | |
| Gestionnaire site 1 | | | |
| Gestionnaire site 2 | | | |
| Direction | | | |
