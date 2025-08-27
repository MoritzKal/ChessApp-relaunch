# ChessApp – Infra Bootstrap

## Quickstart
1. `cp .env.example .env` und Secrets anpassen.
2. `make up` (oder `docker compose -f infra/docker-compose.yml --env-file .env up -d`).
3. Öffne:
   - Grafana: http://localhost:3000 (admin: $GRAFANA_USER)
   - Prometheus: http://localhost:9090
   - MinIO Console: http://localhost:9001
   - MLflow: http://localhost:5000

## Observability
- **Prometheus** scrapt (zunächst) Prometheus selbst – später `api:8080`, `ml:8000`, `mlflow`.
- **Loki + Promtail** sammeln Docker‑Logs aller Services (Labels: `container`, `service`).
- **Grafana** hat fertige Datasources (Prometheus, Loki). Dashboards: Grafana → Dashboards → Ordner "ChessApp" → "ChessApp – Overview".

## Grafana / Explore

### Loki Query Quickstart
Loki erfordert mindestens **eine** nicht-leere Matcher-Bedingung:
- ✅ `{service=~".+"}`, `{service="prometheus"}`
- ❌ `{service=~".*"}`
Tipp: Label Browser nutzen und `service`/`container` auswählen.

## Artefakte & Buckets
- MinIO Buckets: `datasets/`, `models/`, `reports/`, `logs/`, `mlflow/` (automatisch erstellt).

## Ports
- Grafana 3000 · Prometheus 9090 · Loki 3100 · MLflow 5000 · MinIO S3 9000 · MinIO Console 9001 · Postgres 5432
