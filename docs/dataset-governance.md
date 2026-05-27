# Dataset Governance - PyroSense AI

## Principes

1. **Privacy by Design** — les donnees personnelles et identifiants tenant/device sont pseudonymises avant stockage dans les tables dataset.
2. **Minimisation** — seuls les features agrégées et statistiques sont collectees, jamais les echantillons bruts 230V.
3. **Tracabilite** — chaque label, feedback et export est audite avec timestamp, auteur et justification.
4. **Separation des responsabilites** — le tenant owner controle ses donnees ; le platform admin supervise la qualite globale.
5. **Qualite avant quantite** — un dataset de 100 candidats bien labellises vaut mieux que 10 000 candidats sans label.

## Roles et Acces

| Role | Feedback | Candidats | Export | Labels |
|------|:--------:|:---------:|:------:|:------:|
| PLATFORM_ADMIN | Oui | Lecture | Oui | Oui |
| TENANT_ADMIN | Oui | Lecture | Oui | Oui |
| ELECTRICIAN | Oui (soumission) | Non | Non | Non |
| DEVICE_MANAGER | Oui (soumission) | Non | Non | Non |

## Cycle de Vie d'un Candidat

```
                   ┌────────────────┐
                   │ PENDING_LABEL  │ ← creation automatique
                   └───────┬────────┘
                           │ label ajoute (low confidence)
                           ▼
                   ┌────────────────┐
                   │    LABELED     │
                   └───────┬────────┘
                           │ label confirme (high confidence)
                           ▼
                   ┌────────────────┐
                   │   VALIDATED    │
                   └───────┬────────┘
                           │ export
                           ▼
                   ┌────────────────┐
                   │   EXPORTED     │
                   └────────────────┘

                   ┌────────────────┐
                   │   REJECTED     │ ← qualite insuffisante ou corruption
                   └────────────────┘
```

## Regles de Gouvernance

### R1 — Aucun export sans pseudonymisation
Le tenantId reel et le deviceId reel ne doivent jamais apparaitre dans un dataset exporte. La pseudonymisation est appliquee a l'ecriture (pas a l'export).

### R2 — Exports audites
Chaque demande d'export est logguee avec : qui, quand, quels filtres, combien de candidats. Le log d'audit est immutable.

### R3 — Labels avec source
Chaque label doit conserver sa source :
- **TECHNICIAN** : retour terrain lie a une intervention
- **LAB** : mesure en laboratoire avec reference certifiee
- **SYSTEM** : detection automatique par le pipeline d'analyse
- **MANUAL_REVIEW** : revue manuelle par un data scientist

### R4 — Feedback lie a intervention
Le feedback terrain doit obligatoirement etre lie a une intervention existante. Pas de feedback "orphelin".

### R5 — Faux positifs alimentent l'amelioration
Les FalsePositiveFeedback sont exploitees pour :
- Ajuster les seuils de detection (SignalAnalysisEngine)
- Reentrainer les modeles ML futurs
- Reduire le taux de faux positifs (objectif < 5%)

### R6 — Qualite minimum pour export
Un candidat n'est exportable que si :
- quality_tier = HIGH ou MEDIUM (score >= 75)
- Au moins un label est present (sauf `includeUnlabeled=true`)
- Le statut est LABELED ou VALIDATED

### R7 — Retention et suppression
- Les candidats non exportes apres 365 jours sont automatiquement archives
- Les candidats REJECTED sont supprimes apres 90 jours
- Le droit a l'effacement (RGPD Art.17) s'applique — la pseudonymisation rend l'identification impossible sans la cle

## Controles Qualite

| Controle | Frequence | Seuil | Action |
|----------|-----------|-------|--------|
| Ratio labellise/non-labellise | Hebdomadaire | > 30% non-labellise | Notification aux admins |
| Distribution des labels | Mensuelle | > 80% NORMAL | Verifier biais d'echantillonnage |
| Confiance moyenne | Mensuelle | < 0.7 | Former les techniciens |
| Coherence inter-labelleurs | Trimestrielle | Kappa < 0.6 | Reviser la taxonomy |
| Export sans label filter | A chaque export | N/A | Warning si > 1000 candidats sans filtre |

## Conformite RGPD

| Article | Mesure |
|---------|--------|
| Art. 5(1)(c) — Minimisation | Seuls features agrégées, pas de donnees brutes 230V |
| Art. 5(1)(e) — Limitation conservation | Retention 365j max sur candidats non-exportes |
| Art. 6(1)(f) — Interet legitime | Amelioration de la securite electrique = interet legitime |
| Art. 17 — Droit a l'effacement | Pseudonymisation irreversible sans cle |
| Art. 25 — Privacy by design | HMAC a l'ecriture, pas de re-identification possible |
| Art. 30 — Registre des traitements | Audit log de chaque operation |
| Art. 35 — AIPD | Non requise (donnees pseudonymisees, pas de profilage) |

## Environnement Laboratoire

En mode laboratoire (`environment=lab`), les regles suivantes sont assouplies :
- Les donnees brutes electriques peuvent etre stockees (pour validation algorithmes)
- Le pseudonymisation reste obligatoire
- Les labels LAB ont une confiance automatique de 1.0
- Les exports sont restreints a l'equipe R&D (PLATFORM_ADMIN uniquement)
