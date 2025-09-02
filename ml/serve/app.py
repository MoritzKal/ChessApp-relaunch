from __future__ import annotations
import time
from collections import OrderedDict
from typing import List, Optional

import chess
from fastapi import FastAPI, HTTPException, Response
from pydantic import BaseModel, Field
from prometheus_client import Counter, Histogram, generate_latest, CONTENT_TYPE_LATEST

from .model_loader import load_model, MODEL_ID, MODEL_VERSION

app = FastAPI(title="ChessApp Serving", version="0.1.0")
model = load_model()

# --- Prometheus metrics ---
REQ_TOTAL = Counter("chs_predict_requests_total", "Total predict requests", labelnames=("status",))
ERR_TOTAL = Counter("chs_predict_errors_total", "Total predict errors")
CACHE_HITS = Counter("chs_predict_cache_hits_total", "Cache hits for /predict")
LATENCY = Histogram(
    "chs_predict_latency_seconds",
    "Prediction latency seconds",
    buckets=(0.001, 0.005, 0.01, 0.025, 0.05, 0.1, 0.15, 0.25, 0.5, 1.0),
)

# --- Tiny LRU cache for FEN -> (move, policy) ---
class LRU:
    def __init__(self, maxsize: int = 1024):
        self.maxsize = maxsize
        self._d: OrderedDict[str, tuple[str, list[tuple[str, float]]]] = OrderedDict()

    def get(self, key: str):
        if key in self._d:
            self._d.move_to_end(key)
            return self._d[key]
        return None

    def set(self, key: str, val):
        self._d[key] = val
        self._d.move_to_end(key)
        if len(self._d) > self.maxsize:
            self._d.popitem(last=False)

CACHE = LRU(maxsize=2048)

# --- Schemas ---
class PredictRequest(BaseModel):
    fen: str = Field(..., description="Position in Forsyth–Edwards Notation")
    history: Optional[List[str]] = Field(default=None, description="Optional SAN/uci move list")
    temperature: Optional[float] = Field(default=1.0, ge=0.0)
    topk: Optional[int] = Field(default=None, ge=1)

class PolicyItem(BaseModel):
    move: str
    prob: float

class PredictResponse(BaseModel):
    move: str
    policy: List[PolicyItem]
    legal: bool
    model_id: str
    model_version: str

# --- Routes ---
@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest):
    t0 = time.perf_counter()
    try:
        # Basic FEN validation
        try:
            board = chess.Board(req.fen)
        except Exception as e:
            ERR_TOTAL.inc()
            REQ_TOTAL.labels("error").inc()
            raise HTTPException(status_code=422, detail=f"Invalid FEN: {e}")

        cached = CACHE.get(req.fen)
        if cached:
            CACHE_HITS.inc()
            best_move, raw_policy = cached
        else:
            best_move, raw_policy = model.predict(req.fen, req.temperature, req.topk)
            CACHE.set(req.fen, (best_move, raw_policy))

        legal_moves = {m.uci() for m in board.legal_moves}
        legal = best_move in legal_moves
        if not legal:
            ERR_TOTAL.inc()
            REQ_TOTAL.labels("error").inc()
            raise HTTPException(status_code=500, detail="Model returned illegal move.")

        policy = [PolicyItem(move=m, prob=p) for (m, p) in raw_policy]
        resp = PredictResponse(
            move=best_move,
            policy=policy,
            legal=True,
            model_id=MODEL_ID,
            model_version=MODEL_VERSION,
        )
        return resp
    finally:
        LATENCY.observe(time.perf_counter() - t0)
        # If we got here without exception, count as ok; FastAPI exception will skip this path
        # We guard via try/finally above; increment ok only on success:
        # (FastAPI calls finally regardless; we can't know status here, so wrap in middleware if needed.
        # For simplicity, increment in dependency below.)

# Use middleware-like dependency to count success after handler return
@app.middleware("http")
async def _count_ok(req, call_next):
    resp = await call_next(req)
    if req.url.path == "/predict":
        if 200 <= resp.status_code < 400:
            REQ_TOTAL.labels("ok").inc()
        else:
            REQ_TOTAL.labels("error").inc()
    return resp

@app.get("/metrics")
def metrics():
    data = generate_latest()
    return Response(content=data, media_type=CONTENT_TYPE_LATEST)

# Optional: quick health check by scraping metrics
@app.get("/healthz")
def healthz():
    return {"status": "ok"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("ml.serve.app:app", host="0.0.0.0", port=8001, reload=False)
