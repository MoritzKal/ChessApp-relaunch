import importlib

import prometheus_client
import pytest
from httpx import ASGITransport, AsyncClient


@pytest.mark.asyncio
async def test_cache_hits_and_invalidation_on_reload(monkeypatch, valid_fen):
    monkeypatch.setenv("SERVE_ENABLE_LRU", "true")
    reg = prometheus_client.REGISTRY
    for collector in list(reg._collector_to_names):
        reg.unregister(collector)
    import sys

    sys.modules.pop("serve.app.main", None)
    main = importlib.import_module("serve.app.main")

    transport = ASGITransport(app=main.app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        # first call => miss
        await ac.post("/predict", json={"fen": valid_fen})
        miss0 = main.CACHE_MISSES.labels(
            model_id="default", model_version="0"
        )._value.get()
        assert miss0 == 1

        # second call same FEN => hit
        await ac.post("/predict", json={"fen": valid_fen})
        hit0 = main.CACHE_HITS.labels(
            model_id="default", model_version="0"
        )._value.get()
        assert hit0 == 1

        # reload model to new version -> cache should clear
        await ac.post("/models/load", json={"modelId": "default", "modelVersion": "1"})

        # first call after reload => miss for new version
        await ac.post("/predict", json={"fen": valid_fen})
        miss1 = main.CACHE_MISSES.labels(
            model_id="default", model_version="1"
        )._value.get()
        assert miss1 == 1

        # second call after reload => hit for new version
        await ac.post("/predict", json={"fen": valid_fen})
        hit1 = main.CACHE_HITS.labels(
            model_id="default", model_version="1"
        )._value.get()
        assert hit1 == 1
