import argparse
import json
import os
import uuid
from pathlib import Path

import pandas as pd
import chess.pgn
from prometheus_client import CollectorRegistry, Gauge, push_to_gateway


def maybe_pushgateway(count: int) -> None:
    url = os.getenv("PUSHGATEWAY_URL")
    if not url:
        return
    try:
        registry = CollectorRegistry()
        g = Gauge("chs_dataset_rows", "Number of dataset rows", ["split"], registry=registry)
        g.labels(split="raw").set(count)
        push_to_gateway(url, job="dataset_builder", registry=registry)
    except Exception:
        pass


def parse_games(in_path: Path):
    games_rows = []
    positions_rows = []
    with open(in_path, "r", encoding="utf-8") as pgn:
        while True:
            game = chess.pgn.read_game(pgn)
            if game is None:
                break
            game_id = str(uuid.uuid4())
            headers = game.headers
            games_rows.append({
                "id": game_id,
                "date": headers.get("Date"),
                "time_control": headers.get("TimeControl"),
                "eco": headers.get("ECO"),
                "result": headers.get("Result"),
                "white_elo": int(headers.get("WhiteElo", 0) or 0),
                "black_elo": int(headers.get("BlackElo", 0) or 0),
            })
            board = game.board()
            for ply, move in enumerate(game.mainline_moves(), start=1):
                fen_before = board.fen()
                san = board.san(move)
                uci = move.uci()
                board.push(move)
                fen_after = board.fen()
                side_to_move = 'w' if board.turn == chess.WHITE else 'b'
                positions_rows.append({
                    "game_id": game_id,
                    "ply": ply,
                    "fen": fen_after,
                    "fen_before": fen_before,
                    "uci": uci,
                    "san": san,
                    "side_to_move": side_to_move,
                })
    return games_rows, positions_rows


def main():
    parser = argparse.ArgumentParser(description="Convert PGN to Parquet")
    parser.add_argument("--in", dest="inp", required=True, help="Input PGN file")
    parser.add_argument("--out", dest="out", default="out", help="Output directory")
    args = parser.parse_args()

    in_path = Path(args.inp).expanduser().resolve()
    out_dir = Path(args.out).expanduser().resolve()
    parquet_dir = out_dir / "parquet"
    parquet_dir.mkdir(parents=True, exist_ok=True)

    games_rows, positions_rows = parse_games(in_path)

    games_df = pd.DataFrame(games_rows)
    positions_df = pd.DataFrame(positions_rows)

    games_df.to_parquet(parquet_dir / "games.parquet", index=False)
    positions_df.to_parquet(parquet_dir / "positions.parquet", index=False)

    print(json.dumps({
        "event": "pgn_parsed",
        "games": len(games_df),
        "positions": len(positions_df),
        "out": str(out_dir),
    }))

    maybe_pushgateway(len(positions_df))


if __name__ == "__main__":
    main()
