import httpx
import pytest
import respx


@pytest.mark.asyncio
@respx.mock
async def test_loki_query_range_success(client):
    route = respx.get("http://loki.example/loki/api/v1/query_range").mock(
        return_value=httpx.Response(200, json={"data": {"result": []}})
    )
    resp = await client.get(
        "/obs/loki/query_range",
        params={"query": "{}"},
        headers={"X-Obs-Api-Key": "secret"},
    )
    assert resp.status_code == 200
    assert resp.json() == {"data": {"result": []}}
    assert route.call_count == 1
