# API Endpoints (/v1)

> Stabilität gemäß Contract-Board. Nur additive Änderungen. **Standard:** `/v1/ingest` · **Alias (Bestand):** `/v1/data/import` (keine v1-Breakings).
> All `/v1/**` endpoints require a valid JWT unless noted otherwise. `/v1/health` is public.
> `/actuator/**` endpoints are admin-only and require a JWT with `ROLE_ADMIN`; `/actuator/prometheus` additionally expects `Authorization: Bearer <scrape-token>`.

> Alle `/v1/**` Endpunkte erfordern `Authorization: Bearer <JWT>` (Ausnahmen: `/v3/api-docs/**`, `/swagger-ui/**`).

## Health/Meta

- `GET /v1/health` → 200 OK
- `GET /swagger-ui.html` → OpenAPI UI

## Ingest

- `POST /v1/ingest` – Start offline ingest
  - Request: `{ "username": "string", "range": "string?" }`
  - Response `202`: `{ "runId": "uuid" }`
- `GET /v1/ingest/{runId}` – Status/Report
  - Response `200`: `{ "status": "RUNNING|SUCCEEDED|FAILED", "reportUri": "string?" }`
- **Alias (deprecated):** `POST /v1/data/import` → `308` → `/v1/ingest`
- Metriken: `chs_ingest_runs_started_total`, `chs_ingest_runs_succeeded_total`, `chs_ingest_runs_failed_total`, `chs_ingest_active_runs`

### Beispiel

```bash
curl -sS -X POST http://localhost:8080/v1/ingest \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"username":"demo"}'
```

## Datasets

- `POST /v1/datasets` – 201 + `Location: /v1/datasets/{id}`
- `GET /v1/datasets` – Liste (paged; `page`,`size`)
- `GET /v1/datasets/{id}` – Detail (`locationUri`, `sizeRows`, `createdAt`)

### Beispiel

```bash
curl -sS -X POST http://localhost:8080/v1/datasets \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"train","version":"1.0.0","filter":{"foo":"bar"}}'
```

## Training

- `POST /v1/trainings`
- `GET /v1/trainings/{runId}`

## Serving/Play

- `POST /v1/predict` `{ "fen":"<FEN>","topk":3 }` → `{"move":"e2e4","policy":[...]}`

## Models (Registry/Versioning)

- `GET /v1/models` → name, version, stage (staging/prod)
- `GET /v1/models/{id}/versions`
- `POST /v1/models/load` `{ "name":"policy_tiny","version":"1.2.0","stage":"prod" }`
- `POST /v1/models/promote` `{ "name":"policy_tiny","from":"staging","to":"prod" }`

## Games

- `GET /v1/games`
- `GET /v1/games/{id}`
- `GET /v1/games/{id}/positions`

## Observability/Links

- `GET /actuator/**` → requires authentication (ROLE_ADMIN).
- `GET /actuator/prometheus` → additionally requires `Authorization: Bearer <scrape-token>`.
- Logs/Traces via Grafana/Loki (siehe [OBSERVABILITY](./OBSERVABILITY.md))
