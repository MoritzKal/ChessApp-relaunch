#!/usr/bin/env sh
set -eu

API_BASE="${API_BASE:-http://localhost:8080}"
OUT_DIR="${OUT_DIR:-outputs}"
mkdir -p "$OUT_DIR"

# JWT bauen, falls nicht gesetzt
if [ -z "${JWT:-}" ]; then
  JWT="$(./scripts/jwt_make.sh tester "selfplay eval monitoring" 3600)"
fi

new_ik() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 16
  else
    # Fallback: Zeit + RANDOM (wenn vorhanden)
    printf '%s%06d' "$(date +%s)" "${RANDOM:-0}"
  fi
}

curl_json() {
  method="$1"; url="$2"; shift 2
  curl -sS -X "$method" \
    -H "Authorization: $JWT" \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: $(new_ik)" \
    "$url" "$@"
}

poll() {
  url="$1"; field="$2"; tries="${3:-60}"
  i=1
  while [ "$i" -le "$tries" ]; do
    body="$(curl -sS -H "Authorization: $JWT" "$url")"
    # Status aus JSON extrahieren (SUCCEEDED/FAILED)
    status=$(printf '%s' "$body" | sed -n "s/.*\"$field\":\"\([A-Z][A-Z]*\)\".*/\1/p" | head -n1)
    echo "-> $url: ${status:-?} ($i/$tries)"
    printf '%s' "$body" > "$OUT_DIR/last.json"
    if [ "${status:-}" = "SUCCEEDED" ]; then return 0; fi
    if [ "${status:-}" = "FAILED" ]; then echo "FAILED"; return 1; fi
    i=$((i+1))
    sleep 2
  done
  echo "TIMEOUT"; return 1
}

echo "POST /v1/selfplay/runs"
sp_resp="$(curl_json POST "$API_BASE/v1/selfplay/runs" -d '{}')"
printf '%s' "$sp_resp" > "$OUT_DIR/selfplay_start.json"
sp_id=$(printf '%s' "$sp_resp" | sed -n 's/.*"runId":"\([^"]*\)".*/\1/p')
[ -n "${sp_id:-}" ] || { echo "No runId"; cat "$OUT_DIR/selfplay_start.json"; exit 1; }

echo "POLL /v1/selfplay/runs/$sp_id"
poll "$API_BASE/v1/selfplay/runs/$sp_id" "status"
cp "$OUT_DIR/last.json" "$OUT_DIR/selfplay_status.json"

echo "POST /v1/evaluations"
ev_resp="$(curl_json POST "$API_BASE/v1/evaluations" -d '{"modelId":"stub","baselineModelId":"stub"}')"
printf '%s' "$ev_resp" > "$OUT_DIR/eval_start.json"
ev_id=$(printf '%s' "$ev_resp" | sed -n 's/.*"evalId":"\([^"]*\)".*/\1/p')
[ -n "${ev_id:-}" ] || { echo "No evalId"; cat "$OUT_DIR/eval_start.json"; exit 1; }

echo "POLL /v1/evaluations/$ev_id"
poll "$API_BASE/v1/evaluations/$ev_id" "status"
cp "$OUT_DIR/last.json" "$OUT_DIR/eval_status.json"

curl -sS -H "Authorization: $JWT" "$API_BASE/v1/models" > "$OUT_DIR/models.json" || true
curl -sS "$API_BASE/actuator/prometheus" > "$OUT_DIR/prom.txt" || true

echo "OK — artefacts in $OUT_DIR/"
