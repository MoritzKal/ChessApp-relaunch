#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && cd .. && pwd)"
pushd "$ROOT_DIR" >/dev/null

mkdir -p codex-context

echo "[A] Bringing up infra services (db, minio, mlflow, prometheus, loki, promtail, grafana)"
if command -v docker >/dev/null 2>&1; then
  docker compose -f infra/docker-compose.yml --env-file infra/.env up -d db minio mlflow prometheus loki promtail grafana
else
  echo "WARN: docker not found; skipping infra up"
fi

echo "[B] API tests (Maven verify incl. ITs)"
if command -v mvn >/dev/null 2>&1; then
  mvn -f api/api-app/pom.xml -DskipITs=false -DskipTests=false verify
else
  echo "WARN: mvn not found; skipping API tests"
fi

echo "[C] Frontend tests (if present)"
if [ -f "frontend/package.json" ]; then
  if command -v npm >/dev/null 2>&1; then
    npm --prefix frontend ci
    npm --prefix frontend test --if-present || echo "WARN: frontend tests failed or not present"
  else
    echo "WARN: npm not found; skipping frontend"
  fi
else
  echo "INFO: frontend/package.json not found; skipping"
fi

echo "[D] ML tests (pytest)"
if [ -d "ml/tests" ]; then
  if command -v python >/dev/null 2>&1; then
    if python -c "import pytest" >/dev/null 2>&1; then
      python -m pytest -q ml/tests || echo "WARN: ML tests had failures"
    else
      echo "INFO: pytest not installed; skipping ML tests"
    fi
  else
    echo "WARN: python not found; skipping ML tests"
  fi
else
  echo "INFO: ml/tests not found; skipping"
fi

echo "[E] Smoke (optional) if API running on :8080"
SMOKE_REPORT="codex-context/SMOKE_REPORT.md"
HEALTH_URL="http://localhost:8080/actuator/health"
OPENAPI_URL="http://localhost:8080/v3/api-docs"

if command -v curl >/dev/null 2>&1; then
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
    echo "INFO: API not reachable on :8080; skipping smoke report"
  fi
else
  echo "WARN: curl not found; skipping smoke"
fi

popd >/dev/null
echo "All checks finished."

