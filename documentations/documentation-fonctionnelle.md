# PyroSense AI Platform — Documentation Fonctionnelle

> **Version** : MVP 3 (2026-05-28)
> **Public cible** : Product owners, chefs de projet, gestionnaires immobiliers, equipes metier
> **AVERTISSEMENT** : Ce systeme est un outil d'aide a la decision. Il ne garantit PAS l'absence
> d'incendie et ne se substitue PAS aux controles reglementaires obligatoires.
> Toute installation electrique reelle doit etre realisee par un professionnel qualifie (B2V+).

---

## Table des matieres

1. [Presentation du produit](#1-presentation-du-produit)
2. [Probleme adresse](#2-probleme-adresse)
3. [Proposition de valeur](#3-proposition-de-valeur)
4. [Utilisateurs et roles](#4-utilisateurs-et-roles)
5. [Parcours utilisateur](#5-parcours-utilisateur)
6. [Fonctionnalites par module](#6-fonctionnalites-par-module)
7. [Cycle de vie d'un capteur](#7-cycle-de-vie-dun-capteur)
8. [Cycle de vie d'une alerte](#8-cycle-de-vie-dune-alerte)
9. [Cycle de vie d'une intervention](#9-cycle-de-vie-dune-intervention)
10. [Scoring de risque](#10-scoring-de-risque)
11. [Notifications](#11-notifications)
12. [Rapports](#12-rapports)
13. [Tableau de bord](#13-tableau-de-bord)
14. [Gestion multi-tenant](#14-gestion-multi-tenant)
15. [Qualite des donnees](#15-qualite-des-donnees)
16. [Pilote terrain](#16-pilote-terrain)
17. [Limites et exclusions](#17-limites-et-exclusions)
18. [Glossaire](#18-glossaire)

---

## 1. Presentation du produit

### Vision

PyroSense AI est une plateforme de **prevention predictive des incendies electriques**. Elle detecte les signes precurseurs de defauts electriques (echauffement, micro-arcs, degradation d'isolation) grace a des capteurs IoT installes sur les tableaux electriques, et alerte les gestionnaires **avant** qu'un incident ne survienne.

### Positionnement

| Dimension | PyroSense | Inspection classique |
|-----------|-----------|---------------------|
| Frequence | Continu (24/7) | 1 fois/an |
| Detection | Predictive (signes precurseurs) | Reactive (defaut visible) |
| Couverture | Tous les circuits instrumentes | Echantillon lors de la visite |
| Delai | Alerte en minutes | Rapport en semaines |
| Objectivite | Mesures quantifiees | Appreciation visuelle |

### Phase actuelle

| Phase | Statut | Perimetre |
|-------|--------|-----------|
| MVP 1 | Termine | Architecture, services core, scoring statistique |
| MVP 2 | Termine | Dashboard, notifications, rapports PDF, maintenance |
| MVP 3 | Termine | IoT reel, firmware, MQTT securise, pilote 10 capteurs |
| MVP 4 | Planifie | Machine learning, MLOps, scoring ameliore |

---

## 2. Probleme adresse

### Contexte

- **25% des incendies de batiments** sont d'origine electrique (source DGSCGC)
- Un incendie electrique cause en moyenne **50 000 EUR de degats** (hors corporel)
- Les signes precurseurs (micro-arcs, echauffements) apparaissent **des semaines avant l'incendie**
- Les controles reglementaires (NF C 15-100) sont periodiques et ne couvrent qu'un instant T

### Causes principales detectees par PyroSense

| Cause | Description | Signe precurseur |
|-------|-------------|-----------------|
| Connexion desserree | Vibrations ou cycles thermiques desserrent une borne | Temperature locale en hausse progressive |
| Micro-arcs | Arcs electriques dans un defaut d'isolant | Impulsions haute frequence detectables |
| Degradation isolant | Vieillissement, chaleur, humidite | Courant de fuite, harmoniques anormaux |
| Surcharge | Courant excessif prolonge | Echauffement + THD eleve |
| Harmoniques | Equipements electroniques polluant le reseau | THD > 8%, echauffement neutre |

### Valeur pour le gestionnaire

- **Anticipation** : 1 a 4 semaines d'avance sur un incident potentiel
- **Priorisation** : score de risque pour cibler les interventions
- **Traçabilite** : historique complet des alertes et interventions
- **Conformite** : attestation de surveillance continue
- **ROI** : eviter un incendie coute 100x plus qu'un capteur

---

## 3. Proposition de valeur

### Pour le gestionnaire immobilier

> "Je sais en temps reel quel batiment presente un risque electrique et je peux planifier une intervention AVANT l'incident."

- Dashboard avec score de risque par batiment
- Alertes automatiques avec recommandation d'action
- Historique et tendance du risque sur 30 jours
- Rapport mensuel de sante electrique

### Pour l'electricien

> "Je recois une mission precise avec le circuit concerne, le type de defaut suspecte, et les mesures capteur pour preparer mon intervention."

- Notification d'intervention avec localisation (batiment, tableau, circuit)
- Contexte : type d'anomalie, historique, score de risque
- Retour terrain : confirmer ou infirmer le defaut
- Le systeme apprend de chaque intervention

### Pour l'assureur (futur)

> "J'ai une attestation de surveillance continue qui prouve que le batiment est monitore 24/7."

- Certificat de surveillance genere automatiquement
- Taux de disponibilite des capteurs
- Historique des alertes et interventions
- Score de risque moyen sur la periode

---

## 4. Utilisateurs et roles

### Matrice des roles

| Role | Qui | Droits principaux |
|------|-----|-------------------|
| **Administrateur Plateforme** | Equipe PyroSense | Acces complet, cross-tenant, configuration globale |
| **Administrateur Tenant** | DSI / Responsable immobilier | Gestion users, batiments, configuration tenant |
| **Gestionnaire de Propriete** | Property manager | Batiments, alertes, interventions, rapports |
| **Electricien** | Technicien terrain | Interventions assignees, retour terrain |
| **Gestionnaire Devices** | IT / Maintenance | Provisioning capteurs, firmware, revocation |
| **Operateur** | Centre de surveillance | Monitoring temps reel, acquittement alertes |
| **Occupant** | Locataire, usager | Lecture seule statut securite de sa zone |
| **Support** | Helpdesk | Lecture seule pour assistance |

### Droits par ecran

| Ecran | ADMIN | MANAGER | ELECTRICIAN | OPERATOR | OCCUPANT |
|-------|-------|---------|-------------|----------|----------|
| Dashboard | Oui | Oui | Partiel | Oui | Non |
| Batiments | Oui | Oui | Ses sites | Oui | Son zone |
| Alertes | Oui | Oui | Ses alertes | Oui | Non |
| Interventions | Oui | Oui | Assignees | Vue | Non |
| Rapports | Oui | Oui | Non | Non | Non |
| Notifications | Oui | Oui | Oui | Oui | Non |
| Admin (users) | Oui | Oui | Non | Non | Non |
| Settings | Oui | Oui | Non | Non | Non |

---

## 5. Parcours utilisateur

### 5.1 Parcours "Alerte critique"

```
1. Capteur detecte echauffement anormal (temperature +5°C/h)
   │
2. Analyse de signal confirme anomalie (z-score > 3)
   │
3. Score de risque passe a 82 (CRITICAL)
   │
4. Alerte CRITICAL creee automatiquement
   │
5. Intervention URGENTE creee automatiquement
   │
6. Notification SMS + Push + Email au gestionnaire
   │
7. Notification Push a l'electricien assigne
   │
8. Gestionnaire acquitte l'alerte dans le dashboard
   │
9. Electricien realise l'intervention (resserrage borne)
   │
10. Electricien confirme le defaut via l'app (feedback)
     │
11. Scoring ajuste ses parametres (le pattern etait correct)
     │
12. Rapport d'intervention genere (PDF)
```

### 5.2 Parcours "Faux positif"

```
1. Capteur detecte pic de bruit HF (equipement voisin)
   │
2. Score de risque monte a 45 (MODERATE)
   │
3. Alerte WARNING creee
   │
4. Recommandation d'intervention proposee au manager
   │
5. Manager accepte → intervention planifiee
   │
6. Electricien intervient, ne trouve rien d'anormal
   │
7. Electricien marque "Faux positif" avec explication
   │
8. Scoring reduit la confiance pour ce type de pattern
   │
9. Seuil de detection ajuste (moins sensible pour ce cas)
   │
10. Le systeme s'ameliore : moins de faux positifs futurs
```

### 5.3 Parcours "Installation capteur"

```
1. Admin genere un claim token pour le device
   │
2. Electricien installe physiquement le capteur sur le tableau
   │
3. Capteur demarre et envoie une requete de provisioning
   │
4. Plateforme verifie le claim token → delivre credentials HMAC
   │
5. Capteur entre en phase LEARNING (7 jours)
   │
6. Baseline construite (ce qui est "normal" pour CE circuit)
   │
7. Capteur passe en mode ACTIVE (detection activee)
   │
8. Premiere telemetrie recue et validee
```

---

## 6. Fonctionnalites par module

### 6.1 Gestion des devices

| Fonctionnalite | Description |
|----------------|-------------|
| Enregistrement | Declarer un nouveau capteur avec serial, modele, firmware |
| Provisioning | Delivrer credentials HMAC via claim token single-use |
| Activation | Passer le device en mode surveillance active |
| Heartbeat | Signal de vie periodique (etat, uptime, WiFi, buffer) |
| Revocation | Desactiver immediatement un device compromis |
| Rotation credentials | Renouveler la cle HMAC (ancienne revoquee) |
| Firmware tracking | Suivi de version firmware par device |

### 6.2 Ingestion de telemetrie

| Fonctionnalite | Description |
|----------------|-------------|
| Reception MQTT | Ecoute en temps reel des messages capteurs |
| Validation protocole | Schema, signature HMAC, anti-replay, freshness |
| Validation metier | Plages physiques, coherence, qualite signal |
| Idempotency | Deduplication des messages deja traites |
| Persistence time-series | Stockage optimise pour requetes temporelles |
| Publication events | Diffusion aux services d'analyse |
| Mode drain | Tolerance etendue pour messages bufferises (72h) |

### 6.3 Analyse de signal

| Fonctionnalite | Description |
|----------------|-------------|
| Baseline learning | Apprentissage du "normal" par device (7 jours) |
| Detection z-score | Alerter si mesure s'eloigne de la baseline |
| Detection micro-arcs | Identifier les patterns d'arcs recurrents |
| Detection temperature | Montee en temperature anormale |
| Detection harmoniques | Derive du taux de distorsion harmonique |
| Lissage exponentiel | Filtrer le bruit pour reveler la tendance |

### 6.4 Scoring de risque

| Fonctionnalite | Description |
|----------------|-------------|
| Score composite | 0-100, base sur 6 facteurs ponderes |
| Explicabilite | Decomposition par facteur contributeur |
| Tendance | IMPROVING / STABLE / DEGRADING / CRITICAL |
| Prediction | Estimation du delai avant incident |
| Feedback loop | Ajustement confiance via retours terrain |

### 6.5 Alertes

| Fonctionnalite | Description |
|----------------|-------------|
| Creation automatique | Depuis score de risque > seuil |
| Deduplication | Pas de doublons pour meme device + meme type |
| Acquittement | Operateur prend en charge l'alerte |
| Resolution | Alerte fermee apres intervention |
| Faux positif | Marquage + ajustement scoring |
| Escalation | Si non traitee : FIRST (1h) → SECOND (4h) → EMERGENCY (8h) |
| SLA tracking | Temps de reponse et resolution par priorite |

### 6.6 Interventions

| Fonctionnalite | Description |
|----------------|-------------|
| Creation depuis alerte | CRITICAL auto, WARNING sur validation manager |
| Planification | Date prevue, priorite, type |
| Assignation | Affecter a un electricien |
| Diagnostic | Observations terrain avant completion |
| Completion | Resultat : defaut confirme, resolu, non trouve, faux positif |
| Annulation | Avec raison obligatoire |
| Feedback | Retour vers le scoring (amelioration continue) |
| Commentaires | Fil de discussion sur l'intervention |
| Kanban | Vue 5 colonnes (CREATED, PLANNED, ASSIGNED, IN_PROGRESS, COMPLETED) |

### 6.7 Notifications

| Fonctionnalite | Description |
|----------------|-------------|
| Multi-canal | Email, SMS, Push, Webhook, Dashboard |
| Routage par severite | INFO→Dashboard, WARNING→Email, CRITICAL→SMS+Push+Email |
| Preferences utilisateur | Choix des canaux, heures calmes, langue |
| Critical override | CRITICAL passe malgre les preferences (si configure) |
| Anti-spam | Deduplication alertFingerprint + recipientId + channel |
| Retry | 3 tentatives avec backoff exponentiel |
| Templates | 9 templates en francais, 4 canaux chacun |
| Consentement | Respect RGPD (opt-in SMS/Push) |

### 6.8 Rapports

| Type | Contenu | Periodicite |
|------|---------|-------------|
| Bilan Mensuel | KPIs, risques, alertes, interventions, recommandations | Mensuel |
| Attestation Surveillance | Periode, capteurs, disponibilite, signature SHA-256 | Sur demande |
| Rapport Alerte Critique | Nature, contexte, evaluation, actions immediates | Par alerte |
| Rapport Intervention | Resume, indicateurs, impact sur le risque | Par intervention |

Tous les rapports :
- Format PDF professionnel (A4)
- Telechargeables via token securise (single-use, 15min)
- Contiennent un disclaimer legal
- Numerotation unique (ex: MH-202605-00042)

### 6.9 Administration

| Fonctionnalite | Description |
|----------------|-------------|
| Gestion utilisateurs | CRUD users, roles, desactivation |
| Audit log | Historique de toutes les actions (qui, quoi, quand) |
| Configuration tenant | Seuils d'alerte, contacts urgence, politique notifications |
| Preferences notification | Par tenant : critical override, verification obligatoire |

---

## 7. Cycle de vie d'un capteur

```
┌────────────┐     ┌──────────────┐     ┌────────────┐
│ REGISTERED │────▶│ PROVISIONED  │────▶│   ACTIVE   │
└────────────┘     └──────────────┘     └─────┬──────┘
                                              │
                        ┌─────────────────────┼─────────────────┐
                        │                     │                 │
                        ▼                     ▼                 ▼
                 ┌────────────┐       ┌────────────┐    ┌────────────┐
                 │  OFFLINE   │       │MAINTENANCE │    │  REVOKED   │
                 │(heartbeat  │       │(temporaire)│    │ (terminal) │
                 │  manque)   │       └────────────┘    └────────────┘
                 └─────┬──────┘
                       │ (heartbeat revient)
                       ▼
                 ┌────────────┐
                 │   ACTIVE   │
                 └────────────┘
```

### Etats

| Etat | Signification | Duree typique |
|------|---------------|---------------|
| REGISTERED | Declare mais pas encore installe | Jours/semaines |
| PROVISIONED | Credentials delivres, en attente d'activation | Minutes |
| ACTIVE | Surveillance normale, telemetrie recue | Permanent (cible) |
| OFFLINE | Plus de heartbeat depuis > seuil | Temporaire |
| MAINTENANCE | Intervention planifiee sur le capteur | Heures |
| REVOKED | Desactive definitivement (compromis, decommissionne) | Terminal |

---

## 8. Cycle de vie d'une alerte

```
┌────────┐     ┌──────────────┐     ┌─────────────┐     ┌──────────┐
│  OPEN  │────▶│ ACKNOWLEDGED │────▶│ IN_PROGRESS │────▶│ RESOLVED │
└────────┘     └──────────────┘     └─────────────┘     └──────────┘
    │                                      │
    │                                      ▼
    │                              ┌────────────────┐
    └─────────────────────────────▶│ FALSE_POSITIVE │
                                   └────────────────┘
```

### Severites

| Severite | Seuil score | Reaction attendue |
|----------|-------------|-------------------|
| INFO | Score 30-44 | Surveillance renforcee, pas d'action |
| WARNING | Score 45-64 | Recommandation intervention (validation manager) |
| CRITICAL | Score 65+ | Intervention automatique, notification immediate |

### Escalation

Si une alerte n'est pas acquittee :
- **+1h** : escalation FIRST (rappel email)
- **+4h** : escalation SECOND (SMS manager)
- **+8h** : escalation EMERGENCY (appel + notification direction)

---

## 9. Cycle de vie d'une intervention

```
┌─────────┐     ┌─────────┐     ┌──────────┐     ┌─────────────┐     ┌───────────┐
│ CREATED │────▶│ PLANNED │────▶│ ASSIGNED │────▶│ IN_PROGRESS │────▶│ COMPLETED │
└─────────┘     └─────────┘     └──────────┘     └─────────────┘     └───────────┘
                                                         │
                                                         ▼
                                                  ┌───────────┐
                                                  │ CANCELLED │
                                                  │(avec raison)│
                                                  └───────────┘
```

### Resultats possibles

| Resultat | Signification | Impact scoring |
|----------|---------------|----------------|
| DEFECT_CONFIRMED | Defaut reel trouve et corrige | Boost confiance (+0.05) |
| DEFECT_NOT_RESOLVED | Defaut trouve mais non corrigible | Maintien alerte |
| NO_DEFECT_FOUND | Rien d'anormal observe | Reduction legere (-0.01) |
| FALSE_POSITIVE | Le systeme a eu tort | Reduction confiance (-0.03) |
| INCONCLUSIVE | Impossible de determiner | Neutre |
| PREVENTIVE_MAINTENANCE | Maintenance preventive realisee | Neutre |

### Regles metier

- **Diagnostic obligatoire** avant completion (observations terrain requises)
- **Annulation** : raison obligatoire non vide
- **Alerte CRITICAL** → intervention automatique (URGENT, SLA 4h reponse)
- **Alerte WARNING** → recommandation proposee au manager (accept/reject)
- **Une seule intervention par alerte** (unicite)
- **Impact risque** : riskScoreBefore vs riskScoreAfter mesure

---

## 10. Scoring de risque

### Comment le score est calcule

Le score de risque (0-100) est calcule a partir de 6 facteurs, chacun pondere selon sa correlation avec les defauts electriques reels :

| Facteur | Poids | Ce qu'il mesure |
|---------|-------|-----------------|
| Micro-arcs | 30% | Petits arcs electriques dans les connexions degradees |
| Distorsion harmonique | 20% | Pollution du reseau par les equipements |
| Temperature | 20% | Echauffement des connexions et conducteurs |
| Transitoires | 10% | Pics de courant anormaux |
| Bruit haute frequence | 10% | Signature spectrale de degradation |
| Fiabilite capteur | 10% | Penalite si le capteur est instable/offline |

### Ce que signifie le score

| Score | Niveau | Signification | Action recommandee |
|-------|--------|---------------|-------------------|
| 0-29 | LOW | Installation saine | Surveillance normale |
| 30-59 | MODERATE | Signes a surveiller | Vigilance accrue, pas d'urgence |
| 60-79 | HIGH | Risque eleve | Planifier une intervention sous 7 jours |
| 80-100 | CRITICAL | Danger potentiel | Intervention immediate (< 24h) |

### Explicabilite

Chaque score est accompagne :
- De la **contribution de chaque facteur** (ex: "Temperature: 18/20")
- D'une **tendance** (amelioration, stable, degradation)
- D'une **prediction** ("Incident estime dans 5-10 jours si tendance maintenue")
- D'une **recommandation** ("Planifier inspection thermographique")

### Apprentissage continu

Le scoring s'ameliore a chaque intervention grace au feedback terrain :
- Defaut confirme → le systeme augmente sa confiance pour ce pattern
- Faux positif → le systeme reduit sa sensibilite pour ce pattern
- Apres 50+ retours terrain : seuils optimises pour chaque site

---

## 11. Notifications

### Regles de routage

| Severite de l'alerte | Canaux actives | Delai |
|---------------------|----------------|-------|
| INFO | Dashboard uniquement | Immmediat |
| WARNING | Email + Dashboard | Immediat |
| CRITICAL | SMS + Push + Email + Dashboard | Immediat |

### Preferences utilisateur

L'utilisateur peut configurer :
- **Canaux actifs** : desactiver email pour INFO/WARNING
- **Heures calmes** : ne pas envoyer de notifications entre 22h et 7h (sauf CRITICAL)
- **Langue** : francais (defaut)
- **Digest** : grouper les notifications non-critiques

### Contraintes CRITICAL

Les alertes CRITICAL **ne sont jamais bloquees** par les preferences utilisateur si le tenant l'exige (critical override). Un SMS CRITICAL arrive meme en heures calmes.

### Anti-spam

Meme alerte + meme destinataire + meme canal = pas de doublon pendant la fenetre de deduplication (configurable, defaut 1h).

---

## 12. Rapports

### Bilan Mensuel de Sante Electrique

Genere automatiquement chaque mois :
- Periode couverte
- Capteurs actifs / offline
- Score de risque moyen
- Alertes par severite
- Interventions creees / completees
- Defauts confirmes vs faux positifs
- Batiments les plus a risque
- Recommandations
- Evolution du risque (graphique)

### Attestation de Surveillance

Sur demande, prouve que le batiment est surveille :
- Batiment concerne
- Capteurs installes et leur disponibilite
- Taux de disponibilite sur la periode
- Statut du monitoring (actif/partiel/interrompu)
- Signature logique (SHA-256 du contenu)
- Numero unique

### Disclaimer legal (present sur TOUS les rapports)

> "Ce document constitue une aide a la decision basee sur le monitoring predictif
> des installations electriques. Il ne constitue en aucun cas un diagnostic
> electrique reglementaire au sens du decret n°2010-1016 et ne se substitue pas
> aux controles obligatoires prevus par la reglementation en vigueur."

---

## 13. Tableau de bord

### Vue d'ensemble (Dashboard)

| Element | Contenu |
|---------|---------|
| Score de risque global | Moyenne ponderee par severite |
| Devices en ligne | Nombre et pourcentage |
| Alertes ouvertes | Par severite (INFO, WARNING, CRITICAL) |
| Interventions en cours | Nombre |
| Graphique tendance risque | 30 derniers jours |
| Batiments les plus risques | Top 5 |

### Vue batiment

- Score de risque du batiment
- Liste des capteurs (statut, derniere reception)
- Alertes actives pour ce batiment
- Historique des interventions
- Tableaux electriques (groupement par panel)

### Vue device technique

- Sante du capteur (uptime, RSSI WiFi, buffer)
- Qualite des donnees (score 0-100, grade A-F)
- Statut credentials (actif, derniere rotation)
- Statistiques de rejet (raisons, compteurs)
- Graphiques telemetrie (temperature, THD, puissance)

### Pilote monitoring

- KPIs pilote (devices online, qualite moyenne, alertes, FP rate)
- Table des devices avec statut temps reel
- Journal des incidents
- Progression par rapport aux objectifs

---

## 14. Gestion multi-tenant

### Isolation des donnees

Chaque tenant (organisation cliente) a ses donnees **strictement isolees** :
- Requetes BDD filtrees par `tenant_id`
- JWT contient le `tenant_id` du user connecte
- Impossible d'acceder aux donnees d'un autre tenant
- Meme un admin plateforme doit explicitement choisir un tenant

### Hierarchie

```
Tenant (organisation)
└── Building (batiment)
    └── Electrical Panel (tableau electrique)
        └── Circuit (circuit individuel)
            └── Device (capteur installe)
```

### Configuration par tenant

- Seuils de scoring personnalisables
- Contacts d'urgence
- Politique de notification (critical override oui/non)
- Preferences par defaut des utilisateurs

---

## 15. Qualite des donnees

### Scoring qualite (par device, par jour)

| Grade | Score | Signification |
|-------|-------|---------------|
| A | 90-100 | Excellent : donnees fiables pour analyse |
| B | 75-89 | Bon : utilisable avec confiance |
| C | 50-74 | Moyen : analyse possible mais prudence |
| D | 25-49 | Mauvais : investigation necessaire |
| F | 0-24 | Insuffisant : donnees non exploitables |

### 9 criteres evalues

1. Completude des champs
2. Fraicheur (pas de donnees trop anciennes)
3. Coherence (valeurs dans les plages physiques)
4. Stabilite signal (pas de bruit excessif)
5. Regularite (pas de trous dans les series)
6. Drift horloge (synchronisation temps)
7. Qualite signal auto-diagnostiquee (firmware)
8. Taux de rejection
9. Disponibilite (ratio uptime/downtime)

### Impact sur le scoring

- Grade A-B : scoring normal
- Grade C : penalite fiabilite dans le scoring (+10%)
- Grade D-F : scoring SUSPENDU pour ce device (alerte qualite)

---

## 16. Pilote terrain

### Perimetre MVP 3

- **10 capteurs** sur 1-3 batiments
- **1 tenant pilote** (pilot-tenant-001)
- **3 utilisateurs test** : admin, manager, electricien
- **Duree** : 3-6 mois minimum pour baseline + validation
- **Installation** : par electricien habilite B2V+ uniquement

### Objectifs du pilote

| KPI | Seuil de succes |
|-----|-----------------|
| Taux de disponibilite capteurs | > 95% |
| Qualite donnees moyenne | > 80 (grade A-B) |
| Faux positifs | < 10% des alertes |
| Lead time detection | > 24h avant incident |
| Satisfaction electricien (feedback) | > 3.5/5 |

### Monitoring quotidien

Check-list operateur :
- [ ] Tous les devices online ?
- [ ] Score qualite > 80 sur tous les devices ?
- [ ] Alertes traitees dans les SLA ?
- [ ] Buffer drain en cours sur un device ?
- [ ] Incident de securite (rejet HMAC, replay) ?

### Criteres d'arret

Le pilote peut etre arrete **sans condition** si :
- Risque pour les personnes
- Defaillance repetee des capteurs
- Incompatibilite avec l'installation existante
- Demande du proprietaire ou gestionnaire

---

## 17. Limites et exclusions

### Ce que PyroSense fait

- Detecte des **tendances anormales** dans les signaux electriques
- Calcule un **score de risque indicatif** base sur des statistiques
- **Alerte** les gestionnaires pour planifier des inspections
- **Suit** les interventions et capitalise sur les retours terrain

### Ce que PyroSense ne fait PAS

| Exclusion | Raison |
|-----------|--------|
| Garantir l'absence d'incendie | Aucun systeme ne peut fournir cette garantie |
| Remplacer un diagnostic reglementaire | Non certifie, pas de valeur legale |
| Couper le courant automatiquement | Securite des personnes, pas d'actuation |
| Detecter tous les types de defauts | Limite aux signatures mesurables par les capteurs |
| Fonctionner sur installation non conforme | Prerequis : installation aux normes avant monitoring |
| Remplacer un electricien qualifie | Outil d'aide a la decision, pas de substitution humaine |

### Avertissements reglementaires

> **NON CERTIFIE** : Ce produit n'est pas certifie IEC 61439, NF C 15-100, ou tout autre standard electrique. Il ne peut pas etre utilise comme dispositif de securite.

> **INSTALLATION PROFESSIONNELLE UNIQUEMENT** : Toute intervention sur un tableau electrique reel doit etre realisee par un electricien qualifie habilite B2V minimum.

> **PILOTE = EXPERIMENTATION CONTROLEE** : La phase actuelle est un pilote de validation. Le produit n'est pas commercial et n'engage aucune garantie de resultat.

### Distinction des phases

| Phase | Statut | Ce qui est garanti |
|-------|--------|-------------------|
| Prototype logiciel | MVP 3 termine | Fonctionnalites testees (900+ tests) |
| Prototype hardware | En cours | Design valide, fabrication non demarre |
| Pilote terrain | Conditionnel | Electricien requis, retrait sans condition |
| Produit commercial | Non commence | Certification requise avant vente |

---

## 18. Glossaire

| Terme | Definition |
|-------|-----------|
| **Anomalie** | Deviation significative par rapport au comportement normal appris |
| **Anti-replay** | Mecanisme empechant la reutilisation d'un message deja traite |
| **Baseline** | Profil de comportement "normal" appris pour chaque capteur |
| **Bounded context** | Perimetre fonctionnel autonome (ex: "Alerting", "Maintenance") |
| **Claim token** | Jeton unique a usage unique pour l'enrolement d'un capteur |
| **CRITICAL** | Niveau d'alerte necessitant une intervention immediate |
| **Dashboard** | Interface web de surveillance et pilotage |
| **Deduplication** | Elimination des doublons (messages ou alertes) |
| **Device** | Capteur IoT installe sur un tableau electrique |
| **Drain mode** | Mode ou le capteur vide son buffer apres une deconnexion |
| **Edge** | Calcul realise sur le capteur (pas dans le cloud) |
| **Escalation** | Augmentation du niveau d'urgence si alerte non traitee |
| **Faux positif** | Alerte declenchee sans defaut reel |
| **Feature** | Caracteristique extraite du signal brut (ex: RMS, THD) |
| **Feedback loop** | Boucle d'amelioration basee sur les retours terrain |
| **HMAC** | Hash-based Message Authentication Code (authentification message) |
| **Heartbeat** | Signal de vie periodique envoye par un capteur |
| **Hypertable** | Table TimescaleDB optimisee pour les series temporelles |
| **Intervention** | Mission terrain assignee a un electricien |
| **KPI** | Key Performance Indicator (indicateur de performance) |
| **Micro-arc** | Petit arc electrique dans une connexion degradee |
| **MQTT** | Message Queuing Telemetry Transport (protocole IoT) |
| **Multi-tenant** | Isolation des donnees entre organisations clientes |
| **Nonce** | Nombre unique utilise une seule fois (anti-replay) |
| **Provisioning** | Processus d'enrolement securise d'un capteur |
| **Revocation** | Desactivation definitive des credentials d'un capteur |
| **Risk Score** | Score composite 0-100 indiquant le niveau de risque electrique |
| **SLA** | Service Level Agreement (delai de traitement garanti) |
| **Shadow mode** | Mode ou un modele ML tourne en parallele sans impacter l'utilisateur |
| **THD** | Total Harmonic Distortion (distorsion harmonique totale) |
| **Telemetrie** | Donnees de mesure envoyees par un capteur |
| **Tenant** | Organisation cliente utilisant la plateforme |
| **TimescaleDB** | Extension PostgreSQL pour series temporelles |
| **Z-score** | Nombre d'ecarts-types par rapport a la moyenne |
