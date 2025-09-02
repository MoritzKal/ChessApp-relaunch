#!/usr/bin/env bash
# ChessApp smoke test

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
JWT="${JWT:-}"
MONITORING_TOKEN="${MONITORING_TOKEN:-}"
# Prefer Compose container; fallback auf chs_api
API_CONTAINER="${API_CONTAINER:-}"
if [ -z "${API_CONTAINER}" ]; then
  API_CONTAINER=$(docker compose -f infra/docker-compose.yml ps -q api 2>/dev/null || true)
fi
API_CONTAINER="${API_CONTAINER:-chs_api}"

say() { echo -e "\n$*"; }
ok() { echo -e "✅ $*"; }
fail() { echo -e "❌ $*"; exit 1; }

curl_auth() {
  curl -s -H "Authorization: Bearer ${JWT}" "$@"
}

# Ensure we have a JWT or try to mint a dev token if available
ensure_jwt() {
  if [ -n "${JWT}" ]; then
    return 0
  fi
  # Try dev token endpoint (may be disabled). Ignore on failure.
  TOKEN_JSON=$(curl -s "${BASE_URL}/v1/auth/token?user=smoke-user&roles=USER&scope=api.write&ttl=600" || true)
  JWT=$(echo "$TOKEN_JSON" | sed -n 's/.*"token"\s*:\s*"\([^"]*\)".*/\1/p')
  if [ -z "${JWT}" ]; then
    fail "JWT not set. Provide JWT env var or enable dev token endpoint at /v1/auth/token"
  fi
}

say "Health"
curl -s "${BASE_URL}/actuator/health" | grep -q '"status":"UP"' || fail "API health check failed"
ok "API healthy"

# Dataset lifecycle
say "Dataset"
ensure_jwt
# Use unique defaults to avoid unique-constraint conflicts; allow override via env
DS_NAME="${DS_NAME:-smoke}"
if command -v date >/dev/null 2>&1; then
  DS_VERSION_DEFAULT="v$(date +%s)"
else
  DS_VERSION_DEFAULT="v${RANDOM:-0}"
fi
DS_VERSION="${DS_VERSION:-${DS_VERSION_DEFAULT}}"
DATASET_REQ="{\"name\":\"${DS_NAME}\",\"version\":\"${DS_VERSION}\",\"filter\":{\"source\":\"chess.com\",\"user\":\"smoke\"},\"split\":{\"train\":0.8,\"val\":0.1,\"test\":0.1},\"sizeRows\":1}"
TMP_BODY=$(mktemp)
CODE=$(curl -s -o "$TMP_BODY" -w "%{http_code}" -H "Authorization: Bearer ${JWT}" -H "Content-Type: application/json" -d "${DATASET_REQ}" -X POST "${BASE_URL}/v1/datasets" || true)
BODY=$(tr -d '\r' < "$TMP_BODY")
rm -f "$TMP_BODY"
if [ "$CODE" != "201" ]; then
  echo "dataset create failed (code=$CODE)"
  echo "$BODY"
  fail "dataset create failed"
fi
# Robustly parse UUID
DATASET_ID=$(echo "$BODY" | grep -oE '"id"\s*:\s*"[a-f0-9-]{36}' | grep -oE '[a-f0-9-]{36}' | head -n1)
[ -n "${DATASET_ID}" ] || { echo "$BODY"; fail "no dataset id"; }
ok "created dataset ${DATASET_ID}"

curl_auth "${BASE_URL}/v1/datasets/${DATASET_ID}" | grep -q "${DATASET_ID}" || fail "dataset get failed"
curl_auth "${BASE_URL}/v1/datasets" | grep -q "${DATASET_ID}" || fail "dataset list missing id"
ok "dataset retrieval OK"

