# Test-Guidelines

- Jeder neue Endpoint braucht mind. einen Integrationstest.
- Observability testen: Metriken vorhanden, Logs mit MDC sichtbar (Loki-Query manuell ok).
- Idempotenz & Fehlerpfade explizit testen.
- Bei externen Diensten (MinIO/MLflow): env-gated Tests bzw. Mocks verwenden.
