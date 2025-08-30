import json
import logging
import os
import time
from typing import Dict, List

import chess
from fastapi import FastAPI, HTTPException, Request, Response
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from prometheus_client import Counter, Histogram, Summary, make_asgi_app

from serve.model_loader import MODEL_RELOAD_FAILURES, ModelLoader
from .metrics_stub import (
    chs_dataset_export_duration_seconds,
    chs_dataset_invalid_rows_total,
    chs_dataset_rows,
    chs_selfplay_failures_total,
    chs_selfplay_games_total,
    chs_selfplay_wins_total,
)

# --- logging -----------------------------------------------------
logger = logging.getLogger("serve")
logger.setLevel(logging.INFO)
handler = logging.StreamHandler()
try:  # pragma: no cover - optional dependency
    from python_json_logger import jsonlogger

    handler.setFormatter(jsonlogger.JsonFormatter())
except Exception:  # pragma: no cover - fallback
    handler.setFormatter(logging.Formatter("%(message)s"))
logger.handlers = [handler]


def log_event(event: str, **extra) -> None:
    payload = {"event": event, "component": "serve"}
    payload.update({k: v for k, v in extra.items() if v is not None})
    logger.info(json.dumps(payload))


# --- metrics -----------------------------------------------------
PREDICT_REQUESTS = Counter(
    "chs_predict_requests_total", "Total predict requests", ["model_id", "model_version"]
)
PREDICT_ERRORS = Counter(
    "chs_predict_errors_total", "Total predict errors", ["model_id", "model_version"]
)
PREDICT_LATENCY = Histogram(
    "chs_predict_latency_seconds", "Prediction latency seconds", ["model_id", "model_version"]
)
PREDICT_LATENCY_MS = Summary(
    "chs_predict_latency_ms", "Prediction latency milliseconds", ["model_id", "model_version"]
)
ILLEGAL_REQUESTS = Counter(
    "chs_predict_illegal_requests_total", "Illegal predict requests", ["model_id", "model_version"]
)

# baseline metrics so Prometheus queries yield zero instead of empty
chs_dataset_rows.labels(dataset_id="bootstrap").inc()
chs_selfplay_games_total.labels(result="draw", run_id="bootstrap").inc(0)

# --- app setup ---------------------------------------------------
app = FastAPI(title="ChessApp Serve", version="0.1")
app.mount("/metrics", make_asgi_app())

loader = ModelLoader()
# ensure a model is always active, even if artifact is missing
loader.load("dummy", "0")

DEFAULT_USERNAME = os.getenv("DEFAULT_USERNAME", "M3NG00S3")


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
        artifact = os.path.join(
            loader.root, body.modelId, body.modelVersion, "best.pt"
        )
        if not os.path.exists(artifact):
            raise HTTPException(status_code=404, detail="missing_artifact")
        mid, ver, _ = loader.get_active()
        return ModelsLoadResponse(ok=True, active={"modelId": mid, "modelVersion": ver})
    except HTTPException:
        # ModelLoader already incremented failure metric for missing artifact
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
        board = chess.Board(fen=req.fen)
        legal_moves = [m.uci() for m in board.legal_moves]
        if not legal_moves:
            raise ValueError("No legal moves available")
        move = legal_moves[0]
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
        return PredictResponse(
            move=move, legal=legal_moves, modelId=mid, modelVersion=ver
        )
    except Exception as e:
        PREDICT_ERRORS.labels(**labels).inc()
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
