# API Catalog (Frontend-Sicht)
> **SSOT:** docs/API_ENDPOINTS.md (keine Dupplikate für System-Quelle; hier FE-Ansicht mit konkreten Requests/Antworten)
> **Stand:** 2025-08-31 laut STATE; geprüft am 2025-09-03

## Konventionen
- **Base URL:** `${VITE_API_BASE_URL}` (z. B. `http://localhost:8080`)
- **Auth:** `Authorization: Bearer <jwt>` (Ausnahmen: `/v1/health`, `/v1/auth/token`)
- **Content-Type:** `application/json; charset=utf-8`
- **Idempotenz:** nur GET wird automatisch erneut versucht (UI-Retry-Policy)
- **Zeitformat:** ISO-8601 UTC (falls vorhanden)
- **Location-Header:** bei 202-Antworten (z. B. Ingest-Start)

## Fehler-Modell (UI-vereinheitlicht)
Wir mappen Responses auf:
```json
{
  "ok": false,
  "code": "VALIDATION|NOT_FOUND|UNAUTHORIZED|RATE_LIMIT|INTERNAL",
  "status": 400,
  "message": "human readable",
  "details": { "field": "msg" }
}
```
> Serverseitig variierend; UI normalisiert.

---

## Health & Meta
### GET /v1/health
- **Auth:** none
- **Request:** —
- **Response 200:**
```json
{ "status": "ok" }
```

### GET /swagger-ui.html
- **Zweck:** manuelle Prüfung der Contracts (lokal)

---

## Auth (nur falls verwendet)
### GET /v1/auth/token
- **Auth:** none (Service/Monitoring-Token)
- **Query Params:** `user` (default `user1`), `roles` (`USER`), `scope` (`api.read`), `ttl` (`600` s)
- **Response 200:**
```json
{ "token": "<jwt>", "expires_at": 1714825300, "roles": ["USER"] }
```
> **Hinweis:** Für UI-Login ist später OIDC vorgesehen. Aktuell nutzt die UI ein bereitgestelltes JWT (z. B. aus `.env.local`).

---

## Ingest
### POST /v1/ingest
Startet Import/Preprocessing.
- **Auth:** Bearer JWT
- **Request:** *(kein Body notwendig; runnt mit serverseitigen Defaults)*
- **Response 202:** Header `Location: /v1/ingest/{runId}`; Body
```json
{ "runId": "<uuid>" }
```

### GET /v1/ingest/{runId}
- **Auth:** Bearer JWT
- **Response 200:**
```json
{
  "status": "PENDING|RUNNING|SUCCEEDED|FAILED",
  "reportUri": "s3://reports/ingest/<runId>/report.json"
}
```

> **Alias (Bestand):** `POST /v1/data/import` → 308 Redirect auf `/v1/ingest`

---

## Datasets
### POST /v1/datasets
- **Auth:** Bearer JWT
- **Request:**
```json
{
  "name": "string",
  "version": "string",
  "filter": {},
  "split": {},
  "sizeRows": 123
}
```
- **Response 201:**
```json
{
  "id": "<uuid>",
  "name": "...",
  "version": "...",
  "sizeRows": 123,
  "locationUri": "s3://datasets/...",
  "createdAt": "2025-08-31T12:00:00Z"
}
```

### GET /v1/datasets
- **Query:** `limit` (default 50), `offset` (default 0)
- **Response 200:**
```json
[
  {
    "id": "<uuid>",
    "name": "...",
    "version": "...",
    "sizeRows": 123,
    "locationUri": "s3://datasets/...",
    "createdAt": "2025-08-31T12:00:00Z"
  }
]
```

### GET /v1/datasets/{id}
- **Response 200:** wie `DatasetResponse` oben

---

## Training
### POST /v1/trainings
- **Auth:** Bearer JWT
- **Request:**
```json
{
  "datasetId": "<uuid>",
  "preset": "policy_tiny",
  "params": { "epochs": 3, "lr": 0.001 }
}
```
- **Response 202:** `{ "runId": "<uuid>" }`

### GET /v1/trainings/{runId}
- **Response 200:**
```json
{
  "runId": "<uuid>",
  "status": "queued|running|succeeded|failed",
  "startedAt": "2025-08-31T12:00:00Z",
  "finishedAt": null,
  "metrics": { "val_acc": 0.73 },
  "artifactUris": { "logs": "s3://..." }
}
```

---

## Serving / Play
### POST /v1/predict
- **Headers (optional):** `X-Run-Id`, `X-Username`
- **Request:**
```json
{ "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1" }
```
- **Response 200:**
```json
{ "move": "e2e4", "legal": ["e2e4"], "modelId": "dummy", "modelVersion": "0" }
```

---

## Models (Registry)
### GET /v1/models
- **Response 200:**
```json
[ { "modelId": "policy_tiny", "displayName": "Policy Tiny", "tags": ["prod"] } ]
```

### GET /v1/models/{id}/versions
- **Response 200:**
```json
[ { "modelVersion": "1.2.0", "createdAt": "2025-08-31T12:00:00Z", "metrics": {} } ]
```

### POST /v1/models/load
- **Request:**
```json
{ "modelId": "policy_tiny", "artifactUri": "s3://models/policy_tiny/1.2.0" }
```
- **Response 200:** `{ "modelId": "policy_tiny", "modelVersion": "1.2.0" }`

---

## Games
### GET /v1/games
- **Query:** `username` (required), `limit` (default 50), `offset` (default 0), `result`, `color`, `since` (YYYY-MM-DD)
- **Response 200:**
```json
[ { "id": "g_1", "endTime": "...", "timeControl": "...", "result": "WHITE", "whiteRating": 1500, "blackRating": 1400 } ]
```

### GET /v1/games/{id}
- **Response 200:**
```json
{ "id": "g_1", "endTime": "...", "timeControl": "...", "result": "WHITE", "whiteRating": 1500, "blackRating": 1400, "pgnRaw": "..." }
```

### GET /v1/games/{id}/positions
- **Response 200:**
```json
[ { "ply": 12, "fen": "...", "sideToMove": "WHITE" } ]
```

---

## Observability Links
- `GET /actuator/prometheus` (Monitoring JWT notwendig)
- Logs/Traces via Grafana/Loki (Dashboard UID `chs-overview-v1`)

