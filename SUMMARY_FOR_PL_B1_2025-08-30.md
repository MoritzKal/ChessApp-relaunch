# Block B1 – Model-Registry (read-only): Summary (2025-08-30)

**Branch/PR** `feature/model-registry-readonly` → PR auf `develop`

**Endpunkte**

* `GET /v1/models`
* `GET /v1/models/{id}/versions`

**OpenAPI**

* `swagger-ui.html` zeigt beide Endpunkte inkl. Schemas (`ModelSummary`, `ModelVersionSummary`).

**Tests**

* IT: 200/404 (+ Metrics-Smoke). 5xx über defekte/fehlende `registry.json` abgedeckt.

**Metriken**

* `chs_model_registry_requests_total{endpoint,status}`
* *(optional)* `chs_model_registry_versions_total{model_id}`

**Logs (MDC)**

* `username`, `component=api.registry`, `run_id`, `endpoint`, `model_id`

**Prometheus-Belege (Queries)**

* `sum by (endpoint,status) (chs_model_registry_requests_total)`
* `chs_model_registry_versions_total`

**Loki-Beleg**

* `{component="api.registry"} | json | endpoint="/v1/models"`

**Stabilität**

* /v1 nur additiv, keine Breaking Changes.

**How-to Check (manuell)**

```bash
curl -s :8080/v1/models | jq
curl -s :8080/v1/models/policy_tiny/versions | jq
curl -s :8080/actuator/prometheus | grep chs_model_registry_requests_total | head
```
