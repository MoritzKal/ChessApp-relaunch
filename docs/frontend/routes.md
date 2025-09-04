# Routes & IA (Vue Router)

/                → Overview (Dashboard/Status)
/data            → Data (Import, Games, Positions)
/datasets        → Datasets (Builder, Versions)
/trainings        → Training (Runs, Live-Metrics)
/models          → Models (Registry, Promote/Load)
/evaluation      → Evaluation (A/B)
/play            → Play (Board, Clock, Strength)
/observability   → Observability (Grafana/Loki Embeds)
/config          → Configuration Workbench (KI/Workflow)

## Guards
- Auth erforderlich: /play, /training, /models, /datasets (schreibende Aktionen)
- Lazy-Loading je Route; globaler 404-Catch

## Layout
- AppBar + NavDrawer + Main + GlobalToasts
- Dark-Mode-Toggle, User-Menu (Token/Logout)
- `router-view` + `Suspense`
