# Endpoint Stability Policy

## Grundsätze
- **/v1** bleibt abwärtskompatibel. Attribute **hinzufügen** ist ok; Entfernen/Umbenennen **nicht**.
- Breaking Changes nur via **/v2** + Migrationshinweis.

## Contract-Tests
- Relevante Contracts werden getestet (z. B. Controller‑IT).

## Deprecation
- Markieren in OpenAPI & Doks, Sunset‑Datum angeben, Logging von Zugriffen optional.
