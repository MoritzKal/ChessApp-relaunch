"""Prometheus metrics for offline evaluation."""
try:
    from prometheus_client import Counter, Gauge, Histogram
except Exception:  # pragma: no cover - optional dependency
    class _Noop:
        def __init__(self, *args, **kwargs):
            pass

        def labels(self, *args, **kwargs):  # noqa: D401 - noop
            return self

        def inc(self, *args, **kwargs) -> None:  # noqa: D401 - noop
            return None

        def set(self, *args, **kwargs) -> None:  # noqa: D401 - noop
            return None

        def observe(self, *args, **kwargs) -> None:  # noqa: D401 - noop
            return None

    Counter = Gauge = Histogram = _Noop

LAST_VAL_ACC_TOP1 = Gauge(
    "chs_eval_last_val_acc_top1", "Last evaluated top-1 accuracy"
)
LAST_VAL_LOSS = Gauge(
    "chs_eval_last_val_loss", "Last evaluated validation loss"
)
LAST_ECE = Gauge(
    "chs_eval_last_ece", "Last evaluated expected calibration error"
)
RUNTIME_SECONDS = Histogram(
    "chs_eval_runtime_seconds", "Evaluation runtime in seconds",
    buckets=[1, 2, 5, 10, 20, 50, 100]
)
ERRORS_TOTAL = Counter(
    "chs_eval_errors_total", "Total evaluation errors", ["type"]
)
