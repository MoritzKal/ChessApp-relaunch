#!/usr/bin/env bash
set -euo pipefail

# generate_smoke_report [health_url] [openapi_url] [output_file]
# Generates a simple smoke report if API is reachable.
generate_smoke_report() {
  local health_url="${1:-http://localhost:8080/actuator/health}"
  local openapi_url="${2:-http://localhost:8080/v3/api-docs}"
  local output="${3:-codex-context/SMOKE_REPORT.md}"

  if ! command -v curl >/dev/null 2>&1; then
    echo "curl not found" >&2
    return 1
  fi

  if curl -fsS "$health_url" >/dev/null 2>&1; then
    {
      echo "# Smoke Report"
      echo "Generated: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
      echo
      echo "## /actuator/health"
      curl -fsS "$health_url" || true
      echo
      echo "## /v3/api-docs"
      curl -fsS "$openapi_url" | tee codex-context/openapi.json >/dev/null || true
    } >"$output"
    echo "Wrote $output and updated codex-context/openapi.json"
  else
    echo "API not reachable on :8080; aborting" >&2
    return 2
  fi
}
