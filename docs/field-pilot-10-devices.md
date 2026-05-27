# Plan Pilote Terrain MVP 3 — 10 Capteurs PyroSense AI

## 1. Objectifs du Pilote

### Objectif principal

Valider le fonctionnement du systeme PyroSense AI en conditions reelles sur un nombre limite de capteurs (10), afin de :

- Collecter des donnees electriques reelles pour calibrer les algorithmes de detection
- Mesurer la robustesse du hardware et du firmware en environnement operationnel
- Valider le pipeline complet : capteur → ingestion → analyse → scoring → alerte → intervention
- Evaluer la pertinence des seuils de detection (faux positifs, detection reelle)
- Recueillir les retours terrain des electriciens et gestionnaires

### Objectifs secondaires

- Constituer un dataset ML initial (minimum 3 mois de telemetrie labellisee)
- Valider les procedures d'installation, maintenance et support
- Mesurer les KPIs operationnels (uptime, qualite signal, latence)
- Tester la boucle de feedback terrain (confirmation/invalidation alertes)
- Preparer le passage a l'echelle (100 capteurs)

### Ce que ce pilote N'EST PAS

- Ce n'est PAS un produit commercial — aucune garantie de service
- Ce n'est PAS un systeme de securite incendie certifie
- Ce n'est PAS un substitut a la verification electrique reglementaire
- Ce n'est PAS une promesse de detection de tous les defauts electriques
- C'est une **experimentation controlee** en partenariat avec les gestionnaires de sites

---

## 2. Perimetre

### Inclus

| Element | Quantite | Detail |
|---------|:--------:|--------|
| Capteurs PyroSense | 10 | Prototype valide en laboratoire |
| Sites | 2-3 | Batiments tertiaires ou residentiels collectifs |
| Tableaux electriques | 5-10 | 1-2 capteurs par tableau |
| Circuits monitores | 10-20 | Circuits identifies a risque |
| Duree | 3-6 mois | Minimum 3 mois pour donnees significatives |
| Backend cloud | 1 instance | Stack complete (ingestion → scoring → alerting) |
| Dashboard monitoring | 1 | Acces equipe + gestionnaire (lecture seule) |
| Support technique | L1/L2 | Heures ouvrees + astreinte CRITICAL |

### Fonctionnalites actives

- Telemetrie temps reel (intervalle 10s)
- Detection anomalies statistiques (z-score, baseline adaptative)
- Scoring de risque multi-facteurs
- Alertes WARNING et CRITICAL
- Heartbeat et monitoring device health
- Buffer offline (72h)
- Feedback loop (confirmation/invalidation par electricien)
- Rapports mensuels automatiques

---

## 3. Hors Perimetre

| Element | Raison | Prevu pour |
|---------|--------|-----------|
| Detection ML avancee (LSTM, Isolation Forest) | Donnees insuffisantes | Post-pilote (MVP 4) |
| Certification IEC 61439 / NF C 15-100 | Processus reglementaire | Phase industrialisation |
| Notifications SMS/Push gestionnaires | Risque de sur-alerter | Quand taux FP < 5% |
| Application mobile | Priorite basse pour 10 devices | MVP 4 |
| Haute disponibilite backend | Cout disproportionne pour 10 devices | Phase production |
| OTA firmware automatique | Risque trop eleve sans rollback terrain valide | Quand process valide |
| Facturation / SLA contractuel | Pilote = gratuit/partenariat | Phase commerciale |
| Monitoring 24/7 avec astreinte | Equipe reduite | Phase 100 capteurs |
| Integration BMS / GTB existants | Complexite specifique site | Post-pilote |

---

## 4. Criteres de Selection des Sites

### Criteres obligatoires

| # | Critere | Justification |
|---|---------|---------------|
| S-01 | Accord ecrit du gestionnaire/proprietaire | Cadre legal |
| S-02 | Accessibilite du tableau electrique | Installation et maintenance |
| S-03 | Couverture WiFi au niveau du tableau | Connectivite capteur |
| S-04 | Electricien referent identifie et disponible | Interventions |
| S-05 | Pas d'obligation reglementaire de monitoring (hors scope) | Eviter responsabilite |
| S-06 | Alimentation 230V disponible pres du tableau | Alimentation capteur |
| S-07 | Local technique ferme a cle | Securite physique capteur |

### Criteres souhaitables

| # | Critere | Avantage |
|---|---------|----------|
| S-08 | Batiment avec historique d'incidents electriques | Donnees plus riches |
| S-09 | Tableau electrique > 10 ans | Probabilite defauts plus elevee |
| S-10 | Variete de charges (moteurs, eclairage, chauffage) | Diversite signaux |
| S-11 | Proximite geographique equipe tech (< 1h) | Rapidite intervention |
| S-12 | Gestionnaire motive et disponible pour feedback | Qualite retours |
| S-13 | Plusieurs tableaux dans le meme batiment | Optimisation deplacements |

### Criteres d'exclusion

