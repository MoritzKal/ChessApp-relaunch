import importlib
import pytest
from httpx import ASGITransport, AsyncClient
import prometheus_client
from prometheus_client import CollectorRegistry

VALID_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"


@pytest.mark.asyncio
async def test_cache_hits_and_invalidation_on_reload(monkeypatch):
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
        await ac.post("/predict", json={"fen": VALID_FEN})
        miss0 = main.CACHE_MISSES.labels(model_id="dummy", model_version="0")._value.get()
        assert miss0 == 1

        # second call same FEN => hit
        await ac.post("/predict", json={"fen": VALID_FEN})
        hit0 = main.CACHE_HITS.labels(model_id="dummy", model_version="0")._value.get()
        assert hit0 == 1

        # reload model to new version -> cache should clear
        await ac.post("/models/load", json={"modelId": "dummy", "modelVersion": "1"})

        # first call after reload => miss for new version
        await ac.post("/predict", json={"fen": VALID_FEN})
        miss1 = main.CACHE_MISSES.labels(model_id="dummy", model_version="1")._value.get()
        assert miss1 == 1

        # second call after reload => hit for new version
        await ac.post("/predict", json={"fen": VALID_FEN})
        hit1 = main.CACHE_HITS.labels(model_id="dummy", model_version="1")._value.get()
        assert hit1 == 1
