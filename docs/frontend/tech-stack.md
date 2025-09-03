# Tech Stack & Versionierung

**Runtime/Frameworks**

* **Vue 3** (Composition API, `<script setup>`). **Pinned:** ^3.5.x (ESM).
* **Vite** (Dev/Build). **Pinned:** ^7.1.x.
* **Vuetify 3** (UI-Kit). **Pinned:** ^3.9.x.
* **Pinia** (State). **Pinned:** ^3.0.x.
* **Vue Router** (Routing). **Pinned:** ^4.4.x.
* **Axios** (HTTP). **Pinned:** ^1.11.x.
* **echarts / vue-echarts** *oder* **chart.js / vue-chart-3** (Charts) – Entscheidung per RFC; Default: **ECharts** (gute Dark-Theme-Lesbarkeit).
* **zod** (Schema-Validation) + **zodios** (optionales Typed-API-Layer) – nice-to-have.

**Tooling**

* **Vitest** (Unit), **Cypress** (E2E), **ESLint** + **Prettier** (CI Gate).
* **Mock Service Worker (msw)** für API-Stubs im Storybook/Tests (optional in v0.2).

**Browser Support**: Evergreen Desktop (Chromium, Firefox, Safari aktuell), iPadOS Safari. Mobile Phone: später.

**Design Assets**: Variable Fonts **Inter** (UI) & **JetBrains Mono** (code/notation). Icons: **Material Design Icons** + **Chess Glyphs** (SVG, custom).

**Theme/Tokens**: siehe `theme-tokens.md`.

**Netzwerk/Contracts**

* Alle Calls **/v1 stabil** (nur additive Änderungen). Import via **`POST /v1/ingest`**. Prometheus & Logs via Proxy-Links im UI (Observability-Slots auf jeder Seite).
