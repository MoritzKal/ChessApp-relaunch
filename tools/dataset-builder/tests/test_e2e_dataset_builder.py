import json
import subprocess
import sys
from pathlib import Path

import pandas as pd

ROOT = Path(__file__).resolve().parents[1]


def test_e2e_dataset_builder(tmp_path):
    out_dir = tmp_path / "dataset"
    pgn = ROOT / "tests/fixtures/sample.pgn"

    subprocess.run(
        [sys.executable, str(ROOT / "pgn_to_parquet.py"), "--in", str(pgn), "--out", str(out_dir)],
        check=True,
    )

    games = out_dir / "parquet/games.parquet"
    positions = out_dir / "parquet/positions.parquet"

    proc = subprocess.run(
        [
            sys.executable,
            str(ROOT / "build_dataset.py"),
            "--games",
            str(games),
            "--positions",
            str(positions),
            "--name",
            "sample",
            "--out",
            str(out_dir),
        ],
        check=True,
        capture_output=True,
        text=True,
    )

    events = [json.loads(line) for line in proc.stdout.strip().splitlines()]
    assert any(e.get("event") == "dataset_built" for e in events)

    for split in ["train", "val", "test"]:
        assert (out_dir / f"parquet/{split}.parquet").exists()

    stats_dir = out_dir / "stats"
    manifest = out_dir / "manifest/dataset.json"
    assert manifest.exists()

    for fname in ["rows.json", "eco.json", "ply.json"]:
        assert (stats_dir / fname).is_file()
    for png in ["eco.png", "ply.png"]:
        p = stats_dir / png
        assert p.is_file() and p.stat().st_size > 0

    rows = json.loads((stats_dir / "rows.json").read_text())
    assert rows["train"] > 0 and rows["val"] > 0 and rows["test"] > 0
    assert rows["train"] + rows["val"] + rows["test"] == rows["positions"]
