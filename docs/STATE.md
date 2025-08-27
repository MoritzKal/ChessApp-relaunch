# STATE – Infra Services (local)

| Service    | URL                          | Notes |
|------------|------------------------------|-------|
| Grafana    | http://localhost:3000        | admin: $GRAFANA_USER |
| Prometheus | http://localhost:9090        | scrape interval 5s |
| Loki       | http://localhost:3100        | via Promtail docker discovery |
| MinIO S3   | http://localhost:9000        | access key in `.env` |
| MinIO UI   | http://localhost:9001        | console |
| MLflow     | http://localhost:5000        | artifacts → MinIO bucket `mlflow` |
| Postgres   | localhost:5432               | db: $POSTGRES_DB |

## Buckets (MinIO)
- datasets/
- models/
- reports/
- logs/
- mlflow/

## Nächste Schritte
- `api` & `ml` Services hinzufügen, Prometheus‑Metrics (`chs_*`) und strukturierte JSON‑Logs (Labels: `run_id`, `dataset_id`, `model_id`, `username`, `component`).
