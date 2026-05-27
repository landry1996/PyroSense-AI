# Protocole de Test Laboratoire — Capteur PyroSense AI

## 1. Objectifs du Test Laboratoire

### Objectif principal

Valider le fonctionnement complet du prototype capteur PyroSense AI en environnement controle avant toute installation terrain, en garantissant :

- La securite des personnes et du materiel
- La conformite du firmware aux specifications du protocole MQTT v1
- La precision des mesures dans les plages definies
- La robustesse face aux conditions degradees (perte reseau, buffer offline, signaux bruites)
- La detection correcte des anomalies simulees (micro-arcs, transitoires, temperature)
- L'integration complete avec le backend cloud (ingestion, validation, scoring)

### Objectifs secondaires

- Identifier les defauts firmware avant deploiement terrain
- Calibrer les seuils de detection sur signaux connus
- Mesurer les performances (latence, consommation memoire, autonomie buffer)
- Valider le protocole de provisioning securise
- Documenter les ecarts entre comportement attendu et observe

### Hors perimetre

- Tests de certification EMC/CEM (necessitent chambre anechoique)
- Tests thermiques extremes (necessitent chambre climatique)
- Tests d'endurance longue duree (> 72h) — phase ulterieure
- Tests sur installation electrique reelle sous tension secteur

---

## 2. Materiel Necessaire

### Plateforme capteur

| Composant | Reference | Quantite | Usage |
|-----------|-----------|:--------:|-------|
| ESP32-S3 DevKitC-1 N16R8 | Espressif | 2 | DUT (Device Under Test) + spare |
| ADC externe | ADS1115 (I2C, 16-bit) | 2 | Acquisition signal |
| Sonde temperature | MAX31865 + PT100 | 2 | Mesure temperature |
| LED status RGB | WS2812B | 2 | Indicateur visuel etat |
| Breadboard + cables | — | 1 lot | Assemblage prototype |
| Alimentation regulee 3.3V/5V | Bench PSU | 1 | Alimentation stable |

### Equipement de generation de signaux

| Equipement | Usage | Specification |
|------------|-------|---------------|
| Generateur de fonctions | Signaux sinusoidaux, carres, triangulaires | 0.1 Hz – 200 kHz, amplitude 0-5V |
| Generateur de bruit | Bruit blanc/rose, perturbations HF | Bande 1 kHz – 1 MHz |
| Source de chaleur controlee | Simulation temperature elevee | Pistolet a air chaud + thermocouple ref |
| Simulateur micro-arc | Impulsions HF breves (50-100 kHz) | **Uniquement en basse tension (< 50V)** |
| Attenuateur variable | Simulation degradation signal | 0 – 60 dB |
| Oscilloscope numerique | Reference de mesure | Min. 100 MHz, 4 voies |
| Multimetre de reference | Calibration | Classe 0.05% (type Fluke 87V) |

### Infrastructure reseau et backend

| Composant | Usage |
|-----------|-------|
| Routeur WiFi dedie (2.4 GHz) | Reseau isole pour tests |
| Switch manageable | Simulation coupure reseau (port disable) |
| PC serveur (Docker) | Backend local (ingestion, scoring, Kafka, PostgreSQL) |
| PC monitoring | Grafana, logs, captures Wireshark |
| Cage de Faraday optionnelle | Isolation RF pour tests HF |

### Outillage logiciel

| Outil | Usage |
|-------|-------|
| ESP-IDF 5.x + idf.py | Build et flash firmware |
| MQTT Explorer | Monitoring messages MQTT temps reel |
| Wireshark | Capture reseau WiFi/TCP |
| Grafana + Prometheus | Monitoring metriques backend |
| Serial monitor (minicom/putty) | Logs firmware temps reel |
| Docker Compose | Stack backend locale |
| Script injection signaux | Automatisation sequences de test |

---

## 3. Preconditions Securite

### Regles imperatives

> **AVERTISSEMENT** : Aucun test ne doit etre realise sur circuit electrique sous tension secteur (230V AC).
> Tous les signaux injectes sont en basse tension (< 50V DC ou < 25V AC) via generateur de fonctions.

1. **Tension maximale sur banc** : 48V DC / 25V AC (TBTS — Tres Basse Tension de Securite)
2. **Isolation galvanique** : le banc de test est isole du reseau electrique du batiment par transformateur d'isolement pour l'alimentation des equipements
3. **Protection** : disjoncteur differentiel 30mA sur l'alimentation du banc, meme si aucune tension secteur n'est manipulee
4. **Environnement** : sol antistatique, temperature ambiante 18-25°C, humidite < 70%
5. **Equipement personnel** : bracelet antistatique obligatoire lors de manipulation des PCB
6. **Extinction d'urgence** : bouton d'arret d'urgence accessible coupant toute alimentation du banc

