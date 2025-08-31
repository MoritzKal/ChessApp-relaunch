# API Endpoints (/v1)

> Stabilität gemäß Contract-Board. Nur additive Änderungen. **Standard:** `/v1/ingest` · **Alias (Bestand):** `/v1/data/import` (keine /v1-Breakings).

## Health/Meta

- `GET /v1/health` → 200 OK
- `GET /swagger-ui.html` → OpenAPI UI

## Ingest

- `POST /v1/ingest`
  - Body: `{"username":"<name>","from":"2025-01","to":"2025-08"}`
- **Alias:** `POST /v1/data/import` → intern Alias auf `/v1/ingest`
- `GET /v1/ingest/{runId}` → Status

### Beispiel (curl)

```bash
curl -sS -X POST http://localhost:8080/v1/ingest \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","from":"2025-01","to":"2025-08"}'
```

Datasets
POST /v1/datasets { "filters": {...}, "split":{"train":0.8,"val":0.1,"test":0.1} }

GET /v1/datasets · GET /v1/datasets/{id}

Training
POST /v1/trainings { "datasetId":"<uuid>", "preset":"policy_tiny", "params":{"epochs":10,"batchSize":512,"lr":3e-4} }

GET /v1/trainings/{runId}

Serving/Play
POST /v1/predict { "fen":"<FEN>", "history":[...], "temperature":0.9, "topk":3 }

Antwort: { "move":"e2e4","policy":[...] }

Models (Registry/Versioning)
GET /v1/models → name, version, stage (staging/prod)

POST /v1/models/load { "name":"policy_tiny", "version":"1.2.0", "stage":"prod" }

POST /v1/models/promote { "name":"policy_tiny", "from":"staging","to":"prod" }

Observability/Links
GET /actuator/prometheus (Scrape)

Logs/Traces via Grafana/Loki (siehe OBSERVABILITY)
