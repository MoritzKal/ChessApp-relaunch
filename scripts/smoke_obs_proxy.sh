#!/usr/bin/env bash
set -euo pipefail
BASE=${1:-http://localhost:8088}
KEY_HEADER=${OBS_KEY:+-H "X-Obs-Api-Key: ${OBS_KEY}"}

echo "Health:"
curl -sf ${BASE}/healthz | jq .

echo "Prom up:"
curl -sf ${KEY_HEADER} "${BASE}/obs/prom/query?query=up" | jq '.status'

echo "Loki labels (may be empty):"
curl -sf ${KEY_HEADER} "${BASE}/obs/loki/query?query={}" | jq '.status'