| # | Critere | Raison |
|---|---------|--------|
| E-01 | Site classe ICPE / SEVESO | Responsabilite disproportionnee |
| E-02 | Etablissement recevant du public (ERP) categorie 1-2 | Reglementation lourde |
| E-03 | Site sans WiFi et sans possibilite d'installation | Bloquant technique |
| E-04 | Tableau electrique en cours de renovation | Mesures non stables |
| E-05 | Site avec obligation monitoring reglementaire | Confusion avec monitoring certifie |

---

## 5. Criteres de Selection des Tableaux Electriques

### Criteres techniques

| # | Critere | Specification |
|---|---------|---------------|
| T-01 | Tension nominale | 230V monophase ou 400V triphase |
| T-02 | Courant nominal circuit | 10A – 63A (plage capteur) |
| T-03 | Espace disponible dans le tableau | Min. 2 modules DIN libres |
| T-04 | Accessibilite cables pour pose pinces amperometriques | Cables non colles/tasses |
| T-05 | Protection differentielle amont | 30mA obligatoire |
| T-06 | Age du tableau | Preference > 5 ans (plus de risque) |
| T-07 | Reperage des circuits | Schema ou etiquetage existant |

### Circuits a prioriser

| Priorite | Type de circuit | Raison |
|----------|----------------|--------|
| HAUTE | Circuits avec charges inductives (moteurs, pompes) | Harmoniques, micro-arcs |
| HAUTE | Circuits anciens (> 15 ans) | Degradation isolation |
| MOYENNE | Circuits forte puissance (chauffage, cuisson) | Echauffement, surcharge |
| MOYENNE | Circuits avec connections multiples (multiprises) | Connexions desserrees |
| BASSE | Circuits eclairage LED | Reference "propre" |

### Documentation pre-existante requise

- Schema unifilaire du tableau (si disponible)
- Derniere verification electrique (rapport Consuel ou diagnostic)
- Historique incidents connus
- Liste des disjoncteurs et calibres

---

## 6. Pre-visite Technique

### Objectif

Evaluer la faisabilite technique de l'installation AVANT tout engagement.

### Duree

1 a 2 heures par site, realisee par l'ingenieur hardware + electricien referent.

### Points a verifier

| # | Point | Verification | Outil |
|---|-------|-------------|-------|
| PV-01 | Couverture WiFi au tableau | RSSI > -70 dBm | Smartphone + app WiFi Analyzer |
| PV-02 | Espace DIN disponible | Mesure physique | Metre |
| PV-03 | Accessibilite cables | Visuels, passage pinces CT | Inspection |
| PV-04 | Alimentation capteur | Prise disponible ou possible | Inspection |
| PV-05 | Temperature ambiante | < 40°C en conditions normales | Thermometre |
| PV-06 | Humidite | < 80% HR | Hygrometre |
| PV-07 | Protection IP du tableau | Min. IP30 (pas de poussiere excessive) | Visuel |
| PV-08 | Etat general cablage | Pas de defaut visible flagrant | Visuel electricien |
| PV-09 | Accessibilite pour maintenance | Acces libre, pas de stockage devant | Visuel |
| PV-10 | Contraintes site (horaires, cles) | Plages d'intervention possibles | Entretien gestionnaire |

### Livrable pre-visite

- Fiche site remplie (GO / NO-GO avec justification)
- Photos (tableau ouvert, espace disponible, routeur WiFi, acces)
- Plan d'implantation propose (quel circuit, quel emplacement capteur)
- Liste du materiel specifique necessaire

---

## 7. Checklist Installation

Voir document detaille : **`docs/field-installation-checklist.md`**

### Resume

L'installation est realisee **exclusivement par un electricien habilite** (minimum B2V, idealement BR) :

1. Mise hors tension du tableau (consignation)
2. Verification d'absence de tension (VAT)
3. Pose de la pince amperometrique (CT) sur le circuit cible
4. Fixation du module capteur sur rail DIN
5. Raccordement alimentation capteur (module DIN 230V → 5V)
6. Remise sous tension
7. Verification LED capteur (bleu clignotant = BOOTING)
8. Provisioning via application/API
9. Verification premier heartbeat recu par backend
10. Verification premiere telemetrie recue
11. Etiquetage (numero capteur, circuit monitore, date)
12. Photo post-installation
13. Signature PV installation

---

## 8. Checklist Reseau

| # | Verification | Critere | Action si NOK |
|---|-------------|---------|---------------|
| R-01 | RSSI WiFi au tableau | > -70 dBm | Ajouter repeteur/AP |
| R-02 | Bande passante disponible | > 100 kbps upload | Verifier saturation |
| R-03 | Port MQTT (8883 TLS) ouvert | Connexion reussie | Configurer firewall |
| R-04 | DNS resolvable | Endpoint backend accessible | Verifier DNS local |
| R-05 | Pas de portail captif | Connexion directe | Exclure MAC du captif |
| R-06 | DHCP actif | IP attribuee automatiquement | Ou configurer IP fixe |
| R-07 | Pas de blocage MAC | MAC capteur autorisee | Ajouter whitelist |
| R-08 | Stabilite connexion (5 min) | 0 deconnexion | Investiguer si instable |
| R-09 | Latence vers backend | < 200ms RTT | Verifier route |
| R-10 | Isolation reseau IoT (recommande) | VLAN dedie ou SSID isole | Configurer si possible |