### Qualification du personnel

| Action | Qualification minimale |
|--------|----------------------|
| Manipulation signaux < 50V | Developpeur firmware (formation ESD) |
| Generation impulsions HF (micro-arc simule) | Ingenieur hardware |
| Toute manipulation > 50V (INTERDIT en labo dev) | Electricien habilite B2V+ (hors scope) |
| Observation, prise de notes | Aucune qualification specifique |

### Avant chaque session

- [ ] Verifier que le banc est hors tension secteur
- [ ] Verifier l'absence de condensateurs charges
- [ ] Verifier la mise a la terre du banc
- [ ] Verifier la calibration des instruments de reference (< 6 mois)
- [ ] Informer l'equipe de la session de test (horaire, duree, risques)

---

## 4. Roles et Responsabilites

### Developpeur Firmware

- Flash et configuration du firmware sur le DUT
- Monitoring des logs serie en temps reel
- Interpretation des etats machine du device
- Debug en cas de crash, watchdog, ou comportement inattendu
- Mise a jour du firmware entre sessions de test
- Documentation des bugs firmware trouves

### Ingenieur Hardware

- Montage et verification du banc de test
- Configuration et pilotage du generateur de signaux
- Injection des signaux simules (normaux et anormaux)
- Verification de la calibration des instruments
- Mesure de reference a l'oscilloscope
- Diagnostic pannes hardware (capteur defaillant, soudure froide, etc.)

### Electricien (si present — uniquement pour scenarii avances)

- Validation de la securite electrique du banc
- Realisation de scenarii avances si tension > TBTS (hors scope labo dev)
- Verification de l'isolement et des protections
- Conseil sur la representativite des signaux simules vs defauts reels

### Observateur QA

- Execution du protocole de test pas a pas
- Enregistrement des resultats (PASS/FAIL/PARTIAL)
- Chronometrage des sequences
- Capture ecran des dashboards et logs
- Redaction du rapport de test
- Ouverture des fiches d'anomalie
- Verification de la tracabilite (versions firmware, backend, config)

---

## 5. Scenarios de Test

### SC-01 : Demarrage Device (Cold Boot)

**Objectif** : Valider la sequence de boot complete du firmware.

**Preconditions** :
- Firmware flashe, configuration vierge (factory reset)
- WiFi disponible, MQTT broker demarre
- Backend stack operationnelle

**Etapes** :

| # | Action | Resultat attendu | Mesure |
|---|--------|-----------------|--------|
| 1 | Mettre sous tension le DUT | LED status : bleu clignotant (BOOTING) | Temps boot |
| 2 | Observer la sequence d'init | Logs serie : init sensors OK, init WiFi OK, init MQTT OK | Logs |
| 3 | Attendre transition CONNECTING | LED : vert clignotant, connexion MQTT etablie | Temps total boot → connected |
| 4 | Verifier le heartbeat initial | Premier heartbeat recu par backend dans les 10s | Delai 1er heartbeat |

**Criteres PASS** :
- Boot complet en < 15s
- Aucun crash/reboot
- Connexion WiFi + MQTT reussie au 1er essai
- Premier heartbeat recu par backend

---

### SC-02 : Provisioning Securise

**Objectif** : Valider le protocole d'enrolement initial du device.

**Preconditions** :
- Device en etat PROVISIONING (premier demarrage ou factory reset)
- Claim token genere via API backend
- Backend provisioning endpoint accessible

**Etapes** :

| # | Action | Resultat attendu | Mesure |
|---|--------|-----------------|--------|
| 1 | Demarrer device sans credentials | Etat PROVISIONING, LED : jaune | Transition temps |
| 2 | Envoyer claim token via endpoint | Device recoit credentials MQTT | Latence provisioning |
| 3 | Observer transition → CONNECTING | Connexion MQTT avec nouvelles credentials | Succes/echec |
| 4 | Tenter re-provisioning avec meme token | Rejet : token deja consomme | Code erreur |
| 5 | Tenter avec token expire (forcer expiry) | Rejet : token expire | Code erreur |
| 6 | Tenter avec token invalide | Rejet : token invalide, compteur rate limit | Logs |

**Criteres PASS** :
- Provisioning reussi en < 5s
- Token single-use effectif
- Token expire correctement rejete
- Rate limiting actif apres 10 echecs

---

### SC-03 : Heartbeat Periodique

**Objectif** : Valider l'envoi regulier des heartbeats et la detection de perte.

**Preconditions** :
- Device en etat ACTIVE, connecte
- Backend monitoring actif

**Etapes** :

