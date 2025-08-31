# API – Endpunkte (Kurzreferenz)

## Health & Meta
- `GET /v1/health` – `{ "status": "ok" }`
- `GET /swagger-ui.html` – OpenAPI UI
- `GET /actuator/prometheus` – Prometheus-Metriken (API)

## Datasets
- `POST /v1/datasets` – legt Dataset an (Manifest → MinIO)
- `GET /v1/datasets` – Liste (paged)
- `GET /v1/datasets/{id}` – Details inkl. `locationUri`

## Ingest
- `POST /v1/ingest` – startet Ingest (offline/online konfigurierbar), Antwort `{ runId }`
- `GET /v1/ingest/{runId}` – Status & Counts; `reportUri`

## Training
- `POST /v1/trainings` – startet Trainingslauf (delegiert an ML), Antwort `{ runId }`
- `GET /v1/trainings/{runId}` – Status/Progress/Artefakte

## Serving
- `POST /v1/predict` – Proxy zu Serve `/predict`, Request `{ fen }`, Response `{ move, legal, modelId, modelVersion }`
- `POST /v1/models/load` – Proxy zu Serve `/models/load` (Dummy/Artefakt-Laden)

> Observability-Instrumentierung fügt Metriken & Logs hinzu, **ohne** den `/v1`-Request/Response-Contract zu verändern.

## Model Registry (read-only)
- `GET /v1/models` – Liste der Modelle `{ modelId, displayName, tags[] }`
- `GET /v1/models/{id}/versions` – Versionen `{ modelVersion, createdAt, metrics{} }`

**Beispiele**
```bash
curl -s http://localhost:8080/v1/models | jq
curl -s http://localhost:8080/v1/models/policy_tiny/versions | jq
curl -s http://localhost:8080/actuator/prometheus | grep chs_model_registry_requests_total | head
```
