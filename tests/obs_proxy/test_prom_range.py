import httpx
import pytest
import respx


@pytest.mark.asyncio
@respx.mock
async def test_prom_range_success(client):
    route = respx.get("http://prom.example/api/v1/query_range").mock(
        return_value=httpx.Response(
            200,
            json={
                "status": "success",
                "data": {"resultType": "matrix", "result": []},
            },
        )
    )
    resp = await client.get(
        "/obs/prom/range",
        params={"query": "up", "start": "0", "end": "10", "step": "1"},
        headers={"X-Obs-Api-Key": "secret"},
    )
    assert resp.status_code == 200
    assert resp.json() == {
        "status": "success",
        "data": {"resultType": "matrix", "result": []},
    }
    assert route.call_count == 1


@pytest.mark.asyncio
@respx.mock
async def test_prom_range_4xx(client):
    route = respx.get("http://prom.example/api/v1/query_range").mock(
        return_value=httpx.Response(400, json={"error": "bad"})
    )
    resp = await client.get(
        "/obs/prom/range",
        params={"query": "up", "start": "0", "end": "10", "step": "1"},
        headers={"X-Obs-Api-Key": "secret"},
    )
    assert resp.status_code == 400
    assert resp.json() == {"error": "bad"}
    assert route.call_count == 1


@pytest.mark.asyncio
@respx.mock
async def test_prom_range_retry(client):
    route = respx.get("http://prom.example/api/v1/query_range").mock(
        side_effect=[
            httpx.Response(500),
            httpx.Response(
                200,
                json={
                    "status": "success",
                    "data": {"resultType": "matrix", "result": []},
                },
            ),
        ]
    )
    resp = await client.get(
        "/obs/prom/range",
        params={"query": "up", "start": "0", "end": "10", "step": "1"},
        headers={"X-Obs-Api-Key": "secret"},
    )
    assert resp.status_code == 200
    assert route.call_count == 2


@pytest.mark.asyncio
@respx.mock
async def test_prom_range_timeout(client):
    route = respx.get("http://prom.example/api/v1/query_range").mock(
        side_effect=httpx.TimeoutException("timeout")
    )
    resp = await client.get(
        "/obs/prom/range",
        params={"query": "up", "start": "0", "end": "10", "step": "1"},
        headers={"X-Obs-Api-Key": "secret"},
    )
    assert resp.status_code == 502
    body = resp.json()
    assert body["upstream"] == "prom"
    assert route.call_count == 2


@pytest.mark.asyncio
async def test_prom_range_missing_api_key(client):
    resp = await client.get(
        "/obs/prom/range",
        params={"query": "up", "start": "0", "end": "10", "step": "1"},
    )
    assert resp.status_code == 401
