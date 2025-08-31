# Project Overview – ChessApp Geläutert

**Ziel:** Schach-App mit Trainings-, Serving/Play- und Observability-Stack.
**Prinzipien:** Observability-first · Tests Pflicht · Wasserfall pro Block · Keine v1-Breakings.

## Komponenten

- **API/Serving:** Endpunkte für Predict, Models und Ingest
- **Training/Registry:** MLflow Runs & Artefakte, Model-Registry (read-only)
- **Observability:** Prometheus/Grafana/Loki, chs\_\* Metriken, strukturierte JSON-Logs

## Weiterführend (SSOT)

- Planung/Status: [ROADMAP](./ROADMAP.md)
- Ist-Stand: [STATE](./STATE.md)
- API-Details & Beispiele: [API_ENDPOINTS](./API_ENDPOINTS.md)
- KPIs, Panels, Alerts: [OBSERVABILITY](./OBSERVABILITY.md)
