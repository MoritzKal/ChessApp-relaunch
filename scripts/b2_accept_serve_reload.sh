#!/usr/bin/env bash
set -euo pipefail

# ---------- Konfiguration ----------
SERVE_BASE="${SERVE_BASE:-http://localhost:8001}"
API_BASE="${API_BASE:-http://localhost:8080}"
# Test-Modelle (kannst du per ENV überschreiben)
MODEL_ID="${MODEL_ID:-policy_tiny}"
MODEL_VERSION="${MODEL_VERSION:-2025-08-29}"
BAD_VERSION="${BAD_VERSION:-__does_not_exist__}"   # sollte nicht existieren
# Optional: Loki für Log-Check (leer lassen wenn nicht vorhanden)
LOKI_URL="${LOKI_URL:-}"       # z.B. http://localhost:3100
LOKI_QUERY_RANGE_S="${LOKI_QUERY_RANGE_S:-300}"  # letzte 5 Min

VALID_FEN='rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'
INVALID_FEN='this is not fen'

# ---------- Helpers ----------
RED="$(printf '\033[31m')"; GREEN="$(printf '\033[32m')"; YELLOW="$(printf '\033[33m')"; BOLD="$(printf '\033[1m')"; RESET="$(printf '\033[0m')"
pass(){ echo -e "${GREEN}✔${RESET} $*"; }
fail(){ echo -e "${RED}✖${RESET} $*"; exit 1; }
info(){ echo -e "${YELLOW}∙${RESET} $*"; }
section(){ echo -e "\n${BOLD}== $* ==${RESET}"; }

need_cmd(){ command -v "$1" >/dev/null 2>&1 || fail "Benötigtes Tool fehlt: $1"; }

http_status(){ # url method [json]
  local url="$1" method="$2" data="${3:-}"
  if [[ -n "$data" ]]; then
    curl -s -o /dev/null -w "%{http_code}" -X "$method" -H 'Content-Type: application/json' --data "$data" "$url"
  else
    curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url"
  fi
}

http_json(){ # url method [json]
  local url="$1" method="$2" data="${3:-}"
  if [[ -n "$data" ]]; then
    curl -sS -X "$method" -H 'Content-Type: application/json' --data "$data" "$url"
  else
    curl -sS -X "$method" "$url"
  fi
}

metrics_dump(){
  # folgt Redirects und funktioniert mit /metrics und /metrics/
  curl -sS -L "${SERVE_BASE%/}/metrics"
}


# Prom-Wert lesen; wenn nicht vorhanden → 0
prom_val(){
  local metric="$1"; local selector_regex="${2:-}"
  # Sucht Zeilen wie: metric{labels} value   ODER: metric value
  if [[ -n "$selector_regex" ]]; then
    metrics_dump | awk -v m="^${metric}\\{" -v r="$selector_regex" '
      $0 ~ m && $0 ~ r {
        # value ist letztes Feld
        v=$NF; print v; found=1
      }
      END{ if(!found) print 0 }'
  else
    metrics_dump | awk -v m="^${metric}($| )" '
      $0 ~ m {
        v=$NF; print v; found=1
      }
      END{ if(!found) print 0 }'
  fi
}
# Summe aller Samples einer Metrik (egal ob gelabelt oder nicht)
prom_sum(){
  local metric="$1" ; local selector_regex="${2:-}"
  # Grep filtert vor, awk summiert letztes Feld; keine \{ Escapes nötig
  if [[ -n "$selector_regex" ]]; then
    metrics_dump | grep -E "^${metric}(\{|\s)" | grep -E "$selector_regex" | awk '{sum+=$NF} END{printf "%.6f\n", (sum?sum:0)}'
  else
    metrics_dump | grep -E "^${metric}(\{|\s|$)" | awk '{sum+=$NF} END{printf "%.6f\n", (sum?sum:0)}'
  fi
}

prom_has_metric_with_labels(){
  local metric="$1"; local selector_regex="$2"
  metrics_dump | grep -E "^${metric}\{.*${selector_regex}.*\}" >/dev/null
}

delta(){
  python - <<'PY' "$1" "$2"
import sys
a=float(sys.argv[1]); b=float(sys.argv[2])
print(f"{b-a:.6f}")
PY
}

