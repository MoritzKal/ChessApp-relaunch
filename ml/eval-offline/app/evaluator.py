"""Offline evaluation core logic."""
from __future__ import annotations

import json
import math
import os
import random
import time
import uuid
from pathlib import Path
from typing import Iterable, Tuple

from .metrics import (
    LAST_ECE,
    LAST_VAL_ACC_TOP1,
    LAST_VAL_LOSS,
    RUNTIME_SECONDS,
    ERRORS_TOTAL,
)

try:  # optional dependencies
    import pandas as pd
except Exception:  # pragma: no cover - handled gracefully
    pd = None
try:
    import matplotlib.pyplot as plt
except Exception:  # pragma: no cover
    plt = None
try:
    import mlflow
except Exception:  # pragma: no cover
    mlflow = None

MOVES = [
    "a2a3",
    "a2a4",
    "b2b3",
    "b2b4",
    "c2c3",
    "c2c4",
    "d2d3",
    "d2d4",
    "e2e3",
    "e2e4",
]


class DummyModel:
    """Fallback model producing uniform random moves."""

    def predict_proba(self, fen: str) -> dict[str, float]:  # pragma: no cover - simple
        p = 1.0 / len(MOVES)
        return {m: p for m in MOVES}


def load_dataset(path: str | None, limit: int) -> list[Tuple[str, str]]:
    """Load dataset from Parquet or generate dummy data."""
    data: list[Tuple[str, str]] = []
    if path and pd is not None:
        try:
            df = pd.read_parquet(path)
            fens: Iterable[str] = df["fen"].tolist()
            labels: Iterable[str] = df["label_uci"].tolist()
            data = list(zip(fens, labels))[:limit]
        except Exception:
            data = []
    if not data:
        random.seed(0)
        start_fen = "rn1qkbnr/pppbpppp/8/3p4/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 3"
        for _ in range(limit):
            data.append((start_fen, random.choice(MOVES)))
    return data


def load_model(model_id: str) -> DummyModel:
    """Load torch model if available otherwise return DummyModel."""
    try:  # pragma: no cover - optional
        import torch
    except Exception:  # pragma: no cover - torch missing
        return DummyModel()
    path = Path("artifacts/models") / model_id / "best.pt"
    if path.exists():
        try:
            model = torch.load(path, map_location="cpu")
            model.eval()
            return model  # type: ignore[return-value]
        except Exception:
            ERRORS_TOTAL.labels(type="model_load").inc()
    return DummyModel()


def run_evaluation(
    *,
    model_id: str,
    dataset: str | None,
    metrics: list[str] | None,
    batch_size: int,
    limit: int,
    seed: int,
    outdir: str | None,
    eval_id: str | None = None,
) -> tuple[dict[str, float], str]:
    """Execute evaluation and return metrics and outdir."""
    random.seed(seed)
    eval_id = eval_id or uuid.uuid4().hex
    start = time.time()

    data = load_dataset(dataset, limit)
    model = load_model(model_id)

    total_loss = 0.0
    top1 = 0
    top3 = 0
    bin_totals = [0] * 10
    bin_correct = [0] * 10

    for fen, label in data:
        probs = getattr(model, "predict_proba", DummyModel().predict_proba)(fen)
        p_true = probs.get(label, 1e-12)
        total_loss -= math.log(p_true)
        ordered = sorted(probs.items(), key=lambda kv: kv[1], reverse=True)
        predictions = [m for m, _ in ordered]
        if predictions and predictions[0] == label:
            top1 += 1
        if label in predictions[:3]:
            top3 += 1
        p_max = ordered[0][1] if ordered else 1.0
        idx = min(int(p_max * 10), 9)
        bin_totals[idx] += 1
        if predictions and predictions[0] == label:
            bin_correct[idx] += 1

    n = max(len(data), 1)
    val_loss = total_loss / n
    val_acc_top1 = top1 / n
    val_acc_top3 = top3 / n

    ece = 0.0
    for i in range(10):
        if bin_totals[i]:
            acc = bin_correct[i] / bin_totals[i]
            conf = (i + 0.5) / 10
            ece += abs(acc - conf) * bin_totals[i] / n

    metrics_dict = {
        "val_loss": val_loss,
        "val_acc_top1": val_acc_top1,
        "val_acc_top3": val_acc_top3,
        "ece": ece,
    }

    out_path = Path(outdir or f"artifacts/eval/{eval_id}")
    out_path.mkdir(parents=True, exist_ok=True)
    (out_path / "metrics.json").write_text(json.dumps(metrics_dict, indent=2))
    report = {
        "evalId": eval_id,
        "modelId": model_id,
        "dataset": dataset,
        "startedAt": start,
        "finishedAt": time.time(),
        "metrics": metrics_dict,
    }
    (out_path / "report.json").write_text(json.dumps(report, indent=2))

    if plt is not None:
        try:
            fig, ax = plt.subplots()
            confs = [(i + 0.5) / 10 for i in range(10)]
            accs = [bin_correct[i] / bin_totals[i] if bin_totals[i] else 0 for i in range(10)]
            ax.plot(confs, accs, marker="o")
            ax.plot([0, 1], [0, 1], linestyle="--")
            ax.set_xlabel("Confidence")
            ax.set_ylabel("Accuracy")
            fig.savefig(out_path / "calibration.png")
            plt.close(fig)

            fig, ax = plt.subplots()
            ax.bar(["top1", "top3"], [val_acc_top1, val_acc_top3])
            fig.savefig(out_path / "topk.png")
            plt.close(fig)
        except Exception:
            (out_path / "calibration.png").write_text("plot error")
            (out_path / "topk.png").write_text("plot error")
    else:  # pragma: no cover - env without matplotlib
        (out_path / "calibration.png").write_text("plot unavailable")
        (out_path / "topk.png").write_text("plot unavailable")

    if mlflow is not None and os.getenv("MLFLOW_TRACKING_URI"):
        try:  # pragma: no cover - external service
            with mlflow.start_run(run_name=f"offline-eval:{eval_id}"):
                mlflow.log_params(
                    {
                        "model_id": model_id,
                        "dataset": dataset,
                        "batch_size": batch_size,
                        "limit": limit,
                    }
                )
                mlflow.log_metrics(metrics_dict)
                mlflow.log_artifacts(str(out_path))
        except Exception:
            ERRORS_TOTAL.labels(type="mlflow").inc()

    runtime = time.time() - start
    RUNTIME_SECONDS.observe(runtime)
    LAST_VAL_ACC_TOP1.set(val_acc_top1)
    LAST_VAL_LOSS.set(val_loss)
    LAST_ECE.set(ece)
    return metrics_dict, str(out_path)
