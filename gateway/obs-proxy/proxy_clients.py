import asyncio
from typing import Any, Dict, Tuple

import httpx

from config import Settings


def prom_client(settings: Settings) -> httpx.AsyncClient:
    client = httpx.AsyncClient(
        base_url=settings.PROM_BASE_URL, timeout=settings.TIMEOUT_SECONDS
    )
    client._retries = settings.RETRIES
    client._upstream = "prom"
    return client


def loki_client(settings: Settings) -> httpx.AsyncClient:
    client = httpx.AsyncClient(
        base_url=settings.LOKI_BASE_URL, timeout=settings.TIMEOUT_SECONDS
    )
    client._retries = settings.RETRIES
    client._upstream = "loki"
    return client


async def forward_get(
    client: httpx.AsyncClient, path: str, params: Dict[str, Any]
) -> Tuple[int, Dict[str, Any], Dict[str, str]]:
    retries = getattr(client, "_retries", 0)
    upstream = getattr(client, "_upstream", "")
    for attempt in range(retries + 1):
        try:
            response = await client.get(path, params=params)
            if response.status_code >= 500 and attempt < retries:
                await response.aclose()
                await asyncio.sleep(0.2 * (2 ** attempt))
                continue
            data = response.json()
            headers = dict(response.headers)
            await response.aclose()
            return response.status_code, data, headers
        except (httpx.RequestError, httpx.TimeoutException) as exc:
            if attempt < retries:
                await asyncio.sleep(0.2 * (2 ** attempt))
                continue
            return 502, {"error": str(exc), "upstream": upstream}, {}
