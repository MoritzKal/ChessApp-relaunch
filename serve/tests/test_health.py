import pytest
from httpx import ASGITransport, AsyncClient

from serve.app.main import app


@pytest.mark.asyncio
async def test_health():
    transport = ASGITransport(app=app, raise_app_exceptions=False)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        resp = await ac.get("/health")
    assert resp.status_code == 200
    assert resp.json() == {"status": "ok"}
