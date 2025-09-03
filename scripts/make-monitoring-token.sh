#!/usr/bin/env bash
set -euo pipefail

secret="${JWT_SECRET:-${APP_SECURITY_JWT_SECRET:-}}"
if [ -z "${secret}" ]; then
  echo "Set APP_SECURITY_JWT_SECRET or JWT_SECRET" >&2
  exit 2
fi

b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

header='{"alg":"HS256","typ":"JWT"}'
now=$(date +%s); exp=$((now+24*3600))
payload=$(jq -nc --arg sub prometheus --arg iss chessapp-dev --arg aud api --arg scope monitoring \
  --argjson iat "$now" --argjson exp "$exp" \
  '{sub:$sub,iss:$iss,aud:$aud,scope:$scope,iat:$iat,exp:$exp}')
h=$(printf '%s' "$header"  | b64url)
p=$(printf '%s' "$payload" | b64url)
sig=$(printf '%s' "$h.$p" | openssl dgst -binary -sha256 -hmac "$secret" | b64url)
echo "$h.$p.$sig"

