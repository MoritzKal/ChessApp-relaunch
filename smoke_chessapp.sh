#!/usr/bin/env bash
# ChessApp smoke test

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
JWT="${JWT:-}"
MONITORING_TOKEN="${MONITORING_TOKEN:-}"
API_CONTAINER="${API_CONTAINER:-chs_api}"

say() { echo -e "\n$*"; }
ok() { echo -e "✅ $*"; }
fail() { echo -e "❌ $*"; exit 1; }

curl_auth() {
  curl -s -H "Authorization: Bearer ${JWT}" "$@"
}

say "Health"
curl -s "${BASE_URL}/actuator/health" | grep -q '"status":"UP"' || fail "API health check failed"
ok "API healthy"

# Dataset lifecycle
say "Dataset"
DATASET_REQ='{"name":"smoke","version":"v1","filter":{"source":"chess.com","user":"smoke"},"split":{"train":0.8,"val":0.1,"test":0.1},"sizeRows":1}'
RESP=$(curl_auth -X POST -H "Content-Type: application/json" -d "${DATASET_REQ}" "${BASE_URL}/v1/datasets") || fail "dataset create failed"
DATASET_ID=$(echo "$RESP" | sed -n 's/.*"id":"\([a-f0-9-]*\)".*/\1/p')
[ -n "${DATASET_ID}" ] || fail "no dataset id"
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
curl -s \
  -H "X-Run-Id: smoke-run" \
  -H "X-Dataset-Id: smoke-dataset" \
  -H "X-Model-Id: smoke-model" \
  -H "X-Username: smoke-user" \
  -H "X-Component: smoke-script" \
  "${BASE_URL}/actuator/health" >/dev/null

if command -v docker >/dev/null 2>&1; then
  LOGS=$(docker logs --tail 200 "${API_CONTAINER}" 2>&1 || true)
  for token in smoke-run smoke-dataset smoke-model smoke-user smoke-script; do
    echo "$LOGS" | grep -q "$token" || fail "log missing token $token"
  done
  ok "MDC tokens present in logs"
else
  echo "docker not available; skipping log check"
fi

echo "SMOKE OK"

