import os, time, json, uuid, logging
from datetime import datetime
from typing import Optional, Dict, Any

from fastapi import FastAPI, BackgroundTasks, HTTPException, Response, APIRouter
from pydantic import BaseModel, Field
from prometheus_client import Counter, Gauge, Histogram, CONTENT_TYPE_LATEST, CollectorRegistry, generate_latest, REGISTRY
import mlflow
from .metrics_registry import get_registry

try:
    import boto3
    from botocore.config import Config as BotoConfig
except Exception:
    boto3 = None

# --- Config (ENV) ---
ML_S3_ENDPOINT = os.getenv("ML_S3_ENDPOINT", "http://minio:9000")
AWS_REGION = os.getenv("AWS_REGION", "us-east-1")
AWS_ACCESS_KEY_ID = os.getenv("AWS_ACCESS_KEY_ID")
AWS_SECRET_ACCESS_KEY = os.getenv("AWS_SECRET_ACCESS_KEY")
MLFLOW_TRACKING_URI = os.getenv("MLFLOW_TRACKING_URI", "file:///tmp/mlruns")
DEFAULT_USERNAME = os.getenv("DEFAULT_USERNAME", "M3NG00S3")
ARTIFACT_BUCKET = os.getenv("ML_ARTIFACT_BUCKET", "mlflow")

mlflow.set_tracking_uri(MLFLOW_TRACKING_URI)

# --- Logging (JSON) ---
logger = logging.getLogger("ml")
logger.setLevel(logging.INFO)
handler = logging.StreamHandler()
try:
    from python_json_logger import jsonlogger
    handler.setFormatter(jsonlogger.JsonFormatter())
except Exception:
    handler.setFormatter(logging.Formatter('%(message)s'))
logger.handlers = [handler]

def log_event(event: str, **extra):
    payload = {"ts": datetime.utcnow().isoformat() + "Z", "event": event, "component": "training", "username": DEFAULT_USERNAME}
    payload.update({k: v for k, v in extra.items() if v is not None})
    logger.info(json.dumps(payload))

# --- Prometheus registry/metrics ---

IS_MULTIPROC = bool(os.environ.get("PROMETHEUS_MULTIPROC_DIR"))
try:
    # Only defined in single-process mode
    from .metrics_registry import REGISTRY as _SINGLE_REGISTRY  # type: ignore
except Exception:
    _SINGLE_REGISTRY = None

_metric_kwargs = {"registry": _SINGLE_REGISTRY} if _SINGLE_REGISTRY is not None else {}

RUNS_TOTAL = Counter("chs_training_runs_total", "Total training runs started",
                     ["username", "component"], **_metric_kwargs)
LOSS = Gauge("chs_training_loss", "Training loss (last step)",
             ["run_id", "dataset_id", "username", "component"], **_metric_kwargs)
VAL_ACC = Gauge("chs_training_val_accuracy", "Validation accuracy (last step)",
                ["run_id", "dataset_id", "username", "component"], **_metric_kwargs)
STEP_SEC = Histogram("chs_training_step_duration_seconds", "Per-step duration seconds",
                     ["run_id", "dataset_id", "username", "component"], **_metric_kwargs)

# --- In-memory run store (MVP) ---
class RunState(BaseModel):
    runId: str
    datasetId: Optional[str] = None
    status: str = "queued"
    currentEpoch: int = 0
    epochs: int = 10
    stepsPerEpoch: int = 50
    lr: float = 1e-3
    metrics: Dict[str, float] = Field(default_factory=dict)
    startedAt: Optional[str] = None
    finishedAt: Optional[str] = None
    artifactUris: Dict[str, str] = Field(default_factory=dict)
    mlflowRunId: Optional[str] = None

RUNS: Dict[str, RunState] = {}

# --- Schemas ---
class TrainRequest(BaseModel):
    runName: Optional[str] = None
    runId: Optional[str] = None
    datasetId: Optional[str] = None
    epochs: int = 10
    stepsPerEpoch: int = 50
    lr: float = 1e-3

app = FastAPI(title="ChessApp ML", version="0.1")
router = APIRouter(prefix="/internal", tags=["internal"])

@app.get("/health")
def health():
    return {"status": "ok"}

@app.get("/metrics")
@app.get("/metrics/")
def metrics():
    # Expose metrics from the appropriate registry.
    # - Multi-process: aggregate from default via MultiProcessCollector
    # - Single-process: include both our custom registry and the default one
    if IS_MULTIPROC:
        data = generate_latest(get_registry())
    else:
        reg_training = get_registry()
        data = generate_latest(reg_training) + generate_latest(REGISTRY)
    return Response(data, media_type=CONTENT_TYPE_LATEST)

# Ensure baseline self-play counter exists so Prometheus queries don't return empty.
try:
    from .metrics_registry import chs_selfplay_games_total, labelset
    chs_selfplay_games_total.labels(**labelset(run_id="bootstrap", policy="baseline", username=DEFAULT_USERNAME, component="ml")).inc(0)
except Exception:
    pass

# Internal dataset metrics endpoint (A2)
try:
    from ml.service.metrics_dataset import MetricsPayload, observe as observe_dataset_metrics

    @router.post("/dataset/metrics")
    def dataset_metrics(payload: MetricsPayload):
        observe_dataset_metrics(payload)
        return {"ok": True}
    app.include_router(router)
except Exception as _e:
    # If metrics module not available, we simply don't expose the route.
    pass

