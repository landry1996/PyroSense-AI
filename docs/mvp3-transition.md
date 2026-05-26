# MVP 3 — Transition Plan : Du Prototype Logiciel au Pilote Terrain

**Date :** 2026-05-27
**Prerequis :** MVP 2 complet, audit realise (4 vulnerabilites corrigees), 571+ tests backend passent.

---

## Classification des Niveaux de Maturite

| Niveau | Description | Statut |
|--------|-------------|:------:|
| **Logiciel pret** | Architecture, tests, securite, observabilite — deploiement cloud possible | **ATTEINT** |
| **Prototype capteur** | Composants selectionnes, firmware MQTT fonctionnel, banc de test valide | A FAIRE |
| **Pilote laboratoire** | Capteurs reels en environnement controle, calibration seuils | A FAIRE |
| **Pilote terrain** | 10 batiments reels, 50-100 capteurs, electriciens formes, donnees collectees | A FAIRE |
| **Produit commercial certifie** | IEC 61439, NF C 15-100, marquage CE, RC Pro, support 24/7 | A FAIRE |

---

## 1. Ce qui est pret apres MVP 2

### Logiciel (100% fonctionnel)

| Composant | Etat | Details |
|-----------|:----:|---------|
| 11 microservices Java 21 | PRET | Hexagonal, DDD, multi-tenant, 571+ tests |
| Pipeline event-driven | PRET | Kafka 6 topics, DLQ, idempotence, ExponentialBackOff |
| Detection statistique | PRET | Welford baseline, z-score, micro-arc, THD, temperature |
| Scoring de risque | PRET | Formule composite 6 facteurs, explainability |
| Alerting + escalade SLA | PRET | Lifecycle complet, deduplication, severite |
| Workflow alerte → intervention | PRET | Auto CRITICAL, recommandation WARNING, feedback loop |
| Dashboard Angular 18 | PRET | 12 ecrans, responsive, WebSocket temps reel |
| Rapports PDF | PRET | 4 types, signature SHA-256, download securise |
| Notifications multi-canal | PRET | 5 canaux, 9 templates, quiet hours, anti-spam |
| Observabilite | PRET | Micrometer, Prometheus 21 alertes, OTEL tracing, Loki |
| Securite | PRET | RBAC 8 roles, tenant isolation auditee, anti mass-assignment |
| Docker Compose | PRET | Profil mvp2, scripts start/stop/reset |
| CI/CD | PRET | 7 jobs GitHub Actions |
| Documentation | PRET | 27+ documents techniques |

### Ports ML deja en place (NoOp adapters)

| Port | Interface | Adapter actuel |
|------|-----------|----------------|
| `MachineLearningInferencePort` | SignalWindow → AnomalyPrediction | NoOp (retourne "no prediction") |
| `RiskModelPort` | Facteurs → Score alternatif | NoOp (delegue a la formule statistique) |
| `FeedbackLoopPort` | Outcomes intervention → ajustement seuils | Events publies, pas de consumer ML |

### Feedback loop deja implemente

- `ElectricalDefectConfirmedEvent` → renforcement positif (baisse seuil)
- `FalsePositiveConfirmedEvent` → renforcement negatif (hausse seuil)
- `RiskImpact` record (riskScoreBefore / riskScoreAfter / avoidedIncidentEstimateDays)
- Hit-rate par pattern et par device deja trackable

---

## 2. Ce qui manque pour un pilote terrain

### Bloquants (sans quoi aucun deploiement reel)

| # | Element manquant | Responsable | Effort |
|---|-----------------|-------------|--------|
| 1 | Capteurs physiques (pince ampero CT, sonde T°, detecteur arc) | Partenaire hardware | 3 mois |
| 2 | Gateway IoT (ESP32/STM32/RPi) + firmware MQTT | Ingenieur firmware | 3 mois |
| 3 | Provisioning securise (certificats X.509 par device) | DevSecOps | 2 semaines |
| 4 | Infrastructure cloud (Kubernetes, PostgreSQL manage, Kafka manage) | DevOps | 4 semaines |
| 5 | Keycloak realm configure (vrais utilisateurs, roles) | Backend dev | 1 semaine |
| 6 | Site pilote identifie (accord proprietaire, contrat) | Business | 1-2 mois |
| 7 | Electricien partenaire (validation terrain) | Business | 1 mois |
| 8 | Assurance pilote (responsabilite civile) | Juridique | 1 mois |
| 9 | Calibration seuils sur donnees reelles (30 jours minimum) | Data + firmware | 1 mois in situ |
| 10 | TLS/HTTPS sur toutes les communications | DevOps | 1 semaine |

### Non-bloquants mais necessaires pour la qualite

