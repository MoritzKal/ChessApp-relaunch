import os
import sys
import types
from pathlib import Path

import httpx
import pytest

BASE_DIR = Path(__file__).resolve().parents[2] / "gateway" / "obs-proxy"
pkg = types.ModuleType("obs_proxy")
pkg.__path__ = [str(BASE_DIR)]
sys.modules.setdefault("obs_proxy", pkg)

os.environ.setdefault("PROM_BASE_URL", "http://prom.example")
os.environ.setdefault("LOKI_BASE_URL", "http://loki.example")
os.environ.setdefault("OBS_API_KEY", "secret")
os.environ.setdefault("CORS_ALLOW_ORIGINS", "http://localhost:5173")
os.environ.setdefault("RATE_LIMIT", "5/minute")
os.environ.setdefault("RETRIES", "1")
os.environ.setdefault("TIMEOUT_SECONDS", "0.01")

from obs_proxy.config import get_settings  # type: ignore
get_settings.cache_clear()
from obs_proxy.app import app  # type: ignore


@pytest.fixture
async def client() -> httpx.AsyncClient:
    async with httpx.AsyncClient(app=app, base_url="http://test") as ac:
        yield ac
