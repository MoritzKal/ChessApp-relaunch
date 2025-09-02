from __future__ import annotations
from dataclasses import dataclass
from typing import List, Tuple
import random
import chess

MODEL_ID = "dummy-policy"
MODEL_VERSION = "0.1.0"

@dataclass
class DummyModel:
    model_id: str = MODEL_ID
    model_version: str = MODEL_VERSION

    def predict(self, fen: str, temperature: float | None = 1.0, topk: int | None = None) -> Tuple[str, List[tuple[str, float]]]:
        """Return (best_move, policy) where policy is list[(uci, prob)]."""
        board = chess.Board(fen)
        legal = [m.uci() for m in board.legal_moves]
        if not legal:
            raise ValueError("No legal moves for given FEN.")
        # Uniform 'policy' as a placeholder; deterministic but shuffled slightly to avoid bias.
        probs = 1.0 / len(legal)
        policy = [(mv, probs) for mv in legal]
        # Choose move: either top-1 from sorted list or a stable pseudo-random pick
        # Keep deterministic: pick the lexicographically first move.
        best_move = sorted(legal)[0]
        if topk is not None and topk > 0:
            topk = min(topk, len(policy))
            policy = sorted(policy, key=lambda x: (-x[1], x[0]))[:topk]
        return best_move, policy

def load_model() -> DummyModel:
    # Hook for loading a real model artifact from M3 in the future.
    return DummyModel()
