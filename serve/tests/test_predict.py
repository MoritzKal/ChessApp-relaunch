import pytest
from httpx import AsyncClient

from serve.app.main import app

VALID_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
INVALID_FEN = "invalid fen"


@pytest.mark.asyncio
async def test_predict_valid_fen():
    async with AsyncClient(app=app, base_url="http://test") as ac:
        resp = await ac.post("/predict", json={"fen": VALID_FEN})
    assert resp.status_code == 200
    data = resp.json()
    assert data["move"] in data["legal"]
    assert data["modelId"] == "dummy"


@pytest.mark.asyncio
async def test_predict_invalid_fen():
    async with AsyncClient(app=app, base_url="http://test") as ac:
        resp = await ac.post("/predict", json={"fen": INVALID_FEN})
    assert resp.status_code == 400
    assert resp.json()["error"] == "invalid_fen"
