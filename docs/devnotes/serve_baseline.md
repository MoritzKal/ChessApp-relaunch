# Serve Baseline

## Ports
- FastAPI serve: `8001`
- Spring Boot API proxy: `8080`

## Endpoints
- `GET /health` – FastAPI health check
- `GET /metrics` – Prometheus metrics for serve
- `POST /predict` – chess move prediction
- `POST /models/load` – load model from S3
- `POST /dataset/metrics` – publish dataset statistics
- `GET /v1/health` – API proxy health

## Metrics
- `chs_predict_requests_total{username,model_id,status}`
- `chs_predict_latency_seconds`
- `chs_predict_illegal_requests_total`
- `chs_dataset_rows{dataset_id}`
- `chs_dataset_invalid_rows_total{dataset_id}`
- `chs_dataset_export_duration_seconds`

## Logging
- serve uses `python_json_logger` and emits JSON lines
- API uses a JSON console pattern via Spring Boot's `logging.pattern.console`
