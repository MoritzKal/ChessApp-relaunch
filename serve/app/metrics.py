from prometheus_client import Counter, Histogram

chs_predict_latency_ms = Histogram(
    "chs_predict_latency_ms",
    "Prediction latency in milliseconds",
    ["model_id", "model_version"],
    buckets=[5, 10, 20, 30, 50, 75, 100, 150, 250, 400],
)

chs_predict_errors_total = Counter(
    "chs_predict_errors_total",
    "Total prediction errors",
    ["model_id", "model_version", "code"],
)

chs_models_loaded_total = Counter(
    "chs_models_loaded_total",
    "Models loaded",
    ["model_id", "model_version"],
)

chs_model_reload_failures_total = Counter(
    "chs_model_reload_failures_total",
    "Model reload failures",
    ["model_id", "model_version", "reason"],
)


def observe_predict(
    ms: float,
    model_id: str,
    model_version: str,
    status_code: int,
    error_code: str | None = None,
) -> None:
    """Record latency and errors for predict."""
    chs_predict_latency_ms.labels(
        model_id=model_id, model_version=model_version
    ).observe(ms)
    code = (
        error_code if error_code else (str(status_code) if status_code >= 400 else None)
    )
    if code:
        chs_predict_errors_total.labels(
            model_id=model_id, model_version=model_version, code=code
        ).inc()


def inc_model_loaded(model_id: str, model_version: str) -> None:
    chs_models_loaded_total.labels(model_id=model_id, model_version=model_version).inc()


def inc_reload_failure(model_id: str, model_version: str, reason: str) -> None:
    chs_model_reload_failures_total.labels(
        model_id=model_id, model_version=model_version, reason=reason
    ).inc()
