# MVP 2 - Guide de Lancement Local

## Prerequis

- Docker Desktop 4.x+ (Docker Engine 24+, Compose V2)
- ~8 GB RAM disponible pour Docker
- Ports libres : 4200, 5432, 6379, 8080, 8086-8091, 9090, 3000, 29092

## Demarrage Rapide

```bash
# Demarrer tout le MVP 2 (build + run)
./scripts/start-mvp2-local.sh

# Sans rebuild (si deja construit)
./scripts/start-mvp2-local.sh --no-build

# Infrastructure seule (pour dev IDE)
./scripts/start-mvp2-local.sh --infra-only
```

## Architecture du Profil `mvp2`

Le profil `mvp2` demarre un sous-ensemble cible des services :

```
┌─────────────────────────────────────────────────────────┐
│  Frontend (http://localhost:4200)                        │
│  Angular Dashboard → nginx → proxy /api/                │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│  API Gateway (http://localhost:8080)                     │
│  JWT validation, routing, rate limiting, CORS           │
└────┬─────────┬──────────┬──────────┬──────────┬────────┘
     │         │          │          │          │
┌────▼───┐ ┌──▼────┐ ┌───▼───┐ ┌───▼────┐ ┌───▼───────┐
│Dashboard│ │Alert- │ │Maint- │ │Report- │ │Notificat- │
│Service  │ │ing    │ │enance │ │ing     │ │ion        │
│:8088    │ │:8086  │ │:8089  │ │:8091   │ │:8087      │
└────┬────┘ └──┬────┘ └───┬───┘ └───┬────┘ └───┬───────┘
     │         │          │          │          │
┌────▼─────────▼──────────▼──────────▼──────────▼────────┐
│  Infrastructure                                         │
│  PostgreSQL:5432 | Redis:6379 | Kafka:29092 | Keycloak  │
└─────────────────────────────────────────────────────────┘
```

## Services et Ports

| Service | Port | URL | Description |
|---------|:----:|-----|-------------|
| Frontend Angular | 4200 | http://localhost:4200 | Dashboard SPA |
| API Gateway | 8080 | http://localhost:8080 | Point d'entree REST |
| Dashboard Service | 8088 | http://localhost:8088 | Read models agreg |
| Alerting Service | 8086 | http://localhost:8086 | Gestion alertes |
| Notification Service | 8087 | http://localhost:8087 | Envoi notifications |
| Maintenance Service | 8089 | http://localhost:8089 | Interventions |
| Reporting Service | 8091 | http://localhost:8091 | Generation rapports |
| PostgreSQL | 5432 | — | Base de donnees |
| Redis | 6379 | — | Cache |
| Kafka | 29092 | — | Messaging |
| Keycloak | 8180 | http://localhost:8180 | IAM |
| Prometheus | 9090 | http://localhost:9090 | Metriques |
| Grafana | 3000 | http://localhost:3000 | Dashboards |

## Ports de Debug (JDWP)

Avec `docker-compose.override.yml` actif :

| Service | Debug Port |
|---------|:----------:|
| API Gateway | 5010 |
| Dashboard Service | 5020 |
| Alerting | 5016 |
| Notification | 5017 |
| Reporting | 5018 |
| Maintenance | 5019 |

Configurer IntelliJ/VS Code : Remote JVM Debug → `localhost:<debug-port>`.

## Modes de Developpement

### Mode 1 : Tout Dockerise

```bash
./scripts/start-mvp2-local.sh
# Ouvrir http://localhost:4200
```

Ideal pour tester le workflow complet. Les services sont construits dans Docker.

### Mode 2 : Infrastructure Docker + Services IDE

```bash
./scripts/start-mvp2-local.sh --infra-only
# Puis lancer les services depuis votre IDE (profil Spring: local)
# Frontend: cd pyrosense-dashboard && ng serve
```

Ideal pour le developpement quotidien avec hot-reload et debug breakpoints.

### Mode 3 : Demarrage Progressif

```bash
# 1. Infrastructure
docker compose --env-file .env.docker up -d postgres redis kafka keycloak prometheus grafana

# 2. Un service a la fois
docker compose --env-file .env.docker --profile mvp2 up -d alerting-service

# 3. Ajouter progressivement
docker compose --env-file .env.docker --profile mvp2 up -d maintenance-service notification-service
```

