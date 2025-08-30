import pytest
from httpx import ASGITransport, AsyncClient
from prometheus_client import generate_latest

from serve.app.main import ILLEGAL_REQUESTS, PREDICT_LATENCY_MS, app

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
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        await ac.post("/predict", json={"fen": VALID_FEN})
        await ac.post("/predict", json={"fen": INVALID_FEN})
    metrics = generate_latest().decode()
    assert 'chs_predict_requests_total{model_id="dummy",model_version="0"}' in metrics
    assert 'chs_predict_errors_total{model_id="dummy",model_version="0"}' in metrics


@pytest.mark.asyncio
async def test_latency_summary_observes_requests():
    before = PREDICT_LATENCY_MS.labels(model_id="dummy", model_version="0")._count.get()
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        await ac.post("/predict", json={"fen": VALID_FEN})
    after = PREDICT_LATENCY_MS.labels(model_id="dummy", model_version="0")._count.get()
    assert after == before + 1
