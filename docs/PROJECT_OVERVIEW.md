# ChessApp – Projektüberblick (Stand: 2025-08-29)

## Was ist ChessApp?
ChessApp ist eine modulare Plattform, um
1) Daten von chess.com-Accounts zu importieren (Ingest),
2) Datasets zu verwalten,
3) Trainingsläufe für ein Schachmodell zu orchestrieren und nachzuverfolgen,
4) ein Inference-Serving bereitzustellen, und
5) alles davon durchgängig zu beobachten (Dashboards, Metriken, Logs, Artefakte).

## Architektur (High Level)
- **Infra (Docker Compose):** Postgres, MinIO (+ Buckets), MLflow, Prometheus, Grafana, Loki, Promtail.
- **API (Java 21, Spring Boot 3.x, Maven, Multi-Module):** Actuator/Prometheus, JSON-Logs (MDC), OpenAPI.
- **ML (FastAPI @8000):** Trainingsservice (PR#7) – simuliertes Training mit MLflow/Artefakten/Metriken.
- **Serve (FastAPI @8001):** Inference-Service – `/predict` mit legaler Zugauswahl (Stub) und Telemetrie.
- **Observability:** Prometheus-Scrapes, Loki-Logs, Grafana „ChessApp – Overview“ Dashboard.

## Aktueller Funktionsumfang (MVP-Vertical Slices)
- **Datasets:** `POST/GET /v1/datasets` – legt Manifest in MinIO an, zählt Metriken, loggt strukturiert.
- **Ingest:** Offline-Demo-PGN → persistiert games/moves/positions, idempotent, schreibt Report in MinIO, Metriken/Logs.
- **Training:** ML-Service simuliert Runs, Prometheus `chs_training_*`, MLflow-Artefakte (`best.pt`, `training_report.json`).
- **Serving:** Serve-Service liefert legale Züge zu FEN, Prometheus `chs_predict_*`, JSON-Logs, API-Proxy `/v1/predict`.

## Quickstart (lokal)
1. `cp .env.example .env` – Secrets prüfen/anpassen.
2. `make up` – startet den Stack.
3. Öffne: Grafana `:3000`, Prometheus `:9090`, MinIO `:9001`, MLflow `:5000`.
4. **Datasets** anlegen: `POST /v1/datasets`.
5. **Ingest** (offline) starten: `POST /v1/ingest`.
6. **Trainingslauf** starten: `POST /v1/trainings` (erzeugt MLflow-Run & Artefakte).
7. **Serving testen:** `POST /v1/predict { "fen": "<FEN>" }`; Dashboard kontrollieren.

## Observability-Hinweise
- **Prometheus**: `/actuator/prometheus` (API), `/metrics` (ml/serve). Präfix `chs_*` für geschäftliche KPIs.
- **Loki/Grafana Explore**: Query-Beispiel: `{service=~".+"}` (keine `.*`-Matcher). Logs sind JSON und per `| json` filterbar.
- **Dashboards**: Grafana → Folder „ChessApp“ → „ChessApp – Overview“. Panels für Ingest, Training, Serving.

## Governance & Regeln
- **Wasserfall pro Block**: Ein Feature wird Ende-zu-Ende fertig (API + Telemetrie + Dashboard + Tests), erst dann der nächste Block.
- **Tests verpflichtend**: Jeder neue Endpunkt und jede neue Komponente braucht Tests.
- **Observability-first**: Neue Funktionen liefern *mindestens* Metriken (`chs_*`) und strukturierte JSON-Logs mit MDC (`run_id`, `dataset_id`, `model_id`, `username`, `component`).

