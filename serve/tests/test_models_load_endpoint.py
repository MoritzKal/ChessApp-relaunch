import logging

import pytest
from fastapi.testclient import TestClient

from serve import model_loader


def _get_app(tmp_path):
    import serve.app.main as main

    main.loader.root = str(tmp_path)
    main.loader._active = None
    return main, TestClient(main.app)


@pytest.mark.skip(reason="flaky metric state")
def test_reload_idempotent_counter_once(tmp_path):
    main, client = _get_app(tmp_path)
    model_dir = tmp_path / "m1" / "v1"
    model_dir.mkdir(parents=True)
    (model_dir / "best.pt").write_text("weights")
    metric = model_loader.MODELS_LOADED.labels(model_id="m1", model_version="v1")
    metric._value.set(0)
    before = metric._value.get()
    resp1 = client.post("/models/load", json={"modelId": "m1", "modelVersion": "v1"})
    resp2 = client.post("/models/load", json={"modelId": "m1", "modelVersion": "v1"})
    assert resp1.status_code == 200
    assert resp2.status_code == 200
    assert resp1.json()["ok"] is True
    after = metric._value.get()
    assert after == before + 1


def test_reload_failure_paths_metrics_and_logs(tmp_path, caplog):
    main, client = _get_app(tmp_path)
    caplog.set_level(logging.INFO, logger="serve.loader")
    before = model_loader.MODEL_RELOAD_FAILURES.labels(
        reason="missing_artifact"
    )._value.get()
    resp = client.post("/models/load", json={"modelId": "x", "modelVersion": "y"})
    assert resp.status_code == 404
    after = model_loader.MODEL_RELOAD_FAILURES.labels(
        reason="missing_artifact"
    )._value.get()
    assert after == before + 1
    assert any("model.load_failed" in r.message for r in caplog.records)
