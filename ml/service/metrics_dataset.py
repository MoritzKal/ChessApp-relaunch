from prometheus_client import Counter, Summary
from pydantic import BaseModel, Field
from typing import Dict, Optional

# Prometheus metrics
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

# To keep label cardinality bounded, only allow known reasons; others map to 'other'
ALLOWED_INVALID_REASONS = {"uci_not_legal"}

def observe(payload: MetricsPayload):
    labels = dict(dataset_id=payload.dataset_id, run_id=payload.run_id or "local")
    # Rows
    chs_dataset_rows_total.labels(**labels).inc(payload.rows)
    # Invalid split by reason with bounded cardinality
    for reason, count in (payload.invalid.reasons or {}).items():
        safe_reason = reason if reason in ALLOWED_INVALID_REASONS else "other"
        chs_dataset_invalid_rows_total.labels(reason=safe_reason, **labels).inc(count)
    # Duration
    if payload.export_duration_ms is not None:
        chs_dataset_export_duration_ms.labels(**labels).observe(payload.export_duration_ms or 0.0)
