# Getting Started (lokal)

## Voraussetzungen
- Docker & Docker Compose
- Java 21 + Maven (falls lokal gebaut werden soll)
- Python 3.11 (für lokale ML/Serve-Builds optional)

## Start
```bash
cp .env.example .env
make up
docker compose -f infra/docker-compose.yml ps
```

## Wichtige URLs
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090
- MinIO Console: http://localhost:9001
- MLflow: http://localhost:5000
- API (Spring): http://localhost:8080
- ML-Service: http://localhost:8000
- Serve-Service: http://localhost:8001

## Health & Checks
- Prometheus → Status → Targets (alle grünen Jobs)
- Grafana → Explore (Datasource „Loki“) → Query `{service=~".+"}` → Logs sichtbar
- API: `GET /v1/health`, `GET /swagger-ui.html`, `GET /actuator/prometheus`
- ML:  `GET /health`, `GET /metrics`
- Serve: `GET /health`, `GET /metrics`
