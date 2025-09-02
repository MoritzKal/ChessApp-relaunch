import httpx
import pytest
import respx


@pytest.mark.asyncio
@respx.mock
async def test_loki_query_success(client):
    route = respx.get("http://loki.example/loki/api/v1/query").mock(
        return_value=httpx.Response(200, json={"data": {"result": []}})
    )
    resp = await client.get(
        "/obs/loki/query", params={"query": "{}"}, headers={"X-Obs-Api-Key": "secret"}
    )
    assert resp.status_code == 200
    assert resp.json() == {"data": {"result": []}}
    assert route.call_count == 1


@pytest.mark.asyncio
@respx.mock
async def test_loki_query_4xx(client):
    route = respx.get("http://loki.example/loki/api/v1/query").mock(
        return_value=httpx.Response(400, json={"error": "bad"})
    )
    resp = await client.get(
        "/obs/loki/query", params={"query": "{}"}, headers={"X-Obs-Api-Key": "secret"}
    )
    assert resp.status_code == 400
    assert resp.json() == {"error": "bad"}
    assert route.call_count == 1


@pytest.mark.asyncio
@respx.mock
async def test_loki_query_retry(client):
    route = respx.get("http://loki.example/loki/api/v1/query").mock(
        side_effect=[
            httpx.Response(500),
            httpx.Response(200, json={"data": {"result": []}}),
        ]
    )
    resp = await client.get(
        "/obs/loki/query", params={"query": "{}"}, headers={"X-Obs-Api-Key": "secret"}
    )
    assert resp.status_code == 200
    assert route.call_count == 2


@pytest.mark.asyncio
@respx.mock
async def test_loki_query_timeout(client):
    route = respx.get("http://loki.example/loki/api/v1/query").mock(
        side_effect=httpx.TimeoutException("timeout")
    )
    resp = await client.get(
        "/obs/loki/query", params={"query": "{}"}, headers={"X-Obs-Api-Key": "secret"}
    )
    assert resp.status_code == 502
    body = resp.json()
    assert body["upstream"] == "loki"
    assert route.call_count == 2


@pytest.mark.asyncio
@respx.mock
async def test_loki_query_rate_limit(client):
    from obs_proxy.app import app as obs_app  # type: ignore

    obs_app.state.limiter.storage.clear()  # reset before test
    route = respx.get("http://loki.example/loki/api/v1/query").mock(
        return_value=httpx.Response(200, json={"data": {"result": []}})
    )
    for _ in range(5):
        ok = await client.get(
            "/obs/loki/query", params={"query": "{}"}, headers={"X-Obs-Api-Key": "secret"}
        )
        assert ok.status_code == 200
    resp = await client.get(
        "/obs/loki/query", params={"query": "{}"}, headers={"X-Obs-Api-Key": "secret"}
    )
    assert resp.status_code == 429
    assert route.call_count == 5
    obs_app.state.limiter.storage.clear()
