Module Summary

- api/api-app: Spring Boot 3 (Java 21)
  - Purpose: Backend API exposing /v1 endpoints, Flyway migrations, Prometheus metrics, JSON logs.
  - Build: `mvn -f api/api-app/pom.xml -DskipTests -DskipITs=true package`
  - Run (codex profile): `mvn -f api/api-app/pom.xml spring-boot:run -Dspring-boot.run.profiles=codex`

- ml: Python utilities and experiments
  - Purpose: ML workflows; artifacts/logs stored in MinIO/MLflow.
  - Test: `python -m pytest -q ml/tests` (after adding deps like pytest)

- frontend: Vite/Vue app (if present)
  - Purpose: UI for interacting with datasets/games/models.
  - Dev: `npm --prefix frontend run dev -- --mode codex`
  - Test (if package.json present): `npm --prefix frontend test --if-present`

- infra: Docker Compose stack
  - Purpose: Local infra (db, minio, mlflow, prometheus, loki, promtail, grafana).
  - Up: `docker compose -f infra/docker-compose.yml --env-file infra/.env up -d db minio mlflow prometheus loki promtail grafana`
  - Down: `docker compose -f infra/docker-compose.yml down`

- docs: Project documentation
  - Key file: `docs/CODEX-SETUP.md` (local start, checks, observability guidance)

Observability Principles

- Metrics: prefix `chs_*`, scrape via Prometheus, views in Grafana.
- Logs: JSON logs with labels `run_id`, `dataset_id`, `model_id`, `username=M3NG00S3`, `component`.
- Artifacts: Use MinIO buckets; MLflow for experiment metadata and artifact URIs.

