#!/usr/bin/env bash
set -Eeuo pipefail

URL="${URL:-http://127.0.0.1:8001}"
PRED="$URL/predict"

echo "== Healthcheck =="
if curl -fsS "$URL/healthz" >/dev/null 2>&1; then
  echo "OK: /healthz"
elif curl -fsS "$URL/health" >/dev/null 2>&1; then
  echo "OK: /health"
else
  echo "WARN: Weder /healthz noch /health erreichbar (non-fatal)."
fi

FENS=(
"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
"rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
"rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
"rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
"r1bqk1nr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 3 4"
"rnbqkbnr/ppp1pppp/8/3p4/2P5/8/PP1PPPPP/RNBQKBNR w KQkq - 0 2"
"8/8/8/8/4k3/8/4K3/8 w - - 0 1"
"r2q1rk1/pp1nbppp/2np1n2/2p1p3/2P1P3/1PNPB1P1/PB1N1PBP/R2Q1RK1 w - - 0 10"
"r3k2r/pppq1ppp/2n1bn2/3p4/3P4/2N1PN2/PPPQ1PPP/R3K2R w KQkq - 0 10"
"r1bq1rk1/ppp2ppp/2n2n2/3pp3/1b1PP3/2N2N2/PPP1BPPP/R1BQ1RK1 w - - 0 7"
)

ok=0; i=0
for fen in "${FENS[@]}"; do
  resp="$(curl -fsS -X POST "$PRED" -H 'Content-Type: application/json' -d "{\"fen\":\"$fen\"}" || true)"
  if [ -z "$resp" ]; then
    echo "[$i] ERROR: keine Antwort"; i=$((i+1)); continue
  fi

  python3 - "$resp" "$fen" <<'PY' || true
import json,sys
resp = json.loads(sys.argv[1])
fen  = sys.argv[2]
move = resp.get("move") or resp.get("best_move") or ""
# Case A: legal ist bool
if isinstance(resp.get("legal"), bool):
    legal = resp["legal"]
# Case B: legal ist Liste der legalen UCI Züge
elif isinstance(resp.get("legal"), list):
    legal = (move in set(resp["legal"]))
else:
    legal = None

print(f"FEN: {fen}")
print(f" move: {move}")
print(f" legal flag: {resp.get('legal')}")
print(f" model: {resp.get('model_id') or resp.get('modelId')} v{resp.get('model_version') or resp.get('modelVersion')}")

if legal is True:
    print(" RESULT: OK\n")
    sys.exit(0)
elif legal is False:
    print(" RESULT: ILLEGAL\n"); sys.exit(1)
else:
    print(" RESULT: UNKNOWN (Feld 'legal' weder bool noch Liste)\n"); sys.exit(2)
PY
  rc=$?
  if [ $rc -eq 0 ]; then ok=$((ok+1)); fi
  i=$((i+1))
done

echo "== Smoke Summary =="
echo "OK: $ok/10"
test $ok -eq 10 || { echo "Smoke nicht komplett grün."; }

echo "== p95 Benchmark (200 calls) =="
python3 - <<'PY' "$PRED"
import json, time, sys, statistics, urllib.request
URL = sys.argv[1]
FENS = [
"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
"rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
"rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
"rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
"r1bqk1nr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 3 4",
"rnbqkbnr/ppp1pppp/8/3p4/2P5/8/PP1PPPPP/RNBQKBNR w KQkq - 0 2",
"8/8/8/8/4k3/8/4K3/8 w - - 0 1",
"r2q1rk1/pp1nbppp/2np1n2/2p1p3/2P1P3/1PNPB1P1/PB1N1PBP/R2Q1RK1 w - - 0 10",
"r3k2r/pppq1ppp/2n1bn2/3p4/3P4/2N1PN2/PPPQ1PPP/R3K2R w KQkq - 0 10",
"r1bq1rk1/ppp2ppp/2n2n2/3pp3/1b1PP3/2N2N2/PPP1BPPP/R1BQ1RK1 w - - 0 7",
]
dur=[]
body=lambda fen: json.dumps({"fen":fen}).encode("utf-8")
for i in range(200):
    fen=FENS[i%len(FENS)]
    t0 = time.perf_counter()
    req = urllib.request.Request(URL, data=body(fen), headers={"Content-Type":"application/json"})
    with urllib.request.urlopen(req) as r: json.load(r)
    dur.append(time.perf_counter()-t0)
dur.sort()
p95 = dur[int(0.95*len(dur))-1]*1000.0
print(f"calls={len(dur)} p95={p95:.1f}ms")
assert p95 <= 150.0, f"p95 too high: {p95:.1f}ms"
PY

echo "== Metrics (nach Traffic) =="
curl -s "$URL/metrics" | grep -E '^chs_predict_(requests_total|latency_seconds|errors_total|cache_hits_total)' || \
  echo "Hinweis: Keine chs_predict_* gefunden – dein Build exportiert evtl. andere/keine Custom-Metriken."