---

## 9. Checklist Securite

| # | Verification | Responsable | Obligatoire |
|---|-------------|-------------|:-----------:|
| SEC-01 | Consignation tableau avant intervention | Electricien | OUI |
| SEC-02 | VAT (verification absence tension) | Electricien | OUI |
| SEC-03 | EPI portes (gants isolants, ecran facial si > 230V) | Electricien | OUI |
| SEC-04 | Habilitation electrique valide (B2V min.) | Electricien | OUI |
| SEC-05 | Personne informee de l'intervention | Gestionnaire | OUI |
| SEC-06 | Acces pompiers non obstrue | Tous | OUI |
| SEC-07 | Capteur n'obstrue pas l'acces aux disjoncteurs | Electricien | OUI |
| SEC-08 | Cablage capteur conforme NF C 15-100 (section, protection) | Electricien | OUI |
| SEC-09 | Protection differentielle amont fonctionnelle | Electricien | OUI |
| SEC-10 | Etiquette "monitoring PyroSense" visible | Equipe | OUI |
| SEC-11 | Procedure de desactivation d'urgence documentee | Equipe | OUI |
| SEC-12 | Coordonnees contact urgence sur l'etiquette | Equipe | OUI |

---

## 10. Plan de Provisioning

### Workflow

```
1. Pre-generer claim tokens (batch 10) via API admin
2. Associer chaque token a un device serial + site + circuit
3. Sur site, lors installation :
   a. Electricien met sous tension le capteur
   b. Capteur entre en mode PROVISIONING (LED jaune)
   c. Technicien saisit le claim token via app/CLI
   d. Capteur recoit credentials MQTT + config
   e. Transition → CONNECTING → ACTIVE
4. Verifier premier heartbeat dans dashboard
5. Verifier premiere telemetrie dans dashboard
6. Marquer device comme "DEPLOYED" dans le registre
```

### Registre des devices

| # | Serial | Site | Tableau | Circuit | Claim Token | Date Install | Statut |
|---|--------|------|---------|---------|-------------|--------------|--------|
| 1 | PS-001 | | | | | | |
| 2 | PS-002 | | | | | | |
| ... | ... | | | | | | |
| 10 | PS-010 | | | | | | |

### Securite provisioning

- Claim tokens generes le jour J (validite 24h)
- Un token par device (single-use)
- En cas d'echec : regenerer token, max 3 tentatives par device
- Credentials MQTT : TLS 1.3, HMAC-SHA256 pour signature payload
- Rotation credentials prevue a 6 mois (ou sur incident)

---

## 11. Plan de Collecte de Donnees

### Donnees collectees

| Type | Frequence | Volume estime (10 devices) | Retention |
|------|-----------|---------------------------|-----------|
| Telemetrie (14 features) | 10s | ~50 MB/jour | 90 jours brut |
| Heartbeat | 60s | ~2 MB/jour | 90 jours |
| Anomalies detectees | Event-driven | Variable | Indefinie |
| Alertes | Event-driven | Variable | Indefinie |
| Feedback terrain | Manuel | ~1/semaine/device | Indefinie |

### Features collectees par telemetrie

| Feature | Unite | Plage attendue |
|---------|-------|----------------|
| rmsVoltage | V | 220-240 |
| rmsCurrent | A | 0-63 |
| activePower | W | 0-14500 |
| powerFactor | ratio | 0.5-1.0 |
| thd | % | 0-30 |
| frequency | Hz | 49.5-50.5 |
| temperature | °C | 10-60 |
| hfNoiseLevel | dBm | -80 to -20 |
| arcEnergy | uJ | 0-1000 |
| transientCount | count | 0-100 |
| signalQuality | score | 0-100 |
| microArcCount | count | 0-50 |
| peakCurrent | A | 0-200 |
| crestFactor | ratio | 1.0-5.0 |

### Labellisation

- Labels generes automatiquement par le systeme : NORMAL, MICRO_ARC_SUSPECTED, etc.
- Labels confirmes par electricien apres intervention : CONFIRMED/FALSE_POSITIVE
- Objectif : minimum 50 labels confirmes sur 3 mois
- Pseudonymisation appliquee avant stockage dataset (HMAC-SHA256)

### RGPD et confidentialite

- Pas de donnees personnelles collectees par le capteur
- Donnees electriques pseudonymisees dans le dataset ML
- Accord du gestionnaire = base legale (interet legitime + consentement)
- Droit de retrait : desactivation et suppression donnees sur demande
- Pas de transmission a des tiers (donnees restent sur infra PyroSense)

---

## 12. Plan de Monitoring Quotidien

Voir checklist detaillee : **`docs/pilot-daily-monitoring-checklist.md`**

### Resume

