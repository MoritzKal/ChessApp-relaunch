# Architektur

## Komponenten
- **API (Spring Boot)** – Business-API, Datasets, Ingest-Orchestrierung, Trainings/Serving-Proxy, Actuator-Prometheus, JSON-Logs (Logstash-Encoder).
- **ML (FastAPI)** – Trainingsschleife (simuliert), MLflow-Tracking, Artefakte in MinIO, Prometheus-Metriken, strukturierte Logs.
- **Serve (FastAPI)** – Inference-Stub: legaler Zug aus FEN; Prometheus-Metriken, strukturierte Logs; API-Proxy `/v1/predict`.
- **Infra** – Prometheus, Grafana, Loki/Promtail, MinIO, MLflow, Postgres.

## Datenflüsse (Kurz)
- **Ingest**: PGN → Parser → DB (`games/moves/positions`) → Report (MinIO) → Metriken/Logs.
- **Dataset**: Request → DB-Record → Manifest (MinIO) → Metriken/Logs.
- **Training**: API → ML (`/train`) → MLflow Run/Artefakte → Metriken/Logs → API-Status.
- **Serving**: API/Frontend → Serve (`/predict`) → Antwort (move, legal) → Metriken/Logs.

## Observability-Konzept
- **Metriken**: `chs_*` (business), `jvm_*`/`process_*` (system). Prometheus scrapt API (`/actuator/prometheus`) sowie ML/Serve (`/metrics`).
- **Logs**: JSON (einzeilig), MDC-Felder (`run_id`, `dataset_id`, `model_id`, `username`, `component`).
- **Dashboards**: „ChessApp – Overview“ bündelt Ingest/Training/Serving + Logs-Panel (`| json`).

