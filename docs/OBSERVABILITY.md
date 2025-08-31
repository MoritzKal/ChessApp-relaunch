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

## Alerts (Beispiele)

- `ALERT chs_predict_latency_p95_high IF chs_predict_latency_p95 > 200 for 5m`
- `ALERT chs_predict_error_rate_spike IF increase(chs_predict_errors_total[5m]) / increase(chs_predict_requests_total[5m]) > 0.02`

## Panels (Grafana)

- Serve: p50/p95 Latenz, Error-Rate, QPS
- Training: loss/val_acc, throughput
- Ingest: jobs/min, failures
- Logs: Loki Query `{service=~".+"}` + Filter (`component`, `model_id`)
