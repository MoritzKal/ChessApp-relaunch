#!/usr/bin/env bash
set -euo pipefail

URL="${1:-http://localhost:8001/predict}"
BODY='{"fen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"}'

wrk -t2 -c100 -d5s -s <(cat <<'LUA'
wrk.method = "POST"
wrk.body   = os.getenv("BODY")
wrk.headers["Content-Type"] = "application/json"
LUA
) "$URL" | tee /tmp/wrk.out

grep '95%' /tmp/wrk.out || true
