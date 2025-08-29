# Endpoint Stability Policy

## Grundsätze
- **/v1** bleibt abwärtskompatibel. Attribute **hinzufügen** ist ok; Entfernen/Umbenennen **nicht**.
- Breaking Changes nur via **/v2** + Migrationshinweis.

## Contract-Tests
- Relevante Contracts werden getestet (z. B. Controller‑IT).

## Deprecation
- Markieren in OpenAPI & Doks, Sunset‑Datum angeben, Logging von Zugriffen optional.
+ # Endpoint Stability Policy
+ ## Zweck
+ Schnittstellen stabil halten; Änderungen nachvollziehbar und messbar ausrollen.
+ 
+ ## Geltungsbereich
+ Alle HTTP-Endpunkte unter `/v1/*` (API), inkl. proxied ML/Serve.
+ 
+ ## Stability-Levels
+ - **Stable:** vertraglich fixiert, nur additive Änderungen.
+ - **Experimental:** gekennzeichnet; darf sich ändern.
+ - **Deprecated:** mit Migrationspfad & Abschaltdatum angekündigt.
+ 
+ ## Erlaubt in /v1 (additiv)
+ - Neue optionale Felder/Endpunkte.
+ - Neue enum-Werte **nur** wenn Clients tolerant sind (sonst RFC).
+ - Zusätzliche Telemetrie (chs_* Metriken/Logs) ohne Umbenennung.
+ 
+ ## Verboten in /v1 (breaking)
+ - Felder/Endpunkte entfernen oder umbenennen.
+ - Typ-/Semantikwechsel, Statuscodes ändern, Pflichtfelder hinzufügen.
+ - Metriken umbenennen ohne Parallel-Emission.
+ 
+ ## Breaking Changes → /v2 & RFC
+ - **Pflicht:** RFC mit Ziel/Impact/Plan, Migration, Tests, Observability.
+ - **Migration:** Parallelbetrieb oder Shim, wo praktikabel (≥ 7 Tage empfohlen).
+ - **OpenAPI:** Versionieren, Clients informieren.
+ 
+ ## Deprecation-Prozess
+ - Ankündigung über Communicator-PL.
+ - Fristen: ≥ 24 h `development`, ≥ 48 h `main` (internes Projekt, schnelle Zyklen).
+ - Telemetrie: Nutzung alter Felder/Calls messen und im Dashboard sichtbar machen.
+ 
+ ## Tests & Observability
+ - Contract-/Integrationstests verpflichtend.
+ - Metriken/Logs: Parität oder Doppel-Emission bei Umbenennungen (3 Tage).
+ 
+ ## Governance
+ - Communicator-PL pflegt Contract-Board & RFC-Archiv.
+ - Ausnahmen nur mit expliziter Freigabe (Lead-PL) + dokumentiertem Risiko.