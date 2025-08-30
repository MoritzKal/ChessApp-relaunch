#!/usr/bin/env bash
set -euo pipefail
ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
cd "$ROOT/infra"
echo "==> Bringing up stack (if not running)"
docker compose up -d --build
echo "==> Waiting for Prometheus to respond..."
for i in {1..30}; do
  if curl -sf http://localhost:9090/-/ready >/dev/null; then
    echo "Prometheus ready"; break; fi; sleep 2; done
echo "==> Checking targets (api/ml/serve)..."
curl -sG --data-urlencode 'query=up{job=~"api|ml|serve"}' http://localhost:9090/api/v1/query | jq .
echo "==> Check Grafana reachable..."
curl -sfI http://localhost:3000 >/dev/null && echo "Grafana OK" || echo "Grafana not yet ready"
echo "==> Loki quick log probe (may be empty):"
echo '{"level":"INFO","message":"preflight","component":"obs","run_id":"dry","dataset_id":"dry","model_id":"dry","model_version":"0","username":"local"}' | nc -w1 localhost 3100 || true
echo "Preflight done."
