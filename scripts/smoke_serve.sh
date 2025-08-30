#!/usr/bin/env bash
# Smoke test for FastAPI serve service and API proxy
set -euo pipefail

HOST="${HOST:-localhost}"

# Serve health
curl -fs "http://${HOST}:8001/health" >/dev/null

# Serve metrics (fail if chs_predict_* series missing)
curl -fs "http://${HOST}:8001/metrics" | grep '^chs_predict_' >/dev/null

# API health
curl -fs "http://${HOST}:8080/v1/health" >/dev/null

echo "smoke-serve completed"
