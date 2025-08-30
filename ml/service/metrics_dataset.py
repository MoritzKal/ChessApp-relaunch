from prometheus_client import Counter, Summary
from pydantic import BaseModel, Field
from typing import Dict, Optional

chs_dataset_rows_total = Counter(
    "chs_dataset_rows_total",
    "Total dataset rows processed",
    labelnames=("dataset_id", "run_id"),
)
chs_dataset_invalid_rows_total = Counter(
    "chs_dataset_invalid_rows_total",
    "Invalid dataset rows by reason",
    labelnames=("dataset_id", "run_id", "reason"),
)
chs_dataset_export_duration_ms = Summary(
    "chs_dataset_export_duration_ms",
    "Export duration in milliseconds",
    labelnames=("dataset_id", "run_id"),
)

class InvalidReasons(BaseModel):
    reasons: Dict[str, int] = Field(default_factory=dict)

class MetricsPayload(BaseModel):
    dataset_id: str
    run_id: Optional[str] = "local"
    rows: int
    invalid: Optional[InvalidReasons] = InvalidReasons()
    export_duration_ms: Optional[float] = 0.0

def observe(payload: MetricsPayload):
    labels = dict(dataset_id=payload.dataset_id, run_id=payload.run_id or "local")
    chs_dataset_rows_total.labels(**labels).inc(payload.rows)
    for reason, count in (payload.invalid.reasons or {}).items():
        chs_dataset_invalid_rows_total.labels(reason=reason, **labels).inc(count)
    if payload.export_duration_ms is not None:
        chs_dataset_export_duration_ms.labels(**labels).observe(float(payload.export_duration_ms or 0.0))
