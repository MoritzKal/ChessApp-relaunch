import json
import logging
import os
import time
from functools import lru_cache
from typing import Dict, List, Optional

import chess
from fastapi import FastAPI, HTTPException, Request, Response
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from prometheus_client import Counter, Histogram, make_asgi_app

from model_loader import MODEL_RELOAD_FAILURES, ModelLoader
from .logging_config import bind_context, reset_context, setup_logging
from .metrics_stub import (
    chs_dataset_export_duration_seconds,
    chs_dataset_invalid_rows_total,
    chs_dataset_rows,
    chs_selfplay_failures_total,
    chs_selfplay_games_total,
    chs_selfplay_wins_total,
)

# --- logging (B3 MDC-style) -------------------------------------
setup_logging()
logger = logging.getLogger("serve")


def log_event(event: str, **extra) -> None:
    payload = {"event": event, "component": "serve"}
    payload.update({k: v for k, v in extra.items() if v is not None})
    logger.info(json.dumps(payload))


# --- metrics (B2 + B3) ------------------------------------------
PREDICT_REQUESTS = Counter(
    "chs_predict_requests_total", "Total predict requests", ["model_id", "model_version"]
)

# B3: include error code label
PREDICT_ERRORS = Counter(
    "chs_predict_errors_total",
    "Total predict errors by code",
    ["model_id", "model_version", "code"],
)

# keep seconds histogram from B2
PREDICT_LATENCY = Histogram(
    "chs_predict_latency_seconds",
    "Prediction latency seconds",
    ["model_id", "model_version"],
    buckets=[0.005, 0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1.0],
)

# switch to histogram in milliseconds for B3 (name preserved)
PREDICT_LATENCY_MS = Histogram(
    "chs_predict_latency_ms",
    "Prediction latency milliseconds",
    ["model_id", "model_version"],
    buckets=[5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000],
)

ILLEGAL_REQUESTS = Counter(
    "chs_predict_illegal_requests_total", "Illegal predict requests", ["model_id", "model_version"]
)
CACHE_HITS = Counter(
    "chs_predict_cache_hits_total", "Predict cache hits", ["model_id", "model_version"]
)
CACHE_MISSES = Counter(
    "chs_predict_cache_misses_total", "Predict cache misses", ["model_id", "model_version"]
)

# baseline metrics so Prometheus queries yield data
chs_dataset_rows.labels(dataset_id="bootstrap").inc()
chs_selfplay_games_total.labels(result="draw", run_id="bootstrap").inc(0)

ENABLE_LRU = os.getenv("SERVE_ENABLE_LRU", "false").lower() == "true"
FAKE_FAST = os.getenv("SERVE_FAKE_FAST", "false").lower() == "true"
DEFAULT_USERNAME = os.getenv("DEFAULT_USERNAME", "M3NG00S3")


def _predict_core_impl(fen: str, model_id: str, model_version: str):
    if FAKE_FAST:
        board = chess.Board(fen=fen)  # still validate FEN
        move = next(iter(board.legal_moves)).uci()
        return move, [move]
    board = chess.Board(fen=fen)
    legal_moves = [m.uci() for m in board.legal_moves]
    if not legal_moves:
        raise ValueError("No legal moves available")
    move = legal_moves[0]
    return move, legal_moves


if ENABLE_LRU:
    @lru_cache(maxsize=128)
    def _cached_predict_core(fen: str, model_id: str, model_version: str):
        CACHE_MISSES.labels(model_id=model_id, model_version=model_version).inc()
        return _predict_core_impl(fen, model_id, model_version)

    def predict_core(fen: str, model_id: str, model_version: str):
        hits_before = _cached_predict_core.cache_info().hits
        result = _cached_predict_core(fen, model_id, model_version)
        if _cached_predict_core.cache_info().hits > hits_before:
            CACHE_HITS.labels(model_id=model_id, model_version=model_version).inc()
        return result

    def _clear_cache() -> None:
        _cached_predict_core.cache_clear()
else:
    def predict_core(fen: str, model_id: str, model_version: str):
        return _predict_core_impl(fen, model_id, model_version)

    def _clear_cache() -> None:
        pass


# --- app setup ---------------------------------------------------
app = FastAPI(title="ChessApp Serve", version="0.1")
app.mount("/metrics", make_asgi_app())

loader = ModelLoader(invalidate_cache=_clear_cache)
# ensure a model is always active, even if artifact is missing
loader.load("dummy", "0")


