import json
import re
import time
from pathlib import Path

import pytest
from fastapi.testclient import TestClient


@pytest.fixture(scope="session")
def context():
    return {
        "user": "e2e_user",
        "model_id": "e2e_model",
        "version": "v0",
        "stages": {},
        "metrics": {},
    }


@pytest.fixture(scope="session", autouse=True)
def write_report(context):
    yield
    ts = time.strftime("%Y%m%d_%H%M%S")
    report_dir = Path("reports")
    report_dir.mkdir(exist_ok=True)
    lines = ["# E2E Run", "", "| Stage | Duration (s) |", "|-------|--------------|"]
    for stage, meta in context["stages"].items():
        lines.append(f"| {stage} | {meta['duration']:.2f} |")
    if context["metrics"]:
        lines.extend(["", "## Metrics"])
        for k, v in context["metrics"].items():
            lines.append(f"- {k}: {v}")
    (report_dir / f"e2e_run_{ts}.md").write_text("\n".join(lines))


@pytest.fixture(scope="session")
def client():
    import sys
    from pathlib import Path
    import os

    serve_dir = Path(__file__).resolve().parents[2] / "serve"
    sys.path.insert(0, str(serve_dir))
    os.environ.setdefault("SERVE_MODEL_ROOT", str((Path("artifacts")).resolve()))
    from app.main import app

    return TestClient(app)


@pytest.mark.ingest
@pytest.mark.integration
def test_ingest_stage(context):
    start = time.perf_counter()
    user = context["user"]
    base = Path(f"data/raw/{user}")
    base.mkdir(parents=True, exist_ok=True)
    sample = {
        "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        "move": "e2e4",
    }
    (base / "games.jsonl").write_text(json.dumps(sample) + "\n")
    context["stages"]["ingest"] = {"duration": time.perf_counter() - start}


@pytest.mark.training
@pytest.mark.integration
def test_initial_training_stage(context):
    start = time.perf_counter()
    model_root = Path("artifacts") / context["model_id"] / context["version"]
    model_root.mkdir(parents=True, exist_ok=True)
    (model_root / "model.pt").write_text("fake model")
    (model_root / "best.pt").write_text("fake model")
    (model_root / "metrics.json").write_text(json.dumps({"acc": 0.0}))
    context["stages"]["training"] = {"duration": time.perf_counter() - start}


@pytest.mark.serve
@pytest.mark.integration
def test_serve_stage(context, client):
    start = time.perf_counter()
    model_id, version = context["model_id"], context["version"]
    artifact = Path("artifacts")/model_id/version/"best.pt"
    if artifact.exists():
        artifact.unlink()
    resp = client.post("/models/load", json={"modelId": model_id, "modelVersion": version})
    assert resp.status_code == 404
    artifact.write_text("fake model")
    resp = client.post("/models/load", json={"modelId": model_id, "modelVersion": version})
    assert resp.status_code == 200
    assert resp.json()["active"]["modelId"] == model_id
    context["stages"]["serve"] = {"duration": time.perf_counter() - start}


@pytest.mark.predict
@pytest.mark.integration
def test_predict_stage(context, client):
    start = time.perf_counter()
    fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    resp = client.post("/predict", json={"fen": fen})
    assert resp.status_code == 200
    assert len(resp.json()["move"]) >= 4
    bad = client.post("/predict", json={"fen": "invalid"})
    assert bad.status_code == 400
    context["stages"]["predict"] = {"duration": time.perf_counter() - start}


@pytest.mark.observability
@pytest.mark.integration
def test_observability_stage(context, client):
    start = time.perf_counter()
    metrics = client.get("/metrics")
    assert metrics.status_code == 200
    text = metrics.text
    assert "chs_predict_latency_ms_bucket" in text
    ml = context["model_id"]
    ver = context["version"]
    assert f'chs_models_loaded_total{{model_id="{ml}",model_version="{ver}"}}' in text.replace(" ", "")
    assert f'chs_predict_errors_total{{code="400",model_id="{ml}",model_version="{ver}"}}' in text.replace(" ", "")
    req = re.search(
        r'chs_predict_requests_total{model_id="[^"]+",model_version="[^"]+"} (\d+(?:\.\d+)?)',
        text,
    )
    err = re.search(
        r'chs_predict_errors_total{model_id="[^"]+",model_version="[^"]+",code="400"} (\d+(?:\.\d+)?)',
        text,
    )
    context["metrics"] = {
        "predict_requests_total": float(req.group(1)) if req else 0.0,
        "predict_errors_total": float(err.group(1)) if err else 0.0,
    }
    context["stages"]["observability"] = {"duration": time.perf_counter() - start}
