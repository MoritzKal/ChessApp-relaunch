#!/usr/bin/env bash
set -euo pipefail
sub="${1:-tester}"; scope="${2:-selfplay eval monitoring}"; ttl="${3:-3600}"
secret="${JWT_SECRET:-dev-secret}"; exp=$(( $(date +%s) + ttl ))
hdr='{"alg":"HS256","typ":"JWT"}'
pld=$(printf '{"sub":"%s","scope":"%s","exp":%d}' "$sub" "$scope" "$exp")
b64(){ openssl base64 -A | tr '+/' '-_' | tr -d '='; }
seg1="$(printf '%s' "$hdr" | b64)"; seg2="$(printf '%s' "$pld" | b64)"
sig="$(printf '%s.%s' "$seg1" "$seg2" | openssl dgst -binary -sha256 -hmac "$secret" | b64)"
printf 'Bearer %s.%s\n' "$seg1" "$seg2"."$sig"
