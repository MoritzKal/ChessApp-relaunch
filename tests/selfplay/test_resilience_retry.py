import time
from fastapi.testclient import TestClient
import httpx

from selfplay_runner.app.main import create_app
from selfplay_runner.app.runner import SelfPlayRunner
from selfplay_runner.app.serve_client import ServeClient
from selfplay_runner.app.metrics import errors_total
from selfplay_runner.app import storage, elo


def test_resilience_retry(monkeypatch):
    # patch ServeClient.predict to timeout twice then succeed
    calls = {"n": 0}

    def fake_predict(self, fen: str, model_id: str) -> str:
        if calls["n"] < 2:
            calls["n"] += 1
            errors_total.labels(type="serve_timeout").inc()
            raise httpx.TimeoutException("boom")
        return "e2e4"

    monkeypatch.setattr(ServeClient, "predict", fake_predict)

    # patch runner to call predict and finish
    def fake_run(self, run_id: str) -> None:
        info = self.runs[run_id]
        # retry until success
        while True:
            try:
                self.client.predict("fake", info.request.modelId)
                break
            except httpx.TimeoutException:
                continue
        info.results.append({"gameIdx": 0, "result": "win", "plyCount": 1})
        info.progress["played"] = 1
        info.metrics["winRate"] = 1.0
        info.metrics["elo"] = elo.win_rate_to_elo(1.0)
        report = {
            "runId": run_id,
            "request": info.request.model_dump(),
            "startedAt": info.started_at,
            "finishedAt": info.started_at,
            "results": info.results,
            "summary": {"winRate": 1.0, "elo": info.metrics["elo"]},
        }
        info.report_uri = storage.save_report(run_id, report)
        info.status = "completed"

    monkeypatch.setattr(SelfPlayRunner, "_run", fake_run)

    app = create_app()
    client = TestClient(app)
    resp = client.post(
        "/runner/selfplay/start",
        json={"modelId": "cand", "baselineId": "base", "games": 1, "concurrency": 1, "seed": 0},
    )
    run_id = resp.json()["runId"]
    time.sleep(0.1)
    st = client.get(f"/runner/selfplay/runs/{run_id}").json()
    assert st["status"] == "completed"
    assert errors_total.labels(type="serve_timeout")._value.get() >= 2
