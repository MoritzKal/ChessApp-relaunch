# Frontend-Theme & UI Spezifikation – ChessApp (v0.1)

**Stand:** 2025‑09‑03 (Europe/Berlin) · **Status:** Draft → Review · **Scope:** Vue 3 + Vite + Vuetify Frontend inkl. Theme, Layout (BaseFrame), Komponenten‑Inventar, Informationsarchitektur, I/O‑Interaktionen, Dashboard‑Datenlandkarte.

**Leitplanken:** Observability‑first · Tests Pflicht · **keine /v1‑Breakings** (Alias: `/v1/data/import` ⇒ **`/v1/ingest`**).

---

## 1) Technischer Stack & Versionierung

**Runtime/Frameworks**

* **Vue 3** (Composition API, `<script setup>`). **Pinned:** ^3.5.x (ESM).
* **Vite** (Dev/Build). **Pinned:** ^7.1.x.
* **Vuetify 3** (UI‑Kit). **Pinned:** ^3.9.x.
* **Pinia** (State). **Pinned:** ^3.0.x.
* **Vue Router** (Routing). **Pinned:** ^4.4.x.
* **Axios** (HTTP). **Pinned:** ^1.11.x.
* **echarts / vue-echarts** *oder* **chart.js / vue-chart-3** (Charts) – Entscheidung per RFC; Default: **ECharts** (gute Dark‑Theme‑Lesbarkeit).
* **zod** (Schema‑Validation) + **zodios** (optionales Typed‑API‑Layer) – nice‑to‑have.

**Tooling**

* **Vitest** (Unit), **Cypress** (E2E), **ESLint** + **Prettier** (CI Gate).
* **Mock Service Worker (msw)** für API‑Stubs im Storybook/Tests (optional in v0.2).

**Browser Support**: Evergreen Desktop (Chromium, Firefox, Safari aktuell), iPadOS Safari. Mobile Phone: später.

**Design Assets**: Variable Fonts **Inter** (UI) & **JetBrains Mono** (code/notation). Icons: **Material Design Icons** + **Chess Glyphs** (SVG, custom).

**Theme/Tokens**

* **Farben (Dark Default)**

  * `primary` Brass **#CBA35C** · `secondary` Walnut **#8B5E3C**
  * `background` **#0B0E0C** · `surface` **#0F1F1C** · `surface-variant` **#15322D**
  * `on-surface` **#EDEDED** · `outline` **#2C3A36**
  * Status: `info #6BB3FF`, `success #5FBF77`, `warning #F2C14E`, `error #E57373`
* **Typografie**: Scale 12/14/16/20/24/32/40; Weights 400/600.
* **Spacing**: 4‑pt Raster (4‑8‑12‑16‑24‑32‑48).
* **Radius**: 10/14/20 px; **Elevation**: warme, weiche Schatten; **Motion**: 120/200/320 ms.
* **Holz‑Akzent**: subtil (Gradients/Noise), keine großen Texturen by default.

**Netzwerk/Contracts**

* Alle Calls **/v1 stabil** (nur additive Änderungen). Import via **`POST /v1/ingest`**. Prometheus & Logs via Proxy‑Links im UI (Observability‑Slots auf jeder Seite).

---

## 2) App‑Struktur (Front‑End)

```
/frontend
  src/
    assets/            # Logos, SVGs, (optionale) Holz‑Textures (WebP klein)
    components/        # Reusable UI (Cards, Tiles, Charts, Board, ...)
    composables/       # useAxios, useSse, usePagination, ...
    layouts/           # BaseFrame (AppShell)
    pages/             # Overview, Data, Datasets, Training, Models, Evaluation, Play, Observability
    router/            # routes.ts
    stores/            # pinia: data, datasets, training, models, evaluation, play, ui
    styles/            # theme.css, variables.css
    plugins/           # vuetify.ts, axios.ts, echarts.ts
    types/             # API Types (zod), DTOs
    main.ts
```

**BaseFrame (Canvas/AppShell)**

