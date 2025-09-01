API Surface (/v1)

- GET /v1/health: Simple health ping, returns `{ "status": "ok" }`.

- Games
  - GET /v1/games: List games for a user.
    - Query: `username` (string, required), `limit` (int, default 50), `offset` (int, default 0), `result` (enum), `color` (enum), `since` (ISO date).
  - GET /v1/games/{id}: Get a single game detail by UUID.
  - GET /v1/games/{id}/positions: List positions (FEN, moves) for the game.

- Datasets
  - POST /v1/datasets: Create dataset (JSON body `CreateDatasetRequest`).
  - GET /v1/datasets: List datasets (paged, default sort `createdAt` DESC).
  - GET /v1/datasets/{id}: Get dataset by UUID.

- Ingest
  - POST /v1/ingest: Start ingest job; accepts optional JSON `{ username, offline, from, to }`; returns `{ runId }` (202 Accepted).
  - GET /v1/ingest/{runId}: Get ingest status and counters.

OpenAPI Export

- Source: http://localhost:8080/v3/api-docs
- Saved JSON: `codex-context/openapi.json`
- How to refresh: `curl http://localhost:8080/v3/api-docs > codex-context/openapi.json`

Notes

- API namespace is `/v1/*` exclusively for app endpoints; Actuator at `/actuator/*` exposes health/metrics.
- OpenAPI is provided via springdoc, configured to include `/v1/**` routes.

