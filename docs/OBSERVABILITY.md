# Observability

## Prinzipien

- **chs\_\*** Metriken
- strukturierte **JSON-Logs** (MDC: run_id, dataset_id, model_id, username, component)
- **Prometheus + Grafana + Loki**, **MLflow** für Runs/Artefakte

## KPIs (Serve/Play)

- **chs_predict_latency_p95** (ms) — Ziel <150
- **chs_predict_error_rate** (%) — Ziel <1
- **chs_predict_qps** (req/s)
- **ml_training_val_acc_top1**
- **ml_training_throughput** (samples/s)

## Weitere Metriken (Auszug)

- **chs_predict_requests_total**
- **chs_predict_errors_total**
- **chs_predict_cache_hits_total**
- **chs_predict_cache_misses_total**
- **chs_model_registry_requests_total**
- **chs_training_runs_total**
- **chs_dataset_export_duration_seconds**

## Alerts (Beispiele)

- `ALERT chs_predict_latency_p95_high IF chs_predict_latency_p95 > 200 for 5m`
- `ALERT chs_predict_error_rate_spike IF increase(chs_predict_errors_total[5m]) / increase(chs_predict_requests_total[5m]) > 0.02`

## Panels (Grafana)

- Serve: p50/p95 Latenz, Error-Rate, QPS
- Training: loss/val_acc, throughput
- Ingest: jobs/min, failures
- Logs: Loki Query `{service=~".+"}` + Filter (`component`, `model_id`)

## Metrics SSOT
The authoritative metric catalog is stored in `docs/observability/metrics.catalog.v1.yaml` (schema chessapp.metrics/1).
Consumers (FE dashboards, BA/PL/SRE chats) should read from this file.
