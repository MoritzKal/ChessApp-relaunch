# CONTRIBUTING

## Branching & Commits
- Branch: `feature/<slug>`, `fix/<slug>`
- Commit‑Stil: `type(scope): message` (z. B. `api(dataset): add create/list/get`)

## PR‑Prozess
- Aufgabenpaket lesen, Scope bestätigen.
- Tests Pflicht (grün).
- SUMMARY FOR PL beilegen (SRE‑Chats).

## Coding Guidelines
- **Java (API):** Spring Boot 3.x, JPA (UUID), Flyway für Schema, JSON‑Logs (Logstash Encoder), Controller‑IT/Repo‑IT.
- **Python (ML/Serve):** FastAPI, `prometheus_client`, `structlog`/json‑logger, pytest.
- **Infra:** Compose, Provisioning, „Infra‑as‑Docs“ (Doku pflegen).
