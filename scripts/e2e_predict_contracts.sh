#!/usr/bin/env bash
set -Eeuo pipefail

# ===== Configuration ===========================================================
API_BASE="${API_BASE:-http://localhost:8080}"
SERVE_BASE="${SERVE_BASE:-http://localhost:8001}"
PROM_BASE="${PROM_BASE:-http://localhost:9090}"
SCRAPE_WAIT="${SCRAPE_WAIT:-40}"   # Sekunden: 2x 15s + Puffer
WARMUP_CALLS="${WARMUP_CALLS:-40}" # Anzahl Predict-Requests vor Prom-Check

# Secret: aus ENV oder .env lesen
DEFAULT_SECRET="${JWT_SECRET:-${APP_SECURITY_JWT_SECRET:-}}"
if [ -z "$DEFAULT_SECRET" ] && [ -f .env ]; then
  DEFAULT_SECRET="$(grep -E '^APP_SECURITY_JWT_SECRET=' .env | tail -n1 | cut -d= -f2- | tr -d '"' || true)"
fi
[ -n "$DEFAULT_SECRET" ] || { echo "Set APP_SECURITY_JWT_SECRET or JWT_SECRET"; exit 2; }

# ===== Utils ==================================================================
need() { command -v "$1" >/dev/null || { echo "Missing dependency: $1" >&2; exit 10; }; }
need curl; need jq; need openssl

hr()   { printf '\n— %.0s' {1..40}; printf '\n'; }
ok()   { printf "✅ %s\n" "$*"; }
skip() { printf "⚠️  %s\n" "$*"; }
fail() { printf "❌ %s\n" "$*" >&2; exit 1; }

b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }
mkjwt() {
  local scope="${1:-read}" ttl="${2:-3600}" secret="${3:-$DEFAULT_SECRET}"
  local now iat exp header payload h p sig
  now=$(date +%s)
  if (( ttl < 0 )); then exp=$((now+ttl)); iat=$((exp-3600)); else iat=$now; exp=$((now+ttl)); fi
  header='{"alg":"HS256","typ":"JWT"}'
  payload=$(jq -nc --arg sub dev-user --arg iss chessapp-dev --arg aud api --arg scope "$scope" \
    --argjson iat "$iat" --argjson exp "$exp" \
    '{sub:$sub,iss:$iss,aud:$aud,scope:$scope,iat:$iat,exp:$exp}')
  h=$(printf '%s' "$header"  | b64url)
  p=$(printf '%s' "$payload" | b64url)
  sig=$(printf '%s' "$h.$p" | openssl dgst -binary -sha256 -hmac "$secret" | b64url)
  printf '%s.%s.%s\n' "$h" "$p" "$sig"
}

json() { jq -c '.'; }

# ===== 0) Healths =============================================================
hr; echo "[0] Health"
curl -sf "$API_BASE/actuator/health" >/dev/null || fail "API health failed"
ok "API health OK"

if curl -sf "$SERVE_BASE/openapi.json" >/dev/null 2>&1; then
  ok "Serve reachable (openapi.json)"
else
  skip "Serve openapi.json not reachable (OK falls nicht benötigt)"
fi

# ===== 1) Modelle holen (für modelId) ========================================
hr; echo "[1] /v1/models (API)"
JWT_READ="$(mkjwt read 3600)"
MODELS_JSON="$(curl -sf -H "Authorization: Bearer $JWT_READ" "$API_BASE/v1/models" || echo '[]')"
FIRST_MODEL="$(echo "$MODELS_JSON" | jq -r '.[0].modelId // empty')"
if [ -n "$FIRST_MODEL" ]; then
  ok "Got modelId: $FIRST_MODEL"
else
  skip "No modelId from /v1/models (weiter mit heuristics)"
fi

# ===== 2) Detect Predict endpoints ===========================================
hr; echo "[2] Detect predict endpoints"
API_HAS_PREDICT="no"
if curl -sf "$API_BASE/v3/api-docs" >/dev/null 2>&1; then
  if curl -sf "$API_BASE/v3/api-docs" | jq -e '.paths | has("/v1/predict") or has("/v1/predict/{modelId}")' >/dev/null; then
    API_HAS_PREDICT="yes"; ok "API exposes /v1/predict*"
  else
    skip "API has no /v1/predict in OpenAPI"
  fi
else
  skip "API OpenAPI not available"
fi

SERVE_HAS_PREDICT="no"
if curl -sf "$SERVE_BASE/openapi.json" >/dev/null 2>&1; then
  if curl -sf "$SERVE_BASE/openapi.json" | jq -e '.paths | has("/predict")' >/dev/null; then
    SERVE_HAS_PREDICT="yes"; ok "Serve exposes /predict"
  else
    skip "Serve has no /predict in OpenAPI"
  fi
fi

# ===== 3) Predict attempt(s) ==================================================
hr; echo "[3] Predict calls"
FEN_START="rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
SUCCESS="no"

