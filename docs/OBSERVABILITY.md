# Observability

## Prinzipien

- **chs\_\*** Metriken
- strukturierte **JSON-Logs** (MDC: run_id, dataset_id, model_id, username, component)
- **Prometheus + Grafana + Loki**, **MLflow** für Runs/Artefakte

Scrape-Policy: `/actuator/prometheus` erfordert Monitoring-JWT (Role `MONITORING` oder Scope `monitoring`); `/actuator/health` bleibt öffentlich. Details zur Authentifizierung siehe [Security Guide](SECURITY_GUIDE.md).

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
- **chs_ingest_starts_total**
- **chs_ingest_success_total**
- **chs_ingest_failed_total**
- **chs_ingest_duration_seconds**
- **chs_ingest_active**

Siehe Grafana Panel *Ingest* (Prometheus).

## Alerts (Beispiele)

- `ALERT chs_predict_latency_p95_high IF chs_predict_latency_p95 > 200 for 5m`
- `ALERT chs_predict_error_rate_spike IF increase(chs_predict_errors_total[5m]) / increase(chs_predict_requests_total[5m]) > 0.02`

## Panels (Grafana)

- Dashboard „ChessApp – Overview“ (UID `chs-overview-v1`): `p_reqmin`, `p_p95`, `p_errratio`
- Serve: p50/p95 Latenz, Error-Rate, QPS
- Training: loss/val_acc, throughput
- Ingest: jobs/min, failures
- Logs: Loki Query `{service=~".+"}` + Filter (`component`, `model_id`)

## Proxy API

- `GET /obs/prom/instant?query=...&time=`
- `GET /obs/prom/range?query=...&start=&end=&step=`
- `GET /obs/loki/query?query=`
- `GET /obs/loki/query_range?query=...&start=&end=`

## PromQL Beispiele

```
sum(rate(http_server_requests_seconds_count[1m]))                     # RPS
increase(http_server_requests_seconds_count{status=~"5.."}[5m]) / increase(http_server_requests_seconds_count[5m])  # Error Rate
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[5m]))) * 1000  # Latency
avg_over_time(chs_training_it_per_sec{run_id="<ID>"}[5m])            # Throughput
avg_over_time(ml_training_loss{run_id="<ID>"}[5m])                   # Loss
avg_over_time(ml_training_val_acc{run_id="<ID>"}[5m])                # Val-Acc
avg_over_time(chs_engine_elo[1h])                                     # ELO
```

Micrometer: `http_server_requests_seconds_*`

## Metrics SSOT
The authoritative metric catalog is stored in `docs/observability/metrics.catalog.v1.yaml` (schema chessapp.metrics/1).
Consumers (FE dashboards, BA/PL/SRE chats) should read from this file.
