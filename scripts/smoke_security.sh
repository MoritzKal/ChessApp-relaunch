#!/usr/bin/env bash
set -euo pipefail

BASE_URL=${BASE_URL:-http://localhost:8080}
SECRET=${API_JWT_SECRET:-change-me}
ORIGIN=${FRONTEND_ORIGIN:-http://localhost:5173}

mkjwt() { # args: JSON claims via stdin → prints token
  python3 - "$SECRET" <<'PY'
import json, time, base64, hmac, hashlib, sys
secret=sys.argv[1].encode()
claims=json.loads(sys.stdin.read())
now=int(time.time())
claims.setdefault("iat", now)
claims.setdefault("exp", now+600)
hdr={"alg":"HS256","typ":"JWT"}
b64=lambda b: base64.urlsafe_b64encode(b).rstrip(b'=')
si=b'.'.join([b64(json.dumps(hdr,separators=(',',':')).encode()),
              b64(json.dumps(claims,separators=(',',':')).encode())])
sig=b64(hmac.new(secret, si, hashlib.sha256).digest())
print((si+b'.'+sig).decode())
PY
}

USER_TOKEN=$(printf '{"sub":"u1","preferred_username":"u1","roles":["USER"]}' | mkjwt)
MON_TOKEN=$(printf  '{"sub":"mon","roles":["MONITORING"]}' | mkjwt)

pass() { echo "✅ $*"; }
fail() { echo "❌ $*"; exit 1; }

code() { curl -sS -o /dev/null -w "%{http_code}" "$@"; }

# 1) /v1 requires JWT
[ "$(code "$BASE_URL/v1/datasets")" = "401" ] && pass "/v1 ohne Token → 401" || fail "/v1 ohne Token nicht 401"

# 2) /v1 with USER → 200
[ "$(code -H "Authorization: Bearer $USER_TOKEN" "$BASE_URL/v1/datasets")" = "200" ] && pass "/v1 mit USER → 200" || fail "/v1 mit USER nicht 200"

# 3) /actuator/prometheus with MONITORING → 200
[ "$(code -H "Authorization: Bearer $MON_TOKEN" "$BASE_URL/actuator/prometheus")" = "200" ] && pass "prometheus mit MONITORING → 200" || fail "prometheus mit MONITORING nicht 200"

# 4) /actuator/prometheus with USER → 403
[ "$(code -H "Authorization: Bearer $USER_TOKEN" "$BASE_URL/actuator/prometheus")" = "403" ] && pass "prometheus mit USER → 403" || fail "prometheus mit USER nicht 403"

# 5) Docs frei
[ "$(code "$BASE_URL/v3/api-docs")" = "200" ] && pass "v3/api-docs frei → 200" || fail "v3/api-docs nicht 200"
[ "$(code "$BASE_URL/swagger-ui/index.html")" = "200" ] && pass "swagger-ui frei → 200" || fail "swagger-ui nicht 200"

# 6) CORS Preflight
PRE=$(curl -sSI -X OPTIONS "$BASE_URL/v1/datasets" \
  -H "Origin: $ORIGIN" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization, Content-Type")
echo "$PRE" | grep -qi "^HTTP/.* 200" && echo "$PRE" | grep -qi "^Access-Control-Allow-Origin: $ORIGIN" \
  && pass "CORS Preflight ok (200, Allow-Origin=$ORIGIN)" \
  || fail "CORS Preflight nicht korrekt"

echo "🎉 Smoke-Tests erfolgreich."
