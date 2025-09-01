"""CLI tool for preparing chess move training data.

This script loads M2 parquet data with columns `fen` and `uci`, optionally
`split`, applies a character level encoding to FEN strings, builds a move
vocabulary, and prepares train/validation splits. It intentionally does not
include model training logic yet.

Refer to the broader project documentation for architectural context:
- docs/PROJECT_OVERVIEW.md
- docs/STATE.md
"""

from __future__ import annotations

import argparse
import logging
import os
import random
from dataclasses import dataclass
from typing import Dict, Iterable, Tuple

import numpy as np
import pandas as pd
import torch
from torch import nn
from torch.utils.data import Dataset


LOGGER = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Reproducibility
# ---------------------------------------------------------------------------

def set_seed(seed: int) -> None:
    """Set seeds for `random`, `numpy`, and `torch`.

    Parameters
    ----------
    seed: int
        The seed value to use for RNGs.
    """
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    torch.cuda.manual_seed_all(seed)
    torch.backends.cudnn.deterministic = True
    torch.backends.cudnn.benchmark = False
    LOGGER.debug("Seed set to %s", seed)


# ---------------------------------------------------------------------------
# FEN Encoder
# ---------------------------------------------------------------------------

# Public vocabulary for FEN characters (PAD=0)
FEN_VOCAB: Dict[str, int] = {
    ch: idx + 1
    for idx, ch in enumerate(sorted(set("prnbqkPRNBQK12345678/ wKQkq-0123456789")))
}


class FENEncoder:
    """Encode FEN strings at character level with fixed padding."""

    PAD = 0

    def __init__(self, max_len: int) -> None:
        self.max_len = max_len
        self.stoi: Dict[str, int] = dict(FEN_VOCAB)
        self.itos: Dict[int, str] = {idx: ch for ch, idx in self.stoi.items()}
        LOGGER.debug("FENEncoder vocabulary size %d", len(self.stoi) + 1)

    def encode(self, fen: str) -> torch.Tensor:
        ids = [self.stoi.get(ch, self.PAD) for ch in fen]
        if len(ids) > self.max_len:
            ids = ids[: self.max_len]
        else:
            ids.extend([self.PAD] * (self.max_len - len(ids)))
        return torch.tensor(ids, dtype=torch.long)


# ---------------------------------------------------------------------------
# Dataset and vocabulary builder
# ---------------------------------------------------------------------------

def build_move_vocab(moves: Iterable[str]) -> Dict[str, int]:
    """Build a move vocabulary mapping moves to indices."""
    vocab = {move: idx for idx, move in enumerate(sorted(set(moves)))}
    LOGGER.debug("Move vocab size %d", len(vocab))
    return vocab


@dataclass
class ParquetMoves(Dataset):
    """PyTorch dataset reading encoded FENs and UCI moves from a DataFrame."""

    df: pd.DataFrame
    encoder: FENEncoder
    move_vocab: Dict[str, int]

    def __len__(self) -> int:  # pragma: no cover - simple length
        return len(self.df)

    def __getitem__(self, idx: int) -> Tuple[torch.Tensor, torch.Tensor]:
        row = self.df.iloc[idx]
        fen_tensor = self.encoder.encode(row["fen"])
        move_idx = torch.tensor(self.move_vocab[row["uci"]], dtype=torch.long)
        return fen_tensor, move_idx


# ---------------------------------------------------------------------------
# TinyPolicy model
# ---------------------------------------------------------------------------


class TinyPolicy(nn.Module):
    """Minimal policy network over FEN tokens.

    The architecture follows the sequence:
    Embedding → Conv1d ×2 → AdaptiveAvgPool1d → MLP head.
    """

    def __init__(
        self,
        vocab_size: int,
        emb_dim: int,
        num_classes: int,
        max_len: int = 90,
        p_drop: float = 0.1,
    ) -> None:
        super().__init__()
        self.max_len = max_len
        self.embed = nn.Embedding(vocab_size + 1, emb_dim, padding_idx=0)
        self.dropout = nn.Dropout(p_drop)
        self.conv = nn.Sequential(
            nn.Conv1d(emb_dim, emb_dim, kernel_size=3, padding=1),
            nn.ReLU(),
            nn.Conv1d(emb_dim, emb_dim, kernel_size=3, padding=1),
            nn.ReLU(),
        )
        self.pool = nn.AdaptiveAvgPool1d(1)
        self.head = nn.Sequential(
            nn.Linear(emb_dim, emb_dim),
            nn.ReLU(),
            nn.Dropout(p_drop),
            nn.Linear(emb_dim, num_classes),
        )

    def forward(self, x: torch.LongTensor) -> torch.Tensor:
        """Return logits for each move class.

        Parameters
        ----------
        x: torch.LongTensor
            Tensor of shape ``(B, L)`` with token IDs.
        """

        emb = self.embed(x)  # (B, L, E)
        emb = self.dropout(emb)
        emb = emb.transpose(1, 2)  # (B, E, L)
        features = self.conv(emb)
        pooled = self.pool(features).squeeze(-1)
        return self.head(pooled)

