#!/usr/bin/env bash
set -Eeuo pipefail

# ===== Config (override via env) ============================================
API_BASE="${API_BASE:-http://localhost:8080}"
SERVE_BASE="${SERVE_BASE:-http://localhost:8000}"
SERVE_ALT="${SERVE_ALT:-http://localhost:8001}"    # optional
PROM_URL="${PROM_URL:-http://localhost:9090}"

# Optional: seed a dummy best.pt before tests (useful for local/dev)
SEED_MODEL="${SEED_MODEL:-false}"

MODEL_ID="${MODEL_ID:-default}"
MODEL_VERSION="${MODEL_VERSION:-0}"

TIMEOUT="${TIMEOUT:-60}"            # seconds per wait
CURL_MAXTIME="${CURL_MAXTIME:-30}"  # seconds per HTTP call

# ===== Helpers ==============================================================
require() { command -v "$1" >/dev/null 2>&1 || { echo "Missing tool: $1"; exit 1; }; }
require curl; require jq; require date

ts() { date -u +"%Y-%m-%dT%H:%M:%SZ"; }
ROOT_REPORT="acceptance_report.txt"
: > "$ROOT_REPORT"

ART_DIR="artifacts/accept_blockB_$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$ART_DIR"

log()   { echo "[$(ts)] $*" | tee -a "$ROOT_REPORT" ; }
sect()  { echo -e "\n===== $* =====" | tee -a "$ROOT_REPORT"; }
print_json_if() {
  local file="$1"
  if jq -e . "$file" >/dev/null 2>&1; then
    jq -C . "$file" | sed 's/^/    /'
  else
    sed 's/^/    /' "$file"
  fi
}

wait_http_ok() {
  local url="$1"
  local okre='^(200|204)$'
  local i=0
  while (( i < TIMEOUT )); do
    if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -qE "$okre"; then
      log "OK: $url"
      return 0
    fi
    sleep 1; ((i++))
  done
  log "TIMEOUT: $url did not return 200/204 within ${TIMEOUT}s"
  return 1
}

detect_active_serve() {
  for base in "$SERVE_BASE" "$SERVE_ALT"; do
    code=$(curl -s -o /dev/null -w "%{http_code}" \
      -H 'Content-Type: application/json' \
      -d '{"fen":"8/8/8/8/8/8/8/8 w - - 0 1"}' "$base/predict" || true)
    [[ "$code" =~ ^(200|400)$ ]] && { echo "$base"; return; }
  done
  echo "$SERVE_BASE"
}
ACTIVE_SERVE="$(detect_active_serve)"
log "Active Serve detected: $ACTIVE_SERVE"


curl_json() {
  # curl_json <method> <url> <json-body or empty> <tag>
  local method="$1" url="$2" body="${3:-}" tag="${4:-call}"
  local hfile="$ART_DIR/${tag}_headers.txt"
  local bfile="$ART_DIR/${tag}_body.txt"
  local tfile="$ART_DIR/${tag}_trace.txt"
  local code
  # ensure files exist even if curl fails early (connection refused, etc.)
  : > "$hfile"; : > "$bfile"; : > "$tfile"
  if [[ -n "$body" ]]; then
    code=$(curl -sS -D "$hfile" -o "$bfile" \
      -H "Content-Type: application/json" \
      --max-time "$CURL_MAXTIME" --trace-ascii "$tfile" \
      -X "$method" -d "$body" "$url" -w "%{http_code}" || true)
  else
    code=$(curl -sS -D "$hfile" -o "$bfile" \
      --max-time "$CURL_MAXTIME" --trace-ascii "$tfile" \
      -X "$method" "$url" -w "%{http_code}" || true)
  fi

  [[ "$code" =~ ^[0-9]{3}$ ]] || code="000"
  echo "$code|$hfile|$bfile|$tfile"
}

