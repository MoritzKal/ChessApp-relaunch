import importlib
import time

import pytest
from httpx import ASGITransport, AsyncClient

from serve.app.main import (
    ILLEGAL_REQUESTS,
    PREDICT_ERRORS,
    PREDICT_LATENCY_MS,
    PREDICT_REQUESTS,
    app,
)

VALID_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
INVALID_FEN = "invalid fen"


@pytest.mark.asyncio
async def test_predict_accepts_valid_fen():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/predict", json={"fen": VALID_FEN})
    assert resp.status_code == 200
    data = resp.json()
    assert data["move"] in data["legal"]
    assert data["modelId"] == "dummy"


@pytest.mark.asyncio
async def test_predict_rejects_invalid_fen_400_and_metric():
    before = ILLEGAL_REQUESTS.labels(model_id="dummy", model_version="0")._value.get()
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/predict", json={"fen": INVALID_FEN})
    assert resp.status_code == 400
    body = resp.json()
    assert body["error"]["code"] == "INVALID_FEN"
    assert body["error"]["detail"]["fen"] == INVALID_FEN
    after = ILLEGAL_REQUESTS.labels(model_id="dummy", model_version="0")._value.get()
    assert after == before + 1


@pytest.mark.asyncio
async def test_predict_metrics_have_version_labels():
    transport = ASGITransport(app=app)
    before_req = PREDICT_REQUESTS.labels(model_id="dummy", model_version="0")._value.get()
    before_err = PREDICT_ERRORS.labels(model_id="dummy", model_version="0")._value.get()
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        await ac.post("/predict", json={"fen": VALID_FEN})
        await ac.post("/predict", json={"fen": INVALID_FEN})
    after_req = PREDICT_REQUESTS.labels(model_id="dummy", model_version="0")._value.get()
    after_err = PREDICT_ERRORS.labels(model_id="dummy", model_version="0")._value.get()
    assert after_req == before_req + 2
    assert after_err == before_err + 1


@pytest.mark.asyncio
async def test_latency_summary_observes_requests():
    before = PREDICT_LATENCY_MS.labels(model_id="dummy", model_version="0")._count.get()
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        await ac.post("/predict", json={"fen": VALID_FEN})
    after = PREDICT_LATENCY_MS.labels(model_id="dummy", model_version="0")._count.get()
    assert after == before + 1


@pytest.mark.asyncio
async def test_latency_p95_under_30ms_with_fake_predictor(monkeypatch):
    monkeypatch.setenv("SERVE_FAKE_FAST", "true")
    import serve.app.main as main
    from prometheus_client import REGISTRY

    def _unregister(m):
        for metric in [
            m.PREDICT_REQUESTS,
            m.PREDICT_ERRORS,
            m.PREDICT_LATENCY,
            m.PREDICT_LATENCY_MS,
            m.ILLEGAL_REQUESTS,
            m.CACHE_HITS,
            m.CACHE_MISSES,
        ]:
            try:
                REGISTRY.unregister(metric)
            except KeyError:
                pass

    _unregister(main)
    importlib.reload(main)
    transport = ASGITransport(app=main.app)
    durations = []
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        for _ in range(100):
            start = time.perf_counter()
            resp = await ac.post("/predict", json={"fen": VALID_FEN})
            assert resp.status_code == 200
            durations.append(time.perf_counter() - start)
    durations.sort()
    p95 = durations[int(len(durations) * 0.95) - 1] * 1000
    assert p95 <= 30
    monkeypatch.delenv("SERVE_FAKE_FAST", raising=False)
    _unregister(main)
    importlib.reload(main)
