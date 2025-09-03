import json
import time
from datetime import datetime
from fastapi.testclient import TestClient

from selfplay_runner.app.main import create_app
from selfplay_runner.app.runner import SelfPlayRunner
from selfplay_runner.app import storage, elo


def test_runner_smoke(monkeypatch):
    # patch run to produce deterministic 60% win rate and create report
    def fake_run(self, run_id: str) -> None:
        info = self.runs[run_id]
        games = info.request.games
        wins = int(games * 0.6)
        for i in range(games):
            res = "win" if i < wins else "loss"
            info.results.append({"gameIdx": i, "result": res, "plyCount": 1})
            info.progress["played"] += 1
        p = wins / games
        info.metrics["winRate"] = p
        info.metrics["elo"] = elo.win_rate_to_elo(p)
        report = {
            "runId": run_id,
            "request": info.request.model_dump(),
            "startedAt": info.started_at,
            "finishedAt": datetime.utcnow().isoformat(),
            "results": info.results,
            "summary": {"winRate": p, "elo": info.metrics["elo"]},
        }
        info.report_uri = storage.save_report(run_id, report)
        info.status = "completed"

    monkeypatch.setattr(SelfPlayRunner, "_run", fake_run)

    app = create_app()
    client = TestClient(app)
    resp = client.post(
        "/runner/selfplay/start",
        json={"modelId": "cand", "baselineId": "base", "games": 10, "concurrency": 2, "seed": 0},
    )
    run_id = resp.json()["runId"]

    # poll until completed
    while True:
        st = client.get(f"/runner/selfplay/runs/{run_id}").json()
        if st["status"] == "completed":
            break
        time.sleep(0.1)

    assert 0.5 <= st["metrics"]["winRate"] <= 0.8
    assert st["metrics"]["elo"] > 0
    report_path = st["reportUri"]
    data = json.loads(open(report_path).read())
    assert data["summary"]["winRate"] == st["metrics"]["winRate"]
