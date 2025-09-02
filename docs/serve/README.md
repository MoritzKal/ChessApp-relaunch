# M4 – Serving & Predict (Stand-alone)

**Ziel:** FastAPI-Dienst liefert legale Züge bei niedriger Latenz mit Prometheus-Metriken. **Add-only** – keine Änderungen an `/api/**`, Root-Makefile oder bestehender Compose/Doku.

## Start
```bash
# Build & run
pip install -r ml/serve/requirements.txt  # lokal (optional)
docker compose -f infra/compose.serve.yml up --build -d serve

# Logs
docker compose -f infra/compose.serve.yml logs -f serve

# Stop
docker compose -f infra/compose.serve.yml down --remove-orphans

# Alternativ via Makefile.serve
make -f Makefile.serve serve-up
make -f Makefile.serve serve-bench   # prüft p95 <= 150ms lokal
make -f Makefile.serve serve-down
```

Endpunkte

POST /predict
Request:

{ "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", "history": null, "temperature": 1.0, "topk": null }


Response:

{
  "move": "a2a3",
  "policy": [{"move":"a2a3","prob":0.0833}, {"move":"a2a4","prob":0.0833}, "..."],
  "legal": true,
  "model_id": "dummy-policy",
  "model_version": "0.1.0"
}


GET /metrics – Prometheus-Format; enthält:

chs_predict_requests_total{status="ok|error"}

chs_predict_latency_seconds (Histogram)

chs_predict_errors_total

chs_predict_cache_hits_total

Smoke-FENs (10)
rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1
rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2
rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2
r1bqk1nr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 3 4
rnbqkbnr/ppp1pppp/8/3p4/2P5/8/PP1PPPPP/RNBQKBNR w KQkq - 0 2
8/8/8/8/4k3/8/4K3/8 w - - 0 1
r2q1rk1/pp1nbppp/2np1n2/2p1p3/2P1P3/1PNPB1P1/PB1N1PBP/R2Q1RK1 w - - 0 10
r3k2r/pppq1ppp/2n1bn2/3p4/3P4/2N1PN2/PPPQ1PPP/R3K2R w KQkq - 0 10
r1bq1rk1/ppp2ppp/2n2n2/3pp3/1b1PP3/2N2N2/PPP1BPPP/R1BQ1RK1 w - - 0 7

DoD-Checkliste

 10 FEN-Smoke → POST /predict liefert legale Züge.

 make -f Makefile.serve serve-bench zeigt p95 ≤ 150 ms lokal (CPU).

 curl :8001/metrics zeigt chs_predict_*.

 Keine bestehenden Dateien geändert.
