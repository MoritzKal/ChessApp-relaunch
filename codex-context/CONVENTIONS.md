# Konventionen

## Observability
- Prometheus-Metriken: Präfix `chs_*`, sinnvolle Namen & Labels (`application`, `component`, `username`).
- Logs: JSON (einzeilig), MDC-Felder setzen (Filter liest Standard-Header X-Run-Id, X-Username, X-Component).

## APIs
- Stabile Pfade `/v1/...`, OpenAPI via springdoc.
- Fehler → klare HTTP-Codes, Logs mit Kontext (run_id).

## Datenbank & Migrationen
- Nur via Flyway (keine `ddl-auto=update` in PROD).
- ENUMS stabil halten; JSONB für flexible Felder.

## Tests
- Controller-IT (MockWebServer wo sinnvoll), Repo-IT (Testcontainers), Service-Unit.
- Für ML/Serve: pytest, Prometheus-/Health-Checks.
