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

- `GET /actuator/prometheus` (Scrape)
- Logs/Traces via Grafana/Loki (siehe [OBSERVABILITY](./OBSERVABILITY.md); Dashboard UID `chs-overview-v1`)
