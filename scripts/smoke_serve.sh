#!/usr/bin/env bash
# Smoke test for FastAPI serve service and API proxy
set -euo pipefail

HOST="${HOST:-localhost}"

# Serve health
curl -fs "http://${HOST}:8001/health" >/dev/null

# Serve metrics (optional prefix check)
if curl -fs "http://${HOST}:8001/metrics" | grep -q '^chs_predict_'; then
  echo "chs_predict_* metrics found"
else
  echo "chs_predict_* metrics missing"
fi

# API health
curl -fs "http://${HOST}:8080/v1/health" >/dev/null

echo "smoke-serve completed"
