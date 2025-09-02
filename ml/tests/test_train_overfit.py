import json
import os
import subprocess
import sys
from pathlib import Path


def run_cli(data_path: Path, out_dir: Path, metrics_out: Path, seed: int = 0, epochs: int = 5) -> None:
    env = os.environ.copy()
    env["MLFLOW_TRACKING_URI"] = str(out_dir / "mlruns")
    cmd = [
        sys.executable,
        "-m",
        "ml.training.train",
        "--cpu",
        "--data",
        str(data_path),
        "--out-dir",
        str(out_dir),
        "--epochs",
        str(epochs),
        "--batch-size",
        "4",
        "--emb-dim",
        "32",
        "--run-name",
        "test",
        "--experiment",
        "test",
        "--metrics-out",
        str(metrics_out),
        "--seed",
        str(seed),
    ]
    subprocess.run(cmd, check=True, env=env)


def test_train_overfit(mini_parquet, tmp_path):
    out_dir = tmp_path / "out"
    metrics_out = tmp_path / "metrics.json"
    best_path = Path("ml/training/artifacts/best.pt")
    if best_path.exists():
        best_path.unlink()
    run_cli(mini_parquet, out_dir, metrics_out, seed=123, epochs=5)
    assert best_path.is_file()
    metrics = json.loads(metrics_out.read_text())
    assert metrics["best_val_acc_top1"] >= 0.40
