import argparse, getpass, json, os, random, time
from pathlib import Path

import chess

from .config import SelfPlayConfig
from ..metrics_registry import (
    chs_selfplay_runs_active, chs_selfplay_games_total,
    chs_selfplay_moves_total, chs_selfplay_win_rate,
    chs_selfplay_dataset_rows_total, chs_selfplay_failures_total,
    labelset,
)
from ..logging_utils import setup_json_logging, log_event


def selfplay_loop(cfg: SelfPlayConfig):
    rng = random.Random(cfg.seed)
    username = os.environ.get("CHS_USERNAME", getpass.getuser())
    labels = labelset(cfg.run_id, cfg.policy, username)
    out_base = Path(cfg.out_dir) / cfg.run_id
    out_base.mkdir(parents=True, exist_ok=True)
    games_fp = (out_base / "games.jsonl").open("a", encoding="utf-8")
    samples_fp = (out_base / "samples.jsonl").open("a", encoding="utf-8")
    parquet_rows = []
    white_wins = total_finished = 0
    chs_selfplay_runs_active.labels(**labels).inc()
    log_event("selfplay.started", **labels, games=cfg.games, max_moves=cfg.max_moves)
    t0 = time.time()
    try:
        for g in range(cfg.games):
            game_id = f"{cfg.run_id}-{g:06d}"
            board = chess.Board()
            moves_uci = []
            for m in range(cfg.max_moves):
                if board.is_game_over():
                    break
                move = rng.choice(list(board.legal_moves))
                moves_uci.append(move.uci())
                samples_row = {
                    "run_id": cfg.run_id,
                    "game_id": game_id,
                    "ply": board.fullmove_number * 2 - (0 if board.turn else 1),
                    "fen": board.fen(),
                    "uci": move.uci(),
                    "side_to_move": "w" if board.turn else "b",
                }
                samples_fp.write(json.dumps(samples_row, ensure_ascii=False) + "\n")
                parquet_rows.append(samples_row)
                chs_selfplay_moves_total.labels(**labels).inc()
                board.push(move)
            result = "*"
            if board.is_game_over():
                outcome = board.outcome()
                if outcome.winner is True:
                    result = "1-0"
                    white_wins += 1
                elif outcome.winner is False:
                    result = "0-1"
                else:
                    result = "1/2-1/2"
                total_finished += 1
            games_fp.write(json.dumps({
                "run_id": cfg.run_id,
                "game_id": game_id,
                "policy": cfg.policy,
                "result": result,
                "moves": moves_uci,
                "max_moves": cfg.max_moves,
                "seed": cfg.seed,
            }, ensure_ascii=False) + "\n")
            chs_selfplay_games_total.labels(**labels).inc()
            if total_finished > 0:
                chs_selfplay_win_rate.labels(**labels).set(white_wins / total_finished)
        if parquet_rows:
            import pyarrow as pa, pyarrow.parquet as pq
            table = pa.Table.from_pylist(parquet_rows)
            pq.write_table(table, out_base / "samples.parquet")
            chs_selfplay_dataset_rows_total.labels(**labels).inc(len(parquet_rows))
        log_event("selfplay.completed", **labels, duration_s=round(time.time() - t0, 2))
    except Exception as e:
        chs_selfplay_failures_total.labels(**labels).inc()
        log_event("selfplay.failed", **labels, error=str(e))
        raise
    finally:
        games_fp.close()
        samples_fp.close()
        chs_selfplay_runs_active.labels(**labels).dec()


def main():
    setup_json_logging()
    p = argparse.ArgumentParser()
    p.add_argument("--run-id", required=True)
    p.add_argument("--policy", default="random", choices=["random"])
    p.add_argument("--games", type=int, default=100)
    p.add_argument("--max-moves", type=int, default=200)
    p.add_argument("--seed", type=int, default=42)
    p.add_argument("--out", default="data/selfplay")
    a = p.parse_args()
    cfg = SelfPlayConfig(
        run_id=a.run_id, policy=a.policy, games=a.games,
        max_moves=a.max_moves, seed=a.seed, out_dir=Path(a.out)
    )
    selfplay_loop(cfg)


if __name__ == "__main__":
    main()
