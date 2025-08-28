Observability Guide

- Metrics: prefix `chs_*`
  - Export via Micrometer Prometheus registry.
  - Default tags: `application=api`, `component=api`, `username=${CHESS_USERNAME:M3NG00S3}`.
  - Scraped by Prometheus (`infra/prometheus`) and visible in Grafana.

- Logs: JSON structure
  - Logback JSON encoder enabled (see `logback-codex.xml`).
  - Include labels: `run_id`, `dataset_id`, `model_id`, `username=M3NG00S3`, `component`.
  - Ingest via Promtail → Loki; explore in Grafana.

- Artifacts: MinIO and MLflow
  - MinIO buckets: datasets, models, reports, logs, mlflow (created on infra up).
  - MLflow tracks experiments; `artifact_uri` fields reference `s3://` paths.
  - Persist artifacts with stable URIs; include `run_id` for traceability.

Principles

- Observability-first: instrument before feature-complete.
- Metrics: meaningful, low-cardinality, stable naming with `chs_*` prefix.
- Logs: structured JSON, correlation IDs, avoid PII.
- Artifacts: immutable, reproducible, auditable storage in MinIO; metadata in MLflow.

