# Observability Proxy (M6)

Zweck: Read-only Proxy, damit das Vue-Frontend Prometheus & Loki ohne Grafana direkt abfragen kann. Keine Änderungen an /api/** oder bestehenden Dashboards.

## Endpunkte
GET /healthz
GET /obs/prom/query?query=<promql>
GET /obs/prom/range?query=<promql>&start=<rfc3339|unix>&end=<...>&step=<duration>
GET /obs/loki/query?query=<logql>&limit=&direction=&start=&end=
GET /obs/loki/range?query=<logql>&limit=&direction=&start=&end=

Security: optionaler Header `X-Obs-Api-Key` (env `OBS_API_KEY`).

Rate-Limit: standardmäßig 60/min pro IP (env `RATE_LIMIT`), CORS: `CORS_ALLOW_ORIGINS` (CSV).

Timeout/Retry: timeout=3s, retries=2 (5xx/Timeout).

## ENV
PROM_BASE_URL, LOKI_BASE_URL, OBS_API_KEY, CORS_ALLOW_ORIGINS, RATE_LIMIT, TIMEOUT_SECONDS, RETRIES.

## Compose
Siehe infra/compose.obs-proxy.yml; Start: `make -f Makefile.obs obs-up`.

## Beispiel-Queries
curl -H "X-Obs-Api-Key: $OBS_API_KEY" "http://localhost:8088/obs/prom/query?query=up"
curl -H "X-Obs-Api-Key: $OBS_API_KEY" "http://localhost:8088/obs/loki/query?query={app=\"api\"}"

## DoD / Smoke
- Prom Range-Query liefert Daten via Proxy.
- Loki Query (Label-Filter z. B. {component=\"training\"}) liefert Daten via Proxy.
- Dashboards unter infra/grafana/dashboards/obs/ importierbar (UIDs obs-*-v1).