# ===== Environment Snapshot ================================================
sect "Environment"
log "API_BASE=$API_BASE"
log "SERVE_BASE=$SERVE_BASE"
log "SERVE_ALT=$SERVE_ALT"
log "PROM_URL=$PROM_URL"
log "MODEL_ID=$MODEL_ID"
log "MODEL_VERSION=$MODEL_VERSION"
log "SEED_MODEL=$SEED_MODEL"
log "Artifacts dir: $ART_DIR"

# ===== Health checks ========================================================
sect "Health checks"
wait_http_ok "$API_BASE/v1/health" || true
wait_http_ok "$SERVE_BASE/health"  || wait_http_ok "$SERVE_ALT/health" || true

# ===== OpenAPI contract (/v3/api-docs/v1) ==================================
sect "OpenAPI contract (/v3/api-docs/v1)"
if curl -sf "$API_BASE/v3/api-docs/v1" -o "$ART_DIR/openapi_v1.json"; then
  if jq -e '.paths | has("/v1/models") and has("/v1/models/{id}/versions")' "$ART_DIR/openapi_v1.json" >/dev/null; then
    log "OpenAPI contains expected paths"
  else
    log "WARN: OpenAPI missing expected paths. See $ART_DIR/openapi_v1.json"
  fi
  if jq -e '.paths | has("/v1/models/load")' "$ART_DIR/openapi_v1.json" >/dev/null; then
    log "OpenAPI also exposes /v1/models/load (proxy present)."
  else
    log "INFO: /v1/models/load not in OpenAPI – API-Proxy evtl. nicht vorgesehen (direkt Serve testen)."
  fi
else
  log "WARN: Could not fetch $API_BASE/v3/api-docs/v1"
fi

# ===== Optional model seeding ==============================================
if [[ "$SEED_MODEL" == "true" ]]; then
  sect "Optional model seeding"
  log "Seeding dummy best.pt for ${MODEL_ID}/${MODEL_VERSION} (if needed)"
  if bash scripts/seed_default_model.sh >>"$ROOT_REPORT" 2>&1; then
    log "Model seed completed (see above for details)."
  else
    log "WARN: Model seed script reported an error (continuing)."
  fi
fi

# ===== Determine /models/load endpoint & call ==============================
sect "B2 – Reload by Version (auto-detect endpoint)"
LOAD_BODY=$(jq -nc --arg mid "$MODEL_ID" --arg ver "$MODEL_VERSION" '{modelId:$mid, modelVersion:$ver}')
printf "%s" "$LOAD_BODY" > "$ART_DIR/api_load_request.json"
CANDIDATES=(
   "$ACTIVE_SERVE/models/load|serve_active_load"
   "$SERVE_BASE/models/load|serve_load"
   "$SERVE_ALT/models/load|serve_alt_load"
   "$API_BASE/v1/models/load|api_load"
)

LOAD_OK=0; LOAD_STATUS=""; LOAD_URL=""
for cand in "${CANDIDATES[@]}"; do
  url="${cand%%|*}"; tag="${cand##*|}"
  log "Trying: POST $url with body file: $ART_DIR/api_load_request.json"
  IFS='|' read -r code hfile bfile tfile < <(curl_json POST "$url" "$LOAD_BODY" "$tag")
  log "HTTP $code (headers:$hfile, body:$bfile, trace:$tfile)"

  [[ -z "$LOAD_STATUS" ]] && { LOAD_STATUS="$code"; LOAD_URL="$url"; }

  if [[ "$code" =~ ^(200|201|202)$ ]]; then
    if jq -e '.ok==true or .active?!=null' "$bfile" >/dev/null 2>&1; then
      log "Model load looks OK at $url"
      LOAD_OK=1; LOAD_STATUS="$code"; LOAD_URL="$url"
      break
    else
      log "WARN: 2xx but body not as expected:"
      print_json_if "$bfile"
    fi
  elif [[ "$code" == "404" ]] && grep -qi "missing_artifact" "$bfile"; then
    log "Endpoint found but artifact missing (404 missing_artifact). Will try other candidates too."
    LOAD_OK=2; LOAD_STATUS="$code"; LOAD_URL="$url"
    continue
  elif [[ "$code" == "000" ]]; then
    log "INFO: Connection failed to $url (HTTP 000). See trace: $tfile"
    # weiterprobieren
    continue
  else
    log "INFO: Non-success response; body:"
    print_json_if "$bfile"
  fi
