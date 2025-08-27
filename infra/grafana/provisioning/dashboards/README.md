# Dashboards

Füge hier JSON‑Dashboards hinzu. Vorschlag (folgender PR):
- System Overview (Prometheus → Container CPU/Mem/Net)
- ChessApp Training (Loss/Acc via `chs_*`)
- ChessApp Serving (Latenz, QPS, Fehler)
- Logs Explorer (Loki labels: `service`, `container`)

Dashboards werden per Provisioning geladen:
- Datasources: infra/grafana/provisioning/datasources/datasources.yml
- Dashboards:  infra/grafana/provisioning/dashboards/{dashboards.yml,json/}

Übersicht: Dashboards → Folder „ChessApp“ → „ChessApp – Overview“ (UID: chs-overview).