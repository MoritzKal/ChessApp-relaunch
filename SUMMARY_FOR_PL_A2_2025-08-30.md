# Block A2 – Dataset Schema v0: Summary (2025-08-30)

- Fix: Windows Git-Bash MSYS path rewrite prevented dataset export. Added MSYS guards and shell-wrapped exec in `scripts/a2_verify_min.sh`; added Windows notes and troubleshooting docs; mapped `data/` and `out/` volumes in compose.

## Results
- Rows (chs_dataset_rows_total): <fill after run>
- Invalid rows total (sum by reason): <fill after run>
- Export duration (ms): avg over last run window
  - `sum(chs_dataset_export_duration_ms_sum) / sum(chs_dataset_export_duration_ms_count)`

## Verification Steps
- Script: `./scripts/a2_verify_min.sh` (Git-Bash, PowerShell, macOS/Linux)
- Swagger: `http://localhost:8000/docs` shows `POST /internal/dataset/metrics`
- Metrics: `http://localhost:8000/metrics` contains
  - `chs_dataset_rows_total`
  - `chs_dataset_invalid_rows_total{reason}`
  - `chs_dataset_export_duration_ms_sum/_count` (> 0 after export)
- Grafana (Dataset Quality):
  - Invalid Rate < 1%
  - Last Export Duration (ms) plausible

## PromQL Cheatsheet
- Invalid Rate (%): `sum(chs_dataset_invalid_rows_total) / sum(chs_dataset_rows_total) * 100`
- Last Export (ms): `sum(chs_dataset_export_duration_ms_sum) / sum(chs_dataset_export_duration_ms_count)`
- Invalid by Reason (1h): `sum by (reason) (increase(chs_dataset_invalid_rows_total[1h]))`
- Rows Processed (5m): `sum(rate(chs_dataset_rows_total[5m]))`

## Risks & Mitigations
- Risk: Platform differences (Windows Git-Bash) rewriting container paths.
  - Mitigation: `MSYS_NO_PATHCONV=1`, `MSYS2_ARG_CONV_EXCL="*"`, and shell-wrapped container exec.
- Risk: Missing host visibility of artifacts.
  - Mitigation: Compose volumes `../data:/app/ml/data`, `../out:/app/ml/out`.

## Artifacts
- Manifest: `out/a2/manifest.json`
- Parquet: `out/a2/compact.parquet`
- Screenshots: Grafana Dataset Quality, `/metrics` excerpt

