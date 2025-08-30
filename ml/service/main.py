from fastapi import FastAPI, APIRouter
from prometheus_client import make_asgi_app
from ml.service.metrics_dataset import MetricsPayload, observe as observe_dataset_metrics

app = FastAPI()
metrics_app = make_asgi_app()
app.mount("/metrics", metrics_app)
router = APIRouter(prefix="/internal", tags=["internal"])

@app.get("/health")
def health():
    return {"status": "ok"}

@router.post("/dataset/metrics")
def dataset_metrics(payload: MetricsPayload):
    """Ingest dataset metrics (internal only)."""
    observe_dataset_metrics(payload)
    return {"ok": True}

app.include_router(router)
