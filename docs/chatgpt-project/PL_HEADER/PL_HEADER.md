Repo: https://github.com/MoritzKal/ChessApp-relaunch
Branch-Workflow: branch von main → feature/<slug> → PR nach development (oder main, falls freigegeben)
Vorab LESEN (in genau dieser Reihenfolge):
1) docs/PROJECT_OVERVIEW.md
2) docs/PROJECT_INFORMATION_v0.1.md   (20-Punkte-Plan, historisch)
3) docs/ROADMAP.md  und  docs/STATE.md
4) docs/API_ENDPOINTS.md  und  docs/OBSERVABILITY.md
5) codex-context/README.md, CONVENTIONS.md, TESTING.md
6) docs/chatgpt-project/HINWEISE.md  und  CHAT_START_TEMPLATE.md

Code-Orte zum Aufklappen:
- infra/docker-compose.yml, infra/prometheus/prometheus.yml,
  infra/grafana/provisioning/**, ml/, serve/, api/**

Pre-Flight (lokal):
- cp .env.example .env  &&  make up
- Prometheus Targets grün: prom, loki, api, ml, serve
- Grafana „ChessApp – Overview“ sichtbar
- Loki Explore Query: {service=~".+"}  → Logs + | json
- API:  GET :8080/v1/health, /swagger-ui.html, /actuator/prometheus
- ML:   GET :8000/health, /metrics
- Serve:GET :8001/health, /metrics

DoD immer:
- Tests grün (neu/angepasst)
- Metriken (chs_*) und JSON-Logs (MDC: run_id, dataset_id, model_id, username, component)
- Dashboard bleibt funktionsfähig (Panels ggf. ergänzen)
- Handover: SUMMARY FOR PL (Template in docs/chatgpt-project/STATUS_KACHEL.txt)

Kommunikation:
- Ereignisgetrieben: Ping bei Start/Blocker/Review/Done
- Breaking Change → RFC über den Communicator-PL (keine heimlichen /v1-Breakings)