| # | Action | Resultat attendu | Mesure |
|---|--------|-----------------|--------|
| 1 | Observer pendant 5 minutes | Heartbeats recus toutes les 60s (±5s) | Intervalles |
| 2 | Verifier contenu heartbeat | state, uptime, RSSI, bufferSize, freeHeap | Payload |
| 3 | Comparer RSSI avec mesure reference | Ecart < 5 dBm | Precision |
| 4 | Verifier increment uptime | Coherent avec temps reel ecoule | Derive |

**Criteres PASS** :
- Regularite < ±5s sur intervalle configure
- Tous les champs presents et valides
- RSSI coherent avec conditions WiFi
- Uptime increment correctement

---

### SC-04 : Telemetrie Normale

**Objectif** : Valider l'acquisition et la transmission de telemetrie en conditions nominales.

**Preconditions** :
- Signal sinusoidal 50 Hz, 1V RMS injecte sur ADC
- Temperature ambiante stable (~22°C)
- Backend ingestion operationnel

**Etapes** :

| # | Action | Resultat attendu | Mesure |
|---|--------|-----------------|--------|
| 1 | Injecter signal 50 Hz 1V RMS | ADC acquiert sans saturation | Forme d'onde oscilloscope |
| 2 | Attendre 1er envoi telemetrie | Message MQTT recu en < 10s | Latence |
| 3 | Verifier RMS calcule | 1.0V ±5% (0.95 – 1.05V) | Precision RMS |
| 4 | Verifier THD calcule | < 1% (signal pur) | Precision THD |
| 5 | Verifier temperature | 22°C ±2°C vs thermocouple ref | Precision temp |
| 6 | Verifier frequence echantillonnage | Payload indique ~860 SPS (ADS1115) | SPS |
| 7 | Verifier signalQuality | > 90 (signal propre) | Score |
| 8 | Observer 10 envois consecutifs | Stabilite des valeurs | Ecart-type |
| 9 | Verifier backend ingestion | Donnees stockees, pas de rejet | Dashboard |
| 10 | Verifier analyse signal | Aucune anomalie detectee | Dashboard |

**Criteres PASS** :
- Precision RMS : ±5% vs reference
- Precision THD : < 2% absolu d'ecart
- Temperature : ±2°C vs reference
- Signal quality > 80
- Aucun rejet backend
- Aucune fausse anomalie

---

### SC-05 : Perte WiFi

**Objectif** : Valider la detection de deconnexion et la transition vers mode offline.

**Preconditions** :
- Device en ACTIVE, telemetrie en cours
- Switch manageable pret pour coupure

**Etapes** :

| # | Action | Resultat attendu | Mesure |
|---|--------|-----------------|--------|
| 1 | Desactiver port WiFi (switch) | Device detecte perte connexion | Temps detection |
| 2 | Observer transition etat | ACTIVE → OFFLINE_BUFFERING | Delai transition |
| 3 | Observer LED | Orange clignotant | Visuel |
| 4 | Verifier logs serie | "WiFi disconnected, entering offline mode" | Logs |
| 5 | Verifier que l'acquisition continue | Donnees acquises et bufferisees | Taille buffer |
| 6 | Attendre 5 min offline | Buffer croit, pas de crash | Stabilite |

**Criteres PASS** :
- Detection perte en < 10s
- Transition fluide sans crash
- Acquisition continue pendant offline
- Buffer fonctionne (taille croit)

---

### SC-06 : Reconnexion

**Objectif** : Valider la reconnexion automatique et le drain du buffer.

**Preconditions** :
- Device en OFFLINE_BUFFERING depuis > 2 minutes
- Buffer contient des donnees

**Etapes** :

| # | Action | Resultat attendu | Mesure |
|---|--------|-----------------|--------|
| 1 | Reactiver port WiFi (switch) | Device detecte reseau disponible | Temps detection |
| 2 | Observer reconnexion MQTT | Connexion retablie, heartbeat envoye | Delai reconnexion |
| 3 | Observer drain du buffer | Messages bufferises envoyes avec isDrain=true | Nombre messages |
| 4 | Verifier ordre des messages | Chronologique (FIFO) | Timestamps |
| 5 | Verifier timestamps | Backend accepte (window 72h) | Acceptance |
| 6 | Observer retour etat ACTIVE | LED : vert fixe, buffer vide | Temps drain |
| 7 | Verifier backend | Toutes les donnees recues sans gaps | Continuite |

**Criteres PASS** :
- Reconnexion automatique en < 30s
- Drain complet du buffer sans perte
- Ordre chronologique preserve
- Backend accepte les donnees differees
- Pas de duplication

---

### SC-07 : Buffer Offline (Stress)

**Objectif** : Valider la capacite et la robustesse du buffer offline sur duree prolongee.

