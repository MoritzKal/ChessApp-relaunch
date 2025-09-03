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
{ "status": "UP" }
```

### GET /swagger-ui.html
- **Zweck:** manuelle Prüfung der Contracts (lokal)

---

## Auth (nur falls verwendet)
### POST /v1/auth/token
- **Auth:** none (Service/Monitoring-Token)
- **Request:** *(nicht verbindlich für Endnutzer-Login; Security-Guides zeigen OIDC als Ziel)*
- **Response 200:**
```json
{ "token": "<jwt>", "expiresIn": 3600 }
```
> **Hinweis:** Für UI-Login ist später OIDC vorgesehen. Aktuell nutzt die UI ein bereitgestelltes JWT (z. B. aus `.env.local`).

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
> **Hinweis:** Endpunkte laut SSOT vorhanden; genauer Body nicht spezifiziert. UI nutzt konservative, minimale Payloads; bitte gegen Swagger prüfen, sobald verfügbar.

### POST /v1/datasets
- **Auth:** Bearer JWT
- **Request (proposed minimal):**
```json
{
  "name": "string",
  "source": "games|positions|mixed",
  "filters": {
    "username": "string|null",
    "dateRange": { "from": "YYYY-MM-DD", "to": "YYYY-MM-DD" },
    "minElo": 0
  }
}
```
- **Response 201:**
```json
{ "id": "ds_...", "name": "...", "status": "READY|BUILDING|FAILED" }
```

### GET /v1/datasets
- **Query:** `page`, `size`, `q`
- **Response 200:**
```json
{ "items": [ { "id": "ds_1", "name": "...", "createdAt": "...", "status": "READY" } ],
  "page": 0, "size": 25, "total": 1 }
```

### GET /v1/datasets/{id}
- **Response 200:**
```json
{ "id": "ds_1", "manifest": { /* domain */ }, "status": "READY" }
```

---

## Training
### POST /v1/trainings
- **Request (proposed minimal):**
```json
{
  "datasetId": "ds_1",
  "config": {
    "epochs": 3,
    "batchSize": 256,
    "lr": 0.001
  }
}
```
- **Response 202:** `{ "runId": "tr_..." }`

### GET /v1/trainings/{runId}
- **Response 200:**
```json
{ "runId": "tr_...", "status": "RUNNING|SUCCEEDED|FAILED", "metrics": { "val_acc": 0.73 } }
```

---

## Serving / Play
### POST /v1/predict
- **Request:**
```json
{ "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", "topk": 3 }
```
- **Response 200:**
```json
{
  "move": "e2e4",
  "policy": [ { "move": "e2e4", "p": 0.72 }, { "move": "d2d4", "p": 0.18 } ],
  "nodes": 12345, "latencyMs": 42
}
```

---

## Models (Registry)
### GET /v1/models
- **Response 200:** `[ { "name": "policy_tiny", "version": "1.2.0", "stage": "prod" } ]`

### GET /v1/models/{id}/versions
- **Response 200:** `[ { "version": "1.2.0", "stage": "prod", "createdAt": "..." } ]`

### POST /v1/models/load
- **Request:**
```json
{ "name": "policy_tiny", "version": "1.2.0", "stage": "prod" }
```
- **Response 202:** `{ "status": "RELOADING" }`

### POST /v1/models/promote
- **Request:**
```json
{ "name": "policy_tiny", "from": "staging", "to": "prod" }
```
- **Response 200:** `{ "ok": true }`

---

## Games
### GET /v1/games
- **Query (proposed minimal):** `username`, `limit`, `offset`
- **Response 200:**
```json
{ "items": [ { "id": "g_1", "white": "...", "black": "...", "pgn": "..." } ],
  "nextOffset": 100 }
```

### GET /v1/games/{id}
- **Response 200:** `{ "id": "g_1", "pgn": "...", "createdAt": "..." }`

### GET /v1/games/{id}/positions
- **Response 200:** `[ { "ply": 12, "fen": "...", "best": "..." } ]`

---

## Observability Links
- `GET /actuator/prometheus` (Monitoring JWT notwendig)
- Logs/Traces via Grafana/Loki (Dashboard UID `chs-overview-v1`)