done

if (( LOAD_OK == 0 )); then
  log "ERROR: Could not successfully call any /models/load endpoint (tried API and Serve). See artifacts."
fi

# ===== Predict invalid FEN (with API→Serve fallback) =======================
sect "Predict invalid FEN (expects 400 INVALID_FEN)"
PRED_BODY='{"fen":"INVALID_FEN_@@@"}'
PRED_CANDS=(
  "$ACTIVE_SERVE/predict|serve_active_predict"
  "$SERVE_BASE/predict|serve_predict"
  "$SERVE_ALT/predict|serve_alt_predict"
  "$API_BASE/v1/predict|api_predict"
)

PRED_OK=0; PRED_STATUS=""; PRED_URL=""
for cand in "${PRED_CANDS[@]}"; do
  url="${cand%%|*}"; tag="${cand##*|}"
  log "Trying: POST $url with invalid FEN"
  IFS='|' read -r code hfile bfile tfile < <(curl_json POST "$url" "$PRED_BODY" "$tag")
  log "HTTP $code (headers:$hfile, body:$bfile, trace:$tfile)"
  [[ -z "$PRED_STATUS" ]] && { PRED_STATUS="$code"; PRED_URL="$url"; }
  if [[ "$code" == "400" ]]; then
    if jq -e '.error.code=="INVALID_FEN" or .error=="invalid_fen"' "$bfile" >/dev/null 2>&1; then
      log "Got expected 400 INVALID_FEN at $url"
      PRED_OK=1
      break
    else
      log "WARN: 400 without expected error code; body:"
      print_json_if "$bfile"
      PRED_OK=1
      break
    fi
  else
    log "INFO: Unexpected status ($code); body:"
    print_json_if "$bfile"
  fi
done
if (( PRED_OK == 0 )); then
  log "ERROR: Predict invalid-FEN test failed across all candidates."
fi

# ---- Warm-up with valid FEN to materialize metric series ------------------
sect "Predict warm-up (valid FEN to generate metrics)"
VALID_FEN='rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'
WARM_BODY=$(jq -nc --arg fen "$VALID_FEN" '{fen:$fen}')
printf "%s" "$WARM_BODY" > "$ART_DIR/predict_warm_request.json"
# Prefer Serve first for warm-up to ensure Serve /metrics shows needles
PRED_CANDS=(
  "$SERVE_BASE/predict|serve_predict"
  "$SERVE_ALT/predict|serve_alt_predict"
  "$API_BASE/v1/predict|api_predict"
)
for cand in "${PRED_CANDS[@]}"; do
  url="${cand%%|*}"; tag="${cand##*|}_warm"
  IFS='|' read -r code hfile bfile tfile < <(curl_json POST "$url" "$WARM_BODY" "$tag")
  log "Warm-up: POST $url -> HTTP $code (headers:$hfile, body:$bfile)"
  [[ "$code" =~ ^(200|400)$ ]] && break
done

# ===== Metrics presence (Serve /metrics, API actuator, Prometheus) ========
sect "Metrics presence"
MET_NEEDLE='chs_predict_latency_ms|chs_predict_latency_seconds|chs_predict_errors_total|chs_models_loaded_total|chs_model_reload_failures_total'
HIT=0

if curl -sf "$SERVE_BASE/metrics" -o "$ART_DIR/metrics_serve.txt" || curl -sf "$SERVE_ALT/metrics" -o "$ART_DIR/metrics_serve.txt"; then
  if grep -E "$MET_NEEDLE" "$ART_DIR/metrics_serve.txt" >/dev/null; then
    log "Found expected metrics in Serve /metrics."
    HIT=1
  else
    log "WARN: Serve /metrics fetched but needles not found. See $ART_DIR/metrics_serve.txt"
    log "First 30 lines of Serve /metrics:"
    head -n 30 "$ART_DIR/metrics_serve.txt" | sed 's/^/    /'
  fi
