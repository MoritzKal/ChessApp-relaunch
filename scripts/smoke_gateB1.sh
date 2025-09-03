#!/usr/bin/env bash
set -euo pipefail

echo "==> Compose up (B1)"
docker compose -f infra/docker-compose.yml -f infra/compose.dev.yml up -d --build

echo "==> Warte auf API-Health (ohne Auth)"
for i in {1..60}; do
  if curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
    echo "Health UP (B1)"
    exit 0
  fi
  sleep 2
done

echo "Health check failed after 120s"
docker compose -f infra/docker-compose.yml -f infra/compose.dev.yml logs api
exit 1
