#!/usr/bin/env bash
# Minimal A2 verification script
# Run from project root (where infra/docker-compose.yml lives)
set -euo pipefail

# Prevent MSYS (Git-Bash on Windows) from rewriting /app paths
export MSYS_NO_PATHCONV=1
export MSYS2_ARG_CONV_EXCL="*"

DATASET_ID="${DATASET_ID:-ds_v0_local}"
PROM_HOST="${PROM_HOST:-localhost}"
PORT_ML="${PORT_ML:-8000}"

# --- Compose wrapper (v2 or v1) ---
if docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose -f infra/docker-compose.yml)
else
  COMPOSE=(docker-compose -f infra/docker-compose.yml)
fi

echo "==> Using compose: ${COMPOSE[*]}"

# --- Ensure ML container is up ---
"${COMPOSE[@]}" up -d --build ml

# --- Get container id ---
CID="$("${COMPOSE[@]}" ps -q ml)"
if [[ -z "$CID" ]]; then
  echo "✗ Could not get ml container id"; exit 2
fi
echo "→ ml container: $CID"

# --- Wait for health endpoint ---
printf '→ Waiting for ML health at http://%s:%s/health\n' "$PROM_HOST" "$PORT_ML"
for i in $(seq 1 60); do
  if curl -fsS "http://${PROM_HOST}:${PORT_ML}/health" >/dev/null; then
    echo "✓ ML healthy"
    break
  fi
  sleep 2
  [[ "$i" == 60 ]] && { echo "✗ ML not healthy"; exit 3; }
done

# --- Endpoint existence check (Swagger) ---
if ! curl -fsS "http://${PROM_HOST}:${PORT_ML}/openapi.json" | grep -q '/internal/dataset/metrics'; then
  echo "✗ Endpoint /internal/dataset/metrics not found in OpenAPI."
  echo "  Ensure the metrics router is integrated (or apply hot-patch) and restart ml."
  exit 4
fi
echo "✓ Found /internal/dataset/metrics"

# --- Ensure sample data exists in container ---
echo "→ Ensuring sample data exists in container at /app/ml/data/sample"
if [[ -d "data/sample" ]]; then
  docker cp "data/sample" "${CID}:/app/ml/data"
else
  "${COMPOSE[@]}" exec -T ml sh -lc 'mkdir -p /app/ml/data/sample && printf "%s\n" "{\"game_id\":\"g1\",\"ply\":1,\"fen\":\"rn1qkbnr/ppp1pppp/8/3p4/3P4/5N2/PPP1PPPP/RNBQKB1R w KQkq - 1 3\",\"uci\":\"e2e4\",\"color\":\"white\",\"result\":\"white\"}" > /app/ml/data/sample/sample.jsonl'
fi
echo "✓ Sample ready"

# --- Run export with metrics push ---
echo "→ Running dataset_export (pushes metrics)"
"${COMPOSE[@]}" exec -T ml sh -lc 'python ml/tools/dataset_export.py \
  --input /app/ml/data/sample \
  --output /app/ml/out/a2/compact.parquet \
  --dataset-id '"${DATASET_ID}"' \
  --manifest /app/ml/out/a2/manifest.json \
  --push-metrics http://ml:8000/internal/dataset/metrics'

# --- Check metrics on host ---
echo "→ Checking /metrics for dataset counters/summaries"
METRICS="$(curl -fsS "http://${PROM_HOST}:${PORT_ML}/metrics")" || { echo "✗ failed to fetch /metrics"; exit 5; }

echo "$METRICS" | grep -q '^chs_dataset_rows_total'         || { echo "✗ chs_dataset_rows_total missing"; exit 6; }
echo "$METRICS" | grep -q '^chs_dataset_invalid_rows_total'  || { echo "✗ chs_dataset_invalid_rows_total missing"; exit 7; }
echo "$METRICS" | grep -q '^chs_dataset_export_duration_ms'  || { echo "✗ chs_dataset_export_duration_ms missing"; exit 8; }

echo "✓ Metrics present. Quick values:"
echo "$METRICS" | grep -E '^chs_dataset_rows_total|^chs_dataset_invalid_rows_total|^chs_dataset_export_duration_ms_(sum|count)' | sed -n '1,200p'

# --- Pull artifacts to host (optional) ---
echo "→ Pulling artifacts to host ./out/a2"
mkdir -p out/a2
docker cp "${CID}:/app/ml/out/a2/." out/a2 || true

echo "✅ Done. See ./out/a2 and http://${PROM_HOST}:${PORT_ML}/metrics"