* **Top AppBar**: Brand, KPI‑Ticker (optional), Actions (Theme‑Toggle, Help), Overflow.
* **Side Nav (Rail/Expand)**: Overview, Data, Datasets, Training, Models, Evaluation, Play, Observability.
* **RouterView**: Seiteninhalt im 12‑Spalten CSS‑Grid.
* **Snackbar (global)**: Success/Warn/Error mit Action.
* **Right Drawer (kontextuell)**: Logs/Details/Filters (optional per Seite).

**Routing**

* `/` Overview · `/data` · `/datasets` · `/training` · `/models` · `/evaluation` · `/play` · `/obs`.
* Lazy‑Loaded Routes; Guard: Auth (JWT optional MVP: Single‑User Secret per ENV).

**HTTP/State**

* **Axios‑Instanz** (`plugins/axios.ts`) mit BaseURL, Interceptors (Auth, Error), Retry (Idempotent GETs).
* **Pinia Stores**: domain‑zentriert (siehe 3). Persistenz (session/local) nur für leichte UI‑Prefs.

---

## 3) Komponenten‑Inventar (v0.1)

**Primitives**

* `AppShell` (BaseFrame), `SideNav`, `AppBar`, `SnackbarHost`, `ConfirmDialog`
* `Card`, `MetricTile`, `KpiRow`, `DataTable` (Server‑Pagination), `ToolbarFilters`, `EmptyState`, `ErrorState`, `LoadingState`
* `Form*` Controls: TextField, Select, Range/Slider, Switch, DateRangePicker

**Charts** (Wrapper‑Komponenten)

* `TimeseriesChart` (loss/acc/req\_s)
* `DistributionChart` (histogram/pie)
* `RadarChart` (Style/Opening)
* `Sparkline` (für Tiles)

**Domain**

* **Data**: `ImportForm`, `GamesTable`, `GameDetailsDrawer`
* **Datasets**: `DatasetBuilderWizard`, `DatasetList`, `DatasetDetails`
* **Training**: `TrainingRunPanel` (Tabs: Metrics | Logs | System), `TrainingWizard`
* **Models**: `ModelRegistryList`, `ModelCompare`, `PromoteDialog`
* **Evaluation**: `ABReport`, `EloSimCard`
* **Play**: `ChessBoard` (Vue‑Wrapper), `MoveProbBar`, `Clock`, `StrengthControls` (Temperature/Top‑k)
* **Observability**: `GrafanaPanelEmbed`, `LokiLogViewer`, `TraceLink`

---

## 4) Pinia Stores (Domänen‑Schnitt)

* `useDataStore()` – Import‑Runs, Games (Liste/Detail), Filter.
* `useDatasetStore()` – Builder, Versionen, aktive Dataset‑ID.
* `useTrainingStore()` – Starten, Status, Live‑Metriken (`loss`, `val_acc_top1`…), Logs‑Link.
* `useModelStore()` – Registry, aktives **prod**‑Modell, Promote/Demote.
* `useEvaluationStore()` – A/B‑Runs, Reports.
* `usePlayStore()` – Boardstate, KI‑Optionen (temperature/topk), letzte Prediction.
* `useUiStore()` – Theme, Snackbars, Busy‑Flags, Feature‑Toggles.

Jeder Store kapselt **I/O** (Axios‑Calls) + **DTO→ViewModel**‑Mapping. Fehler werden zentral an den `SnackbarHost` gemeldet.

---

## 5) I/O – Interaktionen & Formulare (v0.1)

**Data / ImportForm**

* Felder: `username:string` (required), `from:date` (optional), `to:date` (optional)
* Aktion: **POST `/v1/ingest`** `{ username, from?, to? }` → Run‑ID
* Folge: Poll **GET `/v1/ingest/{runId}`** bis `status ∈ {running, completed, failed}`; Metriken: `chs_ingest_*` (Overview‑KPI aktualisieren)

**Datasets / Builder**

* Felder: Zeitkontrolle (enum), Gegner‑Elo (range), Farbe (white/black), Result (W/D/L), Split (train/val/test %)
* Aktion: **POST `/v1/datasets`** `{ filter, split }` → Dataset‑ID
* Folge: **GET `/v1/datasets/{id}`** Details; zeigt Größen/Verteilungen als Charts

