# PyroSense AI — Plan de Passage MVP vers Pilote Terrain

## Avertissement

Bien distinguer les 5 niveaux de maturite :
- **Simulateur** : donnees synthetiques, pas de capteurs reels (ETAT ACTUEL)
- **Prototype labo** : capteurs reels sur banc de test, environnement controle
- **Pilote reel** : capteurs en situation reelle (batiment habite/occupe), donnees terrain
- **Produit certifie** : conforme IEC 61439, NF C 15-100, marquage CE
- **Produit commercial** : distribution, support, garantie, assurance RC Pro

---

## 1. Ce qui est pret cote logiciel

Le MVP logiciel actuel comprend :

- 10 microservices implementes (ingestion, analysis, scoring, alerting, notification, maintenance, reporting, identity, device, gateway)
- Architecture hexagonale, DDD, clean code
- Multi-tenancy, RBAC (8 roles, 34 permissions)
- Kafka event-driven pipeline (telemetry -> analysis -> scoring -> alerting -> notification)
- TimescaleDB time-series storage avec continuous aggregates
- Detection statistique d'anomalies (algorithme de Welford, z-score, detection de micro-arcs, tendances de temperature)
- Moteur de scoring de risque (formule composite ponderee)
- Deduplication d'alertes, escalade SLA
- Generation de rapports PDF
- Simulateur IoT (7 scenarios)
- Stack de developpement local Docker Compose
- CI/CD (GitHub Actions, 6 jobs)
- 500+ tests (unitaires, integration, architecture, securite, performance)
- Documentation complete (20 documents)
- Audit realise, correctifs critiques appliques

**Statut : Software ready for pilot integration** — necessite du hardware reel + deploiement cloud.

---

## 2. Ce qui reste a faire cote hardware

A faire (necessite un partenaire en electronique) :

