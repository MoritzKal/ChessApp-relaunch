# BaseFrame (AppShell)
Enthält Top-AppBar, Side-Nav (Rail/Expand), globalen Snackbar-Host und <RouterView/>.
- AppBar: Brand, Theme-Toggle, Help, Overflow.
- Side-Nav: Overview · Data · Datasets · Training · Models · Evaluation · Play · Observability.
- Right-Drawer (optional): Logs/Details/Filter.
Responsives 12-Spalten-Grid im Content.
Observability-Slot pro Seite (Panel/Logs/Trace-Link).

## Panel Renderer (VM-first)
Renderer sind rein präsentational und kennen nur View-Model-Props – keine Store- oder Service‑Abhängigkeiten.

- `KpiTile` (`src/components/renderers/KpiTile.vue`)
  - Prop `vm: KpiVM { label, value, unit?, delta?, badge?, ariaLabel?, href? }`
  - Optional: `icon` (MDI). Click navigiert zu `href`.

- `ChartTile` (`src/components/renderers/ChartTile.vue`)
  - Prop `vm: SeriesVM { series: [{ label, data: [{x,y}], color?, type? }] , range? }`
  - Range-Switcher (24h/7d/30d), responsive, Canvas (Chart.js). Skeleton ≤ 300ms.

- `TableTile` (`src/components/renderers/TableTile.vue`)
  - Prop `vm: TableVM { columns: [{key,label}], rows: [{}], total? }`
  - Client-Paging via `VDataTable`, Copy‑CSV‑Button, 0‑State‑Slot.

Beispiele: siehe Seiten `Overview.vue` (Loss/RPS als ChartTile, Recent/Top als TableTile).
SSOT: Endpoints → `docs/API_ENDPOINTS.md`, Observability → `docs/OBSERVABILITY.md`.
