#!/usr/bin/env bash
set -euo pipefail

# Secret aus deiner .env (APP_SECURITY_JWT_SECRET)
secret="${JWT_SECRET:-${APP_SECURITY_JWT_SECRET:-dev-secret}}"

# Helper: Base64URL-Encoding
b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

# Header + Payload
header='{"alg":"HS256","typ":"JWT"}'
now=$(date +%s)
exp=$((now+3600))
payload=$(jq -nc --arg sub dev-user --arg iss chessapp-dev --arg aud api --arg scope read \
  --argjson iat "$now" --argjson exp "$exp" \
  '{sub:$sub, iss:$iss, aud:$aud, scope:$scope, iat:$iat, exp:$exp}')

# Signatur
h=$(printf '%s' "$header"  | b64url)
p=$(printf '%s' "$payload" | b64url)
sig=$(printf '%s' "$h.$p" | openssl dgst -binary -sha256 -hmac "$secret" | b64url)

echo "$h.$p.$sig"
