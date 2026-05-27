# Fiche d'Anomalie — PyroSense AI Lab Test

## Ref : DEF-{YYYY}-{NNN}

---

## Identification

| Champ | Valeur |
|-------|--------|
| ID | DEF-{YYYY}-{NNN} |
| Date de detection | {YYYY-MM-DD HH:MM} |
| Session de test | LAB-TEST-{YYYY-MM-DD}-{version} |
| Scenario | SC-{XX} : {nom scenario} |
| Etape du scenario | Etape {N} |
| Detecte par | {nom} ({role}) |

---

## Classification

| Champ | Valeur |
|-------|--------|
| Severite | CRITICAL / HIGH / MEDIUM / LOW |
| Type | FIRMWARE / HARDWARE / BACKEND / INTEGRATION / CONFIGURATION |
| Composant | {module concerne} |
| Reproductible | OUI / NON / INTERMITTENT |
| Bloquant terrain | OUI / NON |

### Grille de severite

| Severite | Definition | Impact terrain |
|----------|-----------|---------------|
| CRITICAL | Crash, perte donnees, securite | Deployment impossible |
| HIGH | Fonctionnalite principale degradee | Risque operationnel eleve |
| MEDIUM | Fonctionnalite secondaire impactee | Acceptable avec contournement |
| LOW | Cosmetique, inconfort, optimisation | Pas d'impact operationnel |

---

## Description

### Comportement observe

{Description factuelle de ce qui s'est passe, sans interpretation}

### Comportement attendu

{Ce qui aurait du se passer selon le protocole de test}

### Ecart

{Difference precise entre observe et attendu (valeurs, timing, etat)}

---

## Contexte de Reproduction

### Conditions

| Condition | Valeur |
|-----------|--------|
| Firmware version | v{X.Y.Z} (hash: {short}) |
| Backend version | v{X.Y.Z} |
| Signal injecte | {type, amplitude, frequence} |
| Temperature ambiante | {X}°C |
| RSSI WiFi | {X} dBm |
| Uptime device | {X} min |
| freeHeap | {X} KB |
| Buffer size | {X} messages |

### Etapes de reproduction

1. {Etape 1}
2. {Etape 2}
3. {Etape 3}
4. → Anomalie observee

### Frequence

- [ ] Systematique (100%)
- [ ] Frequent (> 50%)
- [ ] Intermittent (10-50%)
- [ ] Rare (< 10%)
- [ ] Observe une seule fois

---

## Preuves

### Logs

```
{Extrait de logs pertinent — logs serie firmware ou logs backend}
```

### Captures

| Type | Fichier | Description |
|------|---------|-------------|
| Log serie | `{fichier.log}` ligne {N} | {contexte} |
| Screenshot Grafana | `{fichier.png}` | {metrique concernee} |
| Capture oscilloscope | `{fichier.png/csv}` | {signal au moment du defaut} |
| Export MQTT | `{fichier.json}` | {message problematique} |
| Capture Wireshark | `{fichier.pcap}` | {si probleme reseau} |

---

## Analyse Preliminaire

### Hypothese(s)

1. {Hypothese 1 : cause probable}
2. {Hypothese 2 : cause alternative}

### Composant(s) suspecte(s)

- [ ] Firmware : {module/fichier}
- [ ] Hardware : {composant}
- [ ] Backend : {service/classe}
- [ ] Configuration : {parametre}
- [ ] Environnement : {condition externe}

### Impact si non corrige

{Quel serait l'impact sur une installation terrain si ce defaut n'est pas corrige}

---

## Resolution

### Statut

- [ ] OUVERT — en attente d'analyse
- [ ] EN COURS — correction en developpement
- [ ] CORRIGE — fix implemente, en attente de re-test
- [ ] VERIFIE — re-teste avec succes
- [ ] CLOS — resolu et archive
- [ ] DIFFERE — accepte pour version ulterieure (justification requise)
- [ ] INVALIDE — non reproductible ou comportement normal

### Correction appliquee

| Champ | Valeur |
|-------|--------|
| Fix par | {nom} |
| Date fix | {YYYY-MM-DD} |
| Commit/branche | {hash / branche} |
| Description fix | {ce qui a ete change} |
| Effet de bord potentiel | {ou "aucun"} |

### Re-test

| Champ | Valeur |
|-------|--------|
| Re-teste par | {nom} |
| Date re-test | {YYYY-MM-DD} |
| Session re-test | LAB-TEST-{ref} |
| Resultat | PASS / FAIL |
| Commentaire | |

---

## Contournement (si disponible)

{Description d'un contournement temporaire permettant de continuer les tests ou de deployer avec limitation connue}

---

## Historique

| Date | Auteur | Action |
|------|--------|--------|
| {YYYY-MM-DD} | {nom} | Creation de la fiche |
| | | |
| | | |

---

## Liens

| Lien | Reference |
|------|-----------|
| Session de test | LAB-TEST-{ref} |
| Fiche(s) liee(s) | DEF-{ref} |
| Commit fix | {url ou hash} |
| Documentation impactee | {doc si mise a jour necessaire} |
