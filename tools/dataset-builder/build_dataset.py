import argparse
import json
from pathlib import Path

import numpy as np
import pandas as pd


def main():
    parser = argparse.ArgumentParser(description="Join, filter and split dataset")
    parser.add_argument("--games", required=True, help="Path to games.parquet")
    parser.add_argument("--positions", required=True, help="Path to positions.parquet")
    parser.add_argument("--name", required=True, help="Dataset name")
    parser.add_argument("--out", default="out", help="Output directory")
    parser.add_argument("--min-elo", type=int)
    parser.add_argument("--max-elo", type=int)
    parser.add_argument("--since")
    parser.add_argument("--time-control")
    parser.add_argument("--eco")
    parser.add_argument("--color", choices=["white", "black", "both"], default="both")
    parser.add_argument("--train", type=float, default=0.8)
    parser.add_argument("--val", type=float, default=0.1)
    parser.add_argument("--test", type=float, default=0.1)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    games_path = Path(args.games).expanduser().resolve()
    positions_path = Path(args.positions).expanduser().resolve()

    games_df = pd.read_parquet(games_path)
    positions_df = pd.read_parquet(positions_path)
    df = positions_df.merge(games_df, left_on="game_id", right_on="id", how="inner")
    before = len(df)

    if args.min_elo is not None:
        df = df[(df["white_elo"] >= args.min_elo) & (df["black_elo"] >= args.min_elo)]
    if args.max_elo is not None:
        df = df[(df["white_elo"] <= args.max_elo) & (df["black_elo"] <= args.max_elo)]
    if args.since:
        dates = pd.to_datetime(df["date"], errors="coerce")
        df = df[dates >= pd.to_datetime(args.since)]
    if args.time_control:
        df = df[df["time_control"].fillna("").str.contains(args.time_control)]
    if args.eco:
        ecos = [e.strip() for e in args.eco.split(",") if e.strip()]
        df = df[df["eco"].isin(ecos)]
    if args.color == "white":
        df = df[df["side_to_move"] == "w"]
    elif args.color == "black":
        df = df[df["side_to_move"] == "b"]
    after = len(df)

    total_ratio = args.train + args.val + args.test
    if not np.isclose(total_ratio, 1.0):
        raise ValueError("train + val + test must sum to 1.0")

    rng = np.random.default_rng(args.seed)
    indices = rng.permutation(len(df))
    train_end = int(len(df) * args.train)
    val_end = train_end + int(len(df) * args.val)
    train_df = df.iloc[indices[:train_end]]
    val_df = df.iloc[indices[train_end:val_end]]
    test_df = df.iloc[indices[val_end:]]

    out_dir = Path(args.out).expanduser().resolve()
    parquet_dir = out_dir / "parquet"
    parquet_dir.mkdir(parents=True, exist_ok=True)
    train_df.to_parquet(parquet_dir / "train.parquet", index=False)
    val_df.to_parquet(parquet_dir / "val.parquet", index=False)
    test_df.to_parquet(parquet_dir / "test.parquet", index=False)

    print(json.dumps({
        "event": "dataset_filtered",
        "before": before,
        "after": after,
        "filters": {
            "min_elo": args.min_elo,
            "max_elo": args.max_elo,
            "since": args.since,
            "time_control": args.time_control,
            "eco": args.eco,
            "color": args.color,
        },
        "splits": {
            "train": len(train_df),
            "val": len(val_df),
            "test": len(test_df),
        },
    }))


if __name__ == "__main__":
    main()