**Preconditions** :
- Device en ACTIVE, WiFi coupe

**Etapes** :

| # | Action | Resultat attendu | Mesure |
|---|--------|-----------------|--------|
| 1 | Couper WiFi, laisser 1h offline | Buffer accumule ~60 messages telemetrie | Compteur buffer |
| 2 | Verifier memoire libre | freeHeap > 50 KB | Heap |
| 3 | Verifier filesystem | SPIFFS usage < 80% | Usage stockage |
| 4 | Simuler 4h offline (accelerated) | Buffer gere eviction FIFO si plein | Logs eviction |
| 5 | Reactiver WiFi | Drain complet (ou partiel si eviction) | Messages recus |
| 6 | Verifier integrite donnees | Pas de corruption (checksums OK) | Validation |

**Criteres PASS** :
- Buffer tient minimum 1h sans eviction
- Pas de crash memoire
- Eviction FIFO correcte si depassement
- Donnees post-drain non corrompues

---

### SC-08 : Donnees Hors Plage

**Objectif** : Valider la detection et le traitement de mesures hors des plages normales.

**Preconditions** :
- Device en ACTIVE, telemetrie normale

**Etapes** :

| # | Action | Resultat attendu | Mesure |
|---|--------|-----------------|--------|
| 1 | Injecter signal 0V (court-circuit ADC) | RMS = ~0, flag anomalie | Payload |
| 2 | Injecter signal > pleine echelle ADC | Saturation detectee, signalQuality baisse | Score qualite |
| 3 | Simuler temperature -50°C (valeur hors plage) | Valeur flaggee, envoyee avec warning | Payload flag |
| 4 | Simuler temperature +200°C (valeur hors plage) | Valeur flaggee, envoyee avec warning | Payload flag |
| 5 | Verifier backend | Rejets ou warnings selon severite | Dashboard |
| 6 | Verifier retour normal apres fin injection | Valeurs normales reprennent | Temps retour |

**Criteres PASS** :
- Pas de crash sur valeurs extremes
- Signalement correct (flag, signalQuality degradee)
- Backend rejette ou marque les valeurs incoherentes
- Retour automatique a la normale

---

### SC-09 : Signal Bruite

**Objectif** : Valider le comportement face a un bruit electromagnetique eleve.

**Preconditions** :
- Signal de reference 50 Hz 1V RMS actif
- Generateur de bruit pret

**Etapes** :

| # | Action | Resultat attendu | Mesure |
|---|--------|-----------------|--------|
| 1 | Ajouter bruit blanc SNR 30 dB | Mesure RMS stable, THD augmente legerement | THD, signalQuality |
| 2 | Augmenter bruit SNR 20 dB | signalQuality diminue, RMS toujours < ±10% | Scores |
| 3 | Augmenter bruit SNR 10 dB | signalQuality DEGRADED/POOR, event publie | Event |
| 4 | Augmenter bruit SNR 6 dB | Mesures potentiellement non fiables | Flags |
| 5 | Retirer bruit | Retour signalQuality > 80 en < 30s | Temps retour |
| 6 | Verifier backend | LowSignalQualityDetectedEvent pour SNR < 15 dB | Events |

**Criteres PASS** :
- Pas de crash meme a SNR tres bas
- SignalQuality reflete fidelement le bruit
- Events publies au-dela des seuils
- Retour rapide apres suppression bruit

---

### SC-10 : Temperature Elevee Simulee

**Objectif** : Valider la detection de tendances de temperature anormales.

> **SECURITE** : Utiliser un pistolet a air chaud a distance du PCB (chauffer uniquement la sonde PT100).
> Ne jamais depasser 80°C sur la sonde. Ne pas exposer directement le PCB a la chaleur.

**Preconditions** :
- Temperature ambiante stable ~22°C
- Sonde PT100 accessible pour chauffage controle

**Etapes** :

| # | Action | Resultat attendu | Mesure |
|---|--------|-----------------|--------|
| 1 | Baseline 5 min a 22°C | Temperature stable, pas d'alerte | Baseline |
| 2 | Chauffer sonde lentement (1°C/min) | Temperature monte, deltaT augmente | Rate |
| 3 | Atteindre 40°C | Seuil WARNING, event publie | Event type |
| 4 | Atteindre 55°C | Seuil HIGH, anomalie TEMPERATURE_RISING | Event + anomalie |
| 5 | Stabiliser a 55°C pendant 2 min | Anomalie maintenue, pas de repetition excessive | Dedup |
| 6 | Laisser refroidir naturellement | Temperature descend, anomalie resolue | Temps retour |
| 7 | Verifier backend scoring | RiskScore augmente pendant la montee | Score |

