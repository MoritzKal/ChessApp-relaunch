# App-Struktur (Front-End)

```
/frontend
  src/
    assets/            # Logos, SVGs, (optionale) Holz-Textures (WebP klein)
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

* **Top AppBar**: Brand, KPI-Ticker (optional), Actions (Theme-Toggle, Help), Overflow.
* **Side Nav (Rail/Expand)**: Overview, Data, Datasets, Training, Models, Evaluation, Play, Observability.
* **RouterView**: Seiteninhalt im 12-Spalten CSS-Grid.
* **Snackbar (global)**: Success/Warn/Error mit Action.
* **Right Drawer (kontextuell)**: Logs/Details/Filters (optional per Seite).

**Routing**

* `/` Overview · `/data` · `/datasets` · `/training` · `/models` · `/evaluation` · `/play` · `/obs`.
* Lazy-Loaded Routes; Guard: Auth (JWT optional MVP: Single-User Secret per ENV).

**HTTP/State**

* **Axios-Instanz** (`plugins/axios.ts`) mit BaseURL, Interceptors (Auth, Error), Retry (Idempotent GETs).
* **Pinia Stores**: domain-zentriert (siehe 3). Persistenz (session/local) nur für leichte UI-Prefs.
