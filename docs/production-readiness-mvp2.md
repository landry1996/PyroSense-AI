# MVP 2 - Checklist Production Readiness

## Classification

| Niveau | Signification | Statut actuel |
|--------|---------------|:-------------:|
| PROTOTYPE | Code fonctionnel, donnees simulees, pas de hardware | **ICI** |
| PILOTE | 10 capteurs reels, 1 batiment, supervision 24/7 | A faire |
| PRE-PRODUCTION | 100 capteurs, certification en cours, cloud manage | A faire |
| PRODUCTION | Certifie, HA, DR, support commercial | A faire |

---

## Checklist : Prototype → Pilote Terrain

### Infrastructure

| # | Item | Statut | Bloquant |
|---|------|:------:|:--------:|
| 1 | Cloud provider selectionne (AWS/GCP/Azure) | A FAIRE | Oui |
| 2 | Kubernetes cluster manage (EKS/GKE/AKS) | A FAIRE | Oui |
| 3 | PostgreSQL manage (RDS/Cloud SQL) avec backups | A FAIRE | Oui |
| 4 | Redis manage (ElastiCache/Memorystore) | A FAIRE | Oui |
| 5 | Kafka manage (MSK/Confluent) ou Redpanda | A FAIRE | Oui |
| 6 | Stockage objets pour PDFs (S3/GCS) | A FAIRE | Oui |
| 7 | CDN pour frontend Angular | A FAIRE | Non |
| 8 | DNS + TLS (Let's Encrypt ou ACM) | A FAIRE | Oui |
| 9 | VPN/bastion pour acces admin | A FAIRE | Oui |

### Securite

| # | Item | Statut | Bloquant |
|---|------|:------:|:--------:|
| 10 | Audit de securite professionnel (pentest) | A FAIRE | Oui |
| 11 | Keycloak realm configure avec vrais utilisateurs | A FAIRE | Oui |
| 12 | Rotation des secrets (Vault/Secrets Manager) | A FAIRE | Oui |
| 13 | mTLS entre services en cluster | A FAIRE | Non |
| 14 | Network policies Kubernetes | A FAIRE | Oui |
| 15 | WAF devant le gateway | A FAIRE | Non |
| 16 | RGPD : consentement + registre de traitement | A FAIRE | Oui |
| 17 | Politique de retention des donnees personnelles | A FAIRE | Oui |

### Hardware / IoT

| # | Item | Statut | Bloquant |
|---|------|:------:|:--------:|
| 18 | Selection capteurs (pince ampero, sonde T°, detecteur arc) | A FAIRE | Oui |
| 19 | Selection gateway IoT (ESP32/STM32/RPi) | A FAIRE | Oui |
| 20 | Firmware MQTT developpe et teste | A FAIRE | Oui |
| 21 | Protocole de provisioning securise (certificats device) | A FAIRE | Oui |
| 22 | Tests sur banc electrique en labo | A FAIRE | Oui |
| 23 | Calibration algorithmes sur donnees reelles | A FAIRE | Oui |

### Certification

| # | Item | Statut | Bloquant |
|---|------|:------:|:--------:|
| 24 | Dossier IEC 61439 (appareillage) | A FAIRE | Oui (prod) |
| 25 | Conformite NF C 15-100 (installations basse tension) | A FAIRE | Oui (prod) |
| 26 | Marquage CE (directive CEM + basse tension) | A FAIRE | Oui (prod) |
| 27 | Tests thermiques / EMC | A FAIRE | Oui (prod) |
| 28 | Assurance RC Pro | A FAIRE | Oui (prod) |

### Fiabilite

| # | Item | Statut | Bloquant |
|---|------|:------:|:--------:|
| 29 | Haute disponibilite (multi-AZ) | A FAIRE | Non (pilote) |
| 30 | Disaster recovery (RPO < 1h, RTO < 4h) | A FAIRE | Non (pilote) |
| 31 | Tests de charge (1000 devices, 10 batiments) | A FAIRE | Non (pilote) |
| 32 | Chaos testing (service down, network partition) | A FAIRE | Non (pilote) |
| 33 | Runbooks operationnels (incidents, escalation) | A FAIRE | Oui |
| 34 | On-call rotation + PagerDuty/OpsGenie | A FAIRE | Oui |

### Logiciel (deja fait ou en cours)

| # | Item | Statut | Notes |
|---|------|:------:|-------|
| 35 | Architecture hexagonale tous services | FAIT | 11 services |
| 36 | Tests unitaires domain >95% | FAIT | 863+ tests backend |
| 37 | Tests d'integration Kafka/PostgreSQL/Redis | FAIT | Testcontainers |
| 38 | Tests securite (tenant isolation, RBAC) | FAIT | 19+ tests isolation |
| 39 | Tests ArchUnit (hexagonal, naming) | FAIT | 87+ regles |
| 40 | Observabilite (metriques, tracing, logs) | FAIT | Micrometer + OTEL |
| 41 | Event-driven integration (Kafka) | FAIT | 6 topics, DLQ |
| 42 | Alertes Prometheus | FAIT | 21 regles |
| 43 | Frontend Angular complet | FAIT | 12 ecrans |
| 44 | Documentation technique | FAIT | 25+ documents |
| 45 | Docker Compose local | FAIT | Profil mvp2 |
| 46 | CI/CD GitHub Actions | FAIT | 7 jobs |

---

## Risques Techniques Identifies

| Risque | Impact | Probabilite | Mitigation |
|--------|--------|:-----------:|-----------|
| Faux positifs excessifs (>5%) | Perte confiance utilisateurs | Moyenne | Calibration sur donnees reelles, feedback loop false positive |
| Donnees simulees != donnees reelles | Seuils detection inadaptes | Haute | Phase labo obligatoire avant pilote |
| Latence Kafka en charge | Alertes retardees | Faible | Partitionnement 6, monitoring lag |
| PostgreSQL single-instance | Perte donnees si crash | Moyenne | Managed DB avec replicas pour pilote |
| Keycloak non configure | Pas d'auth en pilote | Haute | Configurer realm avant deploiement |
| ML absent (methodes statistiques) | Detection limitee patterns complexes | Moyenne | Suffisant pour MVP, ML prevu MVP 3 |
| Pas de HA | Indisponibilite si un service tombe | Moyenne | Acceptable pour pilote 10 capteurs |

---

## Distinction Claire

### Ce que le MVP 2 prouve

- L'architecture microservices fonctionne de bout en bout
- Les flux event-driven sont fiables (Kafka, DLQ, idempotence)
- L'interface utilisateur est complete et utilisable
- La securite multi-tenant est appliquee a chaque couche
- Les rapports PDF sont professionnels et juridiquement cadres
- Les notifications sont routees selon les preferences utilisateur

### Ce que le MVP 2 ne prouve PAS

- Que les algorithmes de detection fonctionnent sur des donnees electriques reelles
- Que le systeme supporte 1000+ capteurs en production
- Que le hardware IoT est fiable dans un tableau electrique
- Que le systeme respecte les normes electriques en vigueur
- Que les temps de reponse sont acceptables sous charge reelle
- Que le modele commercial est viable

---

## Prochaine Etape Recommandee

**Mois 1-3 : Labo** — Selectionner capteurs, developper firmware, valider sur banc electrique avec donnees reelles. Calibrer les seuils de detection.

**Mois 4-6 : Pilote 10 capteurs** — Deployer sur 1 batiment reel. Monitorer 24/7. Ajuster faux positifs < 5%. Collecter donnees pour ML.

Voir `docs/pilot-transition-plan.md` pour le plan detaille.
