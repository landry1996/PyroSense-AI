# PyroSense Reporting Service

## Rôle

Produire des rapports PDF et exports pour les parties prenantes : gestionnaires immobiliers, assureurs, occupants.

## Types de rapports

| Type | Préfixe | Description |
|------|---------|-------------|
| `MONTHLY_HEALTH` | MH | Bilan mensuel de santé électrique |
| `CONTINUOUS_MONITORING_CERTIFICATE` | CM | Attestation de surveillance continue |
| `CRITICAL_ALERT_REPORT` | CA | Rapport des alertes critiques |
| `INTERVENTION_REPORT` | IR | Rapport d'interventions |
| `ROI_AVOIDED_INCIDENTS` | ROI | ROI incidents évités |
| `INSURER_EXPORT` | IE | Export assureur |

## Architecture

```
adapter/
├── in/rest/         → ReportController (REST API)
├── out/renderer/    → OpenPdfReportRenderer (PDF via OpenPDF)
├── out/persistence/ → JdbcReportRepository, InMemoryDownloadTokenStore
├── out/dataprovider/→ StubReportDataProvider (MVP, à remplacer)
└── out/messaging/   → KafkaReportEventPublisher
application/
├── port/in/         → GenerateReportUseCase, GetReportQuery
├── port/out/        → ReportRepositoryPort, ReportRendererPort, ReportDataProviderPort, DownloadTokenStorePort, ReportEventPublisherPort
└── usecase/         → GenerateReportService, GetReportService
domain/
├── model/           → Report, ReportType, ReportStatus, ReportMetadata, ReportSignature, DownloadToken
└── event/           → ReportGeneratedEvent, ComplianceCertificateGeneratedEvent
config/              → SecurityConfig, UseCaseConfig, KafkaConfig
```

## API REST

### Générer un rapport
```
POST /api/v1/reports/monthly
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "tenantId": "uuid",
  "buildingId": "uuid",
  "type": "MONTHLY_HEALTH",
  "periodStart": "2025-01-01T00:00:00Z",
  "periodEnd": "2025-01-31T23:59:59Z"
}
```

### Consulter un rapport
```
GET /api/v1/reports/{id}
Authorization: Bearer <jwt>
```

### Obtenir un lien de téléchargement sécurisé
```
GET /api/v1/reports/{id}/download-token
Authorization: Bearer <jwt>

Response: { "token": "...", "expiresAt": "..." }
```

### Télécharger un rapport (lien temporaire)
```
GET /api/v1/reports/{id}/download?token=<download-token>
```

**Sécurité du téléchargement :**
- Token à usage unique (invalidé après utilisation)
- Expiration : 15 minutes
- Token cryptographiquement sûr (SecureRandom 32 bytes, Base64url)
- Pas besoin d'authentification JWT pour le download (le token est suffisant)

### Lister les rapports
```
GET /api/v1/reports?tenantId=uuid&type=MONTHLY_HEALTH
GET /api/v1/reports/building/{buildingId}
```

## Événements publiés

| Événement | Topic | Déclencheur |
|-----------|-------|-------------|
| `ReportGeneratedEvent` | reporting-events | Tout rapport généré |
| `ComplianceCertificateGeneratedEvent` | reporting-events | Attestation de conformité générée |

## Sécurité

- **Assureurs** : accès uniquement aux rapports `INSURER_EXPORT` et `CONTINUOUS_MONITORING_CERTIFICATE`
- **Occupants** : accès uniquement à leur logement
- **Gestionnaires** : accès à tout leur portefeuille
- **Tenant isolation** : filtre par `tenant_id` JWT claim

## Signature logique

Chaque rapport généré est signé avec SHA-256. Le hash est stocké dans la table `reports` et retourné dans la réponse API. Cela permet de vérifier l'intégrité du document sans PKI.

## Numérotation unique

Format : `{PREFIX}-{YYYYMM}-{SEQUENCE:05d}`

Exemple : `MH-202503-00042`

## Configuration

| Variable | Description | Défaut |
|----------|-------------|--------|
| `DB_USERNAME` | PostgreSQL user | pyrosense |
| `DB_PASSWORD` | PostgreSQL password | pyrosense |
| `KAFKA_SERVERS` | Kafka bootstrap servers | localhost:9092 |
| `KEYCLOAK_ISSUER` | Keycloak issuer URI | http://localhost:8180/realms/pyrosense |
| `KEYCLOAK_JWK_SET` | Keycloak JWK Set URI | (auto from issuer) |

Port : **8091**

## Tests

- **Domain** : ReportTest (12), DownloadTokenTest (7), ReportSignatureTest (4)
- **Use cases** : GenerateReportServiceTest (5), GetReportServiceTest (8)
- **Integration** : OpenPdfReportRendererTest (4) — vérifie la génération PDF réelle
- **Architecture** : ReportingArchitectureTest (10) — ArchUnit hexagonal
- **Context** : ReportingServiceApplicationTest (1)

## Évolutions prévues

- Remplacer `StubReportDataProvider` par un adaptateur réel (appels aux services alerting/maintenance)
- Templates HTML pour personnalisation avancée des rapports
- Stockage des PDF en Object Storage (S3/MinIO) au lieu de BYTEA
- Planification automatique des rapports mensuels (cron)
- Notification email à la génération d'un rapport
