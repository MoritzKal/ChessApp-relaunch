#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
JWT="${JWT:?JWT required}"

post() {
  local endpoint="$1"
  local body="$2"
  local key
  key=$(uuidgen)
  curl -sS -X POST "$API_BASE$endpoint" \
    -H "Authorization: $JWT" \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: $key" \
    -d "$body"
}

get() {
  local endpoint="$1"
  local key
  key=$(uuidgen)
  curl -sS -X GET "$API_BASE$endpoint" \
    -H "Authorization: $JWT" \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: $key"
}

poll_status() {
  local endpoint="$1"
  local outfile="$2"
  local start timeout status resp
  start=$(date +%s)
  timeout=$((5*60))
  while true; do
    resp=$(get "$endpoint")
    status=$(echo "$resp" | jq -r '.status')
    if [[ "$status" == "SUCCEEDED" || "$status" == "FAILED" ]]; then
      echo "$resp" | jq -S . > "$outfile"
      [[ "$status" == "FAILED" ]] && return 1
      return 0
    fi
    if (( $(date +%s) - start > timeout )); then
      echo "$resp" | jq -S . > "$outfile"
      echo "Timeout while polling $endpoint" >&2
      return 1
    fi
    sleep 5
  done
}

mkdir -p outputs

run_resp=$(post "/v1/selfplay/runs" '{"modelId":"staging","baselineId":"prod","games":10,"concurrency":2,"seed":42}')
run_id=$(echo "$run_resp" | jq -r '.runId')
poll_status "/v1/selfplay/runs/$run_id" "outputs/selfplay_${run_id}.json" || exit 1

eval_resp=$(post "/v1/evaluations" '{"modelId":"staging","datasetId":"val_2025_08","metrics":["val_loss","val_acc_top1","ece"]}')
eval_id=$(echo "$eval_resp" | jq -r '.evalId')
poll_status "/v1/evaluations/$eval_id" "outputs/eval_${eval_id}.json" || exit 1

post "/v1/models/promote" '{"modelId":"staging"}' >/dev/null
get "/v1/models" | jq -S . > outputs/models.json
if [[ $(jq -r '.[] | select(.id=="staging") | .isProd' outputs/models.json) != "true" ]]; then
  echo "Promotion verification failed" >&2
  exit 1
fi

curl -sS "$API_BASE/v3/api-docs" | jq -S . > outputs/openapi.new.json
if [[ -f outputs/openapi.prev.json ]]; then
  diff -u outputs/openapi.prev.json outputs/openapi.new.json || true
fi
cp outputs/openapi.new.json outputs/openapi.prev.json

curl -sS "$API_BASE/actuator/prometheus" > outputs/prom.txt
grep "chs_api_requests_total" outputs/prom.txt
grep -E "chs_selfplay_|chs_eval_" outputs/prom.txt || true
