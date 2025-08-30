#!/usr/bin/env bash
set -euo pipefail
PROM="http://localhost:9090/api/v1/query"
q(){ curl -sG --data-urlencode "query=$1" "$PROM" | jq -re '.data.result[].value[1]';}
echo "Checking Prometheus targets..."
[ "$(q 'up{job="api"}')" = "1" ]
[ "$(q 'up{job="ml"}')" = "1" ]
[ "$(q 'up{job="serve"}')" = "1" ]
echo "Prometheus targets OK"
echo "Alert rules syntax..."
docker compose -f infra/docker-compose.yml exec prom promtool check rules /etc/prometheus/alerts/*.yml
echo "Grafana reachability..."
curl -sfI http://localhost:3000 >/dev/null && echo "Grafana OK"
echo "Loki MDC label query (may be empty but must not error)..."
curl -s "http://localhost:3100/loki/api/v1/query?query={component=\"training\"}" >/dev/null && echo "Loki OK"
echo "Smoke OK"
