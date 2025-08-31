#!/usr/bin/env bash
set -euo pipefail
BASE="${BASE:-http://localhost:8080}"
CID=$(curl -s -D - -H 'Content-Type: application/json' -H 'X-Debug-User: cli' \
  -d '{"name":"user-games","version":"1.0.0","filterJson":"{\"since\":\"2025-01-01\"}","splitJson":"{\"train\":0.8,\"val\":0.1,\"test\":0.1}","sizeRows":12345,"locationUri":"s3://minio/datasets/user-games-1.0.0"}' \
  "$BASE/v1/datasets" | awk '/^Location:/ {print $2}' | tr -d '\r\n')
curl -s "$BASE${CID}" | jq .
curl -s "$BASE/v1/datasets?page=0&size=5" | jq .
echo "OK"
