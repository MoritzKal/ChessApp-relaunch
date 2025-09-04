# Tests & QA

## v0.1
- **Unit (Vitest)**: Stores (Reducer/Mapper), Utils (FEN/Board-Adapter), Chart-Mappers.
- **E2E (Cypress)**: Flows „Import → Dataset → Training → Play“ mit Mock-Backends (msw).
- **Contract**: Schema-Checks gegen API-DTOs (zod).

## Update
- **Auth**: Unit-Tests für `useAuthStore`, Router-Guards, Axios-Interceptor.
- **Config Workbench**: Form-Validierungen (zod), API-Mocks (msw), E2E-Flows: Preset speichern → Workflow starten → Status erscheint.
- **Docker Smoke**: `make fe-up` startet Frontend-Container; Healthcheck `/index.html` 200.
