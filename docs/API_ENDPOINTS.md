# API Endpoints (/v1)

> Stabilität gemäß Contract-Board. Nur additive Änderungen. **Standard:** `/v1/ingest` · **Alias (Bestand):** `/v1/data/import` (keine v1-Breakings).

> **Security:** bearerAuth (JWT) nötig – ausgenommen `/v1/health` und `/v1/auth/token`. Beispiel-Header:
> `Authorization: Bearer <TOKEN>`

## Health/Meta

- `GET /v1/health` → 200 OK
- `GET /swagger-ui.html` → OpenAPI UI

## Ingest

- `POST /v1/ingest` → 202 Accepted  
  - Header: `Location: /v1/ingest/{runId}`  
  - Response: `{"runId":"<UUID>"}`
- `GET /v1/ingest/{runId}` → 200 OK  
  - Response: `{"status":"PENDING|RUNNING|SUCCEEDED|FAILED","reportUri":"s3://reports/ingest/<runId>/report.json"}` (`reportUri` optional)
- **Alias:** `POST /v1/data/import` → intern Alias auf `/v1/ingest`
  - Response: 308 Permanent Redirect

Alias-Beispiel:

```bash
curl -i -X POST http://localhost:8080/v1/data/import
```

Antwort:

```http
HTTP/1.1 308 Permanent Redirect
Location: /v1/ingest
```

### Beispiele

POST

```bash
curl -i -X POST http://localhost:8080/v1/ingest
```

Antwort:

```http
HTTP/1.1 202 Accepted
Location: /v1/ingest/<runId>
{"runId":"<runId>"}
```

GET

```bash
curl -sS http://localhost:8080/v1/ingest/<runId>
```

Antwort:

```json
{"status":"SUCCEEDED","reportUri":"s3://reports/ingest/<runId>/report.json"}
```

## Datasets

- `POST /v1/datasets`
- `GET /v1/datasets`
- `GET /v1/datasets/{id}`
- `GET /v1/datasets/count`
- `GET /v1/datasets/{id}/summary`
- `GET /v1/datasets/{id}/versions`
- `GET /v1/datasets/{id}/schema`
- `GET /v1/datasets/{id}/sample`
- `GET /v1/datasets/{id}/quality`
- `GET /v1/datasets/{id}/ingest/history`

`GET /v1/datasets`

| Parameter | Werte                     | Beschreibung            |
|-----------|--------------------------|------------------------|
| limit     | Zahl (1-100, default 20) | Anzahl Elemente         |
| offset    | Zahl (default 0)         | Offset für Pagination   |
| q         | String                   | optionaler Namefilter   |

## Training

- `POST /v1/trainings`
- `GET /v1/trainings`
- `GET /v1/trainings/count`
- `GET /v1/trainings/{runId}`
- `GET /v1/training/{runId}` (Alias)
- `GET /v1/trainings/{runId}/artifacts`
- `POST /v1/trainings/{runId}/control`

**Beispiele**

`POST /v1/trainings`

```bash
curl -s -X POST /v1/trainings \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"datasetId":"<UUID>","datasetVersion":"v1","modelId":"<UUID>","epochs":10,"batchSize":64,"learningRate":0.001,"optimizer":"adam","seed":42,"notes":"optional","useGPU":true,"priority":"normal"}'
```

Antwort 201:

```json
{"runId":"<UUID>","status":"queued"}
```

`GET /v1/trainings`

| Parameter | Werte                       | Beschreibung            |
|-----------|----------------------------|------------------------|
| status    | active\|finished\|failed    | optionaler Filter       |
| limit     | Zahl (1-100, default 20)   | Anzahl Elemente         |
| offset    | Zahl (default 0)           | Offset für Pagination   |

## Serving/Play

- `POST /v1/predict` `{ "fen":"<FEN>","topk":3 }` → `{"move":"e2e4","policy":[...]}`

## Models (Registry/Versioning)

- `GET /v1/models` → name, version, stage (staging/prod)
- `GET /v1/models/{id}/versions`
- `GET /v1/models/count`
- `POST /v1/models/load` `{ "name":"policy_tiny","version":"1.2.0","stage":"prod" }`
- `POST /v1/models/promote` `{ "name":"policy_tiny","from":"staging","to":"prod" }`

## Games

- `GET /v1/games`
- `GET /v1/games/{id}`
- `GET /v1/games/{id}/positions`
- `GET /v1/games/recent`
- `GET /v1/games/online_count`
- `POST /v1/games/demo`

## Metrics

- `GET /v1/metrics/throughput`
- `GET /v1/metrics/training/{runId}`
- `GET /v1/metrics/utilization`
- `GET /v1/metrics/latency`
- `GET /v1/metrics/mps`
- `GET /v1/metrics/rps`
- `GET /v1/metrics/error_rate`
- `GET /v1/metrics/elo`
- `GET /v1/metrics/health`

`GET /v1/metrics/throughput`

| Parameter | Werte     | Beschreibung                                 |
|-----------|-----------|----------------------------------------------|
| range     | z.B. 24h  | optionaler Zeitraum                          |
| runId     | UUID      | optional für spezifischen Trainingslauf      |

`GET /v1/metrics/health` → `{ "status":"ok","pingMs":0,"errorRate":0.0 }`

## Logs

- `GET /v1/logs/training/{runId}`

## Observability/Links

- `GET /actuator/prometheus` (Scrape)
- `GET /obs/prom/instant`, `GET /obs/prom/range`
- `GET /obs/loki/query`, `GET /obs/loki/query_range`
- Logs/Traces via Grafana/Loki (siehe [OBSERVABILITY](./OBSERVABILITY.md); Dashboard UID `chs-overview-v1`)
