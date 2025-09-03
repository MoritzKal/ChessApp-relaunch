# Pinia State Stores – Domänen

## auth
- state: `token`, `user`
- actions: `login(token)`, `logout()`, `isAuthenticated`

## data
- actions: `importStart() → POST /v1/ingest`, `importStatus(runId)`
- selectors: letzte Runs, Status

## datasets
- actions: `create(payload)`, `list(limit, offset)`, `get(id)`

## training
- actions: `start({datasetId, preset, params})`, `get(runId)`, `stream(runId)` (SSE/WebSocket später)

## models
- actions: `list()`, `load({modelId, artifactUri})`

## games
- actions: `list({username,limit,offset,result,color,since})`, `get(id)`, `positions(id)`

## play
- actions: `predict({fen})`, `newGame()`, `move(uci)`

## observability
- state: grafanaUrl, dashboardUid (`chs-overview-v1`)
- actions: `status()`, `fetchPanels()`

## Konventionen
- Actions im Imperativ, pure side-effects
- Selectors via `computed`
- Fehler zentral in `notifications`-Store bündeln