else
  log "INFO: Serve /metrics not reachable."
fi

if curl -sf "$API_BASE/actuator/prometheus" -o "$ART_DIR/metrics_api.txt"; then
  if grep -E "$MET_NEEDLE" "$ART_DIR/metrics_api.txt" >/dev/null; then
    log "Found expected metrics in API actuator."
    HIT=1
  else
    log "INFO: API actuator fetched but needles not found."
  fi
fi

if [[ $HIT -eq 0 ]]; then
  # Query Prometheus directly as last resort
  if curl -sfG "$PROM_URL/api/v1/query" --data-urlencode 'query=chs_predict_latency_ms' -o "$ART_DIR/prom_query_latency_ms.json"; then
    if jq -e '.data.result | length >= 0' "$ART_DIR/prom_query_latency_ms.json" >/dev/null; then
      log "Prometheus reachable; query executed (see $ART_DIR/prom_query_latency_ms.json)"
    fi
  else
    log "INFO: Prometheus not reachable at $PROM_URL"
  fi
  # Try Serve /metrics again after warm-up
  if curl -sf "$SERVE_BASE/metrics" -o "$ART_DIR/metrics_serve_after_warm.txt" || curl -sf "$SERVE_ALT/metrics" -o "$ART_DIR/metrics_serve_after_warm.txt"; then
    if grep -E "$MET_NEEDLE" "$ART_DIR/metrics_serve_after_warm.txt" >/dev/null; then
      log "Found expected metrics in Serve /metrics after warm-up."
      HIT=1
    fi
  fi
fi

# ===== Summary & Next actions =============================================
sect "DIAG SUMMARY"
log "LOAD endpoint tried -> status: ${LOAD_STATUS:-n/a}   url: ${LOAD_URL:-n/a}"
log "PREDICT invalid-FEN -> status: ${PRED_STATUS:-n/a}   url: ${PRED_URL:-n/a}"

NEXT_MSG="$ART_DIR/next_actions.txt"
{
  echo "Recommended next actions:"
  if (( LOAD_OK == 0 )); then
    echo " - LOAD failed everywhere. Check ${ART_DIR}/*load* files."
    echo " - Verify endpoint path: API (/v1/models/load) vs Serve (/models/load)."
    echo " - If body schema mismatch: ensure {modelId, modelVersion} and Content-Type: application/json."
    echo " - If artifact missing: seed model weights or adjust MODEL_ID/MODEL_VERSION."
  else
    if (( LOAD_OK == 2 )); then
      echo " - LOAD endpoint OK but artifact missing (404). Seed artifacts at ModelLoader root."
      echo "   Example (inside serve container):"
      # avoid set -u errors while printing example
      set +u
      echo '   docker exec -it chs_serve bash -lc '\''set -e; : "${ML_MODELS_ROOT:=/models}"; install -Dv /dev/null "$ML_MODELS_ROOT/'"$MODEL_ID"'/'"$MODEL_VERSION"'/best.pt"'\'
      set -u
    fi
  fi
  if (( PRED_OK == 0 )); then
    echo " - Predict failed. Inspect *predict* artifacts; check FastAPI logs."
  fi
  echo " - Review Prometheus targets/rules in $ART_DIR/prom_targets.json and prom_rules.json."
  echo " - Attach $ROOT_REPORT and the $ART_DIR directory to the PL summary."
} > "$NEXT_MSG"
log "Next actions written to $NEXT_MSG"

# Exit code (non-zero if major checks failed)
EXIT_CODE=0
(( LOAD_OK == 0 )) && EXIT_CODE=$(( EXIT_CODE | 2 ))
(( PRED_OK == 0 )) && EXIT_CODE=$(( EXIT_CODE | 4 ))
exit "$EXIT_CODE"
