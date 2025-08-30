from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

# WICHTIG: Bestehende Registry-Objekte importieren – keine Duplikate erzeugen!
try:
    from app.metrics_registry import (
        chs_dataset_rows_total,
        chs_dataset_invalid_rows_total,
        chs_dataset_export_duration_seconds,
    )
except Exception as e:
    raise RuntimeError(f"metrics_registry import failed: {e}")

router = APIRouter()

class DatasetMetrics(BaseModel):
    dataset_id: str = Field(..., min_length=1)
    rows_total: int = Field(..., ge=0)
    invalid_rows_total: int = Field(0, ge=0)
    # A2 nutzt SECONDS (nicht ms) – deine Exporter zeigen *_seconds_*:
    export_duration_seconds: float | None = Field(None, ge=0)

@router.post("/dataset/metrics")
def post_dataset_metrics(payload: DatasetMetrics):
    try:
        # Zähler additiv
        chs_dataset_rows_total.labels(dataset_id=payload.dataset_id).inc(payload.rows_total)
        if payload.invalid_rows_total:
            chs_dataset_invalid_rows_total.labels(dataset_id=payload.dataset_id).inc(payload.invalid_rows_total)
        # Dauer als Histogramm
        if payload.export_duration_seconds is not None:
            chs_dataset_export_duration_seconds.observe(payload.export_duration_seconds)
        return {"ok": True}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"observe failed: {e}")
