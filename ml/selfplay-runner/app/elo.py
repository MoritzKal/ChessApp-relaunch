"""Helpers for Elo calculations."""
from __future__ import annotations

import math


def win_rate_to_elo(p: float) -> float:
    """Convert win-rate (0<p<1) to Elo difference with clamping."""
    if p <= 0 or p >= 1:
        return 0.0
    elo = -400 * math.log10(1 / p - 1)
    return max(min(elo, 800.0), -800.0)
