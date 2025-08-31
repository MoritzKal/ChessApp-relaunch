#!/usr/bin/env bash
# Smoke test for Prometheus scraping
set -euo pipefail

HOST="${HOST:-localhost}"
PROM="http://${HOST}:9090/api/v1/query"

# Ensure Prometheus responds and scrapes jobs
curl -fsSG --data-urlencode 'query=sum by(job)(up)' "$PROM" >/dev/null

# Ensure chs_ metrics are present
curl -sSG --data-urlencode 'query=chs_predict_requests_total' "$PROM" \
  | jq -e '.data.result | length > 0' >/dev/null

echo "smoke-prom completed"
