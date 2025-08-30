import logging
import os
import time
from typing import List, Optional

import chess
from fastapi import FastAPI, Request, Response
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from prometheus_client import CONTENT_TYPE_LATEST, generate_latest

from .logging_config import bind_context, reset_context, setup_logging
from .metrics import observe_predict, inc_model_loaded, inc_reload_failure
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

setup_logging()
logger = logging.getLogger("serve")


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


current_model = ModelState(modelId="default", modelVersion="0")
inc_model_loaded("default", "0")

app = FastAPI(title="ChessApp Serve", version="0.1")

# baseline metrics to avoid empty queries
chs_dataset_rows.labels(dataset_id="bootstrap").inc()
chs_selfplay_games_total.labels(result="draw", run_id="bootstrap").inc(0)


@app.middleware("http")
async def logging_middleware(request: Request, call_next):
    try:
        try:
            body = await request.json()
        except Exception:
            body = {}
        model_id = (
            body.get("model_id")
            or body.get("modelId")
            or current_model.modelId
            or "default"
        )
        model_version = (
            body.get("model_version")
            or body.get("modelVersion")
            or current_model.modelVersion
            or "0"
        )
        bind_context(
            run_id=request.headers.get("X-Run-Id"),
            dataset_id=request.headers.get("X-Dataset-Id"),
            username=request.headers.get("X-Username", DEFAULT_USERNAME),
            path=request.url.path,
            method=request.method,
            model_id=model_id,
            model_version=model_version,
        )
        try:
            response = await call_next(request)
            bind_context(status=response.status_code)
            logger.info("request")
            return response
        except Exception:
            bind_context(status=500)
            logger.info("request")
            raise
    finally:
        reset_context()


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
                modelId=req.modelId or "model",
                modelVersion="1",
                artifactUri=req.artifactUri,
            )
        except Exception:
            current_model = ModelState(modelId="default", modelVersion="0")
    else:
        current_model = ModelState(modelId="default", modelVersion="0")
    inc_model_loaded(current_model.modelId, current_model.modelVersion)
    return current_model.dict()


def reload_model() -> None:
    """Placeholder for model reload."""
    try:
        # TODO: implement model reload
        pass
    except Exception as exc:  # pragma: no cover - placeholder
        inc_reload_failure(current_model.modelId, current_model.modelVersion, str(exc))
        raise


@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest, response: Response):
    start = time.perf_counter()
    model_id = current_model.modelId or "default"
    model_version = current_model.modelVersion or "0"
    status_code = 200
    error_code = None
    try:
        try:
            board = chess.Board(fen=req.fen)
        except Exception:
            status_code = 400
            return JSONResponse(
                status_code=status_code,
                content={"error": "invalid_fen"},
                headers={"X-Component": "serve"},
            )
        legal_moves = [m.uci() for m in board.legal_moves]
        if not legal_moves:
            status_code = 400
            return JSONResponse(
                status_code=status_code,
                content={"error": "invalid_fen"},
                headers={"X-Component": "serve"},
            )
        move = legal_moves[0]
        response.headers["X-Component"] = "serve"
        return {
            "move": move,
            "legal": legal_moves,
            "modelId": model_id,
            "modelVersion": model_version,
        }
    except Exception:
        status_code = 500
        error_code = "exception"
        raise
    finally:
        ms = (time.perf_counter() - start) * 1000
        observe_predict(ms, model_id, model_version, status_code, error_code)


@app.get("/metrics")
def metrics():
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)


try:
    from app.dataset_api import router as dataset_router

    app.include_router(dataset_router)
except Exception:
    # Fail-safe in DEV, damit der Serve trotzdem startet
    pass
