import os
import sys
import types
from pathlib import Path

import httpx
import pytest
import respx

# Setup fake package for obs_proxy because directory name has hyphen
BASE_DIR = Path(__file__).resolve().parents[1]
pkg = types.ModuleType("obs_proxy")
pkg.__path__ = [str(BASE_DIR)]
sys.modules["obs_proxy"] = pkg

# Configure environment before importing settings/app
os.environ.update(
    {
        "PROM_BASE_URL": "http://prom",
        "LOKI_BASE_URL": "http://loki",
        "OBS_API_KEY": "secret",
        "RETRIES": "1",
        "TIMEOUT_SECONDS": "0.1",
    }
)

from obs_proxy.config import get_settings  # type: ignore
get_settings.cache_clear()
from obs_proxy.app import app  # type: ignore


@pytest.mark.asyncio
async def test_healthz():
    async with httpx.AsyncClient(app=app, base_url="http://test") as ac:
        resp = await ac.get("/healthz")
    assert resp.status_code == 200
    assert resp.json() == {"status": "ok"}


@pytest.mark.asyncio
@respx.mock
async def test_prom_query_success():
    respx.get("http://prom/api/v1/query").mock(
        return_value=httpx.Response(200, json={"data": 1})
    )
    async with httpx.AsyncClient(app=app, base_url="http://test") as ac:
        resp = await ac.get(
            "/obs/prom/query",
            params={"query": "up"},
            headers={"X-Obs-Api-Key": "secret"},
        )
    assert resp.status_code == 200
    assert resp.json() == {"data": 1}


@pytest.mark.asyncio
async def test_auth_required():
    async with httpx.AsyncClient(app=app, base_url="http://test") as ac:
        resp = await ac.get("/obs/prom/query", params={"query": "up"})
    assert resp.status_code == 401


@pytest.mark.asyncio
@respx.mock
async def test_loki_query_failure_returns_502():
    respx.get("http://loki/loki/api/v1/query").mock(side_effect=httpx.TimeoutException("timeout"))
    async with httpx.AsyncClient(app=app, base_url="http://test") as ac:
        resp = await ac.get(
            "/obs/loki/query",
            params={"query": "{}"},
            headers={"X-Obs-Api-Key": "secret"},
        )
    assert resp.status_code == 502
    body = resp.json()
    assert body["upstream"] == "loki"
