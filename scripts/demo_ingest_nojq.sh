#!/usr/bin/env bash
set -euo pipefail
OFFLINE=${1:-false}

PAYLOAD=$( [ "$OFFLINE" = "true" ] \
  && echo '{"username":"M3NG00S3","from":"2025-07","to":"2025-08","offline":true}' \
  || echo '{"username":"M3NG00S3","from":"2025-07","to":"2025-08"}' )

RUN=$(curl -s -X POST localhost:8080/v1/ingest -H "Content-Type: application/json" -d "$PAYLOAD" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["runId"])')
echo "runId=$RUN"

for i in $(seq 1 30); do
  STATUS=$(curl -s "localhost:8080/v1/ingest/$RUN" \
    | python3 -c 'import sys,json;print(json.load(sys.stdin)["status"])')
  echo "status=$STATUS"
  [ "$STATUS" = "succeeded" ] && break
  sleep 2
done

echo "Prometheus sample:"
curl -s localhost:8080/actuator/prometheus | grep -E '^chs_ingest_(games|positions|jobs|duration)'
