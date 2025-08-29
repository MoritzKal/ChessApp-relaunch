# FAQ & Glossar (Stand: 2025-08-29)

## Glossar
- **PGN (Portable Game Notation):** Textformat für Schachpartien (Züge, Metadaten). Beispiel: `1. e4 e5 2. Nf3 ...`
- **FEN (Forsyth–Edwards Notation):** String, der eine Brettstellung beschreibt. Beispiel: `rnbqkbnr/pppppppp/8/... w KQkq - 0 1`
- **SAN (Standard Algebraic Notation):** Kurznotation eines Zugs aus Sicht der Stellung, z. B. `Nf3`, `O-O`.
- **UCI (Universal Chess Interface):** Engine-Protokoll; wir verwenden UCI-Strings für Züge (z. B. `e2e4`).
- **Dataset:** Fachlicher Datensatz (Subset/Version) mit Manifest in MinIO; repräsentiert die Trainings-/Evaluationsbasis.
- **Ingest:** Importprozess (z. B. von chess.com oder Offline-Fixtures) → persistiert `games/moves/positions` + Report.
- **Model:** Trainiertes Artefakt (Version, Framework, Metriken, Artifact-URI).
- **Training Run:** Ausführung eines Trainings (Start/Ende, Status, Metriken, Artefakte, Logs/Report).
- **Serving:** Bereitstellung eines Modells für Vorhersagen (hier: legaler Zug aus FEN; Stub ohne NN).
- **MLflow:** Tracking von Runs, Parametern, Metriken, Artefakten.
- **MinIO (S3):** Objekt-Storage; Buckets: z. B. `datasets/`, `reports/`, `mlflow/`.
- **Prometheus:** Zeitreihenmetriken; wir nutzen Prefix **`chs_*`** für fachliche KPIs.
- **Grafana:** Dashboards & Explore; „ChessApp – Overview“ ist das zentrale Dashboard.
- **Loki/Promtail:** Zentrales Logging (JSON); Explore mit LogQL (`| json` für Parsing).
- **MDC (Mapped Diagnostic Context):** Kontextfelder in Logs: `run_id`, `dataset_id`, `model_id`, `username`, `component`.

## Häufige Fragen (FAQ)

### Wie starte ich das System lokal?
Siehe `docs/GETTING_STARTED.md`. Kurz: `.env` anlegen → `make up` → Targets & Dashboards prüfen.

### Wieso liefert meine Loki-Query einen Parse-Fehler?
LogQL verlangt mind. einen **nicht-leeren Matcher**. Verwende `{service=~".+"}` statt `{service=~".*"}` und nutze anschließend `| json`.

### Wo finde ich die wichtigsten Endpunkte?
- API: `/v1/datasets`, `/v1/ingest`, `/v1/trainings`, `/v1/predict`, `/v1/models/load`
- Observability: API `/actuator/prometheus`, ML/Serve `/metrics`, Grafana „ChessApp – Overview“

### Wie erkenne ich, dass ein Ingest/Training „läuft“?
- **Prometheus**: `chs_ingest_*` bzw. `chs_training_*` verändern sich.
- **Loki**: Ereignisse `ingest.started/…completed` bzw. `training.started/…completed` mit `mdc.run_id`.

### Wie finde ich Artefakte (Reports/Modelle)?
- **MinIO**: Datasets → `s3://datasets/<id>/manifest.json`, Ingest-Reports → `s3://reports/ingest/<runId>/report.json`.
- **MLflow**: Trainingsläufe inkl. Metriken & Artefakten.

### Darf ich Endpunkte verändern?
Nur **abwärtskompatibel** (Attribute hinzufügen ok). Breaking Changes bedürfen PL-Entscheid, Versionierung (`/v2`) und Migrationspfad.

### Wie logge ich korrekt?
Einzeiliges JSON, keine sensiblen Daten. MDC („Kontext“) setzen: `X-Run-Id`, `X-Username`, `X-Component` → werden als `mdc.*` übernommen.

### Welche Metriken muss ich für neue Funktionen anlegen?
- Mindestens einen **Counter** (z. B. `chs_feature_action_total`).
- Optional **Histogram/Timer** für Latenzen (z. B. `chs_feature_latency_seconds`).
- Labels: `application`, `component`, `username` (Common Tags).

### Typische Probleme & schnelle Checks
- **Grafana-Login fehlt** → ggf. Anonymous deaktivieren/Passwort resetten (siehe Runbook).
- **Loki leer** → promtail läuft? Docker-Sock gemountet? Labels vorhanden? Query `{service=~".+"}`.
- **Prometheus-Target rot** → Port/Path korrekt? `/metrics`/`/actuator/prometheus` erreichbar?
- **DB-Migration scheitert** → Flyway-Logs lesen, Schema-Fortschritt prüfen (`flyway_schema_history`).

