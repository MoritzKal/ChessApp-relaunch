# Releases & Environments

## Environments
- **DEV** (Compose lokal): permissiv, schnelles Feedback.
- **STAGING/PROD**: härtere Policies, Secret‑Stores, Reverse Proxy/TLS.

## Release Flow (Vorschlag)
1) Feature‑Branch → PR → `development`
2) Smoke/CI
3) Tag/Release → Deploy STAGING
4) Promote to PROD (manuell)

## Konfiguration
- `.env` pro Environment, Secrets via Store (nicht im Repo).
- Grafana/Prometheus/Loki: getrennte Storage/Retention nach Environment.
