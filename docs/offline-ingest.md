Offline Ingest: Local How‑To

Prerequisites
- API running locally on port 8080 (profile `codex`).
- Local MinIO (S3) on http://localhost:9000 (console http://localhost:9001).
- Buckets `reports` and `logs` exist.

Start MinIO (example)
- docker run -d --name minio -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=chs_minio -e MINIO_ROOT_PASSWORD=chs_minio_password_change_me \
  -v minio-data:/data minio/minio server /data --console-address ":9001"

Create buckets

Option A — via Docker Compose (empfohlen)
- docker compose -f infra/docker-compose.yml --env-file .env up -d minio create-buckets

Option B — mit minio/mc Container (ohne host network)
- Hinweis: Das Image `minio/mc` hat `mc` als ENTRYPOINT; kein `sh` verfügbar. Verwende `MC_HOST_local`, um den Alias zu setzen, und rufe je Befehl einmal `mc` auf.
- MinIO als Container (Name: `minio`), Netzwerk teilen und 127.0.0.1 nutzen:
  - docker run --rm --network container:minio -e MC_HOST_local="http://minio_chs:chs_minio_password_change_me@127.0.0.1:9000" minio/mc mb -p local/reports
  - docker run --rm --network container:minio -e MC_HOST_local="http://minio_chs:chs_minio_password_change_me@127.0.0.1:9000" minio/mc mb -p local/logs
- MinIO auf dem Host (Docker Desktop): `--network host` nicht verfügbar. Stattdessen `host.docker.internal`:
  - docker run --rm -e MC_HOST_local="http://minio_chs:chs_minio_password_change_me@host.docker.internal:9000" minio/mc mb -p local/reports
  - docker run --rm -e MC_HOST_local="http://minio_chs:chs_minio_password_change_me@host.docker.internal:9000" minio/mc mb -p local/logs

Option C — lokal installiertes `mc`
- mc alias set local http://127.0.0.1:9000 minio_chs chs_minio_password_change_me
- mc mb -p local/reports && mc mb -p local/logs

API configuration (profile codex)
- `api/api-app/src/main/resources/application-codex.yml` contains defaults:
  - `s3.endpoint=http://localhost:9000`
  - `s3.region=us-east-1`
  - `s3.accessKey=minio_chs`
  - `s3.secretKey=chs_minio_password_change_me`
- Override with env vars: `S3_ENDPOINT`, `S3_REGION`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`.

Run offline ingest (bash)
- Start job:
  - RESP=$(curl -sS -H 'Content-Type: application/json' \
     -d '{"username":"M3NG00S3","from":"2025-07","to":"2025-08","offline":true}' \
     http://localhost:8080/v1/ingest)
  - RID=$(echo "$RESP" | jq -r .runId)
- Poll status (max 60×, 2s):
  - for i in $(seq 1 60); do S=$(curl -sS http://localhost:8080/v1/ingest/$RID); \
     echo "$S" | jq -e . >/dev/null || { echo "Non‑JSON: $S"; exit 1; }; \
     st=$(echo "$S" | jq -r .status); [ "$st" = succeeded ] && break; \
     [ "$st" = failed ] && { echo "$S" | jq .; exit 1; }; sleep 2; done; echo "$S" | jq .

Verify artifacts
- curl -s http://localhost:8080/v1/ingest/$RID | jq -r .reportUri
  - Expect: `s3://reports/ingest/<runId>/report.json`
- Prometheus (optional):
  - curl -sI http://localhost:8080/actuator/prometheus | tr -d '\r' | grep -i '^Content-Type:'
  - curl -s http://localhost:8080/actuator/prometheus | tr -d '\r' | grep -F 'chs_ingest_skipped_total{'

Troubleshooting
- `reportUri=null` and `error` contains 403/UnknownHost:
  - Ensure MinIO is running and `s3.endpoint` points to it.
  - Ensure buckets exist: `reports`, `logs`.
- First run shows games=0:
  - Likely existing data. Truncate tables or use a fresh DB/volume.