**Training / Wizard**

* Felder: `datasetId`, `preset: "policy_tiny"`, `params:{epochs:number, batchSize:number, lr:number}`
* Aktion: **POST `/v1/trainings`** → `runId`
* Folge: Status via **GET `/v1/trainings/{runId}`** (Polling 2s) *oder* SSE (optional). Tabs: Metrics, Logs, System.

**Models / Registry**

* Listen: **GET `/v1/models`**; Promote: **POST `/v1/models/promote`** `{modelId}` *oder* **POST `/v1/models/load`** für Serving.

**Evaluation / A‑B**

* Felder: `modelA`, `modelB`, `metricSuite` (preset)
* Aktion: **POST `/v1/evaluations`** → `evalId`; Anzeige: **GET `/v1/evaluations/{id}`** (Radar, Tabellen)

**Play / Board**

* Steuerung: `temperature:number` (0.0–1.2), `topk:int` (1–10)
* Zuginferenz: **POST `/v1/predict`** `{ fen, history?, temperature?, topk? }` → `{ move, policy[] }`
* Optional Game‑Flow: **POST `/v1/play/new`**, **POST `/v1/play/{id}/move`** (Legalität/Uhr), **GET `/v1/play/{id}`**

**Observability**

* Prometheus: `/actuator/prometheus` (via Proxy) → Timeseries;
* Logs: Links in Panels (Loki Query mit `run_id`/`component`).

---

## 6) Seiten‑Layouts & Dashboard‑Daten (Was/Wie/Befüllung)

### 6.1 Overview

* **KPI‑Tiles**: *Games Imported*, *Active Training*, *Best Val‑Acc*, *Prod Model*

  * Herkunft: `GET /v1/datasets` (Counts), `GET /v1/trainings?status=running`, `GET /v1/models?stage=prod` + `metrics.best_val_acc`
* **Training Loss (Timeseries)**: Prometheus‑Query `chs_training_loss{run_id=…}`; Live per Poll (5s) oder SSE‑Proxy
* **Recent Trainings (Tabelle)**: `GET /v1/trainings?limit=10` → Spalten Run‑ID, Epoch, Val‑Acc, Status
* **Requests/sec (Timeseries)**: `rate(chs_api_requests_total[1m])` – optional getrennt nach `route="/v1/predict"`
* **Mini‑Board/Promo**: Quick‑Link zur Play‑Seite
* **Leere‑Zustände**: CTA „Import starten“ (öffnet Data/ImportForm)

### 6.2 Data

* **Import‑Karte** + **Progress** (Status, `games/sec`, Errors)
* **GamesTable** mit Filterleiste (Zeitkontrolle, Ergebnis, ELO). Paginierte Server‑Suche `GET /v1/games`.

### 6.3 Datasets

* **BuilderWizard** (Form, Validierung), **DatasetList**
* **Charts**: Verteilungen (Zeitkontrolle, Ergebnis, ELO), Größe pro Split

### 6.4 Training

* **Run‑Controls** (Start, Abbrechen\* später), Param‑Summary
* **Tabs**: *Metrics* (loss/val\_acc/lr/throughput), *Logs* (Loki‑Embed mit `run_id`), *System* (GPU/CPU/Memory – falls exportiert)

### 6.5 Models

* **Registry** mit *staging/prod* Badges; **Vergleich** (Metrik‑Tabelle); **PromoteDialog**

### 6.6 Evaluation

* **A/B Report**: Radar (Style/Opening), Tabellen (Accuracy/Top‑k, Blunder‑Rate), Download Link (Report URI)

### 6.7 Play

* **Board links**, rechts **MoveProbBar**, unten **ACPL‑Sparkline** (wenn Daten vorhanden)
* **Interaktionen**: Menschzug → `play/move` (optional) & **predict** für KI‑Zug/Hinweis

### 6.8 Observability