# --- middleware (B3 request MDC binding) ------------------------
@app.middleware("http")
async def logging_middleware(request: Request, call_next):
    try:
        try:
            body = await request.json()
        except Exception:
            body = {}
        # Prefer requested model (if provided), else active loader model
        try:
            active_mid, active_ver, _ = loader.get_active()
        except Exception:
            active_mid, active_ver = "default", "0"

        model_id = body.get("model_id") or body.get("modelId") or active_mid or "default"
        model_version = (
                body.get("model_version") or body.get("modelVersion") or active_ver or "0"
        )
        # make labels available to handlers
        try:
            request.state.model_id = model_id
            request.state.model_version = model_version
        except Exception:
            pass

        bind_context(
            run_id=request.headers.get("X-Run-Id"),
            dataset_id=request.headers.get("X-Dataset-Id"),
            username=request.headers.get("X-Username", DEFAULT_USERNAME),
            path=str(request.url.path),
            method=request.method,
            model_id=model_id,
            model_version=model_version,
        )
        response = await call_next(request)
        bind_context(status=getattr(response, "status_code", 200))
        logger.info("request")
        return response
    except Exception:
        bind_context(status=500)
        logger.info("request")
        raise
    finally:
        reset_context()


# --- pydantic models --------------------------------------------
class PredictRequest(BaseModel):
    fen: str


class PredictResponse(BaseModel):
    move: str
    legal: List[str]
    modelId: str
    modelVersion: str


class ModelsLoadRequest(BaseModel):
    modelId: str
    modelVersion: str


class ModelsLoadResponse(BaseModel):
    ok: bool
    active: Dict[str, str]


# --- endpoints ---------------------------------------------------
@app.get("/health")
def health() -> Dict[str, str]:
    return {"status": "ok"}


@app.post("/models/load", response_model=ModelsLoadResponse)
def models_load(body: ModelsLoadRequest) -> ModelsLoadResponse:
    try:
        loader.load(body.modelId, body.modelVersion)
        artifact = os.path.join(loader.root, body.modelId, body.modelVersion, "best.pt")
        if not os.path.exists(artifact):
            # ModelLoader should increment failure metric for missing artifact
            raise HTTPException(status_code=404, detail="missing_artifact")
        mid, ver, _ = loader.get_active()
        return ModelsLoadResponse(ok=True, active={"modelId": mid, "modelVersion": ver})
    except HTTPException:
        raise
    except Exception as exc:  # pragma: no cover - defensive
        MODEL_RELOAD_FAILURES.labels(reason="internal").inc()
        raise HTTPException(status_code=500, detail="internal_error") from exc


@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest, request: Request, response: Response) -> PredictResponse:
    start = time.perf_counter()
    run_id = request.headers.get("X-Run-Id")
    username = request.headers.get("X-Username", DEFAULT_USERNAME)
    mid, ver, _ = loader.get_active()
    labels = {"model_id": mid, "model_version": ver}

    log_event(
        "predict.request",
        run_id=run_id,
        username=username,
        fen=req.fen,
        model_id=mid,
        model_version=ver,
    )
    PREDICT_REQUESTS.labels(**labels).inc()

    try:
        move, legal_moves = predict_core(req.fen, mid, ver)
        response.headers["X-Component"] = "serve"
        elapsed = time.perf_counter() - start
        log_event(
            "predict.completed",
            run_id=run_id,
            username=username,
            model_id=mid,
            model_version=ver,
            top_move=move,
            lat_ms=elapsed * 1000,
        )
        return PredictResponse(move=move, legal=legal_moves, modelId=mid, modelVersion=ver)

    except ValueError as e:
        # Invalid FEN / no legal moves -> 400
        PREDICT_ERRORS.labels(code="400", **labels).inc()
        ILLEGAL_REQUESTS.labels(**labels).inc()
        log_event(
            "predict.failed",
            run_id=run_id,
            username=username,
            model_id=mid,
            model_version=ver,
            reason="INVALID_FEN",
            fen=req.fen,
        )
        return JSONResponse(
            status_code=400,
            content={
                "error": {
                    "code": "INVALID_FEN",
                    "message": str(e),
                    "detail": {"fen": req.fen},
                }
            },
            headers={"X-Component": "serve"},
        )

    except Exception:
        # Server-side exception -> 500
        PREDICT_ERRORS.labels(code="exception", **labels).inc()
        log_event(
            "predict.failed",
            run_id=run_id,
            username=username,
            model_id=mid,
            model_version=ver,
            reason="EXCEPTION",
            fen=req.fen,
        )
        raise HTTPException(status_code=500, detail="internal_error")

    finally:
        elapsed = time.perf_counter() - start
        PREDICT_LATENCY.labels(**labels).observe(elapsed)
        PREDICT_LATENCY_MS.labels(**labels).observe(elapsed * 1000)


# Optional dataset API router
try:  # pragma: no cover - optional
    from app.dataset_api import router as dataset_router

    app.include_router(dataset_router)
except Exception:  # pragma: no cover - non-critical for tests
    pass