**Criteres PASS** :
- Detection montee en < 30s (1 cycle telemetrie)
- Seuils corrects (WARNING 40°C, HIGH 55°C)
- Anomalie publiee exactement 1 fois (ou avec dedup correcte)
- Retour a la normale detecte
- Backend scoring reagit

---

### SC-11 : Micro-Arc Simule

**Objectif** : Valider la detection de signatures de micro-arcs electriques.

> **SECURITE** : Les micro-arcs sont simules par injection d'impulsions HF basse tension (< 5V peak) sur l'ADC.
> NE PAS generer d'arcs reels. Cela necessite un environnement haute tension interdit en labo dev.

**Methode de simulation** : Generateur de fonctions configuré pour produire des bursts d'impulsions 50-100 kHz, amplitude 2V peak, duree 1-5 ms, repetition 1/s — simulant la signature spectrale d'un micro-arc sans danger.

**Preconditions** :
- Signal 50 Hz 1V RMS de base actif
- Generateur configure pour burst HF

**Etapes** :

| # | Action | Resultat attendu | Mesure |
|---|--------|-----------------|--------|
| 1 | Baseline 2 min sans burst | hfNoiseLevel bas, pas d'anomalie | Baseline HF |
| 2 | Activer burst unique (1 impulsion) | arcEnergy augmente legerement | arcEnergy |
| 3 | Activer bursts recurrents (1/s, 30s) | Compteur transientCount monte | transientCount |
| 4 | Maintenir 2 min | MICRO_ARC_SUSPECTED detecte par analyse | Anomalie type |
| 5 | Augmenter amplitude burst a 4V | arcEnergy plus eleve, anomalie confirmee | arcEnergy |
| 6 | Arreter bursts | hfNoiseLevel revient a normal | Temps retour |
| 7 | Verifier backend analyse | SignalAnomalyDetectedEvent type=MICRO_ARC | Event |
| 8 | Verifier backend scoring | RiskScore augmente significativement | Score |

**Criteres PASS** :
- Detection micro-arc en < 60s (apres recurrence suffisante)
- arcEnergy correle avec amplitude des bursts
- Anomalie MICRO_ARC publiee
- Pas de faux positif avant activation bursts
- Backend scoring reagit (score > 60)

---

### SC-12 : Transitoires Simules

**Objectif** : Valider la detection de transitoires electriques (surtensions breves).

**Methode de simulation** : Impulsion carree unique, amplitude 3V, duree 50-500 us, injectee sur signal 50 Hz.

**Preconditions** :
- Signal 50 Hz 1V RMS actif
- Generateur mode burst pulse configure

**Etapes** :

| # | Action | Resultat attendu | Mesure |
|---|--------|-----------------|--------|
| 1 | Baseline 1 min | transientCount = 0 | Baseline |
| 2 | Injecter 1 transitoire (3V, 200us) | transientCount++ | Compteur |
| 3 | Injecter 5 transitoires espaces 10s | transientCount = 5 | Compteur |
| 4 | Injecter rafale (10 en 5s) | transientCount = 10+, anomalie potentielle | Detection |
| 5 | Verifier telemetrie | Champ transientCount present et correct | Payload |
| 6 | Verifier analyse backend | Si recurrent → anomalie TRANSIENT_ABNORMAL | Analyse |

**Criteres PASS** :
- Chaque transitoire individuel detecte (> 80% taux detection)
- Compteur incrementé correctement
- Anomalie levee si recurrence > seuil
- Pas de faux comptage sans transitoire

---

### SC-13 : Firmware Version Mismatch

**Objectif** : Valider le comportement quand le firmware annonce une version non reconnue par le backend.

**Preconditions** :
- Backend configure pour accepter firmware v1.0.x
- Device flashe avec firmware v9.9.9 (version fictive)

**Etapes** :

| # | Action | Resultat attendu | Mesure |
|---|--------|-----------------|--------|
| 1 | Demarrer device avec fw v9.9.9 | Connexion MQTT reussie | Connexion |
| 2 | Envoyer telemetrie | Backend log warning "unknown firmware version" | Logs backend |
| 3 | Verifier acceptation donnees | Donnees acceptees (backward compatible) | Ingestion |
| 4 | Verifier event firmware tracking | FirmwareVersionEvent publie | Event |
| 5 | Flasher firmware v0.0.1 (trop ancien) | Backend rejette si schema incompatible | Rejet/Accept |

**Criteres PASS** :
- Version inconnue : warning mais pas rejet (forward compatible)
- Version trop ancienne : rejet avec code explicite
- Event de tracking firmware publie
- Device ne crash pas sur rejet

---

### SC-14 : Credential Revoked

