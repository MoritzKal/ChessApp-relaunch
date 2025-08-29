# Operations Runbooks (Stand: 2025-08-29)

## Start/Stop
- **Start:** `make up`  
- **Stop:** `make down`

## Health & Smoke
- Prometheus Targets **grün**
- Grafana Explore → `{service=~".+"}` zeigt Logs
- API `/v1/health`, ML `/health`, Serve `/health`

## Häufige Störungen & Fixes
- **Grafana Login/Explore fehlt:** Anonymous kurz deaktivieren, Passwort resetten (`grafana-cli admin reset-admin-password`). 
- **Loki Parse Error:** `{service=~".+"}` statt `.*`.
- **Loki Permission/WAL:** Volumes/UID prüfen; WAL-Parameter; Single‑Node ohne externe KV.
- **Prometheus Target „down“:** Endpoint/Path prüfen, Container‑Logs ansehen.
- **Flyway Fehler:** `flyway_schema_history` debuggen, Versionen prüfen.

## Diagnose
- `docker compose ps`, `docker compose logs <svc> --tail=200`
- Prometheus: `up{job="<svc>"}`
- Grafana Explore: `{service="<svc>"} | json`
- MinIO Console: Bucket‑Strukturen, Artefakte vorhanden?

## Backup/Restore (DEV)
- **Postgres:** `pg_dump`/`pg_restore` (Volumes beachten).
- **MinIO:** `mc` oder S3‑kompatible Tools.