# Alias check
say "Alias /v1/data/import"
HDR=$(mktemp)
CODE=$(curl -s -o /dev/null -D "$HDR" -w "%{http_code}" -X POST -H "Authorization: Bearer ${JWT}" "${BASE_URL}/v1/data/import" || true)
if [ "$CODE" = "308" ] && grep -qi '^location: /v1/ingest' "$HDR"; then
  ok "alias redirected to /v1/ingest"
else
  fail "alias failed (code=$CODE)"
fi

# Ingest run
say "Ingest run"
RESP=$(curl_auth -X POST "${BASE_URL}/v1/ingest") || fail "start ingest failed"
RUN_ID=$(echo "$RESP" | sed -n 's/.*"runId":"\([a-f0-9-]*\)".*/\1/p')
[ -n "$RUN_ID" ] || fail "no runId"
ok "runId=${RUN_ID}"

STATUS=""
REPORT=""
for _ in $(seq 1 30); do
  R=$(curl_auth "${BASE_URL}/v1/ingest/${RUN_ID}" || true)
  STATUS=$(echo "$R" | sed -n 's/.*"status":"\([^\"]*\)".*/\1/p')
  REPORT=$(echo "$R" | sed -n 's/.*"reportUri":"\([^\"]*\)".*/\1/p')
  if [ "$STATUS" = "SUCCEEDED" ] || [ "$STATUS" = "FAILED" ]; then
    break
  fi
  sleep 2
done
if [ "$STATUS" != "SUCCEEDED" ] && [ "$STATUS" != "FAILED" ]; then
  fail "ingest run did not finish"
fi
ok "ingest ${STATUS} report=${REPORT}"

# Prometheus protected
say "Prometheus"
UNAUTH=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/actuator/prometheus" || true)
if [ "$UNAUTH" = "401" ] || [ "$UNAUTH" = "403" ]; then
  ok "unauthorized without token"
else
  fail "prometheus accessible without token (code=$UNAUTH)"
fi
curl -s -H "Authorization: Bearer ${MONITORING_TOKEN}" "${BASE_URL}/actuator/prometheus" | grep -q '^# HELP' || fail "prometheus with token failed"
ok "prometheus scrape OK"

# Log probe
say "Logs"
# Erzeuge sicher eine Logzeile mit MDC (autorisiert, damit Controller/Service loggt)
curl -s -X POST \
  -H "Authorization: Bearer ${JWT}" \
  -H "X-Run-Id: smoke-run" \
  -H "X-Dataset-Id: smoke-dataset" \
  -H "X-Model-Id: smoke-model" \
  "${BASE_URL}/v1/ingest" >/dev/null || true
sleep 1

if command -v docker >/dev/null 2>&1; then
  # Fallback: try to resolve container if initial handle fails
  if ! docker logs --tail 1 "${API_CONTAINER}" >/dev/null 2>&1; then
    ALT=$(docker ps --filter "name=chs_api" -q | head -n1)
    if [ -n "$ALT" ]; then API_CONTAINER="$ALT"; fi
  fi

  # Try a short wait-and-retry window and broader log window
  found="false"
  for _ in 1 2 3 4 5; do
    LOGS=$(docker logs --since 2m --tail 2000 "${API_CONTAINER}" 2>&1 || true)
    # Match entweder auf die reinen Werte ODER JSON-Key-Wert-Paare (nur MDC-Headerwerte relevant)
    if echo "$LOGS" | grep -qE 'smoke-(run|dataset|model)'; then
      found="true"; break
    fi
    if echo "$LOGS" | grep -qE '"(run_id|dataset_id|model_id)":"smoke-(run|dataset|model)"'; then
      found="true"; break
    fi
    sleep 1
  done
  if [ "$found" != "true" ]; then
    echo "Last 120 lines for diagnostics:" >&2
    echo "$LOGS" | tail -n 120 >&2 || true
    fail "log missing MDC tokens"
  fi
  ok "MDC tokens present in logs"
else
  echo "docker not available; skipping log check"
fi

echo "SMOKE OK"

