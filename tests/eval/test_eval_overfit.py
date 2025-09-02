import json
import subprocess
import sys
from pathlib import Path

import pandas as pd


def test_eval_overfit(tmp_path: Path):
    start_fen = "rn1qkbnr/pppbpppp/8/3p4/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 3"
    labels = ["a2a3"] * 35 + ["e2e4"] * 15
    df = pd.DataFrame({"fen": [start_fen] * 50, "label_uci": labels})
    dataset = tmp_path / "data.parquet"
    df.to_parquet(dataset)

    outdir = tmp_path / "out"
    cmd = [
        sys.executable,
        "ml/eval-offline/eval.py",
        "--model-id",
        "dummy",
        "--dataset",
        str(dataset),
        "--batch-size",
        "8",
        "--limit",
        "50",
        "--outdir",
        str(outdir),
    ]
    subprocess.run(cmd, check=True)

    metrics = json.loads((outdir / "metrics.json").read_text())
    assert metrics["val_acc_top1"] >= 0.65
    assert (outdir / "calibration.png").exists()
    assert (outdir / "topk.png").exists()