| Heure | Action | Responsable | Outil |
|-------|--------|-------------|-------|
| 08:00 | Verifier dashboard sante devices | Operateur | Grafana |
| 08:15 | Verifier alertes de la nuit | Operateur | Dashboard alerting |
| 08:30 | Verifier metriques ingestion | Operateur | Prometheus |
| 09:00 | Traiter alertes WARNING en attente | Electricien (si besoin) | Dashboard |
| 17:00 | Bilan journalier (5 min) | Operateur | Checklist |
| Hebdo | Revue KPIs + qualite donnees | Tech Lead | Rapport auto |
| Mensuel | Rapport mensuel + revue gestionnaire | Chef projet | Template |

### Alertes automatiques (Prometheus → Slack/email equipe)

| Alerte | Seuil | Action |
|--------|-------|--------|
| Device offline > 30 min | 1 device | Verifier connectivite |
| Device offline > 4h | 1 device | Intervention site |
| Ingestion rejection rate > 5% | Sur 15 min | Verifier firmware/backend |
| Risk score CRITICAL | Score > 85 | Procedure CRITICAL |
| Signal quality POOR | < 40 pendant 1h | Verifier environnement |
| Memory leak detected | freeHeap decroit > 2KB/h | Planifier reboot/update |
| Backend error rate > 5% | Sur 5 min | Investiguer |

---

## 13. Plan de Support

### Niveaux de support

| Niveau | Scope | Delai reponse | Responsable |
|--------|-------|:-------------:|-------------|
| L0 | Auto-monitoring (alertes auto) | Immediat | Systeme |
| L1 | Diagnostic remote (logs, dashboard) | < 2h ouvrees | Operateur |
| L2 | Intervention firmware/backend | < 4h ouvrees | Developpeur |
| L3 | Intervention site (hardware) | < 24h ouvrees | Electricien + HW |

### Canaux de communication

| Canal | Usage | SLA |
|-------|-------|-----|
| Slack #pyrosense-pilot | Communication equipe | Continu |
| Email support@pyrosense | Gestionnaire → equipe | Reponse < 4h ouvrees |
| Telephone astreinte | Urgence CRITICAL uniquement | 7j/7 8h-20h |
| Dashboard (lecture seule) | Gestionnaire consulte | Self-service |

### Escalade

```
Alerte detectee
    → L1 diagnostic (2h)
        → Si resolu : cloture + rapport
        → Si non resolu : escalade L2 (4h)
            → Si firmware : patch + OTA manuel
            → Si backend : hotfix + deploy
            → Si hardware : escalade L3 (24h)
                → Intervention site planifiee
```

### Astreinte

- Heures ouvrees (9h-18h L-V) : equipe complete disponible
- Hors ouvrees : alertes CRITICAL uniquement, traitees le lendemain matin (sauf danger immediat)
- Danger immediat (fumee, odeur, declenchement disjoncteur) : appeler 18 (pompiers) puis equipe

> **IMPORTANT** : PyroSense est un outil de monitoring predictif. En cas de danger electrique reel (odeur de brule, etincelles, chaleur anormale palpable), la procedure est : couper le disjoncteur general + appeler les secours. Le capteur n'est PAS un dispositif de securite certifie.

---

## 14. Procedure en Cas d'Alerte WARNING

### Definition

Alerte WARNING = anomalie detectee qui merite attention mais ne represente pas un danger immediat.
Exemples : THD eleve, temperature en hausse lente, micro-arc isole.

### Procedure

| Etape | Action | Delai | Responsable |
|-------|--------|-------|-------------|
| 1 | Recevoir notification (dashboard + Slack) | Immediat | Systeme |
| 2 | Consulter details alerte (device, anomaly type, score, contexte) | < 30 min | Operateur L1 |
| 3 | Verifier si alerte deja connue/en cours | < 30 min | Operateur L1 |
| 4 | Evaluer : faux positif probable ou alerte reelle ? | < 1h | Operateur L1 |
| 5a | Si faux positif probable : marquer FALSE_POSITIVE + feedback | < 2h | Operateur L1 |
| 5b | Si alerte reelle : acknowledge + planifier inspection | < 4h | Operateur L1 |
| 6 | Inspection par electricien (si 5b) | < 1 semaine | Electricien |
| 7 | Feedback post-inspection (confirme/invalide) | Jour meme | Electricien |
| 8 | Cloturer alerte avec resultat | < 24h post-inspection | Operateur |

### Regles

- Ne PAS alerter le gestionnaire pour chaque WARNING (eviter fatigue alertes)
- Regrouper les WARNING recurrents dans le rapport hebdomadaire
- Si WARNING persiste > 1 semaine sans traitement : escalader en reunion hebdo
- Si 3+ WARNING sur meme device en 24h : traiter comme pré-CRITICAL

---

## 15. Procedure en Cas d'Alerte CRITICAL

### Definition

Alerte CRITICAL = risque eleve detecte necessitant attention rapide.
Exemples : micro-arcs recurrents, temperature > 60°C, score risque > 85.

### Procedure

