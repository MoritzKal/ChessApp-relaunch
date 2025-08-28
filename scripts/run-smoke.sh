#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && cd .. && pwd)"
pushd "$ROOT_DIR" >/dev/null

mkdir -p codex-context
SMOKE_REPORT="codex-context/SMOKE_REPORT.md"
HEALTH_URL="http://localhost:8080/actuator/health"
OPENAPI_URL="http://localhost:8080/v3/api-docs"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl not found" >&2
  exit 1
fi

if curl -fsS "$HEALTH_URL" >/dev/null 2>&1; then
  {
    echo "# Smoke Report"
    echo "Generated: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
    echo
    echo "## /actuator/health"
    curl -fsS "$HEALTH_URL" || true
    echo
    echo "## /v3/api-docs"
    curl -fsS "$OPENAPI_URL" | tee codex-context/openapi.json >/dev/null || true
  } >"$SMOKE_REPORT"
  echo "Wrote $SMOKE_REPORT and updated codex-context/openapi.json"
else
  echo "API not reachable on :8080; aborting" >&2
  exit 2
fi

popd >/dev/null