| # | Element | Impact si absent |
|---|---------|-----------------|
| 11 | Application mobile (PWA ou native) | Electriciens consultent via web |
| 12 | Load testing (K6, 100 devices simultan.) | Risque perf non quantifie |
| 13 | Runbooks operationnels | Incidents geres ad hoc |
| 14 | Sauvegarde automatisee + test restore | Risque perte donnees |
| 15 | Plan RGPD (registre traitement, DPO) | Non conforme mais tolere en pilote prive |

---

## 3. Limites du simulateur actuel

### Ce que le simulateur fait bien

- Genere des donnees MQTT structurees compatibles avec l'ingestion service
- Simule 7 scenarios de degradation progressive
- Multi-tenant, multi-batiment, multi-device
- Acceleration temporelle (mois en minutes)
- Payloads conformes au protocole IoT defini

### Ce que le simulateur NE FAIT PAS

| Limite | Consequence | Impact sur le pilote |
|--------|-------------|---------------------|
| Pas de bruit reel (EMI, harmoniques secteur) | Seuils trop sensibles en environnement reel | Faux positifs eleves les premieres semaines |
| Pas de derive capteur (vieillissement) | Pas de detection de capteur defaillant | Risque fausse securite |
| Pas d'interactions entre circuits | Pas de correlation multi-canal | Manque de patterns reels (ex: couplage inductif) |
| Pas de conditions environnementales | Temperature ambiante, humidite ignores | Baselines incorrectes en ete/hiver |
| Pas de transitoires reels (demarrage moteur, commutation) | Modeles mathematiques simplifies | Micro-arcs confondus avec commutations normales |
| Pas de topologie reelle (longueur cables, sections) | Impedances modelisees uniformement | Scoring imprecis pour installations anciennes |
| Pas de comportement d'occupation | Profils de consommation artificiels | Baseline diurne/nocturne incorrecte |
| Pas de pannes partielles (connexion intermittente) | Signaux propres ou completement absents | Patterns loose connection manques |

### Conclusion simulateur

> Le simulateur valide l'architecture et les flux. Il ne valide PAS les seuils de detection, les taux de faux positifs, ni l'efficacite du scoring. Toute confiance dans la qualite de la detection necessite des donnees terrain reelles.

---

## 4. Donnees a collecter

### Phase 1 : Collecte passive (mois 1-3 du pilote)

| Donnee | Frequence | Volume estime (10 capteurs) | Usage |
|--------|-----------|:---------------------------:|-------|
| Courant RMS (3 phases) | 1 Hz | 2.6M readings/mois | Baseline, surcharge |
| Tension RMS (3 phases) | 1 Hz | 2.6M readings/mois | Qualite alimentation |
| Temperature (point de connexion) | 0.2 Hz | 520K readings/mois | Echauffement |
| THD (Total Harmonic Distortion) | 0.1 Hz | 260K readings/mois | Degradation isolant |
| Transitoires haute frequence | Event-driven | ~100-1000/jour/capteur | Micro-arcs |
| Vibration (optionnel) | 1 Hz | 2.6M readings/mois | Connexions laches |

**Volume total estime : ~50 Go/mois pour 10 capteurs a 1 Hz.**

### Phase 2 : Etiquetage (mois 2-6)

