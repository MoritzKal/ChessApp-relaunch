import os
import pytest
from httpx import AsyncClient

from serve.app.main import app


@pytest.mark.asyncio
async def test_models_load_dummy():
    async with AsyncClient(app=app, base_url="http://test") as ac:
        resp = await ac.post("/models/load", json={})
    assert resp.status_code == 200
    assert resp.json()["modelId"] == "dummy"


@pytest.mark.asyncio
async def test_models_load_artifact_uri_tolerance():
    async with AsyncClient(app=app, base_url="http://test") as ac:
        resp = await ac.post(
            "/models/load", json={"artifactUri": "s3://bucket/key", "modelId": "foo"}
        )
    assert resp.status_code == 200


@pytest.mark.skipif(not os.getenv("RUN_MINIO_TESTS"), reason="RUN_MINIO_TESTS not set")
@pytest.mark.asyncio
async def test_models_load_with_minio():
    uri = os.getenv("MINIO_MODEL_URI")
    if not uri:
        pytest.skip("MINIO_MODEL_URI not set")
    async with AsyncClient(app=app, base_url="http://test") as ac:
        resp = await ac.post("/models/load", json={"artifactUri": uri, "modelId": "x"})
    assert resp.status_code == 200
    assert resp.json()["modelId"] != "dummy"
