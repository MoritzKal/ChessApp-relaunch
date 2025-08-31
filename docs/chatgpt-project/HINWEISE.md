# Hinweise – ChessApp Geläutert (aktualisiert 2025-08-29)

## Rollen & Verantwortung

- **PL (Projektleiter):** Roadmap, Blöcke, Abnahmen, Delegation. Keine Technik‑Details im PL‑Chat.
- **SRE (Implementer/DevOps/QA):** setzt Aufgabenpakete um, testet, liefert _SUMMARY FOR PL_.
- **BA (Business Analyst):** Projektwissen aufnehmen (Code & Docs), Fragen von Stakeholdern beantworten, Nutzerführung erklären.

## Arbeitsprinzipien

- **Wasserfall pro Block**: Ende‑zu‑Ende (API + Telemetrie + Dashboard + Tests) → dann nächster Block.
- **Observability‑first**: `chs_*`‑Metriken, strukturierte JSON‑Logs (MDC: run_id, dataset_id, model_id, username, component), Grafana‑Panels.
- **Tests Pflicht**: Jede neue Funktionalität braucht Tests (API‑IT, pytests, Scrape‑Checks).

## Start eines neuen Chats (Template)

Siehe `docs/chatgpt-project/CHAT_START_TEMPLATE.md`. Kurzform zum Copy‑Paste:

```
Chatname: <PR#/Topic>, Rolle: <PL|SRE|BA>
Branch: <branch>  Scope: <kurz>
Vorab lesen: docs/PROJECT_OVERVIEW.md, docs/ARCHITECTURE.md, codex-context/*
Hinweise: Loki Query {service=~".+"}; keine Scope‑Änderung ohne PL.
```

**Rollen‑Prompts (Header) gibt’s fertig zum Kopieren**: `docs/roles/PL.md`, `docs/roles/SRE.md`, `docs/roles/BA.md` und in `codex-context/prompt_headers/`.

## Artefakte

- **STATUS/REPORT**: Bitte immer _SUMMARY FOR PL_ verwenden (siehe `STATUS_KACHEL.txt`).
- **Patch‑Vorschläge**: Dateipfad + Diff + Begründung + erwarteter Effekt (keine losen Schnipsel).
