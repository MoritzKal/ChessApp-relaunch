import json
import logging
import os
from typing import Callable, Optional, Tuple

from prometheus_client import Counter

# Metrics
MODELS_LOADED = Counter(
    "chs_models_loaded_total",
    "Models successfully loaded",
    ["model_id", "model_version"],
)
MODEL_RELOAD_FAILURES = Counter(
    "chs_model_reload_failures_total",
    "Model reload failures",
    ["reason"],
)

# Logging setup
logger = logging.getLogger("serve.loader")
logger.setLevel(logging.INFO)
handler = logging.StreamHandler()
try:  # pragma: no cover - defensive
    from python_json_logger import jsonlogger

    handler.setFormatter(jsonlogger.JsonFormatter())
except Exception:  # pragma: no cover - fallback
    handler.setFormatter(logging.Formatter("%(message)s"))
logger.handlers = [handler]


def _log(event: str, **extra) -> None:
    payload = {"event": event, "component": "serve.loader"}
    payload.update({k: v for k, v in extra.items() if v is not None})
    logger.info(json.dumps(payload))


class ModelLoader:
    """Load and manage predictors by model id and version."""

    def __init__(self, invalidate_cache: Optional[Callable[[], None]] = None) -> None:
        self.root = os.getenv("SERVE_MODEL_ROOT", "/models")
        self._active: Optional[Tuple[str, str, Callable]] = None
        self._invalidate_cache = invalidate_cache

    def load(self, model_id: str, model_version: str) -> None:
        """Load model artifacts; fall back to stub if missing."""
        if (
            self._active
            and self._active[0] == model_id
            and self._active[1] == model_version
        ):
            # Idempotent: same model stays loaded
            return

        path = os.path.join(self.root, model_id, model_version, "best.pt")
        predictor: Callable
        try:
            if os.path.exists(path):
                predictor = self._load_predictor(path)
                MODELS_LOADED.labels(
                    model_id=model_id, model_version=model_version
                ).inc()
                _log(
                    "model.loaded",
                    model_id=model_id,
                    model_version=model_version,
                    artifact=path,
                )
            else:
                raise FileNotFoundError(path)
        except Exception:
            MODEL_RELOAD_FAILURES.labels(reason="missing_artifact").inc()
            predictor = self._stub_predictor()
            _log(
                "model.load_failed",
                model_id=model_id,
                model_version=model_version,
                reason="missing_artifact",
            )

        self._active = (model_id, model_version, predictor)
        if self._invalidate_cache:
            self._invalidate_cache()

    def get_active(self) -> Tuple[str, str, Callable]:
        if not self._active:
            raise RuntimeError("no model loaded")
        return self._active

    # --- helpers -------------------------------------------------
    def _load_predictor(self, path: str) -> Callable:
        # Placeholder for real model loading
        def predictor(*args, **kwargs):
            return "stub"

        return predictor

    def _stub_predictor(self) -> Callable:
        def predictor(*args, **kwargs):
            return "stub"

        return predictor