# ---------- Preflight ----------
section "Preflight"
need_cmd curl; need_cmd jq; need_cmd awk
info "SERVE_BASE=$SERVE_BASE"
info "API_BASE=$API_BASE"
info "MODEL_ID=$MODEL_ID MODEL_VERSION=$MODEL_VERSION BAD_VERSION=$BAD_VERSION"

# ---------- Health ----------
section "Health-Checks"
[[ "$(http_status "$SERVE_BASE/health" GET)" == "200" ]] || fail "Serve /health nicht OK"
pass "Serve /health"

[[ "$(http_status "$API_BASE/v1/health" GET)" == "200" ]] || info "API /v1/health nicht 200 (ggf. nicht implementiert)"; [[ "$(http_status "$API_BASE/v1/health" GET)" == "200" ]] && pass "API /v1/health"

section "Predict – Valid/Invalid FEN"

# --- VALID FEN ---
resp="$(http_json "$SERVE_BASE/predict" POST "$(jq -nc --arg fen "$VALID_FEN" '{fen:$fen}')" )" || {
  echo "HTTP/Transport-Fehler bei /predict (valid). Response:"
  echo "$resp"
  fail "Predict (valid) HTTP-Fehler"
}

# Akzeptiere: move:string + (policy[] ODER legal[]) + irgendeine Modell-Identifikation
# Modell-Identifikation: .model.id/.model.version ODER .modelId/.modelVersion ODER .model.modelId/.model.modelVersion
if echo "$resp" | jq -e '
  (.move|type=="string")
  and ((.policy?|type=="array") or (.legal?|type=="array"))
  and (
        (.model? and (.model.id? and .model.version?))
     or (.modelId? and .modelVersion?)
     or (.model? and (.model.modelId? and .model.modelVersion?))
  )
' >/dev/null; then
  # Nur zur Info ausgeben, was der Server meldet:
  mid="$(echo "$resp" | jq -r '.model.id // .modelId // .model.modelId // "n/a"')"
  mver="$(echo "$resp" | jq -r '.model.version // .modelVersion // .model.modelVersion // "n/a"')"
  info "Active model laut /predict: ${mid}/${mver}"
  pass "Predict (valid FEN) Schema ok"
else
  echo "Unerwartetes Response-Schema. Antwort war:"
  echo "$resp" | jq . || echo "$resp"
  fail "Predict valid: Response Schema unerwartet (erwarte move + (policy[] ODER legal[]) + irgendeine model*-Kennung)"
fi

# --- INVALID FEN (400) ---
code_invalid="$(http_status "$SERVE_BASE/predict" POST "$(jq -nc --arg fen "$INVALID_FEN" '{fen:$fen}')" )"
[[ "$code_invalid" == "400" ]] || fail "Predict invalid: erwartet 400, bekam $code_invalid"
pass "Predict (invalid FEN) → 400"

# --- Illegal-Counter Delta ---
# Hinweis: Einige Implementierungen labeln die Metrik (z.B. {reason="INVALID_FEN"}).
# prom_sum fasst alle Label-Samples zusammen.
METRIC_ILLEGAL="${METRIC_ILLEGAL:-chs_predict_illegal_requests_total}"

illegal_before="$(prom_sum "$METRIC_ILLEGAL")"
_="$(http_status "$SERVE_BASE/predict" POST "$(jq -nc --arg fen "$INVALID_FEN" '{fen:$fen}')" )" || true
illegal_after="$(prom_sum "$METRIC_ILLEGAL")"
d_illegal="$(delta "$illegal_before" "$illegal_after")"
[[ "$d_illegal" == "1.000000" ]] || fail "$METRIC_ILLEGAL Delta = $d_illegal (erwartet 1)"
pass "$METRIC_ILLEGAL +1"

# ---------- Reload: success & idempotency ----------
section "Reload – Erfolg & Idempotenz"

# Aktives Modell aus dem vorherigen /predict-Response ziehen (bereits in resp)
active_id="$(echo "$resp" | jq -r '.model.id // .modelId // .model.modelId // "dummy"')"
active_ver="$(echo "$resp" | jq -r '.model.version // .modelVersion // .model.modelVersion // "0"')"
info "Aktives Modell erkannt: ${active_id}/${active_ver}"