* **GrafanaPanelEmbed**: Panels für System/Trainingsmetriken; **LokiLogViewer** mit vordefinierten Queries

---

## 6.9 Login (Auth)

**Ziel:** Optionale Auth für gesicherte Deployments; lokal kann ein statischer Token genutzt werden.

**Route:** `/login`

**Formfelder:** `username`, `password` (required), `rememberMe` (bool)

**Flow (prod‑fähig):**

1. **POST** `/v1/auth/login` `{ username, password }` → `{ accessToken, refreshToken?, expiresIn }`
2. Token im `useUiStore().auth` speichern; Axios‑Interceptor setzt `Authorization: Bearer <token>`.
3. Route‑Guard schützt alle Routen außer `/login`.

**Flow (dev‑profil „codex“) – fallback:**

* ENV `VITE_DEV_STATIC_TOKEN` wird clientseitig injiziert; Login nimmt beliebige Credentials an und schreibt den Token.

**UI‑Elemente:** Passwort‑Feld, Show/Hide, Error‑Banner, „Login“ Button, Shortcut `Enter`, Link „Help/Docs“.

**Fehlerfälle:** 401/403 → Snackbar + Link „View Logs“ (Obs‑Proxy).

---

## 6.10 Configuration Workbench (KI/Workflow)

**Ziel:** Oberflächen zum Setzen/Ausführen von Workflows (KI konfigurieren, Datensätze wählen, Training starten, Self‑Play, Export).

**Route:** `/config`

**Tabs & Komponenten:**

* **Model Setup**: `ModelPresetSelect`, `HyperParamsForm` (lr, batchSize, epochs, temperature, topk), `SavePresetBtn`
  • **POST** `/v1/config/model` → Persistenz der aktuellen Konfiguration
* **Dataset Selection**: `DatasetPicker` (Multi), `SplitEditor`
  • **POST** `/v1/config/datasets` `{ ids, split }`
* **Workflow**: `WorkflowBuilder` (Steps: Ingest → Build → Train → Eval → Promote), `RunWorkflowBtn`
  • **POST** `/v1/workflows` `{ steps[], configRef }` → `workflowId`
  • **GET** `/v1/workflows/{id}` (Status, Logs‑Link)
* **Self‑Play**: `SelfPlayPanel` (games, timeControl, concurrency)
  • **POST** `/v1/selfplay` `{ games, timeControl, concurrency }` → `runId`
  • **GET** `/v1/selfplay/{runId}` (progress, results)
* **Export / Snapshot**: `ExportStateCard`
  • **POST** `/v1/export/state` `{ include:{datasets,models,reports} }` → `reportUri`

**I/O Mindestumfang (MVP):** Falls Endpunkte (noch) fehlen, Buttons disabled + Tooltip „Requires backend endpoint“. Keine /v1‑Breakings; nur additive.

**Observability:** Jede Tab‑Seite mit Status‑Panel (PromQL/Logs nach `component` & `run_id`).

---


## 7) Datenmodelle (UI‑relevant, komprimiert)

**TrainingRun** `{ runId, status, epoch?, loss?, val_acc_top1?, startedAt, finishedAt? }`

**Dataset** `{ id, name, version, size_rows, split:{train, val, test}, filter:{...} }`

**Model** `{ id, name, version, stage: "staging"|"prod", metrics:{best_val_acc?, ...} }`

**PredictRequest** `{ fen:string, history?:string[], temperature?:number, topk?:number }` → **PredictResponse** `{ move:string, policy:Array<{uci:string, p:number}> }`

---

## 8) Validierung, Fehler, UX‑Standards

* **Validation**: zod‑Schemas pro Request (Client‑Side), Server‑Fehler → menschenlesbar mappen.
* **Error‑States**: Card‑Level Fehler mit „Retry“ + „View Logs“. Globaler `onAxiosError` → Snackbar.
* **Leere Zustände**: Handlungsaufforderungen (Import starten / Dataset bauen / Training starten).
* **A11y/Contrast**: Messing‑Akzente für Headlines/KPIs, Fließtext in `on-surface`.

---

