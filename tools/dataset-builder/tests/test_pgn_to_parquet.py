import json
import subprocess
import sys
from pathlib import Path

import pandas as pd


def test_pgn_to_parquet(tmp_path):
    script = Path(__file__).resolve().parents[1] / "pgn_to_parquet.py"
    pgn = Path(__file__).resolve().parent / "fixtures" / "sample.pgn"
    out_dir = tmp_path / "dataset"

    result = subprocess.run(
        [sys.executable, str(script), "--in", str(pgn), "--out", str(out_dir)],
        check=True,
        capture_output=True,
        text=True,
    )

    log = json.loads(result.stdout.strip().splitlines()[-1])
    assert log["event"] == "pgn_parsed"

    parquet_root = out_dir / "parquet"
    games_parquet = parquet_root / "games.parquet"
    positions_parquet = parquet_root / "positions.parquet"

    assert games_parquet.exists()
    assert positions_parquet.exists()

    games_df = pd.read_parquet(games_parquet)
    positions_df = pd.read_parquet(positions_parquet)

    assert len(games_df) > 0
    assert len(positions_df) > 0

    assert set(games_df.columns) == {
        "id",
        "date",
        "time_control",
        "eco",
        "result",
        "white_elo",
        "black_elo",
    }
    assert set(positions_df.columns) == {
        "game_id",
        "ply",
        "fen",
        "fen_before",
        "uci",
        "san",
        "side_to_move",
    }

    assert games_df["white_elo"].dtype.kind in "iu"
    assert games_df["black_elo"].dtype.kind in "iu"
    assert positions_df["ply"].dtype.kind in "iu"
    assert positions_df["side_to_move"].isin(["w", "b"]).all()
