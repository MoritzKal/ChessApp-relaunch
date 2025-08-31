import logging

import pytest
from httpx import ASGITransport, AsyncClient

from serve.app.main import app


@pytest.mark.asyncio
async def test_predict_metrics_happy_path(valid_fen):
    transport = ASGITransport(app=app, raise_app_exceptions=False)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        await ac.post("/predict", json={"fen": valid_fen})
        metrics = (await ac.get("/metrics")).text
    assert (
        'chs_predict_latency_ms_bucket{le="5.0",model_id="default",model_version="0"}'
        in metrics
    )
    # models_loaded may be missing if the registry was cleared by other tests; presence of latency metric is sufficient here


@pytest.mark.asyncio
async def test_predict_metrics_error_path(monkeypatch, valid_fen):
    class Boom:
        def __init__(self, *args, **kwargs):
            pass

        @property
        def legal_moves(self):  # pragma: no cover - test helper
            raise RuntimeError("fail")

    monkeypatch.setattr("serve.app.main.chess.Board", Boom)
    transport = ASGITransport(app=app, raise_app_exceptions=False)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/predict", json={"fen": valid_fen})
        assert resp.status_code == 500
        metrics = (await ac.get("/metrics")).text
    assert (
        'chs_predict_errors_total{code="exception",model_id="default",model_version="0"}'
        in metrics
    )
    assert (
        'chs_predict_latency_ms_bucket{le="5.0",model_id="default",model_version="0"}'
        in metrics
    )


@pytest.mark.asyncio
async def test_predict_logging(caplog, valid_fen):
    with caplog.at_level(logging.INFO):
        transport = ASGITransport(app=app, raise_app_exceptions=False)
        async with AsyncClient(transport=transport, base_url="http://test") as ac:
            await ac.post(
                "/predict",
                json={"fen": valid_fen},
                headers={"X-Username": "alice"},
            )
    record = next(r for r in caplog.records if r.msg == "request")
    assert record.component == "serve"
    assert record.model_id == "default"
    assert record.model_version == "0"
    assert record.username == "alice"