| Etape | Action | Delai | Responsable |
|-------|--------|-------|-------------|
| 1 | Recevoir notification (Slack + email + dashboard) | Immediat | Systeme |
| 2 | Acknowledge l'alerte dans le dashboard | < 15 min ouvrees | Operateur L1 |
| 3 | Evaluer le contexte (historique device, derniere inspection, score) | < 30 min | Operateur L1 |
| 4 | Contacter l'electricien referent du site | < 1h | Operateur L1 |
| 5 | Informer le gestionnaire du site | < 2h | Chef projet |
| 6 | Planifier inspection electrique | < 24h ouvrees | Electricien |
| 7 | Inspection sur site avec diagnostic | Date planifiee | Electricien |
| 8 | Si defaut confirme : intervention corrective | Selon urgence | Electricien |
| 9 | Feedback dans le systeme (outcome + commentaire) | Jour meme | Electricien |
| 10 | Rapport incident au gestionnaire | < 48h post-inspection | Chef projet |
| 11 | Cloturer alerte | < 24h post-feedback | Operateur |

### Communication au gestionnaire

Template notification gestionnaire (email) :

```
Objet : [PyroSense Pilote] Alerte detectee — {site} — {circuit}

Bonjour,

Notre systeme de monitoring a detecte une anomalie sur votre installation.

- Type : {type anomalie}
- Localisation : Tableau {X}, circuit {Y}
- Date/heure : {timestamp}
- Evaluation : {interpretation non alarmiste}

Notre electricien referent va proceder a une inspection dans les prochaines 24h.

IMPORTANT : Cette detection est indicative et ne constitue en aucun cas un 
diagnostic electrique certifie. En cas de signe visible de danger (fumee, 
odeur, chaleur anormale), coupez le disjoncteur general et contactez les 
secours (18).

Cordialement,
Equipe PyroSense AI — Pilote Experimental
```

### Regles

- Ne JAMAIS utiliser de termes alarmistes ("incendie imminent", "danger de mort")
- Toujours rappeler que c'est un outil predictif experimental
- Toujours donner la conduite a tenir en cas de danger reel
- Si doute sur la severite : appeler l'electricien AVANT de communiquer

---

## 16. Procedure en Cas de Capteur Offline

### Definition

Capteur offline = pas de heartbeat recu depuis > 3 intervalles (> 3 min).

### Procedure

| Delai offline | Action | Responsable |
|:-------------:|--------|-------------|
| 3-30 min | Surveillance passive (peut etre transitoire) | Systeme |
| 30 min - 4h | Verifier : WiFi site OK ? Backend OK ? Autre device meme site OK ? | Operateur L1 |
| 4h - 24h | Contacter gestionnaire : "acces reseau OK ?" + verifier alimentation | Operateur L1 |
| > 24h | Planifier intervention site (verifier capteur physiquement) | L3 |
| > 72h | Considerer le capteur comme en panne — planifier remplacement | L3 |

### Causes probables

| Cause | Frequence estimee | Diagnostic | Resolution |
|-------|:-----------------:|-----------|-----------|
| Coupure WiFi temporaire | Frequente | Retour automatique | Aucune |
| Coupure alimentation (disjoncteur coupe) | Moyenne | Gestionnaire confirme | Remettre sous tension |
| Panne routeur WiFi | Moyenne | Autres devices meme site | Redemarrer routeur |
| Bug firmware (watchdog loop) | Rare | Logs serie si accessible | Reboot manuel/OTA |
| Panne hardware capteur | Rare | Pas de reponse apres reboot | Remplacement |
| Revocation credentials (erreur) | Tres rare | Logs backend DEVICE_REVOKED | Re-provisionner |

### Regles

- Un device offline ne genere PAS d'alerte au gestionnaire (sauf si > 48h)
- Si TOUS les devices d'un site sont offline : probleme reseau site → contacter gestionnaire
- Buffer offline rattrape les donnees (72h max) : pas de panique pour < 72h

---

## 17. Procedure en Cas de Faux Positif

### Definition

Faux positif = alerte generee sans defaut electrique reel (confirme par electricien).

### Procedure

