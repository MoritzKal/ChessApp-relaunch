# Configuration Workbench (KI/Workflow)
**Route:** /config · Tabs:
1) **Model Setup** – Presets & Hyperparameter (lr, batchSize, epochs, temperature, topk)  
   - POST /v1/config/model (planned)
2) **Dataset Selection** – Auswahl mehrerer Datasets + Split-Editor  
   - POST /v1/config/datasets (planned)
3) **Workflow** – Builder (Steps: Ingest→Build→Train→Eval→Promote)  
   - POST /v1/workflows (planned), GET /v1/workflows/{id}
4) **Self-Play** – games, timeControl, concurrency  
   - POST /v1/selfplay (planned), GET /v1/selfplay/{runId}
5) **Export/Snapshot** – Zustands-Export (Datasets/Models/Reports)  
   - POST /v1/export/state (planned) → reportUri
**Hinweis:** Buttons disabled, wenn Endpunkte (noch) fehlen. Keine /v1-Breakings.
