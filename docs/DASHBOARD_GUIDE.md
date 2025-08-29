# Dashboard Guide (Grafana) – Stand: 2025-08-29

## Provisioning
- Provider: `infra/grafana/provisioning/dashboards/dashboards.yml`
- JSON: `infra/grafana/provisioning/dashboards/json/chessapp-overview.json`

## Panels (Best Practices)
- **Metrik-Namen:** fachlich (`chs_*`), stabil.
- **Loki-Logs:** Query `{service="api"} | json`, Spalten `mdc.*` sichtbar.
- **Legenden:** sprechende Labels (application/component/username).

## Neues Panel hinzufügen (Kurz)
1) JSON duplizieren → neues Panel mit eindeutiger `id`/Position.
2) Query testen (Explore), dann fest ins Dashboard.
3) Commit mit kurzer Begründung.