| Etape | Action | Responsable |
|-------|--------|-------------|
| 1 | Electricien inspecte et ne trouve pas de defaut | Electricien |
| 2 | Marquer l'alerte comme FALSE_POSITIVE dans le dashboard | Electricien/Operateur |
| 3 | Renseigner le contexte (commentaire : pourquoi c'est un FP) | Electricien |
| 4 | Le systeme enregistre le feedback (boucle de feedback) | Automatique |
| 5 | Analyser la cause du faux positif (seuil trop bas ? charge inhabituelle ?) | Tech Lead |
| 6 | Si pattern repetitif : ajuster le seuil ou ajouter exception | Developpeur |
| 7 | Documenter dans le rapport hebdomadaire | Operateur |

### Analyse des faux positifs

Enregistrer pour chaque FP :
- Type d'anomalie ayant declenche l'alerte
- Conditions au moment du declenchement (charge active, meteo, heure)
- Commentaire electricien (explication probable)
- Action corrective (ajustement seuil, nouvelle regle, aucune)

### Objectif

- Mois 1 : taux FP < 20% acceptable (phase d'apprentissage)
- Mois 2 : taux FP < 10% (seuils ajustes)
- Mois 3+ : taux FP < 5% (cible operationnelle)

---

## 18. Procedure en Cas de Retrait Capteur

### Raisons de retrait

| Raison | Initiative | Procedure |
|--------|-----------|-----------|
| Demande du gestionnaire | Gestionnaire | Immediate, sans discussion |
| Panne hardware non reparable | Equipe tech | Planifiee |
| Fin du pilote | Equipe projet | Planifiee |
| Incident securite (rare) | Electricien | Immediate |
| Travaux sur le tableau | Gestionnaire/Electricien | Temporaire ou definitive |

### Procedure de retrait

| # | Action | Responsable |
|---|--------|-------------|
| 1 | Informer l'equipe (Slack + registre) | Initiateur |
| 2 | Revoquer les credentials du device | Operateur (API) |
| 3 | Planifier intervention electricien | Chef projet |
| 4 | Consignation tableau (mise hors tension) | Electricien |
| 5 | Deconnexion capteur (alimentation + CT) | Electricien |
| 6 | Retrait physique module DIN + pince | Electricien |
| 7 | Verification visuelle : rien d'endommage | Electricien |
| 8 | Remise sous tension tableau | Electricien |
| 9 | Verification fonctionnement normal installation | Electricien |
| 10 | Mise a jour registre devices (statut: RETIRED) | Operateur |
| 11 | Suppression donnees si demande (RGPD) | Operateur |
| 12 | PV de retrait signe | Electricien + Gestionnaire |

### Engagement

- Le retrait est **toujours possible** sur simple demande du gestionnaire
- Delai de retrait : < 5 jours ouvres (sauf urgence : jour meme)
- Les donnees collectees sont conservees 90 jours puis purgees (sauf accord contraire)
- Aucun frais ni penalite pour le gestionnaire

---

## 19. KPIs du Pilote

### KPIs techniques

| KPI | Formule | Cible | Mesure |
|-----|---------|:-----:|--------|
| Uptime capteur | (temps connecte / temps total) × 100 | > 95% | Prometheus |
| Taux telemetrie valide | (messages acceptes / messages envoyes) × 100 | > 99% | Prometheus |
| Qualite signal moyenne | moyenne(signalQuality) par device | > 70 | Dashboard |
| Latence end-to-end | P95 (envoi capteur → stockage backend) | < 5s | Prometheus |
| Buffer overflow events | nombre de messages perdus par eviction | 0 | Logs device |
| Reconnexion moyenne | temps moyen reconnexion apres perte WiFi | < 60s | Logs device |
| Memory stability | derive freeHeap sur 24h | < 500 bytes/h | Prometheus |

### KPIs detection

| KPI | Formule | Cible M1 | Cible M3 | Mesure |
|-----|---------|:--------:|:--------:|--------|
| Alertes generees | count(alertes) par mois | Baseline | Stable | Dashboard |
| Alertes confirmees | count(CONFIRMED_DEFECT) / count(alertes inspectees) | > 30% | > 50% | Feedback loop |
| Taux faux positifs | count(FALSE_POSITIVE) / count(alertes inspectees) | < 20% | < 5% | Feedback loop |
| Temps moyen detection → inspection | avg(date_inspection - date_alerte) | < 7j | < 3j | Dashboard |
| Defauts non detectes (miss rate) | defauts trouves hors alerte / total defauts | A mesurer | A reduire | Terrain |

### KPIs operationnels

| KPI | Formule | Cible | Mesure |
|-----|---------|:-----:|--------|
| Temps de traitement alerte WARNING | mediane(acknowledge - creation) | < 4h | Dashboard |
| Temps de traitement alerte CRITICAL | mediane(acknowledge - creation) | < 1h | Dashboard |
| Interventions declenchees | count(interventions liees alertes) | > 0 | Maintenance svc |
| Satisfaction gestionnaire | Score questionnaire (1-5) | > 3.5/5 | Enquete mensuelle |
| Disponibilite backend | uptime backend / temps total | > 99% | Prometheus |

### Reporting KPIs

- Dashboard temps reel : Grafana (equipe technique)
- Rapport hebdomadaire automatique : email equipe
- Rapport mensuel : PDF pour gestionnaire (indicateurs cles + evenements marquants)
- Bilan final pilote : document complet (voir section 25)

---

## 20. Duree Recommandee

### Duree minimale : 3 mois

| Phase | Duree | Objectif |
|-------|-------|----------|
| Semaine 1-2 | 2 sem | Installation + stabilisation + baseline |
| Mois 1 | 4 sem | Collecte donnees + calibration seuils + correction bugs |
| Mois 2 | 4 sem | Operation nominale + feedback loop + ajustements |
| Mois 3 | 4 sem | Validation stabilite + metriques finales + decision |

### Duree recommandee : 6 mois

- 3 mois additionnels permettent de :
  - Couvrir les variations saisonnieres (chauffage, climatisation)
  - Accumuler plus de labels confirmes pour le ML
  - Atteindre un taux FP < 5% stable
  - Tester la robustesse long terme (derives, usure)

### Extension possible

- Si resultats encourageants a 3 mois : prolonger a 6 mois
- Si resultats insuffisants a 3 mois : evaluer les criteres d'arret (section 24)
- Pas de prolongation au-dela de 6 mois sans decision formelle GO/NO-GO

---

## 21. Risques

| # | Risque | Probabilite | Impact | Score |
|---|--------|:-----------:|:------:|:-----:|
| R-01 | Taux de faux positifs > 20% pendant plus de 2 mois | Moyenne | Eleve | 12 |
| R-02 | Panne hardware multiple (> 3 devices) | Faible | Eleve | 8 |
| R-03 | Gestionnaire se desengage (fatigue alertes, mecontentement) | Moyenne | Eleve | 12 |
| R-04 | Probleme WiFi recurrent sur un site | Moyenne | Moyen | 9 |
| R-05 | Bug firmware causant reboot en boucle | Faible | Eleve | 8 |
| R-06 | Faille securite decouverte (credentials compromis) | Tres faible | Critique | 8 |
| R-07 | Incident electrique reel non detecte par le systeme | Faible | Critique | 10 |
| R-08 | Incident electrique sur le site pendant le pilote (non lie au capteur) | Faible | Eleve | 8 |
| R-09 | Indisponibilite backend > 24h | Faible | Moyen | 6 |
| R-10 | Donnees insuffisantes pour calibration ML | Moyenne | Moyen | 9 |
| R-11 | Reglementation imposant certification pour monitoring | Tres faible | Critique | 6 |
| R-12 | Vol ou vandalisme du capteur | Faible | Faible | 3 |

---

## 22. Mitigations

| Risque | Mitigation | Responsable |
|--------|-----------|-------------|
| R-01 (faux positifs) | Feedback loop actif, ajustement seuils bi-hebdo, mode SUGGESTION_ONLY | Tech Lead |
| R-02 (panne HW) | 2 devices spare en stock, procedure remplacement < 48h | Ing. Hardware |
| R-03 (desengagement) | Communication proactive, rapports clairs, ne pas sur-alerter, respect retrait | Chef projet |
| R-04 (WiFi) | Pre-visite reseau, repeteur en stock, SSID dedie si possible | Ing. Hardware |
| R-05 (bug firmware) | Watchdog HW, logs RTC, procedure reboot/reflash manuelle documentee | Dev firmware |
| R-06 (securite) | Revocation immediate, rotation credentials, audit logs, isolement device | Tech Lead |
| R-07 (miss detection) | Disclaimer clair au gestionnaire, pas de promesse, rappeler obligation verif reglementaire | Chef projet |
| R-08 (incident non lie) | Disclaimer signe, assurance RC Pro equipe, pas de lien causal capteur → incident | Chef projet |
| R-09 (backend down) | Buffer 72h device, alertes infra, procedure restart documentee | Operateur |
| R-10 (donnees insuffisantes) | Choix sites a risque, injection defauts controles en labo en parallele | Tech Lead |
| R-11 (reglementation) | Veille juridique, pilote presente comme R&D interne, pas de communication publique | Chef projet |
| R-12 (vol) | Local ferme a cle, etiquetage discret, valeur unitaire faible | Gestionnaire |

---

## 23. Criteres de Succes

### Succes technique (GO pour phase 100 capteurs)

| # | Critere | Seuil | Methode verification |
|---|---------|-------|---------------------|
| CS-01 | Uptime moyen des 10 devices sur 3 mois | > 95% | Prometheus average |
| CS-02 | Taux de telemetrie valide | > 99% | Prometheus ratio |
| CS-03 | Taux de faux positifs a M3 | < 10% | Feedback loop stats |
| CS-04 | Au moins 1 defaut reel detecte avant inspection prevue | > 0 | Feedback CONFIRMED |
| CS-05 | Aucun incident securite lie au capteur | 0 | Registre incidents |
| CS-06 | Dataset exploitable pour ML (> 30 labels confirmes) | > 30 | Dataset candidates |
| CS-07 | Backend stable (disponibilite > 99%) | > 99% | Prometheus |
| CS-08 | Reconnexion automatique fonctionnelle (< 60s median) | < 60s | Logs |

### Succes operationnel

| # | Critere | Seuil | Methode |
|---|---------|-------|---------|
| CO-01 | Satisfaction gestionnaire | > 3/5 | Questionnaire |
| CO-02 | Electricien utilise le feedback (> 1/semaine) | > 12 sur 3 mois | Compteur feedback |
| CO-03 | Procedure d'installation reproductible (< 1h) | < 1h | Chrono moyen |
| CO-04 | Support L1 resout > 80% sans escalade | > 80% | Tickets |
| CO-05 | Aucune demande de retrait pour mecontentement | 0 | Registre |

### Succes business (indicatifs, non bloquants)

| # | Critere | Indicateur |
|---|---------|-----------|
| CB-01 | Gestionnaire souhaite continuer apres pilote | OUI/NON |
| CB-02 | Gestionnaire recommanderait a un pair | OUI/NON |
| CB-03 | Estimation economique positive (evitement sinistre > cout capteur) | Estimation |

---

## 24. Criteres d'Arret

### Arret immediat (securite)

| # | Critere | Action |
|---|---------|--------|
| CA-01 | Incident electrique lie au capteur (echauffement, court-circuit) | Retrait immediat tous capteurs |
| CA-02 | Faille securite non patchable (credentials compromis en masse) | Revocation + retrait |
| CA-03 | Demande formelle du gestionnaire | Retrait dans les 5 jours |

### Arret programme (echec)

| # | Critere | Seuil | Delai evaluation |
|---|---------|-------|:----------------:|
| CA-04 | Taux FP > 30% apres 2 mois malgre ajustements | > 30% a M2 | M2 review |
| CA-05 | Uptime moyen < 80% sur 1 mois | < 80% | Review mensuelle |
| CA-06 | Plus de 5 devices en panne simultanement (irremplacables) | > 50% parc | Immediat |
| CA-07 | Backend instable (> 3 outages > 4h en 1 mois) | 3 outages | Review mensuelle |
| CA-08 | Aucun defaut detecte ni confirmable apres 3 mois | 0 detection utile | M3 review |
| CA-09 | Equipe technique non disponible pour support (depart, reorganisation) | Pas de couverture | Immediat |

### Processus d'arret

1. Constater le critere d'arret (avec preuves)
2. Reunion decision : tech lead + chef projet + gestionnaire
3. Si arret confirme : planifier retrait (section 18)
4. Rapport de cloture (bilan, lecons, donnees recoltees)
5. Donnees conservees 90 jours puis purgees (sauf accord)
6. Communication transparente au gestionnaire

---

## 25. Rapport Final Pilote

Voir template : **`docs/pilot-final-report-template.md`**

### Contenu attendu

1. Resume executif (1 page : GO/NO-GO + chiffres cles)
2. Contexte et objectifs rappeles
3. Deroulement chronologique (jalons, incidents, decisions)
4. Resultats techniques (KPIs, graphes, comparaison cibles)
5. Resultats detection (alertes, confirmations, faux positifs, defauts trouves)
6. Resultats operationnels (satisfaction, temps intervention, support)
7. Analyse des faux positifs (causes, corrections appliquees, evolution)
8. Analyse des vrais positifs (types de defauts, severite, actions)
9. Qualite du dataset ML (volume, labels, exploitabilite)
10. Bilan financier (couts reels vs budget)
11. Lecons apprises (firmware, backend, process, humain)
12. Recommandations pour phase 100 capteurs
13. Decision GO/NO-GO argumentee
14. Annexes (logs, graphes, PV, feedbacks)

### Diffusion

| Destinataire | Version | Contenu |
|-------------|---------|---------|
| Equipe technique | Complete | Toutes sections |
| Gestionnaire(s) | Resumee | Sections 1, 3, 5, 6, 11 |
| Direction / investisseurs | Executive | Section 1 + decision |

---

## Annexes

### A. Documents lies

| Document | Chemin |
|----------|--------|
| Checklist installation | `docs/field-installation-checklist.md` |
| Checklist monitoring quotidien | `docs/pilot-daily-monitoring-checklist.md` |
| Template rapport final | `docs/pilot-final-report-template.md` |
| Protocole test labo (pre-requis) | `docs/lab-test-protocol.md` |
| Architecture edge-cloud | `docs/edge-cloud-architecture.md` |
| Protocole MQTT v1 | `docs/mqtt-protocol-v1.md` |
| Provisioning securise | `docs/device-provisioning.md` |
| Boucle de feedback | `docs/feedback-loop.md` |

### B. Contacts d'urgence

| Role | Nom | Telephone | Disponibilite |
|------|-----|-----------|---------------|
| Chef projet | {a completer} | | L-V 9h-18h |
| Tech Lead | {a completer} | | L-V 9h-18h |
| Electricien referent site 1 | {a completer} | | Sur RDV |
| Electricien referent site 2 | {a completer} | | Sur RDV |
| Gestionnaire site 1 | {a completer} | | L-V ouvrees |
| Gestionnaire site 2 | {a completer} | | L-V ouvrees |
| Urgences | Pompiers | 18 | 24/7 |

### C. Disclaimers obligatoires

A inclure dans tout document fourni au gestionnaire :

> **PyroSense AI est un systeme experimental de monitoring predictif en phase pilote.**
> Il ne constitue en aucun cas :
> - Un dispositif de securite incendie certifie
> - Un substitut a la verification electrique reglementaire (NF C 15-100)
> - Une garantie d'absence de defaut electrique ou d'incendie
> - Un diagnostic electrique au sens de la reglementation
>
> En cas de signe visible de danger electrique (fumee, odeur de brule, etincelles, chaleur anormale), la seule conduite a tenir est :
> 1. Couper le disjoncteur general
> 2. Appeler les secours (18)
>
> L'installation et le retrait des capteurs sont realises exclusivement par un electricien qualifie.
