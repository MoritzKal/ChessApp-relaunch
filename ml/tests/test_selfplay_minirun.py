import os
import sys
from pathlib import Path

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from app.selfplay.config import SelfPlayConfig
from app.selfplay.runner import selfplay_loop


def test_selfplay_minirun(tmp_path: Path):
    run_id = "pytest-mini"
    out_dir = tmp_path / "data" / "selfplay"
    cfg = SelfPlayConfig(run_id=run_id, games=5, max_moves=30, out_dir=out_dir)
    selfplay_loop(cfg)
    base = out_dir / run_id
    assert (base / "games.jsonl").exists() and (base / "games.jsonl").stat().st_size > 0
    assert (base / "samples.jsonl").exists() and (base / "samples.jsonl").stat().st_size > 0
    assert (base / "samples.parquet").exists() and (base / "samples.parquet").stat().st_size > 0