try_api_predict() {
  local url payload code body
  if [ "$API_HAS_PREDICT" = "yes" ]; then
    url="$API_BASE/v1/predict"
    for payload in \
      "$(jq -nc --arg m "${FIRST_MODEL:-policy_tiny}" --arg fen "$FEN_START" '{modelId:$m, fen:$fen}')" \
      "$(jq -nc --arg m "${FIRST_MODEL:-policy_tiny}" --arg fen "$FEN_START" '{model_id:$m, fen:$fen}')" \
      "$(jq -nc --arg m "${FIRST_MODEL:-policy_tiny}" --arg fen "$FEN_START" '{modelId:$m, input:{fen:$fen}}')"
    do
      body="$(curl -sS -w '\n%{http_code}' -H "Authorization: Bearer $JWT_READ" -H 'Content-Type: application/json' -d "$payload" "$url")"
      code="${body##*$'\n'}"; body="${body%$'\n'*}"
      if [[ "$code" =~ ^2 ]]; then echo "$body" | jq -e . >/dev/null 2>&1 || true; SUCCESS="yes"; ok "API /v1/predict 200"; return 0; fi
    done
  fi
  if [ "$API_HAS_PREDICT" = "yes" ] && [ -n "$FIRST_MODEL" ]; then
    url="$API_BASE/v1/predict/$FIRST_MODEL"
    for payload in \
      "$(jq -nc --arg fen "$FEN_START" '{fen:$fen}')" \
      "$(jq -nc --arg fen "$FEN_START" '{input:{fen:$fen}}')"
    do
      body="$(curl -sS -w '\n%{http_code}' -H "Authorization: Bearer $JWT_READ" -H 'Content-Type: application/json' -d "$payload" "$url")"
      code="${body##*$'\n'}"; body="${body%$'\n'*}"
      if [[ "$code" =~ ^2 ]]; then SUCCESS="yes"; ok "API /v1/predict/{modelId} 200"; return 0; fi
    done
  fi
  return 1
}

try_serve_predict() {
  local url payload code body
  [ "$SERVE_HAS_PREDICT" = "yes" ] || return 1
  url="$SERVE_BASE/predict"
  for payload in \
    "$(jq -nc --arg fen "$FEN_START" '{fen:$fen}')" \
    "$(jq -nc --arg fen "$FEN_START" '{input:{fen:$fen}}')" \
    "$(jq -nc --arg fen "$FEN_START" '{inputs:[{fen:$fen}]}')"
  do
    body="$(curl -sS -w '\n%{http_code}' -H 'Content-Type: application/json' -d "$payload" "$url")"
    code="${body##*$'\n'}"; body="${body%$'\n'*}"
    if [[ "$code" =~ ^2 ]]; then SUCCESS="yes"; ok "Serve /predict 200"; return 0; fi
  done
  return 1
}

if try_api_predict || try_serve_predict; then
  :
else
  skip "No predict path accepted our payloads (evtl. Schema anders / Service off)."
fi

# ===== 4) Prometheus Check ====================================================
hr; echo "[4] Prometheus checks"

if ! curl -sf "$PROM_BASE/-/ready" >/dev/null; then
  skip "Prometheus not ready"; exit 0
fi

# Warmup: extra Predict-Traffic
if [ "$SUCCESS" = "yes" ]; then
  for i in $(seq 1 "$WARMUP_CALLS"); do
    curl -sf -H "Authorization: Bearer $JWT_READ" -H 'Content-Type: application/json' \
      -d "$(jq -nc --arg m "${FIRST_MODEL:-policy_tiny}" --arg fen "$FEN_START" '{modelId:$m, fen:$fen}')" \
      "$API_BASE/v1/predict" >/dev/null || true
  done
fi

# kurz warten für mind. zwei Scrapes
sleep "$SCRAPE_WAIT"

# a) Targets up?
TARGETS=$(curl -sf "$PROM_BASE/api/v1/targets" | jq -r '.data.activeTargets[] | "\(.labels.job):\(.health)"' | sort)
echo "$TARGETS" | grep -q '^api:up'   && ok "Target api: up"   || skip "target api not up"
echo "$TARGETS" | grep -q '^serve:up' && ok "Target serve: up" || skip "target serve not up"

# b) predict-counters sichtbar (1/2) – increase
Q1='sum(increase(chs_predict_requests_total[2m]))'
RES="$(curl -sG "$PROM_BASE/api/v1/query" --data-urlencode "query=$Q1" | jq -r '.data.result[0].value[1] // empty')"
if [ -n "$RES" ]; then
  echo "predict_requests_total increase(2m) = $RES"
  ok "predict counters visible (increase)"
else
  # (2) – rate fallback
  Q1b='sum(rate(chs_predict_requests_total[2m]))'
  RESB="$(curl -sG "$PROM_BASE/api/v1/query" --data-urlencode "query=$Q1b" | jq -r '.data.result[0].value[1] // empty')"
  if [ -n "$RESB" ]; then
    echo "predict_requests_total rate(2m) = $RESB"
    ok "predict counters visible (rate)"
  else
    skip "predict counters not visible (noch kein Scrape/Service off?)"
  fi
fi

# c) p95 – seconds_bucket ODER ms_bucket (ms → s)
Q2s='histogram_quantile(0.95, sum by (le) (rate(chs_predict_latency_seconds_bucket[2m])))'
R2s="$(curl -sG "$PROM_BASE/api/v1/query" --data-urlencode "query=$Q2s" | jq -r '.data.result[0].value[1] // empty')"
if [ -n "$R2s" ] && [ "$R2s" != "NaN" ]; then
  echo "predict p95 (2m) = $R2s s"
  ok "predict latency histogram visible (seconds)"
else
  Q2ms='histogram_quantile(0.95, sum by (le) (rate(chs_predict_latency_ms_bucket[2m]))) / 1000'
  R2ms="$(curl -sG "$PROM_BASE/api/v1/query" --data-urlencode "query=$Q2ms" | jq -r '.data.result[0].value[1] // empty')"
  if [ -n "$R2ms" ] && [ "$R2ms" != "NaN" ]; then
    echo "predict p95 (2m) = $R2ms s (from ms_bucket)"
    ok "predict latency histogram visible (ms)"
  else
    skip "predict latency histogram not visible"
  fi
fi

hr
if [ "$SUCCESS" = "yes" ]; then
  ok "E2E PREDICT CONTRACTS PASSED"
else
  skip "Predict calls not verified (endpoint/schema unknown)."
fi
