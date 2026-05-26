# Templates PDF Professionnels — PyroSense AI Platform

## Vue d'ensemble

Le module de rapports PDF professionnels fournit 4 types de documents conformes aux exigences réglementaires françaises, avec un design sobre, professionnel et compatible impression N&B.

## Types de rapports

| Type | Classe | Usage |
|------|--------|-------|
| Bilan Mensuel de Santé Électrique | `MONTHLY_HEALTH` | Rapport périodique complet |
| Attestation de Surveillance Continue | `CONTINUOUS_MONITORING_CERTIFICATE` | Certificat de conformité monitoring |
| Rapport d'Alerte Critique | `CRITICAL_ALERT_REPORT` | Alerte urgente nécessitant intervention |
| Rapport d'Intervention Préventive | `INTERVENTION_REPORT` | Suivi post-intervention |

## Architecture

```
ReportMetadata (données brutes)
       │
       ▼
ReportViewModelBuilder (logique métier)
       │
       ▼
ReportViewModel (modèle intermédiaire)
       │
       ▼
ProfessionalPdfRenderer (rendu OpenPDF)
       │
       ▼
byte[] (PDF A4)
```

### Composants clés

- **ReportViewModel** (`domain.model.ReportViewModel`) — Record immuable contenant Header, Sections, Disclaimer, Signature, Footer
- **ReportViewModelBuilder** (`domain.model.ReportViewModelBuilder`) — Factory statique par type de rapport
- **PdfRendererPort** (`application.port.out.PdfRendererPort`) — Port hexagonal
- **ProfessionalPdfRenderer** (`adapter.out.renderer.ProfessionalPdfRenderer`) — Implémentation OpenPDF

## Structure des sections par type

### Bilan Mensuel (`MONTHLY_HEALTH`)
1. Synthèse exécutive
2. Indicateurs clés (KPI cards)
3. Analyse des risques
4. Bilan des alertes
5. Bilan des interventions
6. Recommandations

### Attestation (`CONTINUOUS_MONITORING_CERTIFICATE`)
1. Objet de l'attestation
2. Paramètres de surveillance
3. Synthèse des détections
4. Conclusion

### Alerte Critique (`CRITICAL_ALERT_REPORT`)
1. Nature de l'alerte
2. Contexte de détection
3. Évaluation du risque
4. Actions recommandées

### Intervention (`INTERVENTION_REPORT`)
1. Résumé de l'intervention
2. Indicateurs d'intervention
3. Impact sur le risque
4. Recommandations post-intervention (conditionnelles)

## Classification des risques

| Score | Niveau | Couleur |
|-------|--------|---------|
| 0–24 | LOW | Vert (30, 130, 60) |
| 25–49 | MODERATE | Gris (100, 100, 100) |
| 50–74 | HIGH | Orange (180, 120, 0) |
| 75–100 | CRITICAL | Rouge (180, 30, 30) |

## Contraintes légales

Chaque rapport contient un disclaimer légal respectant les règles suivantes :
- Mentionne **"aide à la décision"** — le système est un outil, pas un verdict
- Mentionne **"monitoring prédictif"** — transparence sur la nature algorithmique
- Utilise **"recommandation d'inspection"** ou "préconisation" — jamais d'injonction
- **Ne garantit jamais l'absence d'incendie** — aucune certitude absolue
- Référence les normes (NF C 15-100, décret n°2010-1016) sans s'y substituer

### Disclaimer standard
> Ce rapport est un outil d'aide à la décision basé sur le monitoring prédictif des installations électriques. Il ne constitue en aucun cas un diagnostic réglementaire [...]

### Disclaimer certificat
> Cette attestation est un outil d'aide à la décision délivré dans le cadre du monitoring prédictif. Elle ne constitue en aucun cas un diagnostic réglementaire [...]

## Design PDF

- Format A4, marges 40pt
- En-tête : bande sombre (45, 55, 72) avec titre blanc
- Badge de risque coloré (aligné droite)
- KPI en cartes grises (3 par ligne)
- Séparateurs fins entre sections
- Pied de page : "PyroSense AI Platform vX.X.X — DD/MM/YYYY HH:mm" + numéro de rapport + pagination
- Bloc signature logique (SHA-256 + timestamp)
- Police Helvetica 7–16pt selon contexte

## Rétrocompatibilité

Le `GenerateReportService` supporte les deux modes :
- Si `PdfRendererPort` est injecté → utilise le rendu professionnel via ViewModel
- Sinon → fallback sur l'ancien `ReportRendererPort` (rendu simplifié)

## Tests

- `ReportViewModelBuilderTest` — 12 tests (logique métier, classification risque/sévérité)
- `ProfessionalPdfRendererTest` — 17 tests (rendu PDF valide, sections obligatoires, disclaimers, signature)
