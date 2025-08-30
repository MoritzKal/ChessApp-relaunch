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

## Lokales Codex-Profil

Hinweise zum lokalen Entwicklungsprofil findest du in [docs/CODEX-SETUP.md](docs/CODEX-SETUP.md).

## Offline Ingest lokal

Siehe `docs/offline-ingest.md`:
- MinIO starten und Buckets `reports`/`logs` sicherstellen
- API im Profil `codex` starten (enthält lokale S3-Defaults)
- Offline-Ingest via `curl` starten und Status pollen
- `reportUri` prüfen (`s3://reports/ingest/<runId>/report.json`)

## Windows Notes (Git-Bash)
- Git-Bash rewritet absolute UNIX-Pfade (MSYS Path Conversion). Vor Docker-Kommandos setzen:
  - `export MSYS_NO_PATHCONV=1`
  - `export MSYS2_ARG_CONV_EXCL="*"`
- Für ML-Dataset-Export nutze das Skript `scripts/a2_verify_min.sh` oder folgenden Befehl:
  - `docker compose -f infra/docker-compose.yml exec -T ml sh -lc 'python ml/tools/dataset_export.py --input /app/ml/data/sample --output /app/ml/out/a2/compact.parquet --dataset-id ds_v0_local --manifest /app/ml/out/a2/manifest.json --push-metrics http://ml:8000/internal/dataset/metrics'`
- Details: `docs/troubleshooting/WINDOWS_GIT_BASH.md`
