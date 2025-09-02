# Self-Play & Offline Evaluation

## 1. Überblick
Zwei interne Dienste unterstützen Entwicklungs- und Testprozesse:

- **Self-Play Runner** (Port 8011) orchestriert Matches zwischen Kandidaten- und Baseline-Modellen und schreibt Reports.
- **Offline Eval** (Port 8012) bewertet Modelle gegen ein FEN/Label‑Dataset, erstellt Plots und logged Kennzahlen.

## 2. Start / Stop
```
make -f Makefile.selfplay up   # Services starten
make -f Makefile.selfplay logs # Logs verfolgen
make -f Makefile.selfplay down # Services stoppen
```

## 3. Endpunkte
### Self-Play Runner (8011)
**POST /runner/selfplay/start**
```json
{"modelId":"candidate_foo","baselineId":"prod","games":20,"concurrency":4,"seed":42}
```
Antwort → `{"runId":"<uuid>"}`

**GET /runner/selfplay/runs/{runId}**
Antwort →
```json
{"runId":"<uuid>","status":"completed","progress":{"played":20,"total":20},"metrics":{"elo":12.3,"winRate":0.55},"reportUri":"/artifacts/selfplay/<uuid>/report.json"}
```

### Offline Eval (8012)
**POST /runner/eval/start**
```json
{"modelId":"candidate_foo","datasetId":"val_2025_08","metrics":["val_loss","val_acc_top1","ece"]}
```
Antwort → `{"evalId":"<uuid>"}`

**GET /runner/eval/{evalId}**
Antwort →
```json
{"evalId":"<uuid>","status":"completed","metrics":{"val_loss":0.9,"val_acc_top1":0.71,"ece":0.08},"reportUri":"/artifacts/eval/<uuid>/report.json"}
```

## 4. Artefakt-Struktur
```
artifacts/
  selfplay/<runId>/report.json  *.pgn
  eval/<evalId>/report.json  metrics.json  calibration.png  topk.png  …
```

## 5. Prometheus-Metriken
**Self-Play**
- `chs_selfplay_games_total{result="win|loss|draw"}`
- `chs_selfplay_elo_estimate`
- `chs_selfplay_move_time_seconds_bucket`
- `chs_selfplay_queue_depth`
- `chs_selfplay_errors_total{type}`

**Eval**
- `chs_eval_last_val_loss`
- `chs_eval_last_val_acc_top1`
- `chs_eval_last_val_acc_top3`
- `chs_eval_last_ece`
- `chs_eval_runtime_seconds_bucket`
- `chs_eval_errors_total{type}`

## 6. Dashboards
- Self-Play — v1 (UID `obs-selfplay-v1`)
- Offline Eval — v1 (UID `obs-eval-v1`)

Import über Grafana → Dashboards → Import → JSON aus `infra/grafana/dashboards/obs/`.

## 7. Beispiel-Workflows
- **20-Spiele-Smoke:** `bash scripts/selfplay_smoke.sh`
- **Mini-Dataset-Eval:** `bash scripts/eval_smoke.sh`
- **MLflow-Logging:** setze `MLFLOW_TRACKING_URI` und starte Evaluierung; Artefakte werden geloggt.

## 8. Troubleshooting
- `SERVE_PREDICT_URL` nicht erreichbar → Runner nutzt Dummy-Zug-Generator.
- Timeouts/5xx vom Serve werden gezählt (`chs_selfplay_errors_total`).
- Fehlende Modelle → Dummy-Policy.

## 9. DoD & Abnahme
Details und Prüfprotokolle stehen in [ACCEPTANCE.md](ACCEPTANCE.md).
