# Serving Service

The `serve` component exposes a FastAPI service on port **8001** for model loading, prediction and metrics.

## Example usage

```bash
# health
curl -s localhost:8001/health
# dummy model
curl -s -XPOST localhost:8001/models/load -H 'Content-Type: application/json' -d '{}'
# predict (direct)
curl -s -XPOST localhost:8001/predict -H 'Content-Type: application/json' -d '{"fen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"}'
# predict via API proxy
curl -s -XPOST localhost:8080/v1/predict -H 'Content-Type: application/json' -H 'X-Username: M3NG00S3' -d '{"fen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"}'
# metrics
curl -s localhost:8001/metrics | grep chs_predict_
```

## Observability

Metrics are exposed via `/metrics` and prefixed with `chs_predict_`.

Logs can be queried in Loki, e.g.:

```logql
{container="chs_serve"} |= "predict."
```

Each log line carries fields such as `event`, `run_id`, `username`, `model_id`, and `component="serve"`.