**Objectif** : Valider le comportement quand les credentials du device sont revoquees cote backend.

**Preconditions** :
- Device provisionne et en etat ACTIVE
- Acces admin API pour revoquer

**Etapes** :

| # | Action | Resultat attendu | Mesure |
|---|--------|-----------------|--------|
| 1 | Device fonctionne normalement | Telemetrie + heartbeat OK | Baseline |
| 2 | Revoquer credentials via API admin | DeviceRevokedEvent publie | Event |
| 3 | Attendre prochain heartbeat du device | Backend rejette (DEVICE_REVOKED) | Rejet |
| 4 | Observer comportement device | MQTT disconnect, transition REVOKED | Etat |
| 5 | Verifier LED | Rouge fixe (REVOKED) | Visuel |
| 6 | Tenter reconnexion automatique | Echec, pas de boucle infinie de retry | Logs |
| 7 | Verifier qu'aucune donnee n'est acceptee | Tous messages rejetes | Dashboard |
| 8 | Re-provisionner (nouveau claim token) | Device reprend fonctionnement normal | Provisioning |

**Criteres PASS** :
- Revocation effective en < 1 cycle heartbeat
- Device transite proprement vers REVOKED
- Pas de boucle de retry infinie (max 3 tentatives)
- Re-provisioning possible apres revocation
- Aucune donnee acceptee apres revocation

---

## 6. Criteres d'Acceptation Globaux

### Criteres obligatoires (bloquants)

| # | Critere | Seuil | Methode |
|---|---------|-------|---------|
| CA-01 | Aucun crash/watchdog pendant la session complete | 0 occurence | Logs serie |
| CA-02 | Precision RMS sur signal de reference | ±5% | Comparaison oscilloscope |
| CA-03 | Precision THD sur signal connu | ±2% absolu | Comparaison analyseur |
| CA-04 | Precision temperature | ±2°C | Comparaison thermocouple |
| CA-05 | Provisioning securise fonctionnel | 100% success | SC-02 |
| CA-06 | Buffer offline sans perte | 0 message perdu (1h) | SC-07 |
| CA-07 | Reconnexion automatique | < 30s | SC-06 |
| CA-08 | Detection micro-arc simule | > 80% taux detection | SC-11 |
| CA-09 | Pas de faux positif en nominal | 0 anomalie sur 30 min signal propre | SC-04 |
| CA-10 | Backend accepte toutes les donnees valides | 0 rejet indu | Dashboard |

### Criteres souhaitables (non bloquants)

| # | Critere | Seuil | Impact si echoue |
|---|---------|-------|-----------------|
| CS-01 | Boot time | < 10s | Confort utilisateur |
| CS-02 | Latence telemetrie | < 2s end-to-end | Performance |
| CS-03 | Consommation memoire stable | Pas de leak sur 1h | Stabilite long terme |
| CS-04 | SignalQuality > 90 en conditions normales | > 90% du temps | Fiabilite scoring |
| CS-05 | Detection transitoires unitaires | > 90% | Sensibilite |

---

## 7. Mesures a Enregistrer

### Par scenario

| Mesure | Unite | Frequence | Outil |
|--------|-------|-----------|-------|
| Timestamps (debut/fin scenario) | ISO-8601 | Par scenario | Horloge ref |
| Resultat (PASS/FAIL/PARTIAL) | Enum | Par etape | Observateur |
| Logs serie complets | Texte | Continu | minicom → fichier |
| Captures ecran dashboard | PNG | Aux points cles | Screenshot |
| Valeurs de reference (oscilloscope) | CSV | Par point de mesure | Export oscillo |
| Metriques Prometheus | Time-series | Continu | Grafana snapshot |
| Captures MQTT (messages) | JSON | Continu | MQTT Explorer export |
| Captures reseau (si pertinent) | PCAP | Par scenario reseau | Wireshark |

### Metriques systeme device (a chaque heartbeat)

| Metrique | Unite | Seuil alerte |
|----------|-------|-------------|
| freeHeap | octets | < 50 KB |
| uptime | secondes | Reset inattendu = 0 |
| RSSI | dBm | < -80 dBm |
| bufferSize | messages | > 100 (hors scenario offline) |
| cpuTemperature | °C | > 70°C |

### Metriques backend

| Metrique | Unite | Source |
|----------|-------|--------|
| ingestion_received_total | counter | Prometheus |
| ingestion_rejected_total | counter | Prometheus |
| ingestion_latency_ms | histogram | Prometheus |
| analysis_anomalies_detected | counter | Prometheus |
| scoring_risk_score | gauge par device | Prometheus |

---

## 8. Format de Rapport de Test

Le rapport de test suit le template `docs/lab-test-report-template.md`.