- Selection des capteurs de mesure electrique (pinces amperemetriques CT, diviseurs de tension, sondes de temperature, capteurs de vibration, detecteurs d'arc electrique)
- Selection du microcontroleur/gateway (ESP32, STM32, Raspberry Pi, ou automate industriel PLC)
- Conception PCB pour le module de mesure
- Developpement firmware MQTT
- Conception de l'alimentation (rail DIN, batterie de secours)
- Conception du boitier (indice IP adapte a l'environnement tableau electrique)
- Tests EMC (compatibilite electromagnetique)
- Tests thermiques
- Methode d'installation physique dans les tableaux electriques
- Procedure de calibration des capteurs
- BOM (Bill of Materials) hardware

**Note : Ceci necessite un partenaire en genie electrique. L'equipe logiciel ne peut pas faire cela seule.**

---

## 3. Ce qui reste a faire cote IA reelle

Etat actuel honnete :

- **Actuellement** : methodes statistiques uniquement (z-score, baseline Welford, lissage exponentiel)
- **Aucun dataset reel n'existe encore** (besoin de 6-12 mois de donnees terrain minimum)
- **Aucun modele ML entraine** (les ports existent : MachineLearningInferencePort, RiskModelPort — les deux sont des stubs NoOp)

Etapes necessaires :

1. Collecter des donnees de telemetrie reelles depuis le pilote (phase 1 : collecte de donnees uniquement)
2. Etiqueter les anomalies avec le retour des electriciens (ground truth)
3. Construire un dataset (minimum 10 000 echantillons etiquetes couvrant tous les types de defauts)
4. Entrainer des modeles de base (Random Forest, XGBoost pour donnees tabulaires)
5. Evaluer par rapport a la baseline statistique (doit la depasser significativement pour justifier la complexite)
6. Integrer via les ports existants (aucun changement d'architecture necessaire)
7. Pipeline d'apprentissage continu (feedback des interventions de maintenance)

**Timeline : le ML n'apporte de la valeur qu'apres 6-12 mois de collecte de donnees reelles.**

**Les methodes statistiques restent la detection primaire tant que le ML n'a pas prouve sa superiorite.**

---

## 4. Ce qui reste a faire cote certification

- **IEC 61439** (ensembles d'appareillage electrique) — s'applique a l'installation physique
- **NF C 15-100** (norme francaise des installations electriques)
- **EN 50491-5-1** (communication HBES dans les batiments)
- **EN 62606** (dispositifs de detection d'arc electrique)
- **Marquage CE** (necessite conformite EMC + directive basse tension LVD)
- **Directive RED** (si communication sans fil utilisee)
- **Conformite RoHS** (hardware)
- **Enregistrement WEEE** (fin de vie electronique)
- **ISO 13849** (systemes de commande relatifs a la securite) — si le systeme declenche des actions protectrices

**Note : La certification est un processus de 12-24 mois necessitant un laboratoire certifie (ex: LCIE, Bureau Veritas, TUV).**

**Note : Impossible de vendre ou installer dans des batiments reels sans certification electrique.**

---

## 5. Ce qui reste a faire cote cybersecurite IoT

- **ETSI EN 303 645** (baseline cybersecurite IoT)
- Device firmware signing + secure boot
- TLS 1.3 obligatoire pour toute communication MQTT
- Gestion de l'identite des devices (certificats X.509 au lieu de username/password)
- Mecanisme de mise a jour firmware OTA (signe, avec rollback)
- Detection physique de manipulation (tamper detection)
- Element securise sur le hardware (TPM ou equivalent)
- Segmentation reseau (DMZ pour le broker MQTT)
- Test de penetration (OWASP IoT Top 10)
- Plan de reponse aux incidents
- Chiffrement des donnees au repos
- Rotation automatique des cles

---

## 6. Ce qui reste a faire cote cloud

- Choix du fournisseur cloud (AWS IoT Core, Azure IoT Hub, GCP Cloud IoT — ou self-hosted)
- Deploiement Kubernetes (Helm charts, ArgoCD)
- Database : managed PostgreSQL + TimescaleDB Cloud (ou self-managed avec HA)
- Kafka : service manage (Confluent Cloud, MSK) ou cluster self-hosted (3+ brokers)
- Redis : service manage avec HA (ElastiCache, Memorystore)
- Container registry (ECR, ACR, GCR)
- DNS + certificats TLS (Let's Encrypt / AWS ACM)
- CDN pour le frontend Angular
- Strategie de backup (database daily, Kafka retention, point-in-time recovery)
- Disaster recovery (multi-AZ minimum, multi-region pour production)
- Auto-scaling (HPA pour les services, KEDA pour les consumers Kafka)
- Monitoring : Datadog ou Prometheus manage + Grafana Cloud
- Estimation des couts : ~500-1500 EUR/mois pour le pilote (10 devices), ~3000-5000 EUR pour 100 devices

---

## 7. Ce qui reste a faire cote assurance/conformite

- Conformite RGPD (contrat de traitement des donnees, designation DPO)
- Assurance RC Pro (responsabilite civile professionnelle)
- Assurance produit (responsabilite produit pour la securite electrique)
- Conditions d'utilisation (terms of service)
- Clause de non-substitution (le systeme NE REMPLACE PAS les inspections electriques obligatoires)
- Politique de confidentialite (les donnees capteurs ne sont pas des donnees personnelles sauf si liees a des individus)
- Politique de retention des donnees (conforme a la legislation)
- Piste d'audit pour la conformite (qui a accede a quoi, quand)
- Assurance pour les participants au pilote terrain (mitigation du risque)

---

## 8. Plan pilote 10 capteurs

| Aspect | Detail |
|--------|--------|
| Duree | 3-6 mois |
| Site | 1-2 batiments (bureaux ou residentiel), partenaire identifie |
| Capteurs | 10 modules (5 par tableau electrique) |
| Objectif | Valider la chaine complete, collecter donnees reelles |
| Infrastructure | Cloud minimal (1 VM ou petit cluster K8s) |
| Equipe | 1 dev backend, 1 firmware dev, 1 electricien partenaire |
| Budget materiel | ~5 000-8 000 EUR (prototypes, composants, assemblage) |
| Budget cloud | ~500 EUR/mois |
| Metriques | Taux de faux positifs, detection vs inspection manuelle |
| Livrables | Dataset etiquete, rapport de correlation, retour terrain |

---

## 9. Plan pilote 100 capteurs

| Aspect | Detail |
|--------|--------|
| Duree | 6-12 mois |
| Sites | 5-10 batiments (mix tertiaire/residentiel) |
| Capteurs | 100 modules |
| Objectif | Valider scalabilite, fiabilite, premiers modeles ML |
| Infrastructure | Cluster K8s dedie, Kafka 3 brokers, PostgreSQL HA |
| Equipe | 2-3 devs, 1 data scientist, 1 DevOps, 2 electriciens |
| Budget materiel | ~30 000-50 000 EUR |
| Budget cloud | ~3 000-5 000 EUR/mois |
| Partenaires | Bailleur social ou entreprise de facilities management |
| Livrables | ML model v1, taux de detection vs faux positifs, ROI mesure |

---

## 10. Plan industrialisation 1000 capteurs

| Aspect | Detail |
|--------|--------|
| Duree | 12-24 mois (apres pilote 100) |
| Sites | 50-100 batiments |
| Capteurs | 1000+ modules (production serie) |
| Objectif | Certification, produit commercial, SaaS |
| Infrastructure | Multi-region, HA, SLA 99.9% |
| Equipe | 5-8 devs, 2 data scientists, 2 DevOps/SRE, support N1/N2 |
| Budget materiel | 200 000-500 000 EUR (moules, production serie, stockage) |
| Budget cloud | 10 000-20 000 EUR/mois |
| Certification | IEC 61439, CE, NF C 15-100 (budget ~50 000-100 000 EUR) |
| Juridique | Assurance produit, RC Pro, CGV, RGPD DPA |
| Livrables | Produit certifie, SaaS operationnel, documentation commerciale |

---

## 11. KPIs terrain a mesurer

### KPIs de detection

- Taux de vrais positifs (sensibilite) — target > 90%
- Taux de faux positifs — target < 5%
- Temps de detection avant incident — target > 48h
- Correlation score de risque vs intervention confirmee

### KPIs operationnels

- Disponibilite plateforme — target 99.5% (pilote), 99.9% (production)
- Latence ingestion (end-to-end) — target < 5s
- Uptime capteurs — target > 98%
- Taux de perte de messages MQTT — target < 0.1%

### KPIs metier

- Nombre d'interventions preventives declenchees
- Cout evite par intervention (vs reparation curative)
- ROI par batiment supervise
- Satisfaction gestionnaire (NPS)
- Temps moyen entre detection et intervention

---

## 12. Risques

| # | Risque | Probabilite | Impact | Phase |
|---|--------|-------------|--------|-------|
| R1 | Aucun partenaire hardware trouve | Moyenne | Bloquant | Prototype |
| R2 | Donnees terrain insuffisantes pour ML | Haute | Retarde ML (pas bloquant, stats fonctionnent) | Pilote |
| R3 | Taux de faux positifs trop eleve | Moyenne | Perte de confiance utilisateurs | Pilote |
| R4 | Certification refusee (non-conformite) | Moyenne | Retarde commercialisation 6-12 mois | Industrialisation |
| R5 | Incident sur site pilote (responsabilite) | Faible | Impact juridique/reputationnel | Pilote |
| R6 | Scalabilite insuffisante | Faible | Degradation service | 100+ capteurs |
| R7 | Budget insuffisant | Moyenne | Ralentit toutes les phases | Toutes |
| R8 | Reglementation change (obligation capteurs) | Faible | Accelere le marche (opportunite) | Commercial |
| R9 | Concurrent lance produit similaire | Moyenne | Pression time-to-market | Commercial |
| R10 | Panne capteur non detectee | Moyenne | Fausse securite | Pilote |

---

## 13. Mitigations

| Risque | Mitigation |
|--------|-----------|
| R1 | Contacter labs universitaires (INSA, ENSE3), incubateurs hardware, partenariat PME electricite |
| R2 | Les methodes statistiques actuelles fonctionnent sans ML. ML = amelioration, pas prerequis |
| R3 | Seuils configurables par site, periode d'apprentissage (baseline 30 jours), feedback electricien |
| R4 | Engagement precoce avec labo certifie, design for compliance des le prototype |
| R5 | Assurance dediee pilote, clause contractuelle explicite (non-substitution), consentement eclaire |
| R6 | Architecture microservices deja scalable, load tests avant deploiement |
| R7 | Prioriser pilote 10 capteurs (budget minimal ~15K EUR), chercher financement (BPI, subventions) |
| R8 | Veille reglementaire active |
| R9 | Avantage logiciel deja construit, focus sur partenariat hardware rapide |
| R10 | Heartbeat monitoring (deja implemente), alerter si capteur offline > 3 min |

---

## 14. Budget technique approximatif

### Pilote 10 capteurs (3-6 mois)

| Poste | Budget |
|-------|--------|
| Hardware prototypes (10 modules) | 5 000-8 000 EUR |
| Cloud (6 mois) | 3 000-6 000 EUR |
| Electricien installation | 2 000-4 000 EUR |
| Outils/licences | 1 000-2 000 EUR |
| **Total** | **11 000-20 000 EUR** |

### Pilote 100 capteurs (12 mois)

| Poste | Budget |
|-------|--------|
| Hardware (100 modules, petite serie) | 30 000-50 000 EUR |
| Cloud (12 mois) | 36 000-60 000 EUR |
| Equipe supplementaire (data scientist 6 mois) | 30 000-40 000 EUR |
| Installation + maintenance | 15 000-25 000 EUR |
| Certification preliminaire (pre-assessment) | 10 000-20 000 EUR |
| **Total** | **121 000-195 000 EUR** |

### Industrialisation 1000 capteurs

| Poste | Budget |
|-------|--------|
| Production serie | 200 000-500 000 EUR |
| Certification complete | 50 000-100 000 EUR |
| Cloud (annuel) | 120 000-240 000 EUR |
| Equipe (8 personnes, 12 mois) | 500 000-800 000 EUR |
| Juridique/assurance | 30 000-50 000 EUR |
| Commercial/marketing | 50 000-100 000 EUR |
| **Total** | **950 000-1 790 000 EUR** |

---

## 15. Roadmap

### Mois 1-3 : Prototype labo

- [ ] Identifier partenaire hardware / labo universitaire
- [ ] Selectionner composants (capteurs CT, MCU, communication)
- [ ] Developper firmware MQTT (compatible avec le broker existant)
- [ ] Assembler 3-5 prototypes banc de test
- [ ] Valider payload reel vs simulateur (ajuster parsing si necessaire)
- [ ] Deployer plateforme sur cloud minimal (1 VM ou petit K8s)
- [ ] Developper dashboard Angular (MVP 2 — en parallele)
- [ ] Tests en labo : valider correlation capteur reel vs anomalie connue
- [ ] Livrable : prototype fonctionnel sur banc de test

### Mois 4-6 : Pilote terrain 10 capteurs

- [ ] Installation dans 1-2 batiments reels (avec accord proprietaire)
- [ ] Periode d'apprentissage baseline (30 jours sans alertes)
- [ ] Ajustement seuils (z-score, temperature, arc) sur donnees reelles
- [ ] Mesure taux faux positifs / vrais positifs
- [ ] Intervention electricien pour validation terrain (ground truth)
- [ ] Collecte dataset etiquete (premier lot de donnees labellisees)
- [ ] Rapport pilote intermediaire
- [ ] Livrable : donnees reelles, premiers KPIs, feedback terrain

### Mois 7-12 : Scaling + ML v1

- [ ] Etendre a 50-100 capteurs (5-10 sites)
- [ ] Entrainer premier modele ML (si dataset suffisant > 5000 echantillons)
- [ ] A/B test : statistique seul vs statistique + ML
- [ ] Deploiement K8s multi-AZ
- [ ] Lancer processus de certification (IEC 61439)
- [ ] Recruter data scientist
- [ ] Dashboard Angular production-ready
- [ ] Rapports automatises pour gestionnaires
- [ ] Livrable : plateforme scalable, ML v1 ou ML roadmap ajustee

---

## Conclusion

PyroSense AI est un **logiciel mature** (architecture solide, 500+ tests, observabilite, securite). Le passage au pilote terrain depend de :

1. Un partenaire hardware (electronique + firmware)
2. Un site pilote avec accord du proprietaire
3. Un budget de ~15 000 EUR minimum pour 10 capteurs

Le logiciel est pret. Le hardware est le chemin critique.
