import json

from test_train_overfit import run_cli


def test_seed_determinism(mini_parquet, tmp_path):
    out_dir1 = tmp_path / "run1"
    metrics1 = tmp_path / "m1.json"
    run_cli(mini_parquet, out_dir1, metrics1, seed=7, epochs=2)
    out_dir2 = tmp_path / "run2"
    metrics2 = tmp_path / "m2.json"
    run_cli(mini_parquet, out_dir2, metrics2, seed=7, epochs=2)
    m1 = json.loads(metrics1.read_text())["best_val_acc_top1"]
    m2 = json.loads(metrics2.read_text())["best_val_acc_top1"]
    assert abs(m1 - m2) < 1e-6