@app.post("/train")
def train(req: TrainRequest, bg: BackgroundTasks):
    run_id = req.runId or str(uuid.uuid4())
    if run_id in RUNS and RUNS[run_id].status in ("running", "queued"):
        return {"runId": run_id}
    state = RunState(
        runId=run_id, datasetId=req.datasetId, status="running",
        epochs=req.epochs, stepsPerEpoch=req.stepsPerEpoch, lr=req.lr,
        startedAt=datetime.utcnow().isoformat()+"Z"
    )
    RUNS[run_id] = state
    RUNS_TOTAL.labels(DEFAULT_USERNAME, "ml").inc()
    log_event("training.started", run_id=run_id, dataset_id=req.datasetId, lr=req.lr, epochs=req.epochs)

    bg.add_task(_training_loop, state, req.runName)
    return {"runId": run_id}

@app.get("/runs/{run_id}")
def run_status(run_id: str):
    state = RUNS.get(run_id)
    if not state:
        raise HTTPException(status_code=404, detail="Run not found")
    return state

# --- Helpers ---
def _upload_artifacts(run_id: str, report: Dict[str, Any]) -> Dict[str, str]:
    uris = {}
    try:
        best_bytes = b"DUMMY_WEIGHTS"
        report_bytes = json.dumps(report, indent=2).encode("utf-8")

        if boto3 and AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY:
            s3 = boto3.client("s3",
                endpoint_url=ML_S3_ENDPOINT,
                aws_access_key_id=AWS_ACCESS_KEY_ID,
                aws_secret_access_key=AWS_SECRET_ACCESS_KEY,
                region_name=AWS_REGION,
                config=BotoConfig(signature_version="s3v4"))
            key_w = f"models/{run_id}/best.pt"
            key_r = f"reports/{run_id}/training_report.json"
            s3.put_object(Bucket=ARTIFACT_BUCKET, Key=key_w, Body=best_bytes, ContentType="application/octet-stream")
            s3.put_object(Bucket=ARTIFACT_BUCKET, Key=key_r, Body=report_bytes, ContentType="application/json")
            uris["weights"] = f"s3://{ARTIFACT_BUCKET}/{key_w}"
            uris["report"] = f"s3://{ARTIFACT_BUCKET}/{key_r}"
        else:
            os.makedirs(f"/tmp/{run_id}", exist_ok=True)
            with open(f"/tmp/{run_id}/best.pt","wb") as f: f.write(best_bytes)
            with open(f"/tmp/{run_id}/training_report.json","wb") as f: f.write(report_bytes)
            uris["weights"] = f"file:///tmp/{run_id}/best.pt"
            uris["report"] = f"file:///tmp/{run_id}/training_report.json"
    except Exception as e:
        log_event("training.artifact_upload_failed", run_id=run_id, error=str(e))
    return uris

def _training_loop(state: RunState, run_name: Optional[str]):
    labels = {"run_id": state.runId, "dataset_id": state.datasetId or "n/a", "username": DEFAULT_USERNAME, "component": "ml"}
    mlflow_run_id = None
    try:
        mlflow.set_experiment("chessapp_training")
        with mlflow.start_run(run_name=run_name or f"run-{state.runId}") as run:
            mlflow_run_id = run.info.run_id
            mlflow.log_params({"epochs": state.epochs, "steps_per_epoch": state.stepsPerEpoch, "lr": state.lr,
                               "run_id": state.runId, "dataset_id": state.datasetId or ""})
            mlflow.set_tags({"component":"training","run_id":state.runId,"username":DEFAULT_USERNAME})

            loss, acc = 1.0, 0.1
            for epoch in range(1, state.epochs + 1):
                state.currentEpoch = epoch
                for step in range(1, state.stepsPerEpoch + 1):
                    t0 = time.perf_counter()
                    loss = max(0.01, loss * 0.97)
                    acc = min(0.999, acc + 0.005)

                    state.metrics = {"loss": float(round(loss, 6)), "val_acc": float(round(acc, 6))}
                    LOSS.labels(**labels).set(state.metrics["loss"])
                    VAL_ACC.labels(**labels).set(state.metrics["val_acc"])
                    dt = time.perf_counter() - t0
                    STEP_SEC.labels(**labels).observe(dt)

                    global_step = (epoch - 1) * state.stepsPerEpoch + step
                    mlflow.log_metrics({"loss": loss, "val_acc": acc}, step=global_step)
                    time.sleep(0.01)

            report = {
                "runId": state.runId,
                "datasetId": state.datasetId,
                "epochs": state.epochs,
                "best": {"loss": loss, "val_acc": acc},
                "finishedAt": datetime.utcnow().isoformat()+"Z"
            }
            uris = _upload_artifacts(state.runId, report)
            state.artifactUris.update(uris)

            if "report" in uris:
                mlflow.log_params({"artifact_weights_uri": uris.get("weights",""),
                                   "artifact_report_uri": uris.get("report","")})

        state.status = "succeeded"
        state.finishedAt = datetime.utcnow().isoformat()+"Z"
        state.mlflowRunId = mlflow_run_id
        log_event("training.completed", run_id=state.runId, dataset_id=state.datasetId, mlflow_run_id=mlflow_run_id)
    except Exception as e:
        state.status = "failed"
        state.finishedAt = datetime.utcnow().isoformat()+"Z"
        log_event("training.failed", run_id=state.runId, dataset_id=state.datasetId, error=str(e))
try:
    from app.selfplay.api import router as selfplay_router
    app.include_router(selfplay_router)
except Exception:
    # dev-safe: falls Module nicht vorhanden/fehlerhaft, App bleibt startbar
    pass
try:
    from app.dataset_api import router as dataset_router
    app.include_router(dataset_router)
except Exception:
    # dev-safe: ML läuft auch ohne den Endpoint weiter
    pass