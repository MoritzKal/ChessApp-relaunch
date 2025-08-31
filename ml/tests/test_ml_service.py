import pytest
from fastapi.testclient import TestClient

app_module = pytest.importorskip("app.main")
app = app_module.app

def test_health():
    c = TestClient(app)
    r = c.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "ok"

def test_train_and_status_progress():
    c = TestClient(app)
    r1 = c.post("/train", json={"epochs": 2, "stepsPerEpoch": 5, "lr": 0.001})
    assert r1.status_code == 200
    run_id = r1.json()["runId"]
    r2 = c.get(f"/runs/{run_id}")
    assert r2.status_code == 200
    assert r2.json()["status"] in ("running","succeeded")
    for _ in range(50):
        r3 = c.get(f"/runs/{run_id}")
        if r3.json()["status"] == "succeeded":
            break
    assert r3.json()["status"] == "succeeded"
