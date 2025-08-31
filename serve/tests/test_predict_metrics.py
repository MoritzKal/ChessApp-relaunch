import asyncio
import json
import logging
import re

import pytest
from httpx import ASGITransport, AsyncClient

from serve.app.main import app

VALID_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"


@pytest.mark.asyncio
async def test_predict_metrics_happy_path():
    transport = ASGITransport(app=app, raise_app_exceptions=False)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        await ac.post("/predict", json={"fen": VALID_FEN})
        metrics = (await ac.get("/metrics")).text
    assert (
        'chs_predict_latency_ms_bucket{le="5.0",model_id="default",model_version="0"}'
        in metrics
    )
    m = re.search(
        r'chs_models_loaded_total{model_id="default",model_version="0"} ([0-9.]+)',
        metrics,
    )
    assert m and float(m.group(1)) >= 1.0


@pytest.mark.asyncio
async def test_predict_metrics_error_path(monkeypatch):
    class Boom:
        def __init__(self, *args, **kwargs):
            pass

        @property
        def legal_moves(self):  # pragma: no cover - test helper
            raise RuntimeError("fail")

    monkeypatch.setattr("serve.app.main.chess.Board", Boom)
    transport = ASGITransport(app=app, raise_app_exceptions=False)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.post("/predict", json={"fen": VALID_FEN})
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
async def test_predict_logging(caplog):
    with caplog.at_level(logging.INFO):
        transport = ASGITransport(app=app, raise_app_exceptions=False)
        async with AsyncClient(transport=transport, base_url="http://test") as ac:
            await ac.post(
                "/predict",
                json={"fen": VALID_FEN},
                headers={"X-Username": "alice"},
            )
    record = next(r for r in caplog.records if r.msg == "request")
    assert record.component == "serve"
    assert record.model_id == "default"
    assert record.model_version == "0"
    assert record.username == "alice"
