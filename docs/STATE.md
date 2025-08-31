# STATE – Stand: 2025-08-31

## System

- Compose-Stack lauffähig (API, MLflow, Prometheus, Grafana, Loki)
- B1–B3 live (Registry read-only, Serve Reload by Version, Observability-Panels)
- API-01 `/v1/datasets` (Create/Read/List) verfügbar

## In Arbeit (Next)

- Play UI (Real-Predict Hook), Contract-Check `/v1/predict`

## Belege

- Grafana Dashboard „ChessApp – Overview“ (p95 Latenz, Error-Rate)
- MLflow letzter erfolgreicher Trainings-Run

## Database

- 2025-08-31: DB-01 done — Flyway V1 baseline + indices; seeds added; CRUD tests green. (pgvector planned later)

## Changelog

- M1/API-01 done (2025-08-31): CRUD Surface /v1/datasets inkl. OpenAPI, IT, MDC
