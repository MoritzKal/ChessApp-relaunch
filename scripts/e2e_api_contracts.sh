#!/usr/bin/env bash
set -Eeuo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

need() { command -v "$1" >/dev/null || { echo "Missing dependency: $1" >&2; exit 10; }; }
need curl; need jq; need openssl

# Default-Secret ermitteln (ENV oder .env-Datei)
DEFAULT_SECRET="${JWT_SECRET:-${APP_SECURITY_JWT_SECRET:-}}"
if [ -z "${DEFAULT_SECRET}" ] && [ -f .env ]; then
  DEFAULT_SECRET="$(grep -E '^APP_SECURITY_JWT_SECRET=' .env | tail -n1 | cut -d= -f2- | tr -d '"' || true)"
fi
[ -n "${DEFAULT_SECRET}" ] || { echo "Set APP_SECURITY_JWT_SECRET or JWT_SECRET" >&2; exit 2; }

b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

# mkjwt [scope] [ttl] [secret_override]
mkjwt() {
  local scope="${1:-read}"
  local ttl="${2:-3600}"
  local secret="${3:-${JWT_SECRET:-${APP_SECURITY_JWT_SECRET:-${DEFAULT_SECRET}}}}"

  local now iat exp header payload h p sig
  now=$(date +%s)
  if ! printf '%s' "$ttl" | grep -Eq '^-?[0-9]+$'; then
    echo "TTL must be integer seconds" >&2; exit 3
  fi
  if (( ttl < 0 )); then
    exp=$(( now + ttl )); iat=$(( exp - 3600 ))
  else
    iat=$now; exp=$(( now + ttl ))
  fi
  header='{"alg":"HS256","typ":"JWT"}'
  payload=$(jq -nc --arg sub dev-user --arg iss chessapp-dev --arg aud api --arg scope "$scope" \
    --argjson iat "$iat" --argjson exp "$exp" \
    '{sub:$sub,iss:$iss,aud:$aud,scope:$scope,iat:$iat,exp:$exp}')

  h=$(printf '%s' "$header"  | b64url)
  p=$(printf '%s' "$payload" | b64url)
  sig=$(printf '%s' "$h.$p" | openssl dgst -binary -sha256 -hmac "${secret}" | b64url)
  printf '%s.%s.%s\n' "$h" "$p" "$sig"
}

hr() { printf '\n— %.0s' {1..40}; printf '\n'; }
ok() { printf "✅ %s\n" "$*"; }
fail() { printf "❌ %s\n" "$*" >&2; exit 1; }

# 0) Health offen
hr; echo "[0] Health"
curl -sf "${BASE_URL}/actuator/health" >/dev/null || fail "Health not reachable"
ok "Health OK"

# 1) Unauth -> 401
hr; echo "[1] /v1/models unauthenticated -> 401"
code=$(curl -s -o /dev/null -w '%{http_code}' "${BASE_URL}/v1/models")
[ "$code" = "401" ] || fail "Expected 401, got $code"
ok "401 as expected"

# 2) Valid Token -> 200
hr; echo "[2] /v1/models with read token -> 200"
JWT_READ="$(mkjwt read 3600)"
resp=$(curl -sf -H "Authorization: Bearer ${JWT_READ}" "${BASE_URL}/v1/models")
echo "$resp" | jq -e 'type=="array" and (.[0]|has("modelId"))' >/dev/null || fail "Response JSON unexpected"
ok "200 + JSON structure OK"

# 3) Wrong signature -> 401  (SECRET override)
hr; echo "[3] wrong signature -> 401"
BAD="$(mkjwt read 3600 'WRONG-SECRET-123')"
code=$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer ${BAD}" "${BASE_URL}/v1/models")
[ "$code" = "401" ] || fail "Expected 401, got $code"
ok "401 invalid signature"

# 4) Expired -> 401
hr; echo "[4] expired token -> 401"
EXP=$(mkjwt read -60)
code=$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer ${EXP}" "${BASE_URL}/v1/models")
[ "$code" = "401" ] || fail "Expected 401, got $code"
ok "401 expired"

# 5) Prometheus-Authz + Format prüfen (robust)
hr; echo "[5] Prometheus authz"
code=$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer ${JWT_READ}" "${BASE_URL}/actuator/prometheus")
[ "$code" = "403" ] || fail "Expected 403 for read token, got $code"

MON=$(mkjwt monitoring 3600)

tmp_hdr="$(mktemp)"; tmp_body="$(mktemp)"
curl -sS -D "$tmp_hdr" -H "Authorization: Bearer ${MON}" -o "$tmp_body" "${BASE_URL}/actuator/prometheus" >/dev/null
grep -qi '^Content-Type:.*text/plain.*0\.0\.4' "$tmp_hdr" \
  || fail "Prometheus content-type not text/plain;version=0.0.4"

if ! grep -Eq '^# (HELP|TYPE) ' "$tmp_body"; then
  # fallback: suche nach bekannter Metrik-Zeile
  if ! grep -Eq '^(chs_api_requests_total|http_server_requests_seconds_count)' "$tmp_body"; then
    fail "Prometheus text format missing"
  fi
fi
rm -f "$tmp_hdr" "$tmp_body"
ok "Prometheus policy & format OK"

# 6) Metrics presence (robust Regex + frischer Traffic)
hr; echo "[6] Metrics presence"
# frischer Traffic, damit Counter sicher sichtbar sind
for i in {1..10}; do curl -sf -H "Authorization: Bearer ${JWT_READ}" "${BASE_URL}/v1/models" >/dev/null; done
sleep 2

BODY="$(mktemp)"
curl -s -H "Authorization: Bearer ${MON}" "${BASE_URL}/actuator/prometheus" > "$BODY"

# Namensmuster flexibler (requests_total vs request_total)
REQ_PATTERN='^chs_api_.*requests_total'
HIST_PATTERN='^chs_api_.*request_seconds_bucket'

grep -Eq "${REQ_PATTERN}" "$BODY" \
  || fail "chs_* requests counter missing (pattern: ${REQ_PATTERN})"

grep -Eq "${HIST_PATTERN}" "$BODY" \
  || fail "chs_* latency histogram missing (pattern: ${HIST_PATTERN})"

rm -f "$BODY"
ok "chs_* metrics present"

hr; ok "E2E API CONTRACTS PASSED"