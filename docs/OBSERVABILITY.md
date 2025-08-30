# Observability & Dashboards

## Prometheus
- API: `/actuator/prometheus`
- ML/Serve: `/metrics`
- Metriken:
  - Ingest: `chs_ingest_jobs_total`, `chs_ingest_games_total`, `chs_ingest_positions_total`, `chs_ingest_duration_seconds_bucket`/`_sum`/`_count`
  - Dataset: `chs_dataset_build_total`, `chs_dataset_rows`
  - Training: `chs_training_runs_total`, `chs_training_loss`, `chs_training_val_accuracy`, `chs_training_step_duration_seconds_*`
  - Serving:
    - `chs_predict_requests_total` – API-seitige /v1/predict-Aufrufe (Labels: `model_id`, `model_version`)
    - `chs_predict_latency_ms` – End-to-End-Latenz in Millisekunden (Histogram, Labels: `model_id`, `model_version`)
    - `chs_predict_errors_total` – Fehlerschläge nach Status/Code (Labels: `model_id`, `model_version`, `code`)
    - `chs_models_loaded_total` – erfolgreiche Modell-Ladevorgänge (Labels: `model_id`, `model_version`)
    - `chs_model_reload_failures_total` – Reload-Fehler (Labels: `model_id`, `model_version`, `reason`)

### PromQL
- P95-Latenz: `histogram_quantile(0.95, sum by (le,model_id,model_version)(rate(chs_predict_latency_ms_bucket[5m])))`
- Fehlerquote: `sum by(model_id,model_version)(rate(chs_predict_errors_total[5m]))`
- Geladene Modelle (24h): `sum by(model_id,model_version)(increase(chs_models_loaded_total[24h]))`
- Reload-Fehler (24h): `sum by(model_id,model_version,reason)(increase(chs_model_reload_failures_total[24h]))`
- API-Request-Rate: `sum(rate(chs_predict_requests_total[1m]))`

### Alerts
- **PredictHighErrorRate** – Fehlerquote > 0.2 für 5 Min (severity=warning)
- **PredictLatencyP95High** – P95 > 150 ms für 10 Min (severity=warning)
- **ModelReloadFailuresSpike** – mehr als 3 Reload-Fehler in 30 Min (severity=critical)

## Grafana
- Provisionierte Datasources: Prometheus, Loki
- Dashboards:
  - **ChessApp – Overview** (KPI-Panels + Logs-Panel, Link "Predict by Version →")
  - **Predict by Version** – Filter `$model_id`, `$model_version`; Latenz p50/p95/p99, Fehler-Rate, Loads & Reload-Fails. Panel-Links zu MLflow (`mlflow://models/{{model_id}}/versions/{{model_version}}`) und Loki (`{component="serve", model_id="$model_id", model_version="$model_version"} | json`)

## Loki / Explore
- Mindestens ein nicht-leerer Matcher nötig (z. B. `{service=~".+"}`). Danach `| json` verwenden, um MDC-Felder zu filtern.

**Hinweis:** Die Instrumentierung fügt Observability hinzu, ohne die `/v1`-API-Verträge zu ändern.
