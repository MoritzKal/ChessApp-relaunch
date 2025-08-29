# User Flows (für BA/Stakeholder‑Demos)

## 1) Dataset anlegen
1. `POST /v1/datasets` mit Name/Version/Filter
2. MinIO: `datasets/<id>/manifest.json`
3. Prometheus: `chs_dataset_build_total` ++
4. Grafana: Overview zeigt KPI‑Anstieg

## 2) Ingest (offline)
1. `POST /v1/ingest` (offline=true)
2. `GET /v1/ingest/<runId>` → counts & `reportUri`
3. DB: games/moves/positions gefüllt
4. Logs: `ingest.*` mit `mdc.run_id`

## 3) Training (simuliert)
1. `POST /v1/trainings` → `runId`
2. MLflow: Run sichtbar; MinIO: `training_report.json`
3. Prometheus: `chs_training_*`
4. Grafana: Training‑Panels füllen sich

## 4) Serving
1. `POST /v1/predict` mit FEN
2. Response: `move`, `legal`, `modelId`
3. Prometheus: `chs_predict_*` + Heatmap
4. Loki: `predict.*` Events