# ---------------------------------------------------------------------------
# Data loading
# ---------------------------------------------------------------------------

def load_parquet_splits(path: str, seed: int) -> Tuple[pd.DataFrame, pd.DataFrame]:
    """Load parquet data and return train/validation splits.

    If a ``split`` column exists, rows with value ``train`` (case-insensitive)
    form the training set while ``val``, ``valid``, ``validation`` or ``test``
    form the validation set. If no split column exists, an 80/20 random split is
    produced using the provided seed.
    """
    df = pd.read_parquet(path)
    if not {"fen", "uci"}.issubset(df.columns):
        missing = {"fen", "uci"} - set(df.columns)
        raise ValueError(f"Parquet file missing required columns: {missing}")

    if "split" in df.columns:
        split_series = df["split"].str.lower()
        train_df = df[split_series == "train"]
        val_df = df[split_series.isin(["val", "valid", "validation", "test"])]
    else:
        indices = np.arange(len(df))
        rng = np.random.default_rng(seed)
        rng.shuffle(indices)
        pivot = int(0.8 * len(indices))
        train_df = df.iloc[indices[:pivot]]
        val_df = df.iloc[indices[pivot:]]

    LOGGER.info("Loaded %d train and %d validation rows", len(train_df), len(val_df))
    return train_df.reset_index(drop=True), val_df.reset_index(drop=True)


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Train chess move model")
    parser.add_argument("--data", required=True, help="Path to M2 parquet file")
    parser.add_argument("--out-dir", required=True, help="Directory for outputs")
    parser.add_argument("--epochs", type=int, default=1, help="Number of epochs")
    parser.add_argument("--batch-size", type=int, default=32, help="Batch size")
    parser.add_argument("--lr", type=float, default=1e-3, help="Learning rate")
    parser.add_argument("--emb-dim", type=int, default=128, help="Embedding dimension")
    parser.add_argument("--dropout", type=float, default=0.1, help="Dropout rate")
    parser.add_argument("--max-len", type=int, default=90, help="FEN max length")
    parser.add_argument("--patience", type=int, default=5, help="Early stop patience")
    parser.add_argument("--seed", type=int, default=42, help="Random seed")
    parser.add_argument("--cpu", action="store_true", help="Force CPU even if GPU is available")
    parser.add_argument("--experiment", default="default", help="Experiment name")
    parser.add_argument("--run-name", default="run", help="Run name")
    parser.add_argument("--metrics-out", default="metrics.json", help="Where to write metrics")
    return parser.parse_args()


def main() -> None:  # pragma: no cover - CLI entry
    logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
    args = parse_args()
    LOGGER.info("Starting run %s/%s", args.experiment, args.run_name)

    os.makedirs(args.out_dir, exist_ok=True)
    set_seed(args.seed)

    train_df, val_df = load_parquet_splits(args.data, args.seed)
    encoder = FENEncoder(args.max_len)
    move_vocab = build_move_vocab(train_df["uci"])

    train_ds = ParquetMoves(train_df, encoder, move_vocab)
    val_ds = ParquetMoves(val_df, encoder, move_vocab)

    LOGGER.info("Datasets prepared: train=%d, val=%d", len(train_ds), len(val_ds))
    LOGGER.info("Move vocab size: %d", len(move_vocab))

    device = torch.device(
        "cuda" if torch.cuda.is_available() and not args.cpu else "cpu"
    )
    model = TinyPolicy(
        vocab_size=len(FEN_VOCAB),
        emb_dim=args.emb_dim,
        num_classes=len(move_vocab),
        max_len=args.max_len,
        p_drop=args.dropout,
    ).to(device)
    optimizer = torch.optim.AdamW(model.parameters(), lr=args.lr)
    criterion = nn.CrossEntropyLoss()

    LOGGER.info("Model %s initialised on %s", model.__class__.__name__, device)
    LOGGER.info("Optimizer and loss ready (training loop pending)")

    LOGGER.info(
        "Placeholder training loop not yet implemented. See docs for next steps."
    )

    # Future work: implement training loop and metric logging.


if __name__ == "__main__":
    main()
