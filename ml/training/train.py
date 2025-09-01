from __future__ import annotations

import argparse
from typing import Dict, Iterable

try:
    import pandas as pd
except Exception:  # pragma: no cover - defensive
    pd = None

try:
    import torch
    from torch.utils.data import Dataset
except Exception:  # pragma: no cover - torch may be absent in CI
    torch = None
    Dataset = object  # type: ignore


def build_move_vocab(moves: Iterable[str]) -> Dict[str, int]:
    """Build a vocabulary for UCI moves.

    An explicit ``<unk>`` token is reserved at index ``0`` so that the
    dataset can gracefully handle moves that were not observed in the
    training split.
    """
    vocab: Dict[str, int] = {"<unk>": 0}
    for move in moves:
        if move not in vocab:
            vocab[move] = len(vocab)
    return vocab


class ParquetMoves(Dataset):
    """Dataset that yields move indices from a Parquet file."""

    def __init__(self, path: str, move_vocab: Dict[str, int]):
        if pd is None:  # pragma: no cover - pandas may be missing
            raise ImportError("pandas is required to read Parquet files")
        self.df = pd.read_parquet(path)
        self.move_vocab = move_vocab

    def __len__(self) -> int:  # pragma: no cover - trivial
        return len(self.df)

    def __getitem__(self, idx: int) -> int:
        row = self.df.iloc[idx]
        # Use a dedicated ``<unk>`` index for unseen moves so that the
        # validation split may contain moves absent from the training
        # vocabulary without raising ``KeyError``.
        return self.move_vocab.get(row["uci"], self.move_vocab["<unk>"])


def parse_args() -> argparse.Namespace:  # pragma: no cover - simple CLI
    parser = argparse.ArgumentParser(description="Baseline training CLI")
    parser.add_argument("--data", required=True, help="Path to Parquet file")
    return parser.parse_args()


def main() -> None:  # pragma: no cover - CLI wiring
    args = parse_args()
    if pd is None:
        raise SystemExit("pandas is required to run this script")

    df = pd.read_parquet(args.data)
    vocab = build_move_vocab(df["uci"])
    dataset = ParquetMoves(args.data, vocab)
    print(len(dataset))


if __name__ == "__main__":  # pragma: no cover
    main()