### Structure

```
1. Informations generales (date, equipe, versions, materiel)
2. Resume executif (pass/fail, bloquants, highlights)
3. Resultats par scenario (tableau PASS/FAIL/PARTIAL + commentaires)
4. Mesures detaillees (tableaux de valeurs)
5. Anomalies detectees (reference vers fiches d'anomalie)
6. Metriques systeme (graphes Grafana attaches)
7. Conclusion et recommandations
8. Annexes (logs, captures, exports)
```

### Archivage

- Rapport nomme : `LAB-TEST-{YYYY-MM-DD}-{version}.md`
- Stocke dans : `docs/lab-reports/` (non commite en git si volumineux)
- Logs bruts : archives ZIP datees
- Grafana snapshots : liens permanents

---

## 9. Checklist Avant Test

### J-1 (veille de la session)

- [ ] Confirmer disponibilite de l'equipe (4 roles)
- [ ] Verifier que le firmware est a jour et compile sans erreur
- [ ] Verifier que la stack backend Docker est fonctionnelle
- [ ] Preparer les generateurs de signaux (verifier cables, sondes)
- [ ] Imprimer le protocole de test (reference papier)
- [ ] Charger batteries / verifier alimentations
- [ ] Reserver la salle de labo

### J (jour du test, avant demarrage)

- [ ] Verifier mise a la terre du banc
- [ ] Verifier absence de tension residuelle
- [ ] Calibrer instruments de reference (zero ADC, zero temperature)
- [ ] Demarrer Docker stack backend et verifier healthchecks
- [ ] Demarrer Grafana, MQTT Explorer, serial monitor
- [ ] Flasher firmware (version notee dans rapport)
- [ ] Verifier connectivite WiFi du banc de test
- [ ] Effectuer un dry-run SC-01 (boot rapide) pour valider le setup
- [ ] Ouvrir le template de rapport de test
- [ ] Synchroniser les horloges (NTP) des PC et du device

---

## 10. Checklist Apres Test

### Immediat (dans les 30 min)

- [ ] Mettre hors tension le banc de test
- [ ] Sauvegarder tous les logs serie dans le dossier archive
- [ ] Exporter les captures MQTT Explorer
- [ ] Sauvegarder les snapshots Grafana
- [ ] Completer le tableau de resultats (PASS/FAIL)
- [ ] Ouvrir les fiches d'anomalie pour chaque FAIL
- [ ] Backup le firmware flashe (binaire + sha256)
- [ ] Ranger le materiel (generateurs eteints, cables enroules)

### J+1 (lendemain)

- [ ] Rediger le rapport de test complet
- [ ] Prioriser les anomalies (bloquant / majeur / mineur)
- [ ] Planifier les corrections firmware si bloquants
- [ ] Communiquer le resume a l'equipe
- [ ] Archiver rapport + annexes
- [ ] Mettre a jour le TODO avec les actions correctives

---

## 11. Gestion des Incidents

### Incident de securite

| Situation | Action immediate | Responsable |
|-----------|-----------------|-------------|
| Odeur de brule / fumee | Couper alimentation (bouton urgence), evacuer | Tous |
| Choc electrique (improbable en TBTS) | Couper alimentation, premiers secours | Ingenieur HW |
| Court-circuit visible (etincelle) | Couper alimentation, ne pas toucher | Ingenieur HW |
| Surchauffe composant (> 80°C) | Couper alimentation, laisser refroidir | Ingenieur HW |

### Incident technique

| Situation | Action | Impact |
|-----------|--------|--------|
| Crash firmware (watchdog) | Collecter logs, noter contexte, rebooter | Fiche anomalie CRITICAL |
| Perte de donnees buffer | Noter taille buffer avant/apres, analyser logs | Fiche anomalie HIGH |
| Backend indisponible | Relancer Docker stack, verifier volumes | Pause test (max 15 min) |
| Instrument de reference defaillant | Suspendre test, recalibrer ou remplacer | Pause test |
| Resultats incoherents | Verifier cablage, repeter mesure 3 fois | Si reproductible : fiche anomalie |

### Escalade

1. **Incident mineur** (deviation < 10% seuil) : note dans rapport, continue
2. **Incident majeur** (scenario FAIL) : fiche d'anomalie, continue les autres scenarios
3. **Incident bloquant** (crash, securite, perte totale) : arret session, escalade tech lead
4. **Incident securite** : arret immediat, evacuation si necessaire, rapport incident

---

## 12. Criteres de Non-Passage au Terrain

Le passage au terrain est **INTERDIT** si l'un des criteres suivants est observe :

### Bloquants absolus

