import pandas as pd
import pytest
from pathlib import Path


@pytest.fixture(scope="session")
def mini_parquet(tmp_path_factory):
    """Create a tiny synthetic parquet dataset for training tests.

    The dataset contains an imbalanced class distribution across two moves.
    """
    tmp_dir = tmp_path_factory.mktemp("data")
    path = tmp_dir / "mini.parquet"
    rows = []
    for _ in range(8):
        rows.append({"fen": "r", "uci": "a1a2", "split": "train"})
    for _ in range(2):
        rows.append({"fen": "n", "uci": "b1b2", "split": "train"})
    for _ in range(2):
        rows.append({"fen": "r", "uci": "a1a2", "split": "val"})
    rows.append({"fen": "n", "uci": "b1b2", "split": "val"})
    df = pd.DataFrame(rows)
    df.to_parquet(path)
    return path