# ---- Idempotenztest auf aktuellem Stand ----
body_same="$(jq -nc --arg id "$active_id" --arg ver "$active_ver" '{modelId:$id, modelVersion:$ver}')"
m_before="$(prom_sum 'chs_models_loaded_total' "model_id=\"$active_id\".*model_version=\"$active_ver\"")"

status1="$(http_status "$SERVE_BASE/models/load" POST "$body_same")"
resp1="$(http_json "$SERVE_BASE/models/load" POST "$body_same")" || true
m_mid="$(prom_sum 'chs_models_loaded_total' "model_id=\"$active_id\".*model_version=\"$active_ver\"")"

status2="$(http_status "$SERVE_BASE/models/load" POST "$body_same")"
resp2="$(http_json "$SERVE_BASE/models/load" POST "$body_same")" || true
m_after="$(prom_sum 'chs_models_loaded_total' "model_id=\"$active_id\".*model_version=\"$active_ver\"")"

d1=$(python - <<PY "$m_before" "$m_mid"
import sys; print(f"{float(sys.argv[2])-float(sys.argv[1]):.6f}")
PY
)
d2=$(python - <<PY "$m_mid" "$m_after"
import sys; print(f"{float(sys.argv[2])-float(sys.argv[1]):.6f}")
PY
)

# Akzeptiere: erster Call darf 0 ODER 1 sein (je nach Implementierung), der zweite MUSS 0 sein
if [[ "$d2" == "0.000000" ]]; then
  pass "Idempotenz ok auf aktivem Modell (Delta2=0). Delta1=$d1 (ok wenn 0 oder 1)"
else
  echo "resp1=$resp1"
  echo "resp2=$resp2"
  fail "Idempotenz verletzt: zweiter Reload erhöhte Counter (Delta2=$d2, erwartet 0)"
fi

# ---- Optionaler Wechsel auf gewünschtes Zielmodell (falls Artefakt vorhanden) ----
if [[ -n "${MODEL_ID:-}" && -n "${MODEL_VERSION:-}" && ( "$MODEL_ID" != "$active_id" || "$MODEL_VERSION" != "$active_ver" ) ]]; then
  section "Reload – Wechsel auf gewünschtes Modell (optional)"
  body_target="$(jq -nc --arg id "$MODEL_ID" --arg ver "$MODEL_VERSION" '{modelId:$id, modelVersion:$ver}')"

  # Vorzähler für Ziel
  mt_before="$(prom_sum 'chs_models_loaded_total' "model_id=\"$MODEL_ID\".*model_version=\"$MODEL_VERSION\"")"

  code_target="$(http_status "$SERVE_BASE/models/load" POST "$body_target")"
  resp_target="$(http_json "$SERVE_BASE/models/load" POST "$body_target")" || true

  if [[ "$code_target" =~ ^2 ]]; then
    mt_after="$(prom_sum 'chs_models_loaded_total' "model_id=\"$MODEL_ID\".*model_version=\"$MODEL_VERSION\"")"
    d_target=$(python - <<PY "$mt_before" "$mt_after"
import sys; print(f"{float(sys.argv[2])-float(sys.argv[1]):.6f}")
PY
)
    [[ "$d_target" == "1.000000" || "$d_target" == "0.000000" ]] \
      && pass "Reload auf ${MODEL_ID}/${MODEL_VERSION} erfolgreich (Delta=$d_target)" \
      || fail "Unerwartetes Delta beim Ziel-Reload: $d_target"
  else
    info "Ziel-Reload nicht erfolgreich (HTTP $code_target). Antwort:"
    echo "$resp_target"
    info "Das ist ok, wenn das Artefakt (noch) nicht vorhanden ist – wird im Fehlerpfad geprüft."
  fi
fi

# ---------- Reload: Fehlerpfad ----------
section "Reload – Fehlerpfad (nicht vorhandene Version)"
bad_body="$(jq -nc --arg id "$MODEL_ID" --arg ver "$BAD_VERSION" '{modelId:$id, modelVersion:$ver}')"

# Reload-Failure-Metrik (überschreibbar)
METRIC_RELOAD_FAIL="${METRIC_RELOAD_FAIL:-chs_model_reload_failures_total}"

