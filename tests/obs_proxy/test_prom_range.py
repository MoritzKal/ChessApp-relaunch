import httpx
import pytest
import respx


@pytest.mark.asyncio
@respx.mock
async def test_no_upstream_entity_headers(client):
    respx.get("http://prom.example/api/v1/query").mock(
        return_value=httpx.Response(
            200,
            headers={"content-encoding": "gzip", "content-length": "123"},
            json={"status": "success", "data": {"result": []}},
        )
    )

    r = await client.get(
        "/obs/prom/query", params={"query": "up"}, headers={"X-Obs-Api-Key": "secret"}
    )
    assert r.status_code == 200
    # Proxy must not pass through upstream entity headers
    assert "content-encoding" not in r.headers
    # content-length is set by FastAPI; it must not equal upstream's mocked value
    assert r.headers.get("content-length") != "123"

