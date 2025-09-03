#!/usr/bin/env bash
set -euo pipefail

# Secret (Rohtext, kein Base64), bevorzugt JWT_SECRET, sonst APP_SECURITY_JWT_SECRET
secret="${JWT_SECRET:-${APP_SECURITY_JWT_SECRET:-}}"
if [ -z "${secret}" ]; then
  echo "ERROR: APP_SECURITY_JWT_SECRET or JWT_SECRET must be set (raw HS256 secret)" >&2
  exit 2
fi

# Optional: Scope & TTL (Sekunden). TTL<0 => abgelaufenes, aber logisch korrektes Token.
scope="${SCOPE:-read}"
ttl="${TTL:-3600}"

# Helfer: Base64URL
b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

# Zeiten berechnen
now=$(date +%s)
case "${ttl}" in
  ''|*-*|*[^0-9]* ) : ;; # bash-arith kann negative akzeptieren, Absicherung folgt unten
esac
if ! printf '%s' "$ttl" | grep -Eq '^-?[0-9]+$'; then
  echo "ERROR: TTL must be integer seconds (e.g. 3600 or -60)" >&2
  exit 2
fi

if (( ttl < 0 )); then
  exp=$(( now + ttl ))      # exp in der Vergangenheit
  iat=$(( exp - 3600 ))     # iat 1h vor exp
else
  iat=$now
  exp=$(( now + ttl ))
fi

# Header + Payload
header='{"alg":"HS256","typ":"JWT"}'
payload=$(jq -nc \
  --arg sub "dev-user" \
  --arg iss "chessapp-dev" \
  --arg aud "api" \
  --arg scope "$scope" \
  --argjson iat "$iat" \
  --argjson exp "$exp" \
  '{sub:$sub, iss:$iss, aud:$aud, scope:$scope, iat:$iat, exp:$exp}')

# Signatur
h=$(printf '%s' "$header"  | b64url)
p=$(printf '%s' "$payload" | b64url)
sig=$(printf '%s' "$h.$p" | openssl dgst -binary -sha256 -hmac "$secret" | b64url)

printf '%s\n' "$h.$p.$sig"
