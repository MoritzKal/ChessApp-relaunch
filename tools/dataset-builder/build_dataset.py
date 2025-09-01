import argparse
import json
import os
from datetime import datetime
from pathlib import Path

import numpy as np
import pandas as pd

from typing import Dict, Optional


def push_metrics(rows: Dict[str, int], name: str) -> None:
    url = os.getenv("PUSHGATEWAY_URL")
    if not url:
        print(json.dumps({"event": "upload_skipped", "target": "pushgateway"}))
        return
    try:
        print(json.dumps({"event": "upload_started", "target": "pushgateway", "url": url}))
        from prometheus_client import CollectorRegistry, Gauge, push_to_gateway

        registry = CollectorRegistry()
        gauge = Gauge("chs_dataset_rows", "Number of dataset rows", ["split"], registry=registry)
        for split, count in rows.items():
            gauge.labels(split=split).set(count)
        push_to_gateway(url, job=f"dataset_builder_{name}", registry=registry)
        print(json.dumps({"event": "upload_done", "target": "pushgateway", "url": url}))
    except Exception:
        print(json.dumps({"event": "upload_skipped", "target": "pushgateway", "url": url}))


def upload_to_s3(out_dir: Path, name: str, version: str) -> Optional[str]:
    endpoint = os.getenv("MINIO_ENDPOINT")
    access = os.getenv("MINIO_ACCESS_KEY")
    secret = os.getenv("MINIO_SECRET_KEY")
    bucket = os.getenv("MINIO_BUCKET", "chess-datasets")
    secure = os.getenv("MINIO_SECURE", "0") == "1"
    if not endpoint or not access or not secret:
        print(json.dumps({"event": "upload_skipped", "target": "s3"}))
        return None
    try:
        print(json.dumps({"event": "upload_started", "target": "s3", "bucket": bucket}))
        import boto3
        from botocore.client import Config

        scheme = "https" if secure else "http"
        client = boto3.client(
            "s3",
            endpoint_url=f"{scheme}://{endpoint}",
            aws_access_key_id=access,
            aws_secret_access_key=secret,
            config=Config(signature_version="s3v4"),
        )
        prefix = f"datasets/{name}/{version}"
        for file in out_dir.rglob("*"):
            if file.is_file():
                key = f"{prefix}/{file.relative_to(out_dir).as_posix()}"
                client.upload_file(str(file), bucket, key)
        print(json.dumps({"event": "upload_done", "target": "s3", "bucket": bucket}))
        return f"s3://{bucket}/{prefix}/"
    except Exception:
        print(json.dumps({"event": "upload_skipped", "target": "s3"}))
        return None


def main(argv=None):
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
    args = parser.parse_args(argv)

    start_time = datetime.utcnow()

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

    print(
        json.dumps(
            {
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
            }
        )
    )

    filters_applied = {}
    if args.min_elo is not None:
        filters_applied["min_elo"] = args.min_elo
    if args.max_elo is not None:
        filters_applied["max_elo"] = args.max_elo
    if args.since:
        filters_applied["since"] = args.since
    if args.time_control:
        filters_applied["time_control"] = args.time_control
    if args.eco:
        filters_applied["eco"] = args.eco
    if args.color != "both":
        filters_applied["color"] = args.color

    manifest_dir = out_dir / "manifest"
    manifest_dir.mkdir(parents=True, exist_ok=True)
    now = datetime.utcnow()
    manifest = {
        "name": args.name,
        "version": now.strftime("%Y%m%dT%H%M%SZ"),
        "filters": filters_applied,
        "splits": {
            "train": {"fraction": args.train, "rows": len(train_df)},
            "val": {"fraction": args.val, "rows": len(val_df)},
            "test": {"fraction": args.test, "rows": len(test_df)},
        },
        "source": {"games": str(games_path), "positions": str(positions_path)},
        "created_at": now.isoformat() + "Z",
    }

    stats_dir = out_dir / "stats"
    stats_dir.mkdir(parents=True, exist_ok=True)
    with open(stats_dir / "rows.json", "w") as f:
        json.dump(
            {
                "games": len(games_df),
                "positions": len(positions_df),
                "train": len(train_df),
                "val": len(val_df),
                "test": len(test_df),
            },
            f,
        )
    eco_counts = df["eco"].value_counts()
    with open(stats_dir / "eco.json", "w") as f:
        json.dump(eco_counts.to_dict(), f)
    ply_counts = df["ply"].value_counts().sort_index()
    with open(stats_dir / "ply.json", "w") as f:
        json.dump({int(k): int(v) for k, v in ply_counts.to_dict().items()}, f)

    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    plt.figure()
    if not eco_counts.empty:
        eco_counts.head(20).plot(kind="bar")
    plt.tight_layout()
    plt.savefig(stats_dir / "eco.png")
    plt.close()

    plt.figure()
    plt.plot(ply_counts.index, ply_counts.values)
    plt.tight_layout()
    plt.savefig(stats_dir / "ply.png")
    plt.close()

    print(
        json.dumps(
            {
                "event": "stats_written",
                "eco_top": int(min(len(eco_counts), 20)),
                "ply_bins": int(len(ply_counts)),
                "out": str(stats_dir.resolve()),
            }
        )
    )

    rows_counts = {"train": len(train_df), "val": len(val_df), "test": len(test_df)}
    duration = (datetime.utcnow() - start_time).total_seconds()
    print(
        json.dumps(
            {
                "event": "dataset_built",
                "rows": rows_counts,
                "duration": duration,
                "out": str(out_dir.resolve()),
            }
        )
    )

    push_metrics(rows_counts, args.name)
    artifact_uri = upload_to_s3(out_dir, args.name, manifest["version"])
    if artifact_uri:
        manifest["artifact_uri"] = artifact_uri

    with open(manifest_dir / "dataset.json", "w") as f:
        json.dump(manifest, f)


if __name__ == "__main__":
    main()
