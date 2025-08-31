#!/usr/bin/env bash
# Minimal end-to-end smoke covering load, predict and metrics
set -euo pipefail

export E2E_OFFLINE=${E2E_OFFLINE:-1}
MODEL_ID="${MODEL_ID:-e2e_model}"
MODEL_VERSION="${MODEL_VERSION:-v0}"
PORT="${PORT:-8001}"
export SERVE_MODEL_ROOT="$(pwd)/artifacts"

# Ingest (synthetic)
python - <<'PY'
from pathlib import Path
import json
user = "e2e_user"
base = Path(f"data/raw/{user}")
base.mkdir(parents=True, exist_ok=True)
sample = {"fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", "move": "e2e4"}
(base / "games.jsonl").write_text(json.dumps(sample)+"\n")
PY

# Training (stub)
python - <<PY
from pathlib import Path
import json
root = Path("artifacts") / "${MODEL_ID}" / "${MODEL_VERSION}"
root.mkdir(parents=True, exist_ok=True)
(root/"model.pt").write_text("fake model")
(root/"best.pt").write_text("fake model")
(root/"metrics.json").write_text(json.dumps({"acc":0.0}))
PY

# Start serve
python -m uvicorn serve.app.main:app --port "$PORT" >/tmp/e2e_serve.log 2>&1 &
SERVE_PID=$!
trap 'kill $SERVE_PID >/dev/null 2>&1 || true' EXIT
for i in {1..10}; do
  curl -s "http://localhost:${PORT}/health" >/dev/null 2>&1 && break
  sleep 1
done

# Load model (seed if missing)
LOAD_PAYLOAD="{\"modelId\":\"$MODEL_ID\",\"modelVersion\":\"$MODEL_VERSION\"}"
code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:${PORT}/models/load" -H "Content-Type: application/json" -d "$LOAD_PAYLOAD")
if [ "$code" != "200" ]; then
  python - <<PY
from pathlib import Path
root = Path("artifacts")/"${MODEL_ID}"/"${MODEL_VERSION}"
root.mkdir(parents=True, exist_ok=True)
(root/"best.pt").write_text("fake model")
PY
  curl -sSf -X POST "http://localhost:${PORT}/models/load" -H "Content-Type: application/json" -d "$LOAD_PAYLOAD"
fi

# Predict valid
curl -sSf -X POST "http://localhost:${PORT}/predict" -H "Content-Type: application/json" \
  -d '{"fen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"}' >/dev/null

# Predict invalid (expect 400)
code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:${PORT}/predict" -H "Content-Type: application/json" -d '{"fen":"invalid"}')
[ "$code" = "400" ]

# Metrics check
curl -sSf "http://localhost:${PORT}/metrics" | grep chs_predict_latency_ms_bucket >/dev/null

kill $SERVE_PID
wait $SERVE_PID 2>/dev/null || true
