import json
import logging
import os
import time
from typing import List, Optional
from fastapi import FastAPI, Response

import chess
from fastapi import FastAPI, Request, Response
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from prometheus_client import Counter, Histogram, make_asgi_app
from .metrics_stub import (
    chs_selfplay_games_total,
    chs_selfplay_wins_total,
    chs_selfplay_failures_total,
    chs_dataset_rows,
    chs_dataset_invalid_rows_total,
    chs_dataset_export_duration_seconds,
)

# --- Config ---
ML_S3_ENDPOINT = os.getenv("ML_S3_ENDPOINT", "http://minio:9000")
AWS_REGION = os.getenv("AWS_REGION", "us-east-1")
AWS_ACCESS_KEY_ID = os.getenv("AWS_ACCESS_KEY_ID")
AWS_SECRET_ACCESS_KEY = os.getenv("AWS_SECRET_ACCESS_KEY")
DEFAULT_USERNAME = os.getenv("DEFAULT_USERNAME", "M3NG00S3")
app = FastAPI()
# --- Logging ---
logger = logging.getLogger("serve")
logger.setLevel(logging.INFO)
handler = logging.StreamHandler()
try:
    from python_json_logger import jsonlogger
    handler.setFormatter(jsonlogger.JsonFormatter())
except Exception:
    handler.setFormatter(logging.Formatter("%(message)s"))
logger.handlers = [handler]


def log_event(event: str, **extra):
    payload = {"event": event, "component": "serve"}
    payload.update({k: v for k, v in extra.items() if v is not None})
    logger.info(json.dumps(payload))


# --- Metrics ---
PREDICT_REQUESTS = Counter(
    "chs_predict_requests_total",
    "Total predict requests",
    ["username", "model_id", "status"],
)
PREDICT_LATENCY = Histogram(
    "chs_predict_latency_seconds", "Prediction latency seconds"
)
ILLEGAL_REQUESTS = Counter(
    "chs_predict_illegal_requests_total", "Illegal predict requests"
)


# --- Models ---
class ModelState(BaseModel):
    modelId: str
    modelVersion: str
    artifactUri: Optional[str] = None


class LoadModelRequest(BaseModel):
    modelId: Optional[str] = None
    artifactUri: Optional[str] = None


class PredictRequest(BaseModel):
    fen: str


class PredictResponse(BaseModel):
    move: str
    legal: List[str]
    modelId: str
    modelVersion: str


current_model = ModelState(modelId="dummy", modelVersion="0")

app = FastAPI(title="ChessApp Serve", version="0.1")
app.mount("/metrics", make_asgi_app())

# minimal baseline metric to prevent empty dashboards
chs_dataset_rows.labels(dataset_id="bootstrap").inc()
# Ensure a baseline series exists for self-play metrics so Prometheus queries
# like increase(chs_selfplay_games_total[5m]) return 0 instead of empty result.
chs_selfplay_games_total.labels(result="draw", run_id="bootstrap").inc(0)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/models/load")
def models_load(req: LoadModelRequest):
    global current_model
    if req.artifactUri and req.artifactUri.startswith("s3://"):
        try:
            import boto3

            bucket, key = req.artifactUri[5:].split("/", 1)
            s3 = boto3.client(
                "s3",
                endpoint_url=ML_S3_ENDPOINT,
                aws_access_key_id=AWS_ACCESS_KEY_ID,
                aws_secret_access_key=AWS_SECRET_ACCESS_KEY,
                region_name=AWS_REGION,
            )
            s3.download_file(bucket, key, "/tmp/model.weights")
            current_model = ModelState(
                modelId=req.modelId or "model", modelVersion="1", artifactUri=req.artifactUri
            )
        except Exception:
            current_model = ModelState(modelId="dummy", modelVersion="0")
    else:
        current_model = ModelState(modelId="dummy", modelVersion="0")
    return current_model.dict()


@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest, request: Request, response: Response):
    start = time.perf_counter()
    run_id = request.headers.get("X-Run-Id")
    username = request.headers.get("X-Username", DEFAULT_USERNAME)
    status = "ok"
    try:
        log_event("predict.request", run_id=run_id, username=username, model_id=current_model.modelId)
        try:
            board = chess.Board(fen=req.fen)
        except Exception as e:
            status = "error"
            ILLEGAL_REQUESTS.inc()
            PREDICT_REQUESTS.labels(
                username=username, model_id=current_model.modelId, status=status
            ).inc()
            log_event(
                "predict.failed",
                run_id=run_id,
                username=username,
                model_id=current_model.modelId,
                model_version=current_model.modelVersion,
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

        legal_moves = [m.uci() for m in board.legal_moves]
        if not legal_moves:
            status = "error"
            ILLEGAL_REQUESTS.inc()
            PREDICT_REQUESTS.labels(
                username=username, model_id=current_model.modelId, status=status
            ).inc()
            log_event(
                "predict.failed",
                run_id=run_id,
                username=username,
                model_id=current_model.modelId,
                model_version=current_model.modelVersion,
                reason="INVALID_FEN",
                fen=req.fen,
            )
            return JSONResponse(
                status_code=400,
                content={
                    "error": {
                        "code": "INVALID_FEN",
                        "message": "No legal moves available",
                        "detail": {"fen": req.fen},
                    }
                },
                headers={"X-Component": "serve"},
            )

        move = legal_moves[0]
        response.headers["X-Component"] = "serve"
        PREDICT_REQUESTS.labels(username=username, model_id=current_model.modelId, status=status).inc()
        log_event(
            "predict.completed",
            run_id=run_id,
            username=username,
            model_id=current_model.modelId,
            move=move,
        )
        return {
            "move": move,
            "legal": legal_moves,
            "modelId": current_model.modelId,
            "modelVersion": current_model.modelVersion,
        }
    finally:
        elapsed = time.perf_counter() - start
        PREDICT_LATENCY.observe(elapsed)

try:
    from app.dataset_api import router as dataset_router
    app.include_router(dataset_router)
except Exception:
    # Fail-safe in DEV, damit der Serve trotzdem startet
    pass