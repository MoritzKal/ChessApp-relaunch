#!/usr/bin/env bash

API=${API:-http://localhost:8080}
PROM=${PROM:-http://localhost:9090}

note() { echo -e "\033[1;34m==> $*\033[0m"; }
pass() { echo -e "\033[1;32m✔ $*\033[0m"; }
fail() { echo -e "\033[1;31m✘ $*\033[0m"; exit 1; }

note "Pre-flight"
curl -sf $API/v1/health >/dev/null || fail "API health failed"
curl -sf $API/actuator/prometheus >/dev/null || fail "Actuator not reachable"
pass "Health OK"

note "GET /v1/models (200 + Felder)"
models_json=$(curl -sSf $API/v1/models)
echo "$models_json" | jq -e 'type=="array" and length>=1 and .[0]|has("modelId") and has("displayName") and has("tags")' >/dev/null || fail "Models schema mismatch"
model_id=$(echo "$models_json" | jq -r '.[0].modelId')
pass "Models OK (modelId=$model_id)"

note "GET /v1/models/{id}/versions (200 + Felder)"
vers_json=$(curl -sSf $API/v1/models/$model_id/versions)
echo "$vers_json" | jq -e 'type=="array" and .[0]|has("modelVersion") and has("createdAt") and has("metrics")' >/dev/null || fail "Versions schema mismatch"
pass "Versions OK"

note "404 Case"
code=$(curl -s -o /dev/null -w "%{http_code}" $API/v1/models/__does_not_exist__/versions)
test "$code" = "404" || fail "Expected 404, got $code"
pass "404 OK"

note "Metrics Smoke"
curl -sSf $API/actuator/prometheus | grep -q 'chs_model_registry_requests_total' || fail "Counter not exposed via actuator"
curl -s --get "$PROM/api/v1/query" --data-urlencode 'query=sum by (endpoint,status) (chs_model_registry_requests_total)' | jq -e '.status=="success"' >/dev/null || fail "Prometheus API query failed"
pass "Metrics OK"

note "Logs (MDC)"
docker compose logs --since 5m api | grep -q '"component":"api.registry"' || fail "No MDC logs with component=api.registry found"
pass "Logs OK"

note "OpenAPI"
curl -sSf $API/v3/api-docs | jq -e '.paths|has("/v1/models") and has("/v1/models/{id}/versions")' >/dev/null || fail "OpenAPI paths missing"
pass "OpenAPI OK"

echo
pass "All B1 checks PASSED at $(date -Iseconds)"
