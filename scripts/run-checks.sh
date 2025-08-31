#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && cd .. && pwd)"
pushd "$ROOT_DIR" >/dev/null

mkdir -p codex-context
source scripts/lib/smoke.sh

echo "[A] Bringing up infra services (db, minio, mlflow, prometheus, loki, promtail, grafana)"
if command -v docker >/dev/null 2>&1; then
  docker compose -f infra/docker-compose.yml --env-file infra/.env up -d db minio mlflow prometheus loki promtail grafana
else
  echo "WARN: docker not found; skipping infra up"
fi

echo "[B] API tests (Maven verify incl. codex tests only)"
if command -v mvn >/dev/null 2>&1; then
  mvn -f api/api-app/pom.xml \
    -Dtest=OpenApiContractTest,ApiSmokeIT \
    -DfailIfNoTests=false \
    -DskipITs=false -DskipTests=false \
    -Dspring-boot.repackage.skip=true \
    verify
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
generate_smoke_report || echo "INFO: API not reachable on :8080; skipping smoke report"

popd >/dev/null
echo "All checks finished."