| # | Critere | Raison |
|---|---------|--------|
| NP-01 | Crash firmware non resolu | Risque de perte de monitoring sans detection |
| NP-02 | Precision RMS > ±10% | Mesures non fiables, scoring fausse |
| NP-03 | Buffer offline perd des donnees | Gaps non detectables sur le terrain |
| NP-04 | Provisioning securise echoue | Device non securisable |
| NP-05 | Faux positifs > 5% en nominal | Alertes intempestives, perte de confiance |
| NP-06 | Revocation credentials inefficace | Risque securite (device compromis non isolable) |
| NP-07 | Reconnexion > 5 min | Gaps de monitoring trop importants |
| NP-08 | Memory leak detecte (heap decroit > 1KB/h) | Crash previsible apres quelques jours |
| NP-09 | Watchdog trigger > 1 fois en 4h | Instabilite firmware |
| NP-10 | Aucune detection sur micro-arc simule | Objectif premier du capteur non atteint |

### Conditions de passage terrain

Tous les criteres NP-01 a NP-10 doivent etre clears, ET :

- Minimum 4h de fonctionnement continu sans incident
- Tous les scenarios CA-01 a CA-10 PASS
- Rapport de test signe par les 4 roles
- Fiches d'anomalie bloquantes resolues et re-testees
- Version firmware figee (tag git, binaire archive)
- Backend deploy et accessible depuis le site terrain

---

## 13. Recommandations pour Amelioration Firmware/Backend

### Firmware

| # | Recommandation | Priorite | Impact |
|---|---------------|----------|--------|
| F-01 | Implementer watchdog hardware (TWDT) en plus du software | HAUTE | Prevention hard-lock |
| F-02 | Ajouter self-test au boot (ADC loopback, temp read, SPIFFS check) | HAUTE | Detection panne capteur |
| F-03 | Implementer OTA avec rollback automatique (3 boot failures) | MOYENNE | Mise a jour terrain sans intervention |
| F-04 | Ajouter compression LZ4 sur buffer SPIFFS | MOYENNE | +300% capacite offline |
| F-05 | Implementer mode DEGRADED (reduire features si heap < 30KB) | MOYENNE | Survie en conditions extremes |
| F-06 | Ajouter heartbeat diagnostique etendu (1/heure) avec details memoire | BASSE | Monitoring preventif |
| F-07 | Implementer rate limiting publication (max 10 msg/s drain) | HAUTE | Protection backend |
| F-08 | Ajouter monotonic sequence counter per-reboot | BASSE | Detection replay plus robuste |
| F-09 | Logger localement les raisons de reboot (RTC memory) | MOYENNE | Diagnostic terrain |
| F-10 | Implementer calibration automatique au boot (ADC offset) | HAUTE | Precision sans intervention manuelle |

### Backend

| # | Recommandation | Priorite | Impact |
|---|---------------|----------|--------|
| B-01 | Ajouter alerte si device ne heartbeat pas depuis > 3 intervalles | HAUTE | Detection panne terrain |
| B-02 | Implementer dashboard device health (derniere connexion, signal quality trend) | HAUTE | Visibilite operateur |
| B-03 | Ajouter metriques de precision par device (drift RMS vs baseline) | MOYENNE | Detection derive capteur |
| B-04 | Implementer commande backend → device (reboot, recalibrate, update config) | MOYENNE | Intervention a distance |
| B-05 | Ajouter retention policy scoring_adjustments (archivage > 1 an) | BASSE | Gouvernance donnees |
| B-06 | Implementer validation croisee (2 capteurs meme circuit) si deploy multi | BASSE | Reduction faux positifs |
| B-07 | Ajouter export CSV des mesures brutes pour analyse offline | MOYENNE | Debug terrain |
| B-08 | Implementer circuit breaker sur device-service HTTP call | HAUTE | Resilience ingestion |
| B-09 | Ajouter correlation scoring ↔ feedback pour auto-tuning seuils | MOYENNE | Feedback loop complet |
| B-10 | Preparer data pipeline vers feature store (Parquet/S3) pour ML futur | BASSE | Preparation MVP 3+ |

### Integration

| # | Recommandation | Priorite | Impact |
|---|---------------|----------|--------|
| I-01 | Automatiser l'execution des scenarios (script Python + assertions) | HAUTE | Repetabilite, regression |
| I-02 | Creer CI firmware : build + host tests + size check | HAUTE | Qualite firmware continue |
| I-03 | Ajouter test end-to-end automatise : inject → ingest → analyze → score | MOYENNE | Validation pipeline complete |
| I-04 | Documenter les signaux de reference (fichiers WAV/CSV) | MOYENNE | Reproductibilite inter-sessions |
| I-05 | Preparer kit de test portable pour validation terrain initiale | BASSE | Verification post-installation |
