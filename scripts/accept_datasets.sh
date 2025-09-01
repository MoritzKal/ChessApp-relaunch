#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TOKEN="${TOKEN:-test-token}"   # SEC-01 Test-Token
AUTH_HEADER="Authorization: Bearer ${TOKEN}"

jq -V >/dev/null 2>&1 || { echo "jq ist erforderlich"; exit 1; }

echo "== Create =="
CREATE_PAYLOAD='{"name":"mini-ds","filter":{"keep":["rated"]},"split":{"train":0.8,"val":0.2}}'
resp=$(curl -fsS -X POST "$BASE_URL/v1/datasets" \
  -H "Content-Type: application/json" -H "$AUTH_HEADER" \
  -d "$CREATE_PAYLOAD")
id=$(echo "$resp" | jq -r '.id')
test "$id" != "null"

loc=$(curl -i -s -X POST "$BASE_URL/v1/datasets" \
  -H "Content-Type: application/json" -H "$AUTH_HEADER" \
  -d "$CREATE_PAYLOAD" | awk '/^Location:/ {print $2}' | tr -d '\r')
test -n "$loc"

echo "== Get by id =="
got=$(curl -fsS -H "$AUTH_HEADER" "$BASE_URL/v1/datasets/$id")
test "$(echo "$got" | jq -r '.id')" = "$id"

echo "== List (paged, createdAt desc) =="
list=$(curl -fsS -H "$AUTH_HEADER" "$BASE_URL/v1/datasets?page=0&size=5&sort=createdAt,desc")
echo "$list" | jq -e '.content | length >= 1' >/dev/null

echo "== OpenAPI exposes 3 endpoints =="
api=$(curl -fsS -H "$AUTH_HEADER" "$BASE_URL/v3/api-docs")
paths=$(echo "$api" | jq -r '.paths | keys[]' | grep '^/v1/datasets' | wc -l | tr -d ' ')
test "$paths" -ge 3

echo "== Negative: missing auth =="
set +e
code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/v1/datasets")
set -e
[[ "$code" == "401" || "$code" == "403" ]]

echo "ALL CHECKS PASSED ✅"
