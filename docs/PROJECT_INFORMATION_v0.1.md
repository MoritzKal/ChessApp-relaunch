# ChessApp Geläutert – Projektinformation & Roadmap (v0.1)
**Stand:** 27.08.2025 (historisch, konserviert) · **Letztes Update:** 2025-08-29

> Dieses Dokument bewahrt die ursprüngliche Planung („v0.1 – 20‑Punkte‑Plan“) als **Quelle der Wahrheit** für Kontext & Intent. 
> Der aktuelle Fortschritt/Abgleich ist je Punkt vermerkt (✅ erledigt · 🚧 in Arbeit · ⏳ geplant · ❗ überholt).

## Kurzbeschreibung
Ziel: Web‑Applikation, um eine Schach‑KI mit Daten von chess.com‑Accounts zu trainieren, zu testen, zu überwachen – mit einem **reichhaltigen Observability‑Dashboard** und **modularen** Backends (Spring Boot + FastAPI).

---

## 20‑Punkte‑Plan (v0.1)
1) **Infra Bootstrap (Compose, Netz, Volumes)** → ✅ abgeschlossen
2) **Observability Grundgerüst** (Prometheus, Grafana, Loki/Promtail, Provisioning) → ✅
3) **Secrets & .env Schema** (lokal) → ✅ Basis vorhanden · ⏳ Härtung/Secrets-Management
4) **API‑Skeleton** (Spring Boot, Maven, Multi‑Module, OpenAPI, Actuator, JSON‑Logs) → ✅
5) **DB‑Schema & Migrationen** (Flyway Baseline, PG16) → ✅
6) **Dataset Slice** (Create/List/Get, Manifest nach MinIO, Metriken/Logs) → ✅
7) **Ingest Slice** (chess.com/offline, Idempotenz, SAN/Positions, Reports, Metriken) → ✅ (offline)
8) **Training Skeleton** (ML‑Service, MLflow, Artefakte, Metriken, API‑Proxy) → ✅
9) **Serving Skeleton** (Serve‑Service, `/predict`, Metriken, API‑Proxy) → ✅
10) **Overview‑Dashboard** (stabil, nicht provisorisch) → ✅
11) **Security (DEV/PROD Profile)** (Auth, Roles, CORS, Rate‑Limits) → ⏳ geplant
12) **CI‑Pipeline** (Build, Test, Lint, Compose‑Health) → ⏳ geplant
13) **Artifact Storage Governance** (Buckets/Policies für DEV/PROD) → 🚧 Grundlagen, ⏳ Härtung
14) **Contract‑Tests & API‑Stabilität** (Backward Compatibility) → ⏳ geplant
15) **Play‑UI (Vue/Vuetify)** (Board, Explore, Train/Serve Hooks) → ⏳ **bewusst nach hinten gestellt**
16) **Evaluation Suite** (Metriken, A/B vs. Baseline, Reports) → ⏳ geplant
17) **Model Registry** (Promotion, Versionierung, Serving‑Switch) → ⏳ geplant
18) **Cost & Perf Observability** (Ressourcen‑KPIs, Sampling) → ⏳ geplant
19) **Operations Runbooks** (Oncall, Cheatsheets, Troubleshooting) → ⏳ geplant
20) **Release/Environments** (DEV → STAGING → PROD Playbooks) → ⏳ geplant

### Hinweise
- Punkte 1–10 wurden umgesetzt und bilden das stabile Rückgrat. 
- Frontend (15) wurde **explizit** zurückgestellt, bis die Endpunkte final stabil sind (Attribut‑Erweiterungen ok).

---

## Abgleich zur aktuellen Roadmap
- Aktuelle Makro‑Roadmap (Docs/ROADMAP.md) bleibt gültig; v0.1 dient als **historische Referenz**.
- Änderungen gegenüber v0.1 sind marginal (Reihenfolge: Training/Serving vor UI).