## Flux Kafka (Evenements MVP 2)

```
Alert Created (pyrosense.alerts.events)
    ├── → maintenance-service : auto-create intervention
    ├── → notification-service : envoyer notification
    └── → dashboard-service : invalider cache

Intervention Completed (pyrosense.maintenance.events)
    ├── → notification-service : notifier parties prenantes
    └── → dashboard-service : invalider cache

Report Generated (pyrosense.reports.events)
    └── → notification-service : notifier demandeur
```

## Healthchecks

Tous les services exposent `/actuator/health`. Docker attend qu'ils soient healthy avant de demarrer les dependants.

Verifier l'etat :
```bash
docker compose --env-file .env.docker --profile mvp2 ps
```

## Logs

```bash
# Tous les services MVP 2
docker compose --env-file .env.docker --profile mvp2 logs -f

# Un service specifique
docker compose --env-file .env.docker --profile mvp2 logs -f maintenance-service

# Filtrer par niveau
docker compose --env-file .env.docker --profile mvp2 logs -f | grep -i error
```

Les logs sont en format JSON structure (profil `docker`). Champs MDC : `traceId`, `spanId`, `correlationId`, `tenantId`.

## Grafana

Acces : http://localhost:3000 (admin / `admin_local_dev`)

Dashboards pre-configures :
- **PyroSense MVP 2 Services** — sante, latence, Kafka lag, alertes, interventions
- **Platform Overview** — vue globale tous services
- **Alerting & Notifications** — cycle de vie alertes

## Arret et Reset

```bash
# Arret (donnees preservees)
./scripts/stop-mvp2-local.sh

# Arret + suppression conteneurs
./scripts/stop-mvp2-local.sh --remove

# Reset complet (destruction volumes)
./scripts/reset-mvp2-local.sh
./scripts/reset-mvp2-local.sh --force  # Sans confirmation
```

## Variables d'Environnement

Voir `.env.docker` pour les valeurs par defaut. Principales :

| Variable | Defaut | Description |
|----------|--------|-------------|
| DB_USERNAME | pyrosense | Utilisateur PostgreSQL |
| DB_PASSWORD | pyrosense_local_dev | Mot de passe PostgreSQL |
| KAFKA_BOOTSTRAP_SERVERS | kafka:9092 | Serveurs Kafka |
| REDIS_HOST | redis | Hote Redis |
| KEYCLOAK_ISSUER | http://keycloak:8080/realms/pyrosense | Issuer JWT |
| CORS_ORIGINS | http://localhost:4200,http://localhost:3000 | Origines CORS |
| JAVA_OPTS | -Xms256m -Xmx512m -XX:+UseZGC | JVM options |
| TRACING_SAMPLING | 1.0 | Taux d'echantillonnage traces |
| GRAFANA_PASSWORD | admin_local_dev | Mot de passe Grafana |

## Troubleshooting

### Les services ne demarrent pas

```bash
# Verifier les logs du service en erreur
docker compose --env-file .env.docker --profile mvp2 logs alerting-service

# Verifier que l'infrastructure est healthy
docker compose --env-file .env.docker ps
```

### Kafka connection refused

Kafka met ~30s a demarrer. Attendre que le healthcheck passe :
```bash
docker compose --env-file .env.docker ps kafka
```

### Port deja utilise

```bash
# Identifier le processus
lsof -i :8080  # Linux/Mac
netstat -ano | findstr :8080  # Windows
```

### Reset base de donnees

```bash
./scripts/reset-mvp2-local.sh --force
./scripts/start-mvp2-local.sh
```

### Frontend ne se connecte pas au gateway

Verifier que le gateway est healthy et que CORS_ORIGINS inclut `http://localhost:4200`.

## Contraintes

- Aucun secret reel dans les fichiers de configuration
- Variables d'environnement pour toute configuration externe
- Logs lisibles (JSON structure en Docker, texte en local)
- Services demarrables progressivement (depends_on + healthchecks)
- Frontend connecte au gateway via proxy nginx
- Backend connecte a PostgreSQL/Redis/Kafka via Docker DNS
