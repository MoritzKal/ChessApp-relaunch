# Observability & Dashboards

## Prometheus
- API: `/actuator/prometheus`
- ML/Serve: `/metrics`
- Beispiel-Metriken:
  - Ingest: `chs_ingest_jobs_total`, `chs_ingest_games_total`, `chs_ingest_positions_total`, `chs_ingest_duration_seconds_bucket`/`_sum`/`_count`
  - Dataset: `chs_dataset_build_total`, `chs_dataset_rows`
  - Training: `chs_training_runs_total`, `chs_training_loss`, `chs_training_val_accuracy`, `chs_training_step_duration_seconds_*`
  - Serving: `chs_predict_requests_total`, `chs_predict_latency_seconds_*`, `chs_predict_illegal_requests_total`

## Loki / Explore
- Mindestens ein nicht-leerer Matcher nötig (z. B. `{service=~".+"}`). Danach `| json` verwenden, um MDC-Felder zu filtern.

## Grafana
- Provisionierte Datasources: Prometheus, Loki
- Dashboard: **ChessApp – Overview** (KPI-Panels + Logs-Panel)
