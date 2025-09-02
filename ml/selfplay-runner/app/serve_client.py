"""HTTP client for communicating with the model serving API."""
from __future__ import annotations

import os
from typing import Any

import httpx
from tenacity import retry, stop_after_attempt, wait_exponential

from . import metrics

DEFAULT_URL = "http://localhost:8009/v1/predict"


class ServeClient:
    """Client for the external model serve API."""

    def __init__(self, url: str | None = None) -> None:
        self.url = url or os.getenv("SERVE_PREDICT_URL", DEFAULT_URL)
        self._client = httpx.Client(timeout=3.0)

    @retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=0.1))
    def _post(self, payload: dict[str, Any]) -> httpx.Response:
        return self._client.post(self.url, json=payload)

    def predict(self, fen: str, model_id: str) -> str:
        """Request a move from the model serve endpoint."""

        try:
            resp = self._post({"fen": fen, "modelId": model_id})
            resp.raise_for_status()
            data = resp.json()
            return data.get("move", "")
        except httpx.TimeoutException:
            metrics.errors_total.labels(type="serve_timeout").inc()
            raise
        except httpx.HTTPStatusError as exc:  # pragma: no cover - simple
            if 500 <= exc.response.status_code < 600:
                metrics.errors_total.labels(type="serve_5xx").inc()
            else:
                metrics.errors_total.labels(type="other").inc()
            raise
        except Exception:  # pragma: no cover - simple
            metrics.errors_total.labels(type="other").inc()
            raise
