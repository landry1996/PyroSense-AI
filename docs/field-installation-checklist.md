# Checklist Installation Capteur PyroSense AI — Terrain

## Informations Generales

| Champ | Valeur |
|-------|--------|
| Date | ____/____/________ |
| Site | |
| Tableau electrique | |
| Circuit cible | |
| Device serial | PS-___ |
| Electricien | |
| Habilitation | B2V / BR / B2V-BR |
| N° habilitation | |
| Accompagnant | |

---

## Phase 1 : Pre-installation (avant deplacement)

| # | Verification | OK | N/A | Commentaire |
|---|-------------|:--:|:---:|-------------|
| 1.1 | Pre-visite realisee et fiche site validee (GO) | | | |
| 1.2 | Accord ecrit du gestionnaire obtenu | | | |
| 1.3 | Materiel prepare et verifie (capteur, CT, alim, outils) | | | |
| 1.4 | Claim token genere et note | | | |
| 1.5 | Backend operationnel (healthcheck OK) | | | |
| 1.6 | Routeur WiFi site confirme allume | | | |
| 1.7 | Cles/acces au local technique confirmes | | | |
| 1.8 | Creneau horaire valide avec gestionnaire | | | |

---

## Phase 2 : Securite sur site

| # | Verification | OK | N/A | Commentaire |
|---|-------------|:--:|:---:|-------------|
| 2.1 | EPI disponibles (gants isolants, ecran facial, tapis) | | | |
| 2.2 | Gestionnaire ou representant informe de l'intervention | | | |
| 2.3 | Acces pompiers non obstrue | | | |
| 2.4 | Identification du disjoncteur general | | | |
| 2.5 | Identification du disjoncteur du circuit cible | | | |
| 2.6 | Verification protection differentielle amont (30mA) | | | |
| 2.7 | Zone de travail degagee | | | |

---

## Phase 3 : Consignation et mise hors tension

> **OBLIGATOIRE : Seul l'electricien habilite execute cette phase.**

| # | Action | OK | Heure |
|---|--------|:--:|-------|
| 3.1 | Ouverture du disjoncteur du circuit cible | | |
| 3.2 | Condamnation (cadenas ou etiquette) | | |
| 3.3 | VAT (Verification Absence de Tension) — en aval | | |
| 3.4 | Confirmation : "tension absente, travail autorise" | | |

---

## Phase 4 : Installation physique

| # | Action | OK | Commentaire |
|---|--------|:--:|-------------|
| 4.1 | Pose pince amperometrique (CT) sur conducteur phase du circuit | | |
| 4.2 | Verification sens de la pince (fleche vers charge) | | |
| 4.3 | Pince fermee correctement (pas de jeu) | | |
| 4.4 | Passage cable CT proprement (pas de tension mecanique) | | |
| 4.5 | Fixation module capteur sur rail DIN | | |
| 4.6 | Verification fixation solide (pas de jeu) | | |
| 4.7 | Raccordement alimentation (module 230V→5V DIN) | | |
| 4.8 | Verification cablage alimentation (section, serrage) | | |
| 4.9 | Raccordement sonde temperature (si applicable) | | |
| 4.10 | Verification : capteur n'obstrue pas acces disjoncteurs | | |
| 4.11 | Verification : cablage ne gene pas fermeture coffret | | |

---

## Phase 5 : Remise sous tension

| # | Action | OK | Heure |
|---|--------|:--:|-------|
| 5.1 | Verification visuelle finale (pas de cable mal place) | | |
| 5.2 | Fermeture coffret tableau (si possible avec capteur dedans) | | |
| 5.3 | Retrait condamnation | | |
| 5.4 | Remise sous tension disjoncteur circuit | | |
| 5.5 | Verification LED capteur : bleu clignotant (BOOTING) | | |
| 5.6 | Attente transition : vert clignotant (CONNECTING) | | |
| 5.7 | Verification absence de bruit/chaleur anormale | | |

---

## Phase 6 : Provisioning et verification

| # | Action | OK | Valeur |
|---|--------|:--:|--------|
| 6.1 | Saisir claim token | | Token: ____________ |
| 6.2 | LED passe a vert fixe (ACTIVE) | | Heure: ___:___ |
| 6.3 | Premier heartbeat visible dans dashboard | | Delai: ___ s |
| 6.4 | Premiere telemetrie visible dans dashboard | | Delai: ___ s |
| 6.5 | Verifier RSSI affiche | | RSSI: ___ dBm |
| 6.6 | Verifier signalQuality | | Score: ___ /100 |
| 6.7 | Verifier temperature coherente | | Temp: ___°C |
| 6.8 | Verifier RMS coherent (si charge active) | | RMS: ___ V |
| 6.9 | Aucune alerte indue dans les 5 premieres minutes | | |

---

## Phase 7 : Finalisation

| # | Action | OK | Commentaire |
|---|--------|:--:|-------------|
| 7.1 | Etiquette posee sur capteur (serial, circuit, date, contact) | | |
| 7.2 | Photo post-installation (tableau ouvert + capteur visible) | | |
| 7.3 | Photo etiquette lisible | | |
| 7.4 | Mise a jour registre devices (statut: DEPLOYED) | | |
| 7.5 | Informer gestionnaire : "installation terminee, tout OK" | | |
| 7.6 | Remettre cles/acces au gestionnaire | | |
| 7.7 | Ranger outillage, nettoyer zone | | |

---

## Phase 8 : Documentation

| # | Document | Fait | Commentaire |
|---|----------|:----:|-------------|
| 8.1 | PV installation signe (electricien + gestionnaire) | | |
| 8.2 | Registre devices mis a jour | | |
| 8.3 | Photos transmises a l'equipe | | |
| 8.4 | Fiche site mise a jour (post-install) | | |
| 8.5 | Checklist presente archivee | | |

---

## Signatures

| Role | Nom | Date | Signature |
|------|-----|------|-----------|
| Electricien installateur | | | |
| Accompagnant technique | | | |
| Gestionnaire/representant | | | |

---

## Notes et observations

```
(Espace libre pour remarques, difficultes rencontrees, points d'attention)




```

---

## Etiquette Capteur (a imprimer et coller)

```
┌─────────────────────────────────┐
│  PyroSense AI — Pilote R&D      │
│                                  │
│  Device: PS-___                  │
│  Circuit: ___________________    │
│  Date install: ____/____/______  │
│                                  │
│  Contact urgence:                │
│  Tel: ___________________        │
│  Email: _________________        │
│                                  │
│  NE PAS DECONNECTER             │
│  sans contacter l'equipe         │
└─────────────────────────────────┘
```