## 9) Tests (v0.1)

* **Unit (Vitest)**: Stores (Reducer/Mapper), Utils (FEN/Board‑Adapter), Chart‑Mappers.
* **E2E (Cypress)**: Flows „Import → Dataset → Training → Play“ mit Mock‑Backends (msw).
* **Contract**: Schema‑Checks gegen API‑DTOs (zod).

---

## 10) Open Points / Entscheidungen (für Review)

1. **Charts‑Lib**: ECharts vs. Chart.js – *Vorschlag:* ECharts (Dark‑Theme out‑of‑box, Performance).
2. **Live‑Metrik‑Transport**: Polling (5s) jetzt; SSE/WebSocket später (Backend RFC nötig).
3. **Play‑Flow**: Minimal nur `/v1/predict` oder kompletter `/v1/play/*` Flow? – *Vorschlag:* Minimal jetzt, Flow später.
4. **Theming**: Holz‑Hintergrund als CSS Gradients (Default) – Texture (WebP) optional per Toggle.

---

## 11) Anhänge

* **Theme‑Token Tabelle** (Palette, Typo, Spacing, Radius, Elevation, Motion).
* **Komponenten‑Matrix** (Seite × Komponenten) – wird im nächsten Commit als Tabelle ergänzt.
* **Routing‑Matrix** (Route × Guards × SEO‑Meta) – Low‑prio fürs MVP.

---

## 12) Run‑Profile & Container (Dev/Prod)

**Dev (npm, Hot Reload):**

* `npm run dev` → Vite (Port 5173), **Proxy** `/v1` → `http://localhost:8080` (API), `/actuator/prometheus` → Obs‑Proxy.
* ENV: `.env.development` → `VITE_API_BASE=http://localhost:8080` · `VITE_OBS_BASE=http://localhost:3000` · `VITE_DEV_STATIC_TOKEN=`

**Docker – Prod Build:**

* Multi‑Stage Dockerfile (Node 20 Alpine → NGINX Alpine). Static SPA unter `/usr/share/nginx/html`.

**Docker – Dev Container (optional):**

* `Dockerfile.dev` + Compose Service `fe-dev` (bind‑mount, Vite server).

**Compose‑Erweiterung:** `infra/docker-compose.yml` → Service `frontend` (prod) und `fe-dev` (dev). Traefik/Nginx optional.

**Scripts (package.json):** `dev`, `build`, `preview`, `lint`, `test:unit`, `test:e2e`.

**.gitignore Ergänzungen:** `frontend/node_modules`, `frontend/dist`, `*.local.env`, `coverage/`, `cypress/videos`, `cypress/screenshots`.

---

## 13) Tests & QA Update

* **Auth**: Unit‑Tests für `useAuthStore`, Router‑Guards, Axios‑Interceptor.
* **Config Workbench**: Form‑Validierungen (zod), API‑Mocks (msw), E2E‑Flows: Preset speichern → Workflow starten → Status erscheint.
* **Docker Smoke**: `make fe-up` startet Frontend‑Container; Healthcheck `/index.html` 200.

---

## 14) Patch‑Plan (Codex Prompts, v0.1)

1. **docs/** – neue Dateien anlegen: `docs/frontend/login.md`, `docs/frontend/configuration.md`, `docs/frontend/dev-setup.md`, `docs/frontend/docker.md`, `docs/frontend/baseframe.md`. Inhalte gemäß obiger Spezifikation.
2. **frontend/** – Scaffold Auth + BaseFrame + Routing: neue Seite `/login`, Store `auth`, Axios‑Interceptor, Guards.
3. **frontend/** – Configuration Workbench (Tabs, Forms, Platzhalter‑Calls, disabled Buttons bei fehlenden Endpunkten).
4. **infra/** – docker-compose Service `frontend` + optional `fe-dev`; `frontend/Dockerfile`, `frontend/Dockerfile.dev`, `frontend/nginx.conf`.
5. **CI/Docs** – Markdown Lint & Linkcheck erweitern; `Makefile.docs` Ziel `docs-frontend` anhängen.
