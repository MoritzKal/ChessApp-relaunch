import json
import subprocess
import sys
from pathlib import Path

import pandas as pd

ROOT = Path(__file__).resolve().parents[1]


def run_build(out_dir: Path, games: Path, positions: Path):
    subprocess.run(
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
            "--seed",
            "123",
        ],
        check=True,
    )


def test_build_dataset_deterministic(tmp_path):
    pgn_out = tmp_path / "raw"
    subprocess.run(
        [
            sys.executable,
            str(ROOT / "pgn_to_parquet.py"),
            "--in",
            str(ROOT / "tests/fixtures/sample.pgn"),
            "--out",
            str(pgn_out),
        ],
        check=True,
    )

    games = pgn_out / "parquet/games.parquet"
    positions = pgn_out / "parquet/positions.parquet"

    out1 = tmp_path / "build1"
    out2 = tmp_path / "build2"
    run_build(out1, games, positions)
    run_build(out2, games, positions)

    train1 = pd.read_parquet(out1 / "parquet/train.parquet")
    val1 = pd.read_parquet(out1 / "parquet/val.parquet")
    test1 = pd.read_parquet(out1 / "parquet/test.parquet")

    train2 = pd.read_parquet(out2 / "parquet/train.parquet")
    val2 = pd.read_parquet(out2 / "parquet/val.parquet")
    test2 = pd.read_parquet(out2 / "parquet/test.parquet")

    assert train1.equals(train2)
    assert val1.equals(val2)
    assert test1.equals(test2)

    merged = pd.read_parquet(positions).merge(
        pd.read_parquet(games), left_on="game_id", right_on="id", how="inner"
    )
    total = len(merged)
    assert len(train1) + len(val1) + len(test1) == total

    manifest = json.loads((out1 / "manifest/dataset.json").read_text())
    for key in ["name", "version", "filters", "splits", "source", "created_at"]:
        assert key in manifest

    rows = json.loads((out1 / "stats/rows.json").read_text())
    for key in ["games", "positions", "train", "val", "test"]:
        assert key in rows

    eco = json.loads((out1 / "stats/eco.json").read_text())
    assert isinstance(eco, dict)
    ply = json.loads((out1 / "stats/ply.json").read_text())
    assert isinstance(ply, dict) and ply

    for png in ["eco.png", "ply.png"]:
        p = out1 / "stats" / png
        assert p.is_file()
        data = p.read_bytes()
        assert len(data) > 0
        assert data.startswith(b"\x89PNG\r\n\x1a\n")
