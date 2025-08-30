from dataclasses import dataclass
from pathlib import Path


@dataclass
class SelfPlayConfig:
    run_id: str
    policy: str = "random"
    games: int = 100
    max_moves: int = 200
    seed: int = 42
    out_dir: Path = Path("data/selfplay")
