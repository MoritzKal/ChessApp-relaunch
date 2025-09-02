import asyncio
import json
from typing import Any, Dict, Tuple

import httpx

from config import Settings


def prom_client(settings: Settings) -> httpx.AsyncClient:
    client = httpx.AsyncClient(
        base_url=settings.PROM_BASE_URL,
        timeout=settings.TIMEOUT_SECONDS,
        headers={"Accept-Encoding": "identity"},
    )
    client._retries = settings.RETRIES
    client._upstream = "prom"
    return client


def loki_client(settings: Settings) -> httpx.AsyncClient:
    client = httpx.AsyncClient(
        base_url=settings.LOKI_BASE_URL,
        timeout=settings.TIMEOUT_SECONDS,
        headers={"Accept-Encoding": "identity"},
    )
    client._retries = settings.RETRIES
    client._upstream = "loki"
    return client


async def forward_get(
    client: httpx.AsyncClient, path: str, params: Dict[str, Any]
) -> Tuple[int, bytes, str]:
    """Forward a GET and return (status_code, body_bytes, content_type).

    Do not propagate upstream headers; callers will render JSON locally so the
    ASGI framework sets correct entity headers.
    """
    retries = getattr(client, "_retries", 0)
    upstream = getattr(client, "_upstream", "")
    for attempt in range(retries + 1):
        try:
            response = await client.get(path, params=params)
            if response.status_code >= 500 and attempt < retries:
                await response.aclose()
                await asyncio.sleep(0.2 * (2 ** attempt))
                continue
            body = response.content
            ctype = response.headers.get("content-type", "application/octet-stream")
            await response.aclose()
            return response.status_code, body, ctype
        except (httpx.RequestError, httpx.TimeoutException) as exc:
            if attempt < retries:
                await asyncio.sleep(0.2 * (2 ** attempt))
                continue
            body = json.dumps({"error": str(exc), "upstream": upstream}).encode("utf-8")
            return 502, body, "application/json"
