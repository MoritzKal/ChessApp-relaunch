"""Utilities for persisting self-play artifacts."""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

BASE_DIR = Path("artifacts/selfplay")


def save_report(run_id: str, report: dict[str, Any]) -> str:
    """Persist the report for a run and return its path."""
    run_dir = BASE_DIR / run_id
    run_dir.mkdir(parents=True, exist_ok=True)
    path = run_dir / "report.json"
    with path.open("w", encoding="utf-8") as fh:
        json.dump(report, fh)
    return str(path)
