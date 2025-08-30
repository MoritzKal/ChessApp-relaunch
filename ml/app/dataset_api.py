# ml/app/dataset_api.py
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

# Wichtig: vorhandene Namen verwenden. Erst versuchen zu importieren,
# sonst (nur falls im ML-Prozess noch nicht vorhanden) definieren.
try:
    from app.metrics_registry import (
        chs_dataset_rows_total,
        chs_dataset_invalid_rows_total,
        chs_dataset_export_duration_seconds,
    )
except Exception:
    from prometheus_client import REGISTRY, Counter, Histogram
    # gleiche Namen wie in Serve:
    chs_dataset_rows_total = Counter(
        "chs_dataset_rows_total", "Dataset rows exported", ["dataset_id"], registry=REGISTRY
    )
    chs_dataset_invalid_rows_total = Counter(
        "chs_dataset_invalid_rows_total", "Invalid dataset rows", ["dataset_id"], registry=REGISTRY
    )
    chs_dataset_export_duration_seconds = Histogram(
        "chs_dataset_export_duration_seconds", "Export duration in seconds", registry=REGISTRY
        # (Default-Buckets sind okay; Panels rechnen rate/quantile darüber)
    )

router = APIRouter()

class DatasetMetrics(BaseModel):
    dataset_id: str = Field(..., min_length=1)
    rows_total: int = Field(..., ge=0)
    invalid_rows_total: int = Field(0, ge=0)
    export_duration_seconds: float | None = Field(None, ge=0)

@router.post("/dataset/metrics")
def post_dataset_metrics(payload: DatasetMetrics):
    try:
        chs_dataset_rows_total.labels(dataset_id=payload.dataset_id).inc(payload.rows_total)
        if payload.invalid_rows_total:
            chs_dataset_invalid_rows_total.labels(dataset_id=payload.dataset_id).inc(payload.invalid_rows_total)
        if payload.export_duration_seconds is not None:
            chs_dataset_export_duration_seconds.observe(payload.export_duration_seconds)
        return {"ok": True}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"observe failed: {e}")