rf_before="$(prom_sum "$METRIC_RELOAD_FAIL")"
code_bad="$(http_status "$SERVE_BASE/models/load" POST "$bad_body")"
resp_bad="$(http_json "$SERVE_BASE/models/load" POST "$bad_body")" || true
rf_after="$(prom_sum "$METRIC_RELOAD_FAIL")"
d_rf=$(python - <<PY "$rf_before" "$rf_after"
import sys; print(f"{float(sys.argv[2])-float(sys.argv[1]):.6f}")
PY
)

[[ "$code_bad" =~ ^4|5 ]] || fail "Erwartete 4xx/5xx beim Laden unbekannter Version; bekam $code_bad"
[[ "$d_rf" == "1.000000" || "$d_rf" == "0.000000" ]] \
  && pass "$METRIC_RELOAD_FAIL Delta=$d_rf (>=0, Implementierungsspezifisch)" \
  || fail "$METRIC_RELOAD_FAIL Delta=$d_rf (unerwartet)"

# ---------- Metrics Labels & Summary ----------
section "Metriken – Labels & Summary"
# mind. eine valide Predict auslösen
_="$(http_json "$SERVE_BASE/predict" POST "$(jq -nc --arg fen "$VALID_FEN" '{fen:$fen}')" )" || true

prom_has_metric_with_labels "chs_predict_requests_total" "model_id=\"$MODEL_ID\".*model_version=\"$MODEL_VERSION\"" \
  && pass "chs_predict_requests_total gelabelt mit model_id/model_version" \
  || fail "chs_predict_requests_total ohne erwartete Labels"

prom_has_metric_with_labels "chs_predict_errors_total" "model_id=\"$MODEL_ID\".*model_version=\"$MODEL_VERSION\"" \
  && pass "chs_predict_errors_total gelabelt" \
  || fail "chs_predict_errors_total ohne erwartete Labels"

prom_has_metric_with_labels "chs_predict_latency_ms" "model_id=\"$MODEL_ID\".*model_version=\"$MODEL_VERSION\"" \
  && pass "chs_predict_latency_ms Summary vorhanden (mit Labels)" \
  || fail "chs_predict_latency_ms nicht gefunden"

# ---------- API Proxy ----------
section "API Proxy – /v1/models/load"
api_code="$(http_status "$API_BASE/v1/models/load" POST "$load_body")"
[[ "$api_code" == "200" ]] && pass "API Proxy /v1/models/load → 200" || fail "API Proxy /v1/models/load Status=$api_code"

# ---------- Optional: Cache-Metriken ----------
section "Optional – Cache-Metriken"
if metrics_dump | grep -q '^chs_predict_cache_'; then
  pass "Cache-Metriken vorhanden (LRU aktiviert)"
else
  info "Keine Cache-Metriken gefunden (vermutlich LRU deaktiviert) – OK"
fi

# ---------- Optional: Loki Logs ----------
section "Optional – Loki Log-Check"
if [[ -n "$LOKI_URL" ]]; then
  info "Frage Loki ab (letzte ${LOKI_QUERY_RANGE_S}s) …"
  # einfache Instant-Query: count über Events mit component="serve"
  now_ns=$(($(date +%s)*1000000000))
  start_ns=$(( ( $(date +%s) - LOKI_QUERY_RANGE_S ) * 1000000000 ))
  # Query: {component="serve"} | json | event=~"predict.*"
  q='{component="serve"} | json | event=~"predict.*"'
  res="$(curl -sG --data-urlencode "query=${q}" --data-urlencode "start=${start_ns}" --data-urlencode "end=${now_ns}" "$LOKI_URL/loki/api/v1/query_range")"
  count="$(echo "$res" | jq '[.data.result[].values[]] | length' 2>/dev/null || echo 0)"
  [[ "${count:-0}" -gt 0 ]] && pass "Loki liefert predict-Events ($count)" || info "Keine predict-Events in Loki gefunden (optional)"
else
  info "LOKI_URL nicht gesetzt – Log-Check übersprungen (optional)"
fi

echo
echo -e "${BOLD}${GREEN}ALLE PFLICHT-PRÜFUNGEN BESTANDEN.${RESET}"
echo -e "Modell geladen: ${MODEL_ID}/${MODEL_VERSION}  |  Serve=${SERVE_BASE}  API=${API_BASE}"
