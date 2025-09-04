# Frontend State Stores (Pinia)

This document summarizes the domain stores and how pages should consume them. The API and Metrics remain single sources of truth in:
- API contracts: docs/API_ENDPOINTS.md
- Observability notes/links: docs/OBSERVABILITY.md

Do not copy endpoint specs here; link to the SSOT instead.

## Stores

- datasets: caches dataset entities and summaries; provides selectors and polling declarations (`datasets.count`).
- training: caches runs and counts by status; dynamic polling for specific `runId` can be declared at page level.
- metrics: caches scalar and timeseries metrics under stable keys (e.g. `loss:7d`, `train:<runId>:loss:2h`).
- games: caches lists, details, positions; exposes online count and recent games.

## Usage Pattern

Pages never call services directly. Instead:
- Import the relevant store(s) and composables `usePolling`/`useSSE`.
- Trigger one initial load per selector you need and then start polling using the aggregated `pollTargets` from the stores (plus page-specific targets such as a concrete `runId`).
- Use the memoized selectors for rendering. Handle loading/empty/error via store `loading`/`errors` sets.

