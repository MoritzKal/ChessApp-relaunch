# Communicator‑PL Starter Kit

> Quick‑Templates & Tracker, damit Schnittstellen stabil bleiben und Hand‑offs schnell laufen. (Stand: 2025‑08‑29)

---

## 1) RFC‑Vorlage (Kurz)
**Titel:** <prägnant>

**Status:** Draft | Review | Approved | Rejected

**Owner:** <Name/TP>  
**Reviewer:** <TP‑PLs>  
**Bezugsdoku:** `docs/ENDPOINT_STABILITY_POLICY.md`

**Ziel / Motivation**  
<Problem, Zielbild, “Definition of Success”>

**Änderungstyp**  
Additiv (rückwärtskompatibel) | *Breaking* (→ /v2 nötig)

**Scope**  
API Endpunkte: </v1/...>  
Datenbank/Schema: <Ja/Nein>  
Observability: Metriken/Logs/Traces Änderungen?  
Clients/Frontend betroffen: <Ja/Nein>

**Impact**  
Risiken, betroffene Nutzer/Flows, Downtime‑Erwartung.

**Plan**  
1) Umsetzungsschritte  
2) Migrationspfad (Deprecation, Grace‑Period, Fallbacks)  
3) Rollout (Envs, Feature‑Flags)  
4) Telemetrie/Guardrails (chs_* Metriken, JSON‑Logs, Dashboards)

**Tests & Abnahme**  
Unit/IT vorhanden? Scrape‑Checks? Dashboard‑Panels aktualisiert?  
Abnahmekriterien (Given‑When‑Then): <...>

**Entscheidungslog & Anhänge**  
Alternativen, verworfene Optionen, Links auf PRs, Grafana‑Panels, Loki‑Queries.

---

## 2) API‑Contract‑Board (Snapshot)
| Methode | Endpoint | Owner | Stability | Breaking‑Guard | Observability | Tests | Notizen |
|---|---|---|---|---|---|---|---|
| GET | /v1/health | API | **Stable** | Ja (/v2 bei Breaking) | Actuator | IT vorhanden | – |
| GET | /swagger‑ui.html | API | **Stable** | – | – | – | – |
| GET | /actuator/prometheus | API | **Stable** | – | Prometheus | Scrape | – |
| POST | /v1/datasets | API | **Stable** | Ja | chs_dataset_* | IT | – |
| GET | /v1/datasets | API | **Stable** | Ja | – | IT | – |
| GET | /v1/datasets/{id} | API | **Stable** | Ja | – | IT | – |
| POST | /v1/ingest | API | **Stable** | Ja | chs_ingest_* | IT | offline Slice |
| GET | /v1/ingest/{runId} | API | **Stable** | Ja | – | IT | – |
| POST | /v1/trainings | API→ML | **Stable** | Ja | chs_training_* | IT/pytests | delegiert an ML |
| GET | /v1/trainings/{runId} | API→ML | **Stable** | Ja | chs_training_* | IT/pytests | – |
| POST | /v1/predict | API→Serve | **Stable** | Ja | chs_predict_* | IT | Proxy |
| POST | /v1/models/load | API→Serve | **Stable** | Ja | serve_* | IT | Artefakt‑Load |

> Pflege: Bei Änderungen **RFC** verlinken und Spalte *Stability* fortschreiben.

---

## 3) Branches & Hand‑offs Tracker
| TP | Aktueller Branch | Nächster PR → | Nächstes Hand‑off‑Ereignis | Blocker | Ping an |
|---|---|---|---|---|---|
| Backend/API | <feature/...> | development | „API ready for Play UI contract test“ | – | @API‑PL |
| ML/Training+Serving | <feature/...> | development | „serve /predict latency check & schema freeze“ | – | @ML‑PL |
| Frontend | <feature/...> | development | „Play UI consumes /v1/predict (mock→real)“ | API Freeze | @FE‑PL |
| Data Eng (Ingest/Dataset) | <feature/...> | development | „Dataset builder v1 exposed to API“ | – | @DE‑PL |
| Observability/DevOps | <feature/...> | main | „Overview Dashboard panels updated (v9)“ | – | @SRE‑PL |

---

## 4) Ereignis‑Pings (Snippets)
**Bereit zum Test**  
> *Ready for Test:* <TP/Feature> auf `<branch>` → Ziel `<PR target>`. Bitte Contract‑Checks: Endpunkte, Metriken, Logs, Dashboard‑Panels.

**Braucht Entscheidung**  
> *Decision needed:* Option A/B zu <Thema>. Empfehlung: <X>. Impact auf /v1: <keiner|additiv|breaking>.

**Abnahme nötig**  
> *Request for Acceptance:* <Feature> erfüllt DoD. Siehe PR <#>, Dashboard „ChessApp – Overview“, MLflow Run <id>.

**Blocker/Eskalation**  
> *Blocker:* <Beschreibung>. Betroffen: <TPs>. Vorschlag: <Workaround/Entscheidung>. ETL bis <Datum/Zeit>.

---

## 5) Daily SUMMARY (5 Bullets)
1) Änderungen seit gestern: <kurz>
2) Risiken/Blocker: <kurz>
3) Nächste Hand‑offs: <Datum/Uhrzeit, wer→wer>
4) Observability‑Check: Scrapes OK, Panels OK, Logs OK
5) Entscheidungen ausstehend: <X/Y>

---

## 6) Hand‑off Checklisten
**Contract‑Check**  
- [ ] Endpunkte & Schemas unverändert (/v1); nur additive Felder
- [ ] OpenAPI aktualisiert
- [ ] IT/Contract‑Tests grün

**Observability‑Check**  
- [ ] chs_* Metriken vorhanden & gescraped
- [ ] JSON‑Logs mit MDC (run_id, dataset_id, model_id, username, component)
- [ ] Grafana‑Panels angepasst / grün

**Docs**  
- [ ] `docs/API_ENDPOINTS.md` fortgeschrieben  
- [ ] RFC verlinkt  
- [ ] `ENDPOINT_STABILITY_POLICY.md` referenziert

