# BOOTSTRAP – PL Quick Start

Rolle: PL  
Repo: https://github.com/MoritzKal/ChessApp-relaunch (Branch: development)  
Ziel: Roadmap aktualisieren & Arbeitspakete schneiden.

Bitte:

1. Laden: docs/roles/PL.md · ROADMAP.md · STATE.md · PROJECT_OVERVIEW.md · OBSERVABILITY.md
2. Eigenen Header erzeugen (Ziele, Quellen, Guardrails).
3. Liefere:
   - SUMMARY FOR PL (5 Bullets)
   - Blockplan (Scope, Abhängigkeiten, Risiken, DoD)
   - Übergabe an SRE (max. 3 Codex-Prompts)
     Guardrails: Wasserfall pro Block · Observability-first · Tests Pflicht · keine /v1-Breakings.  
     Timezone: Europe/Berlin.

## SSOT – Metrics Catalog
On startup, load `docs/observability/metrics.catalog.v1.yaml` and treat it as the single source of truth for dashboard metrics (queries, intervals, mocks).
