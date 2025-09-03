# Pinia State Stores – Domänen

## auth
- state: `token`, `user`
- actions: `login(token)`, `logout()`, `isAuthenticated`

## data
- actions: `importStart() → POST /v1/ingest`, `importStatus(runId)`
- selectors: letzte Runs, Status

## datasets
- actions: `create(payload)`, `list(q,pag)`, `get(id)`

## training
- actions: `start(cfg)`, `get(runId)`, `stream(runId)` (SSE/WebSocket später)

## models
- actions: `list()`, `load({name,version,stage})`, `promote({name,from,to})`

## games
- actions: `list({username,limit,offset})`, `get(id)`, `positions(id)`

## play
- actions: `predict({fen, topk})`, `newGame()`, `move(uci)`

## observability
- state: grafanaUrl, dashboardUid (`chs-overview-v1`)
- actions: `status()`, `fetchPanels()`

## Konventionen
- Actions im Imperativ, pure side-effects
- Selectors via `computed`
- Fehler zentral in `notifications`-Store bündeln