| Label | Source | Quantite cible |
|-------|--------|:--------------:|
| Vrai positif (defaut confirme) | Electricien terrain | 50+ en 6 mois |
| Faux positif (alerte sans defaut) | Electricien terrain | 200+ en 6 mois |
| Normal (pas d'alerte, pas de defaut) | Automatique | Majorite du dataset |
| Defaut non detecte (missed) | Inspection periodique | Idealement 0, probablement 5-10 |

### Phase 3 : Dataset ML (mois 6-12)

| Critere | Minimum viable | Ideal |
|---------|:--------------:|:-----:|
| Echantillons etiquetes | 5 000 | 50 000 |
| Types de defauts couverts | 4/7 | 7/7 |
| Batiments distincts | 3 | 10 |
| Saisons couvertes | 1 (ete ou hiver) | 4 (cycle annuel) |
| Ratio anomalie/normal | 1:50 | 1:100 (realiste) |

### Metadata contextuelle a collecter

- Type de batiment (residentiel, tertiaire, industriel)
- Age de l'installation electrique
- Type de tableau (divisionnaire, principal, TGBT)
- Section et longueur des cables
- Appareillage en aval (moteurs, CVC, eclairage, informatique)
- Interventions de maintenance (dates, resultats)
- Evenements batiment (travaux, changement d'occupation)

---

## 5. Metriques terrain

### KPIs de detection (objectifs pilote)

| Metrique | Definition | Cible pilote | Cible production |
|----------|-----------|:------------:|:----------------:|
| Sensibilite (recall) | Vrais positifs / (vrais positifs + faux negatifs) | > 80% | > 95% |
| Precision | Vrais positifs / (vrais positifs + faux positifs) | > 60% | > 85% |
| Taux faux positifs | Faux positifs / total alertes | < 20% | < 5% |
| Temps de detection | Delai entre debut anomalie et alerte | < 24h | < 4h |
| Temps anticipation | Delai entre alerte et defaillance confirmee | > 48h | > 7 jours |

### KPIs operationnels

| Metrique | Cible pilote | Cible production |
|----------|:------------:|:----------------:|
| Disponibilite plateforme | 99% | 99.9% |
| Latence ingestion (end-to-end) | < 10s | < 3s |
| Uptime capteurs | > 95% | > 99% |
| Perte messages MQTT | < 1% | < 0.1% |
| MTTR (Mean Time To Restore) | < 4h | < 1h |

### KPIs metier

| Metrique | Methode de mesure |
|----------|------------------|
| Interventions preventives declenchees | Count `intervention.status=COMPLETED` avec `result != FALSE_POSITIVE` |
| Cout evite par intervention | Estimation electricien (reparation curative vs preventive) |
| ROI par batiment | (Cout evite - cout systeme) / cout systeme |
| Satisfaction gestionnaire | NPS enquete trimestrielle |
| Delai detection → intervention | Diff `alert.createdAt` - `intervention.completedAt` |
| Taux adoption | % alertes traitees dans le SLA / total alertes |

### Metriques Prometheus a ajouter pour le terrain

```
pyrosense.terrain.devices.online{building, panel}
pyrosense.terrain.devices.offline.duration{device}
pyrosense.terrain.alerts.confirmed_defect_total{type}
pyrosense.terrain.alerts.false_positive_total{type}
pyrosense.terrain.detection.latency_seconds{type}
pyrosense.terrain.baseline.learning_progress{device}
pyrosense.terrain.data.ingestion_gap_seconds{device}
```

---

## 6. Protocole d'installation pilote

### Pre-requis site

| Critere | Obligatoire | Verification |
|---------|:-----------:|-------------|
| Accord ecrit proprietaire/gestionnaire | Oui | Contrat signe (non-substitution, limitation responsabilite) |
| Tableau electrique accessible | Oui | Visite prealable electricien |
| Alimentation disponible (rail DIN ou prise) | Oui | Verification in situ |
| Connectivite (WiFi ou Ethernet ou 4G) | Oui | Test couverture |
| Aucune intervention en cours | Oui | Verification calendrier maintenance |
| Electricien qualifie disponible | Oui | Habilitation B2V minimum |

### Procedure d'installation (par capteur)

| Etape | Action | Duree | Responsable |
|-------|--------|:-----:|-------------|
| 1 | Consignation du tableau (coupure) | 5 min | Electricien |
| 2 | Pose pince amperemetrique sur depart | 5 min | Electricien |
| 3 | Raccordement sonde temperature (point chaud) | 5 min | Electricien |
| 4 | Fixation boitier gateway (rail DIN ou collage) | 5 min | Electricien |
| 5 | Branchement alimentation gateway | 2 min | Electricien |
| 6 | Remise sous tension | 2 min | Electricien |
| 7 | Verification LED (heartbeat) | 1 min | Technicien IT |
| 8 | Verification reception MQTT (dashboard) | 2 min | Technicien IT |
| 9 | Provisioning device (certificat + enrollment) | 5 min | Technicien IT |
| 10 | Photo installation (audit trail) | 1 min | Electricien |
| 11 | Etiquetage circuit (correspondance physique/logique) | 3 min | Electricien |
| 12 | Validation telemetrie (10 readings OK) | 2 min | Technicien IT |

**Duree totale par capteur : ~40 minutes.**
**Duree par batiment (5 capteurs) : ~3.5 heures (incluant deplacements).**

### Post-installation

| Action | Delai | Critere de succes |
|--------|:-----:|------------------|
| Periode d'apprentissage (baseline) | 30 jours | Baseline stable (variance < 10% sur 7 derniers jours) |
| Premiere alerte autorisee | Jour 31 | Apres validation baseline |
| Premiere intervention terrain | Variable | Alerte CRITICAL ou WARNING > 72h |
| Revue pilote intermediaire | Jour 90 | Rapport KPIs + retours electricien |
| Ajustement seuils | Jour 45-60 | Faux positifs < 20% sur fenetre 7 jours |

---

## 7. Protocole de validation des alertes

### Classification des alertes (terrain)

| Type | Action requise | Delai | Responsable |
|------|---------------|:-----:|-------------|
| CRITICAL | Inspection immediate + rapport | < 4h | Electricien |
| WARNING (recurrent) | Inspection programmee | < 48h | Electricien |
| WARNING (isole) | Observation + note | < 7 jours | Gestionnaire |
| INFO | Aucune action physique | — | — |

### Fiche de validation electricien

Pour chaque alerte necessitant une visite terrain, l'electricien remplit :

```
┌────────────────────────────────────────────────────────┐
│ FICHE VALIDATION ALERTE #[ID]                          │
├────────────────────────────────────────────────────────┤
│ Date visite :          ____/____/________              │
│ Electricien :          ____________________            │
│ Habilitation :         □ B1V  □ B2V  □ BR             │
│                                                        │
│ Tableau :              ____________________            │
│ Circuit concerne :     ____________________            │
│                                                        │
│ CONSTAT TERRAIN :                                      │
│ □ Defaut confirme (type: __________________)          │
│ □ Aucun defaut apparent (faux positif)                │
│ □ Etat a surveiller (pas de defaut immediat)          │
│ □ Impossible a determiner                             │
│                                                        │
│ MESURES PRISES :                                       │
│ Temperature connexion : ____°C (thermographie)        │
│ Resistance isolement :  ____MΩ (megohmmetre)          │
│ Serrage : □ Verifie  □ Reserre  □ Non applicable      │
│                                                        │
│ ACTION CORRECTIVE :                                    │
│ □ Aucune (surveillance)                               │
│ □ Resserrage connexion                                │
│ □ Remplacement composant                              │
│ □ Mise hors service circuit                           │
│ □ Autre : __________________________                  │
│                                                        │
│ COMMENTAIRE :                                          │
│ _____________________________________________________  │
│                                                        │
│ PHOTO JOINTE : □ Oui  □ Non                           │
│                                                        │
│ Signature : ____________________                      │
└────────────────────────────────────────────────────────┘
```

### Correspondance fiche → systeme

| Champ fiche | Champ systeme | Effet sur scoring |
|-------------|--------------|-------------------|
| Defaut confirme | `InterventionResult.DEFECT_CONFIRMED` | Renforcement positif (lower threshold) |
| Faux positif | `InterventionResult.FALSE_POSITIVE` | Renforcement negatif (raise threshold) |
| A surveiller | `InterventionResult.UNDER_OBSERVATION` | Neutre (pas d'ajustement) |
| Impossible a determiner | `InterventionResult.INCONCLUSIVE` | Neutre |

---

## 8. Protocole de confirmation electricien

### Processus dans l'application

```
Alerte generee
    │
    ▼
Notification electricien (push/SMS si CRITICAL)
    │
    ▼
Electricien ouvre intervention sur mobile/web
    │
    ▼
┌───────────────────────────────┐
│ CHECKLIST PRE-INTERVENTION    │
│ □ EPI portes                  │
│ □ Habilitation valide         │
│ □ Consignation si necessaire  │
│ □ Outil de mesure present     │
└───────────────────────────────┘
    │
    ▼
Diagnostic terrain (mesures + observations)
    │
    ▼
Saisie diagnostic dans l'application
    │
    ▼
┌───────────────────────────────┐
│ RESULTAT                      │
│ ○ Defaut confirme → type      │
│ ○ Faux positif                │
│ ○ A surveiller                │
│ ○ Inconclusive                │
└───────────────────────────────┘
    │
    ▼
Completion intervention
    │
    ▼
Evenement publie (Kafka)
    │
    ├── ElectricalDefectConfirmedEvent
    │   └── Scoring: baisser seuil pour ce pattern
    │
    └── FalsePositiveConfirmedEvent
        └── Scoring: monter seuil pour ce pattern
```

### Regles metier de confirmation

| Regle | Implementation | Verification |
|-------|---------------|-------------|
| Diagnostic obligatoire avant completion | `Intervention.complete()` leve exception si `diagnostic == null` | Test unitaire existant (PASS) |
| Photo obligatoire pour defaut confirme | Champ `attachments` requis si `result == DEFECT_CONFIRMED` | A implementer MVP 3 |
| Electricien ne complete que ses interventions | Filtrage par `assignedElectricianId == currentUser` | Corrige dans audit MVP 2 |
| Double validation pour CRITICAL | TENANT_ADMIN doit valider apres electricien | A implementer MVP 3 |
| Delai max pour confirmation | SLA configurable (4h CRITICAL, 48h WARNING) | Existant dans SlaPolicy |

### Metriques de qualite electricien

| Metrique | Calcul | Alerte si |
|----------|--------|-----------|
| Temps moyen de reponse | Avg(intervention.startedAt - alert.createdAt) | > SLA |
| Taux de completion dans SLA | Interventions completees a temps / total | < 80% |
| Qualite diagnostics | Longueur observations > 20 chars | Diagnostics trop courts |
| Taux de faux positifs par electricien | FP confirmes / total interventions | Variance significative entre electriciens |

---

## 9. Feedback loop pour ameliorer le scoring

### Architecture du feedback loop

```
┌─────────────────────────────────────────────────────────────────┐
│  FEEDBACK LOOP                                                   │
│                                                                   │
│  ┌─────────┐     ┌──────────┐     ┌───────────────┐            │
│  │ Alerte  │────▶│Interven- │────▶│ Confirmation  │            │
│  │ generee │     │tion      │     │ electricien   │            │
│  └─────────┘     └──────────┘     └───────┬───────┘            │
│                                            │                     │
│                                            ▼                     │
│  ┌───────────────────────────────────────────────────────┐      │
│  │  EVENT: DefectConfirmed / FalsePositive               │      │
│  └───────────────────────────────────┬───────────────────┘      │
│                                      │                           │
│         ┌────────────────────────────┼────────────────────┐      │
│         ▼                            ▼                    ▼      │
│  ┌──────────────┐  ┌────────────────────┐  ┌─────────────────┐ │
│  │ Ajustement   │  │ Dataset ML enrichi │  │ Metriques       │ │
│  │ seuils stats │  │ (etiquettes)       │  │ precision/recall │ │
│  └──────────────┘  └────────────────────┘  └─────────────────┘ │
│         │                    │                       │            │
│         ▼                    ▼                       ▼            │
│  ┌──────────────┐  ┌────────────────────┐  ┌─────────────────┐ │
│  │ Scoring v2   │  │ Model retrain      │  │ Alertes ajustees│ │
│  │ (seuils mis  │  │ (hebdomadaire)     │  │ (moins de FP)   │ │
│  │  a jour)     │  └────────────────────┘  └─────────────────┘ │
│  └──────────────┘                                                │
└─────────────────────────────────────────────────────────────────┘
```

### Mecanisme d'ajustement statistique (existant)

| Signal | Effet sur seuil z-score | Temporalite |
|--------|:-----------------------:|:-----------:|
| Defaut confirme, meme pattern | Seuil × 0.9 (plus sensible) | Immediat |
| Faux positif, meme pattern | Seuil × 1.15 (moins sensible) | Immediat |
| 3 FP consecutifs meme device | Seuil × 1.3 pour ce device | Immediat |
| 10 VP sur meme type de defaut | Seuil global ajuste (population) | Hebdomadaire |

### Mecanisme ML (a implementer MVP 3)

| Etape | Frequence | Donnees | Output |
|-------|:---------:|---------|--------|
| 1. Export dataset | Quotidien | Telemetrie + labels electricien | Parquet/CSV vers S3 |
| 2. Feature engineering | Hebdomadaire | Rolling stats (1h, 6h, 24h, 7j) | Feature store |
| 3. Train/eval model | Hebdomadaire | Train 80% / Test 20% | Metriques AUC, F1 |
| 4. Deploy si meilleur | Conditionnel | F1 > statistique + 5% | Nouveau model artifact |
| 5. Shadow mode | Continu | Predictions ML en parallele stats | Comparaison sans impact |
| 6. Promotion | Decision humaine | ML consistamment meilleur sur 30 jours | Remplacement stats |

### Criteres de promotion ML

- F1 score ML > F1 score statistique sur 30 jours consecutifs
- Precision ML > 80% (pas trop de faux positifs)
- Recall ML > 85% (pas de defauts manques)
- Latence prediction < 500ms (P99)
- Aucune regression sur types de defauts majeurs (MICRO_ARC, TEMPERATURE_RISE)
- Validation humaine par data scientist

---

## 10. Roadmap MVP 3

### Sprint 3.0 — Fondations (4 semaines)

| Tache | Effort | Dependance |
|-------|:------:|:----------:|
| Keycloak realm complet (export JSON, script init, 3 tenants pilote) | 1 sem | — |
| Infrastructure cloud Terraform (K8s, PostgreSQL, Kafka, Redis) | 2 sem | Choix cloud |
| Helm charts pour 11 services + infra monitoring | 2 sem | Terraform |
| CI/CD deploy pipeline (ArgoCD ou GitHub Actions → K8s) | 1 sem | Helm |
| DNS + TLS (wildcard cert) | 2 jours | Cloud |
| Backups automatises + test restore | 3 jours | Cloud |

### Sprint 3.1 — Prototype Capteur (6 semaines)

| Tache | Effort | Dependance |
|-------|:------:|:----------:|
| Selection composants (CT, T°, MCU, comm.) | 2 sem | Partenaire hardware |
| Schema PCB v1 (breadboard) | 2 sem | Composants |
| Firmware MQTT (ESP32/STM32) | 3 sem | PCB |
| Protocole provisioning (cert X.509, enrollment) | 1 sem | Firmware |
| Tests banc de labo (3 prototypes) | 2 sem | Firmware |
| Validation payload reel vs simulateur | 3 jours | Tests labo |
| Documentation hardware (BOM, schemas, procedure) | 3 jours | Tests |

### Sprint 3.2 — Dataset & Calibration (4 semaines)

| Tache | Effort | Dependance |
|-------|:------:|:----------:|
| Module export telemetrie → Parquet/CSV | 1 sem | — |
| Feature engineering pipeline (rolling stats) | 2 sem | Export |
| Dataset synthese augmente (scenarios avances) | 1 sem | — |
| Calibration seuils sur donnees labo | 2 sem | Sprint 3.1 |
| Endpoint feedback electricien enrichi (photo, mesures) | 1 sem | — |
| Report terrain (format PDF enrichi pour pilote) | 1 sem | — |

### Sprint 3.3 — Pilote Laboratoire (4 semaines)

| Tache | Effort | Dependance |
|-------|:------:|:----------:|
| Installation 5 capteurs sur banc electrique reel | 3 jours | Sprint 3.1 |
| Injection defauts connus (loose connection, overload) | 2 sem | Installation |
| Mesure precision/recall vs defauts injectes | 1 sem | Injection |
| Ajustement algorithmes (seuils, poids scoring) | 2 sem | Mesures |
| Validation heartbeat/offline detection reelle | 3 jours | Installation |
| Rapport labo (avant/apres calibration) | 3 jours | Ajustement |

### Sprint 3.4 — Deploiement Pilote Terrain (6 semaines)

| Tache | Effort | Dependance |
|-------|:------:|:----------:|
| Signature contrat site pilote (2 batiments) | — | Business |
| Installation 10 capteurs (2 batiments × 5) | 2 jours | Contrat + Sprint 3.3 |
| Periode baseline (30 jours, alertes desactivees) | 30 jours | Installation |
| Formation electricien partenaire (app + protocole) | 1 jour | Installation |
| Formation gestionnaire (dashboard + rapports) | 0.5 jour | Installation |
| Activation alertes (jour 31) | — | Baseline |
| Monitoring quotidien (review alertes, heartbeats) | Continu | Activation |

### Sprint 3.5 — ML v1 & Feedback (6 semaines)

| Tache | Effort | Dependance |
|-------|:------:|:----------:|
| Collecte donnees terrain (3 mois minimum) | Continu | Sprint 3.4 |
| Isolation Forest sur telemetrie (unsupervised) | 2 sem | Donnees terrain |
| Autoencoder reconstruction error (alternative) | 2 sem | Donnees terrain |
| Shadow mode (predictions ML en parallele sans action) | 1 sem | Model |
| Dashboard ML metrics (precision, recall, F1 vs stats) | 1 sem | Shadow |
| Decision promotion (si ML > stats sur 30 jours) | Decision | Metrics |

### Sprint 3.6 — Scaling & Ameliorations (4 semaines)

| Tache | Effort | Dependance |
|-------|:------:|:----------:|
| Extension a 10 batiments (50 capteurs) | 2 sem | Validation Sprint 3.4 |
| Load testing K6 (100 devices simultanes) | 1 sem | Extension |
| PWA mobile (vue alertes, confirmation electricien) | 3 sem | — |
| Rapport pilote 3 mois (KPIs, ROI, recommandations) | 1 sem | Donnees 3 mois |

### Timeline globale

```
Mois     1    2    3    4    5    6    7    8    9    10   11   12
         ├────┼────┼────┼────┼────┼────┼────┼────┼────┼────┼────┤
Sprint   3.0  │3.1      │3.2 │3.3      │3.4           │3.5      │3.6
         Cloud│Hardware  │Data│Labo     │Terrain       │ML       │Scale
              │Firmware  │    │         │  Baseline 30j│         │
              │          │    │         │  │   Alertes │         │
              │          │    │         │  │   actives │         │
```

---

## 11. Risques techniques

| # | Risque | Probabilite | Impact | Phase | Mitigation |
|---|--------|:-----------:|:------:|-------|-----------|
| T1 | EMI perturbe les mesures dans le tableau | Haute | Donnees inexploitables | Labo | Blindage, filtrage analogique, validation sur banc avec perturbateurs |
| T2 | Derive capteur non detectee | Moyenne | Fausse securite | Terrain | Calibration croisee entre capteurs, auto-diagnostic firmware |
| T3 | WiFi instable dans les locaux techniques | Haute | Pertes de donnees | Terrain | Fallback 4G, buffer local 24h sur gateway, QoS MQTT 1 |
| T4 | Baseline instable (saisons, travaux) | Moyenne | Faux positifs saisonniers | Terrain | Baseline adaptive fenetre glissante 30j, detection de changement de regime |
| T5 | Latence Kafka sous charge (100 devices) | Faible | Alertes retardees | Scale | Partitionnement 6+, monitoring lag, auto-scaling consumers |
| T6 | PostgreSQL full (time-series volume) | Moyenne | Arret ingestion | Terrain | Retention policy (90j raw, aggregats permanents), monitoring disk |
| T7 | Firmware OTA echoue | Faible | Capteur bricked | Terrain | A/B partitions firmware, rollback automatique, watchdog hardware |
| T8 | Modele ML degrade apres deploy | Moyenne | Plus de FP qu'avec stats | ML | Shadow mode obligatoire 30j, rollback automatique si F1 < baseline |
| T9 | Capteur offline non detecte (heartbeat perdu) | Faible | Fausse securite | Terrain | Heartbeat 60s, alerte a 3min, escalade a 15min, notification gestionnaire |
| T10 | Incompatibilite payload firmware vs ingestion | Moyenne | Donnees rejetees | Labo | Schema versioning, validation labo, tests d'integration firmware ↔ service |

---

## 12. Risques securite

| # | Risque | Probabilite | Impact | Phase | Mitigation |
|---|--------|:-----------:|:------:|-------|-----------|
| S1 | Capteur compromis (firmware malveillant) | Faible | Donnees falsifiees, pivot reseau | Terrain | Secure boot, firmware signing, element securise (TPM/ATECC608) |
| S2 | MQTT broker expose sur Internet | Moyenne | Injection de fausses donnees | Cloud | mTLS obligatoire, ACL par device, broker dans VPC prive |
| S3 | Certificat device vole (extraction firmware) | Moyenne | Usurpation d'identite device | Terrain | Hardware crypto (ATECC608), rotation certificats, revocation CRL |
| S4 | Attaque supply chain (composant hardware) | Faible | Backdoor materielle | Production | Sourcing composants certifies, audit fournisseurs |
| S5 | Man-in-the-middle WiFi local | Moyenne | Interception donnees | Terrain | TLS 1.3 end-to-end (device → broker), certificate pinning |
| S6 | Denial of Service (flood MQTT) | Moyenne | Plateforme indisponible | Cloud | Rate limiting par device, throttling broker, auto-scaling |
| S7 | Acces physique au capteur | Haute | Manipulation, vol | Terrain | Tamper detection (switch/accelerometre), alerte si ouvert |
| S8 | Donnees terrain → vie privee (RGPD) | Moyenne | Non-conformite legale | Terrain | Pas de donnees personnelles dans telemetrie, consentement site |

---

## 13. Risques reglementaires

| # | Risque | Probabilite | Impact | Phase | Mitigation |
|---|--------|:-----------:|:------:|-------|-----------|
| R1 | Non-conformite IEC 61439 | Haute | Interdit de vendre/installer | Production | Engagement labo certifie des le prototype, design for compliance |
| R2 | Responsabilite si incident malgre surveillance | Moyenne | Poursuites judiciaires | Terrain | Clause contractuelle non-substitution, assurance RC Pro pilote |
| R3 | Changement reglementation (obligation capteurs) | Faible | Accelere le marche (opportunite) | Commercial | Veille reglementaire active, lobbying via federation |
| R4 | RGPD : donnees de consommation = donnees personnelles | Moyenne | Mise en demeure CNIL | Terrain | Anonymisation, pas de lien direct personne ↔ circuit, DPO designe |
| R5 | Directive RED (radio) si WiFi/BLE | Haute | Pas de marquage CE | Production | Tests conformite RED des le prototype, choix tech conforme |
| R6 | Non-conformite CEM (emissions/immunite) | Haute | Echec marquage CE | Production | Tests EMC en labo accredite pendant phase proto, blindage |
| R7 | Installation par non-habilite | Faible | Responsabilite entreprise | Terrain | Exigence habilitation B2V, checklist pre-intervention |
| R8 | Obsolescence norme pendant certification | Faible | Reprise dossier | Production | Suivi comites techniques AFNOR/IEC |

---

## 14. Risques UX/adoption

| # | Risque | Probabilite | Impact | Phase | Mitigation |
|---|--------|:-----------:|:------:|-------|-----------|
| U1 | Trop de faux positifs → fatigue alertes | Haute | Gestionnaires ignorent les alertes | Terrain | Periode baseline 30j, seuils conservateurs au debut, ajustement progressif |
| U2 | Interface trop technique pour gestionnaires | Moyenne | Non-utilisation du dashboard | Terrain | UX simplifiee (score 0-100, couleurs, langage non technique), test utilisateur |
| U3 | Electriciens refusent l'outil (vu comme controle) | Moyenne | Pas de feedback terrain | Terrain | Co-conception avec electricien, benefice montre (planification vs urgence) |
| U4 | Temps de reponse trop long (>3s) | Faible | Frustration utilisateur | Cloud | Cache Redis, CDN, lazy loading, pagination SQL |
| U5 | Application mobile absente | Haute | Electricien n'a pas acces en intervention | Terrain | PWA responsive (marche sur mobile), puis native MVP 3.6 |
| U6 | Rapports PDF non compris par assureurs | Moyenne | Pas d'adoption assureurs | Terrain | Format co-designe avec assureur, terminologie normee |
| U7 | Pas de valeur percue (aucun incident evite en pilote) | Haute | ROI non demontrable | Terrain | Metrique "risque detecte" meme sans incident, comparaison avec inspections manuelles |
| U8 | Gestionnaire ne configure pas les seuils | Haute | Seuils par defaut inadaptes | Terrain | Seuils auto-calcules (baseline), configuration optionnelle, defaults intelligents |

---

## 15. Actions prioritaires

### Immediat (semaine 1-2)

| # | Action | Owner | Blocage si non fait |
|---|--------|-------|:-------------------:|
| 1 | Choisir le cloud provider (AWS/GCP/Azure) | CTO/Tech Lead | Bloque toute l'infra |
| 2 | Identifier 2-3 partenaires hardware potentiels | Business | Bloque Sprint 3.1 |
| 3 | Creer Terraform bootstrap (VPC, K8s, DNS) | DevOps | — |
| 4 | Deployer Keycloak avec realm pyrosense | Backend dev | — |
| 5 | Identifier 1 site pilote (gestionnaire immobilier) | Business | Bloque Sprint 3.4 |

### Court terme (semaine 3-6)

| # | Action | Owner | Blocage si non fait |
|---|--------|-------|:-------------------:|
| 6 | Signer partenariat hardware (labo/startup/univ) | Business | Bloque hardware |
| 7 | Commander composants (CT, MCU, breadboard) | Hardware | Bloque firmware |
| 8 | Deployer plateforme sur cloud (Helm + CI/CD) | DevOps | Bloque terrain |
| 9 | Load test 100 devices simules sur cloud | Backend | Risque perf non quantifie |
| 10 | Rediger contrat pilote (non-substitution, RGPD) | Juridique | Bloque terrain |

### Moyen terme (mois 2-4)

| # | Action | Owner | Blocage si non fait |
|---|--------|-------|:-------------------:|
| 11 | Firmware MQTT v1 fonctionnel | Firmware dev | Bloque labo |
| 12 | 3 prototypes assembles | Hardware | Bloque labo |
| 13 | Validation banc de test (labo) | Firmware + Backend | Bloque terrain |
| 14 | Seuils calibres sur donnees labo | Data + Backend | Faux positifs terrain |
| 15 | Formation electricien pilote | Tous | Pas de feedback |

### Long terme (mois 4-12)

| # | Action | Owner | Delivrable |
|---|--------|-------|-----------|
| 16 | Installation terrain (10 capteurs, 2 batiments) | Electricien + IT | Capteurs operationnels |
| 17 | Collecte donnees 3 mois | Automatique | Dataset 50+ labels |
| 18 | ML v1 (Isolation Forest ou Autoencoder) | Data scientist | Model en shadow mode |
| 19 | Extension 50 capteurs si KPIs OK | Tous | Scale pilote |
| 20 | Demarrer pre-assessment certification | Business + Labo | Dossier IEC |

---

## Chemin critique

```
[Partenaire HW] ──▶ [Composants] ──▶ [Firmware] ──▶ [Labo] ──▶ [Terrain]
     Sem 1-4           Sem 3-6        Sem 5-10     Sem 10-14    Sem 14+

[Cloud provider] ──▶ [Terraform] ──▶ [Deploy] ──▶ [Load test]
     Sem 1              Sem 2-4        Sem 4-5      Sem 5-6

[Site pilote ID] ──▶ [Contrat] ──▶ [Installation]
     Sem 1-4           Sem 4-8        Sem 14+
```

**Chemin critique : la selection du partenaire hardware determine tout le calendrier. Sans hardware, pas de labo. Sans labo, pas de terrain. Sans terrain, pas de donnees. Sans donnees, pas de ML.**

---

## Budget estimatif MVP 3

| Poste | Budget (EUR) | Phase |
|-------|:------------:|:-----:|
| Cloud (12 mois) | 6 000 – 12 000 | 3.0 – 3.6 |
| Hardware prototypes (10 modules) | 5 000 – 8 000 | 3.1 |
| Electricien installation + validation | 3 000 – 5 000 | 3.3 – 3.4 |
| Data scientist (6 mois, temps partiel) | 15 000 – 25 000 | 3.5 |
| Certification pre-assessment | 5 000 – 10 000 | 3.6 |
| Juridique (contrat, assurance pilote) | 3 000 – 5 000 | 3.4 |
| **Total MVP 3** | **37 000 – 65 000** | **12 mois** |

---

## Definition of Done — MVP 3

Le MVP 3 est considere comme reussi si :

1. 10 capteurs fonctionnent en conditions reelles depuis > 3 mois
2. Uptime capteurs > 95%
3. Taux de faux positifs < 20% (cible < 10% en fin de pilote)
4. Au moins 1 defaut reel detecte avant incident
5. Dataset etiquete > 500 echantillons (dont 50+ anomalies confirmees)
6. Electricien valide > 80% des alertes dans le SLA
7. Gestionnaire utilise le dashboard au moins 3x/semaine
8. Model ML en shadow mode (meme si non promote)
9. Aucun incident securite sur site pilote
10. Rapport pilote 3 mois produit avec KPIs mesures
