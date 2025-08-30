--- /dev/null
 b/docs/OBS_A3_TESTING.md
@@
# A3 Observability – E2E Test

## Voraussetzungen
- Lokaler Stack läuft: `docker compose -f infra/docker-compose.yml up -d --build`
- Defaults: Grafana `admin:admin` (override via env)

## Ausführung
```bash
make obs-e2e-test
# oder
bash scripts/obs_e2e_test.sh
```

## Was wird geprüft?
1. Prometheus ready & Targets `api|ml|serve == 1`
2. Rule syntax & unit tests (`promtool test rules`)
3. Loki: MDC-Labels `run_id`/`dataset_id` via Probe
4. PromQL Sanity (Key-Exprs)
5. Grafana Dashboards per UID
6. Rule-Gruppen via API

Exit-Code ≠ 0 => Test fehlgeschlagen.
