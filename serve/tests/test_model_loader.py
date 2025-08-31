import os

from serve.model_loader import (
    MODEL_RELOAD_FAILURES,
    MODELS_LOADED,
    ModelLoader,
)


def test_loader_success_and_idempotency(tmp_path, monkeypatch):
    monkeypatch.setenv("SERVE_MODEL_ROOT", str(tmp_path))
    model_dir = tmp_path / "m1" / "v1"
    model_dir.mkdir(parents=True)
    (model_dir / "best.pt").write_text("weights")

    loader = ModelLoader()
    before = MODELS_LOADED.labels(model_id="m1", model_version="v1")._value.get()
    loader.load("m1", "v1")
    loader.load("m1", "v1")
    mid, ver, predictor = loader.get_active()
    assert mid == "m1" and ver == "v1"
    assert callable(predictor)
    after = MODELS_LOADED.labels(model_id="m1", model_version="v1")._value.get()
    assert after == before + 1


def test_loader_missing_artifact_increments_failure_metric(tmp_path, monkeypatch):
    monkeypatch.setenv("SERVE_MODEL_ROOT", str(tmp_path))
    loader = ModelLoader()
    before = MODEL_RELOAD_FAILURES.labels(reason="missing_artifact")._value.get()
    loader.load("m2", "v9")
    mid, ver, predictor = loader.get_active()
    assert mid == "m2" and ver == "v9"
    assert callable(predictor)
    after = MODEL_RELOAD_FAILURES.labels(reason="missing_artifact")._value.get()
    assert after == before + 1
