# Security Guide (DEV → PROD) – Stand: 2025-08-29

## Ziele
- Geheimnisse sicher handhaben, Rollen/Policies trennen, Oberflächen absichern – ohne die Developer‑Experience lokal zu verschlechtern.

## Secrets & Config
- **Lokal (.env):** ok, aber nie commiten. In PROD: Secret‑Store (z. B. SOPS/age, Vault).
- **Grafana Admin/MinIO Root:** im DEV ok, in PROD **rotieren** und nicht als ENV im Repo. Managed Secrets verwenden.
- **CORS/Rate Limits:** DEV permissiv; PROD per Reverse Proxy/API‑Gateway härten.
- **AuthN/Z:** später für UI/API einführen (Keycloak/OIDC o. ä.).
- **Least Privilege:** MinIO Buckets mit minimalen Policies (read/write nur wo nötig).
- **PII/Logs:** Keine sensitiven Nutzerdaten in Logs/Metriken.
- **Dependencies:** Regelmäßig aktualisieren; CVEs beobachten.

## Netzwerk & Exponierung
- Compose‑Netz intern lassen; nur benötigte Ports nach außen publishen.
- Reverse Proxy für PROD (TLS, WAF/Rate Limit, Auth).

## Build/Release
- Unveränderliche Images, reproduzierbare Builds.
- SBOM/Dependency‑Scan (CI).

