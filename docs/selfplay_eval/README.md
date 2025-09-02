# Self-Play & Offline Evaluation

## Dashboards

The repository provides example Grafana dashboards:

- **Self-Play — v1** (UID: `obs-selfplay-v1`)
- **Offline Eval — v1** (UID: `obs-eval-v1`)

Import via Grafana → Dashboards → Import and upload the JSON from `infra/grafana/dashboards/obs/`.

Example queries:

```
sum(rate(chs_selfplay_games_total[5m]))
max(chs_eval_last_val_acc_top1)
```
