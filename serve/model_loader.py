import json
import logging
import os
from typing import Callable, Optional, Tuple

from prometheus_client import Counter
from botocore.exceptions import BotoCoreError, ClientError
try:
    import boto3  # type: ignore
except Exception:  # pragma: no cover
    boto3 = None

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
            if not os.path.exists(path):
                # Optional: try to fetch from S3/MinIO if enabled
                if os.getenv("SERVE_ENABLE_S3_FETCH", "true").lower() == "true":
                    self._try_fetch_from_s3(model_id, model_version, path)
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
        except Exception as e:
            # If file is missing, mark as missing_artifact; else internal
            reason = "missing_artifact" if isinstance(e, FileNotFoundError) else "internal"
            MODEL_RELOAD_FAILURES.labels(reason=reason).inc()
            predictor = self._stub_predictor()
            _log(
                "model.load_failed",
                model_id=model_id,
                model_version=model_version,
                reason=reason,
            )

        self._active = (model_id, model_version, predictor)
        if self._invalidate_cache:
            self._invalidate_cache()

    def get_active(self) -> Tuple[str, str, Callable]:
        if not self._active:
            raise RuntimeError("no model loaded")
        return self._active

    # --- helpers -------------------------------------------------
    def _try_fetch_from_s3(self, model_id: str, model_version: str, dest_path: str) -> None:
        """Attempt to download best.pt from S3/MinIO to dest_path.

        Controlled via env:
        - SERVE_S3_ENDPOINT (defaults to ML_S3_ENDPOINT)
        - SERVE_S3_BUCKET  (defaults to ML_ARTIFACT_BUCKET or 'mlflow')
        - SERVE_S3_KEY_TEMPLATE (defaults to 'models/{model_id}/{model_version}/best.pt')
        - AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY / AWS_REGION
        """
        if boto3 is None:
            return
        endpoint = os.getenv("SERVE_S3_ENDPOINT", os.getenv("ML_S3_ENDPOINT"))
        bucket = os.getenv("SERVE_S3_BUCKET", os.getenv("ML_ARTIFACT_BUCKET", "mlflow"))
        key_tmpl = os.getenv(
            "SERVE_S3_KEY_TEMPLATE", "models/{model_id}/{model_version}/best.pt"
        )
        if not endpoint or not bucket:
            return
        key = key_tmpl.format(model_id=model_id, model_version=model_version)
        os.makedirs(os.path.dirname(dest_path), exist_ok=True)
        try:
            s3 = boto3.client(
                "s3",
                endpoint_url=endpoint,
                aws_access_key_id=os.getenv("AWS_ACCESS_KEY_ID"),
                aws_secret_access_key=os.getenv("AWS_SECRET_ACCESS_KEY"),
                region_name=os.getenv("AWS_REGION", "us-east-1"),
            )
            s3.download_file(bucket, key, dest_path)  # type: ignore[no-untyped-call]
            _log("model.fetched", model_id=model_id, model_version=model_version, bucket=bucket, key=key)
        except (BotoCoreError, ClientError, Exception) as e:  # pragma: no cover
            _log(
                "model.fetch_failed",
                model_id=model_id,
                model_version=model_version,
                error=str(e),
                bucket=bucket,
                key=key,
            )

    def _load_predictor(self, path: str) -> Callable:
        # Placeholder for real model loading
        def predictor(*args, **kwargs):
            return "stub"

        return predictor

    def _stub_predictor(self) -> Callable:
        def predictor(*args, **kwargs):
            return "stub"

        return predictor
